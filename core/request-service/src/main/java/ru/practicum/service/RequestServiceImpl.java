package ru.practicum.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.event.EventRequestDto;
import ru.practicum.dto.event.EventState;
import ru.practicum.dto.request.*;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.exeption.ConflictException;
import ru.practicum.exeption.NotFoundException;
import ru.practicum.feignclient.EventClient;
import ru.practicum.feignclient.UserClient;
import ru.practicum.mapper.RequestMapper;
import ru.practicum.model.Request;
import ru.practicum.repository.RequestRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService {

    private final RequestRepository requestRepository;

    private final RequestMapper requestMapper;

    private final UserClient userClient;

    private final EventClient eventClient;

    @Override
    public List<ParticipationRequestDto> getAll(long userId) {
        UserShortDto userShortDto = getUser(userId);
        List<Request> requests = requestRepository.findByRequesterId(userShortDto.getId());
        return requests.stream()
                .map(requestMapper::toParticipationRequestDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ParticipationRequestDto create(long userId, long evenId) {
        UserShortDto requester = getUser(userId);
        EventRequestDto event = getEvent(evenId);

        if (event.getInitiatorId() == (userId)) {
            throw new ConflictException("The initiator of the event can't add a request to participate in his event");
        }
        if (!requestRepository.findByEventIdAndRequesterId(evenId, userId).isEmpty()) {
            throw new ConflictException("Repeatable request not allowed");
        }
        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("Event not published");
        }
        if (!(event.getParticipantLimit() == 0)) {
            checkEventRequestLimit(event);
        }
        Integer countConfirmedRequest = requestRepository.countRequests(evenId, RequestStatus.CONFIRMED);
        event.setConfirmedRequests(countConfirmedRequest);

        Request request = new Request();
        request.setRequesterId(requester.getId());
        request.setEventId(event.getId());
        request.setCreated(LocalDateTime.now());

        if (event.isRequestModeration() && event.getParticipantLimit() != 0) {
            request.setStatus(RequestStatus.PENDING);
        } else {
            request.setStatus(RequestStatus.CONFIRMED);
        }

        return requestMapper.toParticipationRequestDto(requestRepository.save(request));
    }

    @Transactional
    @Override
    public ParticipationRequestDto cancel(long userId, long requestId) {
        getUser(userId);
        Request request = getRequest(requestId);
        if (request.getRequesterId() != userId) {
            throw new ConflictException("User is not requester");
        }
        request.setStatus(RequestStatus.CANCELED);
        Request saved = requestRepository.save(request);
        return requestMapper.toParticipationRequestDto(saved);
    }

    @Override
    public List<ParticipationRequestDto> findRequestsOnUserEvent(long userId, long eventId) {
        UserShortDto user = getUser(userId);
        getUserEvent(eventId, user.getId());

        List<Request> allRequests = requestRepository.findByEventId(eventId);
        if (allRequests.isEmpty()) {
            return List.of();
        }
        return allRequests.stream().map(requestMapper::toParticipationRequestDto).toList();
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateStatus(RequestParamsUpdate params) {
        UserShortDto user = getUser(params.getUserId());
        EventRequestDto event = getUserEvent(params.getEventId(), user.getId());
        List<Long> requestIds = params.getDto().getRequestIds();
        List<ParticipationRequestDto> updatedRequests = new ArrayList<>();
        if (params.getDto().getStatus().equals(RequestStatus.REJECTED)) {
            for (Long requestId : requestIds) {
                Request request = getRequest(requestId);
                if (request.getStatus().equals(RequestStatus.PENDING)) {
                    request.setStatus(RequestStatus.REJECTED);
                } else {
                    throw new ConflictException("The request have status " + request.getStatus() + " not been rejected yet");
                }
                ParticipationRequestDto participationRequestDto = requestMapper.toParticipationRequestDto(requestRepository.save(request));
                updatedRequests.add(participationRequestDto);
            }
            return new EventRequestStatusUpdateResult(Collections.emptyList(), updatedRequests);
        } else {
            for (Long requestId : requestIds) {
                Request request = getRequest(requestId);
                checkEventRequestLimit(event);
                request.setStatus(RequestStatus.CONFIRMED);
                updatedRequests.add(requestMapper.toParticipationRequestDto(requestRepository.save(request)));
            }
            return new EventRequestStatusUpdateResult(updatedRequests, Collections.emptyList());
        }
    }

    @Override
    public List<EventCountByRequest> getEventIdAndCountRequest(List<Long> eventIds) {
        List<EventCountByRequest> eventIdAndCountRequest = requestRepository.getEventIdAndCountRequest(eventIds);
        Map<Long, Number> eventCountMap = eventIdAndCountRequest.stream()
                .collect(Collectors.toMap(EventCountByRequest::getEventId, EventCountByRequest::getCount));
        return eventIds.stream().map(id -> new EventCountByRequest(id, eventCountMap.getOrDefault(id, 0))).toList();
    }

    @Override
    public Integer countConfirmedRequest(Long eventId) {
        return requestRepository.countRequests(eventId, RequestStatus.CONFIRMED);
    }

    @Override
    public boolean isParticipant(long eventId, long userId) {
        return requestRepository.existsByRequesterIdAndEventIdAndStatus(userId, eventId, RequestStatus.CONFIRMED);
    }

    private void checkEventRequestLimit(EventRequestDto event) {
        if (requestRepository.isParticipantLimitReached(event.getId(), event.getParticipantLimit())) {
            throw new ConflictException("Request limit reached");
        }
    }


    private Request getRequest(long requestId) {
        return requestRepository.findById(requestId).orElseThrow(() -> new NotFoundException("Request not found with id" + requestId));
    }

    private UserShortDto getUser(long userId) {
        try {
            return userClient.getUserById(userId);
        } catch (FeignException e) {
            if (e.status() == 404) {
                log.warn("User not found");
            } else {
                log.warn("Feign error: " + e.status(), e);
            }
            throw e;
        }
    }

    private EventRequestDto getEvent(long eventId) {
        try {
            return eventClient.getEventById(eventId);
        } catch (FeignException e) {
            if (e.status() == 404) {
                log.warn("User not found");
            } else {
                log.warn("Feign error: " + e.status(), e);
            }
            throw e;
        }
    }

    private EventRequestDto getUserEvent(long eventId, long userId) {
        try {
            return eventClient.getEventByIdAndInitiator(eventId, userId);
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
}