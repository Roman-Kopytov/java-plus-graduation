package ru.practicum.service.compilation;

import com.querydsl.core.types.dsl.BooleanExpression;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.StatClient;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.compilation.NewCompilationDto;
import ru.practicum.dto.compilation.PublicCompilationParams;
import ru.practicum.dto.compilation.UpdateCompilationRequest;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.request.EventCountByRequest;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.exeption.NotFoundException;
import ru.practicum.feignclient.RatingClient;
import ru.practicum.feignclient.RequestClient;
import ru.practicum.feignclient.UserClient;
import ru.practicum.mapper.CompilationMapper;
import ru.practicum.mapper.EventMapper;
import ru.practicum.model.Compilation;
import ru.practicum.model.Event;
import ru.practicum.model.QCompilation;
import ru.practicum.repository.CompilationRepository;
import ru.practicum.repository.EventRepository;
import ru.practicum.stat.StatsParams;
import ru.practicum.stat.ViewStatsDTO;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {
    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final StatClient statClient;
    private final EventMapper eventMapper;
    private final RequestClient requestClient;
    private final UserClient userClient;
    private final RatingClient ratingClient;


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

        List<String> uris = eventsIdWithViews.stream()
                .map(ev -> "/events/" + ev.getEventId())
                .toList();

        StatsParams statsParams = StatsParams.builder()
                .uris(uris)
                .unique(true)
                .start(LocalDateTime.now().minusYears(100))
                .end(LocalDateTime.now())
                .build();

        List<ViewStatsDTO> viewStatsDTOS = statClient.getStats(statsParams);
        Map<Long, UserShortDto> initiatorsByEventId = getInitiators(compEvents);
        return eventsIdWithViews.stream().map(ev -> {
            Event finalEvent = compEvents.stream()
                    .filter(e -> e.getId().equals(ev.getEventId()))
                    .findFirst().orElseThrow(() -> new IllegalStateException("Event not found: " + ev.getEventId()));

            int rating = getRating(finalEvent);

            long views = viewStatsDTOS.stream()
                    .filter(stat -> stat.getUri().equals("/events/" + ev.getEventId()))
                    .map(ViewStatsDTO::getHits)
                    .findFirst()
                    .orElse(0L);

            finalEvent.setConfirmedRequests((Integer) ev.getCount());
            UserShortDto userShortDto = initiatorsByEventId.get(ev.getEventId());
            return eventMapper.toEventShortDto(finalEvent, userShortDto, rating, views);
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

    private int getRating(Event event) {
        return ratingClient.countEventRating(event.getId());

    }
}
