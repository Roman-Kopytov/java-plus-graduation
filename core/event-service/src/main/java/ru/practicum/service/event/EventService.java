package ru.practicum.service.event;

import ru.practicum.dto.event.*;
import ru.practicum.model.Event;

import java.util.Collection;
import java.util.List;

public interface EventService {

    //    Приватные пользователи
    List<EventShortDto> getAll(PrivateEventParams params);

    EventFullDto create(long userId, NewEventDto newEventDto);

    EventFullDto getById(long eventId);

    EventFullDto update(long userId, long eventId, UpdateEventUserRequest updateEventUserRequest);

    List<EventShortDto> getAll(PublicEventRequestParams params);

    List<EventFullDto> getAll(AdminEventRequestParams params);

    EventFullDto getById(long userId, long eventId);

    EventFullDto update(long eventId, UpdateEventAdminRequest event);

    Collection<Event> getByIds(List<Long> events);

    EventRequestDto getByIdForRequest(long eventId);

    EventRequestDto getByIdAndInitiatorId(long eventId, long userId);


}
