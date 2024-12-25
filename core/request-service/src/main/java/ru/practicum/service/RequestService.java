package ru.practicum.service;

import ru.practicum.dto.request.EventCountByRequest;
import ru.practicum.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.dto.request.RequestParamsUpdate;

import java.util.List;
import java.util.Set;

public interface RequestService {

    List<ParticipationRequestDto> getAll(long userId);

    ParticipationRequestDto create(long userId, long eventId);

    ParticipationRequestDto cancel(long userId, long requestId);

    List<ParticipationRequestDto> findRequestsOnUserEvent(long userId, long eventId);

    EventRequestStatusUpdateResult updateStatus(RequestParamsUpdate params);

    List<EventCountByRequest> getEventIdAndCountRequest(Set<Long> eventIds);

    Integer countConfirmedRequest(Long eventId);

}