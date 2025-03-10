package ru.practicum.service.compilation;

import com.querydsl.core.types.dsl.BooleanExpression;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.recommendation.RecommendationClient;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.compilation.NewCompilationDto;
import ru.practicum.dto.compilation.PublicCompilationParams;
import ru.practicum.dto.compilation.UpdateCompilationRequest;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.request.EventCountByRequest;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.exeption.NotFoundException;
import ru.practicum.feignclient.RequestClient;
import ru.practicum.feignclient.UserClient;
import ru.practicum.grpc.stats.event.RecommendedEventProto;
import ru.practicum.mapper.CompilationMapper;
import ru.practicum.mapper.EventMapper;
import ru.practicum.model.Compilation;
import ru.practicum.model.Event;
import ru.practicum.model.QCompilation;
import ru.practicum.repository.CompilationRepository;
import ru.practicum.repository.EventRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {
    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final RequestClient requestClient;
    private final UserClient userClient;
    private final RecommendationClient recommendationClient;


    private final CompilationMapper compilationMapper;

    @Override
    @Transactional
    public CompilationDto create(NewCompilationDto newCompilationDto) {
        Collection<Event> events = new ArrayList<>();
        if (newCompilationDto.getEvents() != null) {
            events = eventRepository.findByIdIn(newCompilationDto.getEvents());
        }
        Compilation compilation = compilationMapper.toCompilation(newCompilationDto, events);
        Compilation saved = compilationRepository.save(compilation);
        List<EventShortDto> list = getEventShortDtos(saved);
        return compilationMapper.toCompilationDto(saved, list);
    }

    private List<EventShortDto> getEventShortDtos(Compilation compilation) {
        List<Event> compEvents = compilation.getEvents();
        List<Long> eventIds = compEvents.stream()
                .map(Event::getId)
                .toList();

        List<EventCountByRequest> eventsIdWithViews = requestClient.getEventIdAndCountRequest(eventIds);

        List<RecommendedEventProto> eventsRating = getEventsRating(eventsIdWithViews);
        Map<Long, UserShortDto> initiatorsByEventId = getInitiators(compEvents);
        return eventsIdWithViews.stream().map(ev -> {
            Event finalEvent = compEvents.stream()
                    .filter(e -> e.getId().equals(ev.getEventId()))
                    .findFirst().orElseThrow(() -> new IllegalStateException("Event not found: " + ev.getEventId()));

            double rating = getRating(ev, eventsRating);
            finalEvent.setConfirmedRequests((Integer) ev.getCount());
            UserShortDto userShortDto = initiatorsByEventId.get(ev.getEventId());
            return eventMapper.toEventShortDto(finalEvent, userShortDto, rating);
        }).toList();
    }

    private Map<Long, UserShortDto> getInitiators(List<Event> events) {
        List<Long> initiatorsId = new ArrayList<>();
        for (Event event : events) {
            initiatorsId.add(event.getInitiatorId());
        }
        List<UserShortDto> allUsersByIds = userClient.getUsersByIds(initiatorsId);
        Map<Long, UserShortDto> collect = allUsersByIds.stream().collect(Collectors.toMap(UserShortDto::getId, dto -> dto));
        return events.stream()
                .collect(Collectors.toMap(
                        Event::getId,
                        event -> collect.get(event.getInitiatorId())
                ));
    }

    @Override
    @Transactional
    public void delete(long compId) {
        Compilation compilation = compilationRepository.findById(compId).orElseThrow(() -> new NotFoundException("Compilation with id= " + compId + " was not found"));

        compilationRepository.delete(compilation);
        compilationRepository.flush();
    }

    @Override
    @Transactional
    public CompilationDto update(long compId, UpdateCompilationRequest updateCompilationRequest) {
        Compilation compilation = compilationRepository.findById(compId).orElseThrow(() -> new NotFoundException("Compilation with id= " + compId + " was not found"));
        if (updateCompilationRequest.getEvents() != null) {
            compilation.setEvents(eventRepository.findByIdIn(updateCompilationRequest.getEvents()));
        }
        if (updateCompilationRequest.getTitle() != null) {
            compilation.setTitle(updateCompilationRequest.getTitle());
        }
        if (updateCompilationRequest.getPinned() != null) {
            compilation.setPinned(updateCompilationRequest.getPinned());
        }

        Compilation saved = compilationRepository.save(compilation);
        List<EventShortDto> list = getEventShortDtos(saved);

        return compilationMapper.toCompilationDto(saved, list);
    }

    @Override
    public List<CompilationDto> getAll(PublicCompilationParams params) {
        QCompilation compilation = QCompilation.compilation;
        List<BooleanExpression> conditions = new ArrayList<>();

        if (params.getPinned() != null) {
            conditions.add(compilation.pinned.eq(params.getPinned()));
        }

        BooleanExpression finalCondition = conditions.stream().reduce(BooleanExpression::and).orElse(compilation.isNotNull());

        PageRequest pageRequest = PageRequest.of(params.getFrom() / params.getSize(), params.getSize());

        assert finalCondition != null;
        Iterable<Compilation> compilationsIterable = compilationRepository.findAll(finalCondition, pageRequest);

        List<Compilation> compilations = StreamSupport.stream(compilationsIterable.spliterator(), false).toList();

        return compilations.stream().map(comp -> compilationMapper.toCompilationDto(comp, getEventShortDtos(comp))).toList();
    }

    @Override
    public CompilationDto getById(long compId) {
        Compilation compilation = compilationRepository.findById(compId).orElseThrow(() -> new NotFoundException("Compilation with id= " + compId + " was not found"));

        return compilationMapper.toCompilationDto(compilation, getEventShortDtos(compilation));
    }

    private double getEventRating(Event event) {
        Stream<RecommendedEventProto> interactionsCount = recommendationClient.getInteractionsCount(List.of(event.getId()));
        RecommendedEventProto recommendedEventProto = interactionsCount.findFirst().orElse(RecommendedEventProto.newBuilder().setEventId(event.getId()).setScore(0).build());

        return recommendedEventProto.getScore();
    }

    private List<RecommendedEventProto> getEventsRating(List<EventCountByRequest> eventsIdWithConfirmedRequest) {
        List<Long> collect = eventsIdWithConfirmedRequest.stream()
                .map(EventCountByRequest::getEventId)
                .collect(Collectors.toList());
        Stream<RecommendedEventProto> interactionsCount = recommendationClient.getInteractionsCount(collect);
        return interactionsCount.toList();
    }

    private double getRating(EventCountByRequest event, List<RecommendedEventProto> recommendedEventProtos) {
        return recommendedEventProtos.stream()
                .filter(ev -> ev.getEventId() == (event.getEventId()))
                .map(RecommendedEventProto::getScore)
                .findFirst()
                .orElse(0.0);
    }
}
