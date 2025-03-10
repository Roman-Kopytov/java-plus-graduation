package ru.practicum.service.event;

import com.querydsl.core.types.dsl.BooleanExpression;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.client.recommendation.RecommendationClient;
import ru.practicum.dto.event.*;
import ru.practicum.dto.request.EventCountByRequest;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.exeption.ConflictException;
import ru.practicum.exeption.NotFoundException;
import ru.practicum.feignclient.RequestClient;
import ru.practicum.feignclient.UserClient;
import ru.practicum.grpc.stats.event.RecommendedEventProto;
import ru.practicum.mapper.EventMapper;
import ru.practicum.mapper.LocationMapper;
import ru.practicum.model.Event;
import ru.practicum.model.Location;
import ru.practicum.model.QEvent;
import ru.practicum.repository.EventRepository;
import ru.practicum.repository.LocationRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ru.practicum.dto.event.Sort.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final RequestClient requestClient;
    private final RecommendationClient recommendationClient;
    private final UserClient userClient;
    private final CategoryRepository categoryRepository;
    private final LocationRepository locationRepository;
    private final LocationMapper locationMapper;

    @Override
    public List<EventShortDto> getAll(PublicEventRequestParams params) {
        QEvent event = QEvent.event;
        List<BooleanExpression> conditions = new ArrayList<>();

        int from = params.getFrom();
        int size = params.getSize();

        conditions.add(event.state.eq(EventState.PUBLISHED));

        conditions.add(event.eventDate.after(params.getRangeStart()));
        conditions.add(event.eventDate.before(params.getRangeEnd()));
        if (params.getText() != null) {
            conditions.add(event.description.containsIgnoreCase(params.getText()).or(event.annotation.containsIgnoreCase(params.getText())));
        }
        if (params.getCategories() != null && !params.getCategories().isEmpty()) {
            conditions.add(event.category.id.in(params.getCategories()));
        }
        if (params.getPaid() != null) {
            conditions.add(event.paid.eq(params.getPaid()));
        }
        BooleanExpression finalConditional = conditions.stream().reduce(BooleanExpression::and).get();

        PageRequest pageRequest = PageRequest.of(from > 0 ? from / size : 0, size);

        List<Event> events = eventRepository.findAll(finalConditional, pageRequest).getContent();
        if (events.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, Integer> eventLimitById = events.stream().collect(Collectors.toMap(Event::getId, Event::getParticipantLimit));
        List<EventCountByRequest> eventsIdWithConfirmedRequest =
                requestClient.getEventIdAndCountRequest(List.copyOf(eventLimitById.keySet()));

        if (params.getOnlyAvailable()) {
            eventsIdWithConfirmedRequest = eventsIdWithConfirmedRequest.stream()
                    .filter(ev -> (Integer) ev.getCount() >= eventLimitById.get(ev.getEventId()))
                    .toList();
        }

        List<RecommendedEventProto> eventsRating = getEventsRating(eventsIdWithConfirmedRequest);
        Map<Long, UserShortDto> initiatorsByEventId = getInitiators(events);

        List<EventShortDto> eventShortDtos = new ArrayList<>(eventsIdWithConfirmedRequest.stream()
                .map(ev -> {
                    Event finalEvent = getFinalEvent(ev, events);
                    double rating = getRating(ev, eventsRating);
                    UserShortDto userShortDto = initiatorsByEventId.get(ev.getEventId());
                    return eventMapper.toEventShortDto(finalEvent, userShortDto, rating);
                })
                .toList());

        if (params.getSort() != null) {
            if (params.getSort() == EVENT_DATE) {
                eventShortDtos.sort(Comparator.comparing(EventShortDto::getEventDate).reversed());
            } else if (params.getSort() == VIEWS) {
                eventShortDtos.sort(Comparator.comparing(EventShortDto::getRating).reversed());
            } else if (params.getSort() == TOP_RATING) {
                eventShortDtos.sort(Comparator.comparing(EventShortDto::getRating).reversed());
            }
        }

        return eventShortDtos;
    }


    @Override
    public EventFullDto getById(long eventId) {
        Event event = getEvent(eventId);
        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new NotFoundException("Event is not published");
        }

        Integer requests = requestClient.countConfirmedRequest(eventId);

        double rating = getEventRating(event);
        event.setConfirmedRequests(requests);
        UserShortDto initiator = userClient.getUserById(event.getInitiatorId());
        return eventMapper.toEventFullDto(event, initiator, rating);
    }

    @Override
    public List<EventFullDto> getAll(AdminEventRequestParams params) {
        QEvent event = QEvent.event;
        List<BooleanExpression> conditions = new ArrayList<>();

        int from = params.getFrom();
        int size = params.getSize();

        PageRequest pageRequest = PageRequest.of(from > 0 ? from / size : 0, size);

        conditions.add(event.eventDate.after(params.getRangeStart()));
        conditions.add(event.eventDate.before(params.getRangeEnd()));
        if (params.getUsers() != null && !params.getUsers().isEmpty()) {
            conditions.add(event.initiatorId.in(params.getUsers()));
        }
        if (params.getStates() != null && !params.getStates().isEmpty()) {
            conditions.add(event.state.in(params.getStates()));
        }
        if (params.getCategories() != null && !params.getCategories().isEmpty()) {
            conditions.add(event.category.id.in(params.getCategories()));
        }
        BooleanExpression finalConditional = conditions.stream().reduce(BooleanExpression::and).get();


        List<Event> events = eventRepository.findAll(finalConditional, pageRequest).getContent();
        if (events.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> eventIds = events.stream().map(Event::getId).toList();
        List<EventCountByRequest> eventsIdWithConfirmedRequest
                = requestClient.getEventIdAndCountRequest(eventIds);

        List<RecommendedEventProto> eventsRating = getEventsRating(eventsIdWithConfirmedRequest);
        Map<Long, UserShortDto> initiatorsByEventId = getInitiators(events);

        return eventsIdWithConfirmedRequest.stream()
                .map(ev -> {
                    Event finalEvent = getFinalEvent(ev, events);
                    double rating = getRating(ev, eventsRating);
                    UserShortDto userShortDto = initiatorsByEventId.get(ev.getEventId());
                    return eventMapper.toEventFullDto(finalEvent, userShortDto, rating);
                })
                .toList();
    }

    private Map<Long, UserShortDto> getInitiators(List<Event> events) {
        List<Long> initiatorsId = new ArrayList<>();
        for (Event event : events) {
            initiatorsId.add(event.getInitiatorId());
        }
        List<UserShortDto> allUsersByIds = userClient.getUsersByIds(initiatorsId);
        Map<Long, UserShortDto> dtoMap = allUsersByIds.stream().collect(Collectors.toMap(UserShortDto::getId, dto -> dto));
        return events.stream()
                .collect(Collectors.toMap(Event::getId,
                        event -> dtoMap.get(event.getInitiatorId())));
    }

    //    Приватные пользователи
    @Override
    public List<EventShortDto> getAll(PrivateEventParams params) {
        QEvent event = QEvent.event;
        List<BooleanExpression> conditions = new ArrayList<>();
        conditions.add(event.initiatorId.eq(params.getUserId()));
        BooleanExpression finalCondition = conditions.stream()
                .reduce(BooleanExpression::and)
                .orElse(null);

        PageRequest pageRequest = PageRequest.of(params.getFrom() / params.getSize(), params.getSize());

        List<Event> events = eventRepository.findAll(finalCondition, pageRequest).getContent();
        List<Long> eventIds = events.stream().map(Event::getId).toList();
        List<EventCountByRequest> eventsIdWithConfirmedRequest =
                requestClient.getEventIdAndCountRequest(eventIds);

        List<RecommendedEventProto> eventsRating = getEventsRating(eventsIdWithConfirmedRequest);

        Map<Long, UserShortDto> initiatorsByEventId = getInitiators(events);

        return eventsIdWithConfirmedRequest.stream()
                .map(ev -> {
                    Event finalEvent = getFinalEvent(ev, events);
                    double rating = getRating(ev, eventsRating);
                    UserShortDto userShortDto = initiatorsByEventId.get(ev.getEventId());
                    return eventMapper.toEventShortDto(finalEvent, userShortDto, rating);
                })
                .toList();
    }

    @Override
    @Transactional
    public EventFullDto create(long userId, NewEventDto newEventDto) {
        UserShortDto initiator = getUser(userId);
        if (newEventDto.getEventDate().minusHours(2).isBefore(LocalDateTime.now())) {
            throw new ConflictException("Different with now less than 2 hours");
        }
        Category category = getCategory(newEventDto.getCategory());
        Location location = locationRepository.save(locationMapper.toLocation(newEventDto.getLocation()));

        Event event = eventMapper.toEvent(newEventDto, category, location, EventState.PENDING,
                LocalDateTime.now());
        event.setInitiatorId(initiator.getId());
        event.setConfirmedRequests(0);
        Event saved = eventRepository.save(event);
        return eventMapper.toEventFullDto(saved, initiator, 0);
    }

    @Override
    public EventFullDto getById(long userId, long eventId) {
        UserShortDto userShortDto = getUser(userId);
        Event event = getEvent(eventId);
        if (!event.getInitiatorId().equals(userId)) {
            throw new ConflictException("The user is not the initiator of the event");
        }
        double rating = getEventRating(event);
        return eventMapper.toEventFullDto(event, userShortDto, rating);
    }

    @Override
    @Transactional
    public EventFullDto update(long userId, long eventId, UpdateEventUserRequest updateEventUserRequest) {
        Event event = getEvent(eventId);
        if (!event.getInitiatorId().equals(userId)) {
            throw new ConflictException("The user is not the initiator of the event");
        }
        if (event.getState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("You can't change an event that has already been published");
        }
        if (event.getEventDate().minusHours(2).isBefore(LocalDateTime.now())) {
            throw new ConflictException("Different with now less than 2 hours");
        }

        if (updateEventUserRequest.getAnnotation() != null) {
            event.setAnnotation(updateEventUserRequest.getAnnotation());
        }
        if (updateEventUserRequest.getCategory() != null) {
            Category category = getCategory(updateEventUserRequest.getCategory());
            event.setCategory(category);
        }
        if (updateEventUserRequest.getDescription() != null) {
            event.setDescription(updateEventUserRequest.getDescription());
        }
        if (updateEventUserRequest.getEventDate() != null) {
            event.setEventDate(updateEventUserRequest.getEventDate());
        }
        if (updateEventUserRequest.getLocation() != null) {
            Location location = locationRepository.save(locationMapper.toLocation(updateEventUserRequest.getLocation()));
            event.setLocation(location);
        }
        if (updateEventUserRequest.getPaid() != null) {
            event.setPaid(updateEventUserRequest.getPaid());
        }
        if (updateEventUserRequest.getParticipantLimit() != null) {
            event.setParticipantLimit(updateEventUserRequest.getParticipantLimit());
        }
        if (updateEventUserRequest.getRequestModeration() != null) {
            event.setRequestModeration(updateEventUserRequest.getRequestModeration());
        }
        if (updateEventUserRequest.getTitle() != null) {
            event.setTitle(updateEventUserRequest.getTitle());
        }
        if (updateEventUserRequest.getStateAction() != null) {
            if (updateEventUserRequest.getStateAction().equals(EventAction.SEND_TO_REVIEW)) {
                event.setState(EventState.PENDING);
            } else if (updateEventUserRequest.getStateAction().equals(EventAction.CANCEL_REVIEW)) {
                event.setState(EventState.CANCELED);
            }
        }
        Event saved = eventRepository.save(event);
        double rating = getEventRating(saved);
        UserShortDto user = getUser(event.getInitiatorId());
        return eventMapper.toEventFullDto(saved, user, rating);
    }

    @Override
    @Transactional
    public EventFullDto update(long eventId, UpdateEventAdminRequest eventDto) {
        Event savedEvent = getEvent(eventId);
        UserShortDto user = getUser(savedEvent.getInitiatorId());
        if (eventDto.getStateAction() != null) {
            if (eventDto.getStateAction().equals(EventAction.PUBLISH_EVENT) && !savedEvent.getState().equals(EventState.PENDING)) {
                throw new ConflictException("Event in state " + savedEvent.getState() + " can not be published");
            }
            if (eventDto.getStateAction().equals(EventAction.REJECT_EVENT) && savedEvent.getState().equals(EventState.PUBLISHED)) {
                throw new ConflictException("Event in state " + savedEvent.getState() + " can not be rejected");
            }
            if (eventDto.getStateAction().equals(EventAction.REJECT_EVENT)) {
                savedEvent.setState(EventState.CANCELED);
            }
        }

        if (eventDto.getEventDate() != null) {
            if (savedEvent.getState().equals(EventState.PUBLISHED) && savedEvent.getPublishedOn().plusHours(1).isAfter(eventDto.getEventDate())) {
                throw new ConflictException("Different with publishedOn less than 1 hours");
            }
            savedEvent.setEventDate(eventDto.getEventDate());
        }
        if (eventDto.getAnnotation() != null) {
            savedEvent.setAnnotation(eventDto.getAnnotation());
        }
        if (eventDto.getDescription() != null) {
            savedEvent.setDescription(eventDto.getDescription());
        }
        if (eventDto.getLocation() != null) {
            locationRepository.save(locationMapper.toLocation(eventDto.getLocation()));
        }
        if (eventDto.getCategory() != null) {
            Category category = getCategory(eventDto.getCategory());
            savedEvent.setCategory(category);
        }
        if (eventDto.getPaid() != null) {
            savedEvent.setPaid(eventDto.getPaid());
        }
        if (eventDto.getParticipantLimit() != null) {
            savedEvent.setParticipantLimit(eventDto.getParticipantLimit());
        }
        if (eventDto.getRequestModeration() != null) {
            savedEvent.setRequestModeration(eventDto.getRequestModeration());
        }
        if (eventDto.getTitle() != null) {
            savedEvent.setTitle(eventDto.getTitle());
        }
        if (eventDto.getStateAction() != null && eventDto.getStateAction().equals(EventAction.PUBLISH_EVENT)) {
            savedEvent.setState(EventState.PUBLISHED);
        }
        savedEvent.setPublishedOn(LocalDateTime.now());
        Integer requests = requestClient.countConfirmedRequest(eventId);
        savedEvent.setConfirmedRequests(requests);

        Event updated = eventRepository.save(savedEvent);

        double rating = getEventRating(savedEvent);
        return eventMapper.toEventFullDto(updated, user, rating);
    }

    @Override
    public List<Event> getByIds(List<Long> events) {
        return eventRepository.findAllById(events);
    }

    @Override
    public EventRequestDto getByIdForRequest(long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found with id" + eventId));
        return eventMapper.toEventRequestDto(event);

    }

    @Override
    public EventRequestDto getByIdAndInitiatorId(long eventId, long userId) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event not found with id" + eventId + "for user" + userId));
        return eventMapper.toEventRequestDto(event);
    }

    @Override
    public boolean checkEventVisitedByUser(long eventId, long userId) {
        return requestClient.isParticipant(eventId, userId)
                && getEvent(eventId).getEventDate().isBefore(LocalDateTime.now());
    }

    private Event getEvent(long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found with id: " + eventId));
    }

    private UserShortDto getUser(long userId) {
        try {
            return userClient.getUserById(userId);
        } catch (FeignException e) {
            if (e.status() == 404) {
                log.warn("User not found");
                throw e;
            } else {
                log.warn("Feign error: " + e.status(), e);
                throw e;
            }
        }
    }


    private Category getCategory(long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category with id= " + categoryId + " was not found"));
    }

    private double getRating(EventCountByRequest event, List<RecommendedEventProto> recommendedEventProtos) {
        return recommendedEventProtos.stream()
                .filter(ev -> ev.getEventId() == (event.getEventId()))
                .map(RecommendedEventProto::getScore)
                .findFirst()
                .orElse(0.0);
    }


    private static Event getFinalEvent(EventCountByRequest ev, List<Event> events) {
        Event event = events.stream()
                .filter(e -> e.getId().equals(ev.getEventId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Event not found: " + ev.getEventId()));
        event.setConfirmedRequests((Integer) ev.getCount());
        return event;
    }


    private double getEventRating(Event event) {
        Stream<RecommendedEventProto> interactionsCount = recommendationClient.getInteractionsCount(List.of(event.getId()));
        RecommendedEventProto recommendedEventProto = interactionsCount.findFirst().orElse(RecommendedEventProto.newBuilder()
                .setEventId(event.getId())
                .setScore(0)
                .build());

        return recommendedEventProto.getScore();
    }

    private List<RecommendedEventProto> getEventsRating(List<EventCountByRequest> eventsIdWithConfirmedRequest) {
        List<Long> collect = eventsIdWithConfirmedRequest.stream()
                .map(EventCountByRequest::getEventId)
                .collect(Collectors.toList());
        Stream<RecommendedEventProto> interactionsCount = recommendationClient.getInteractionsCount(collect);
        return interactionsCount.toList();
    }
}
