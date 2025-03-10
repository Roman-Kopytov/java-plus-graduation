package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.client.actions.UserActionClient;
import ru.practicum.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.dto.request.RequestParamsUpdate;
import ru.practicum.grpc.stats.action.ActionTypeProto;
import ru.practicum.service.RequestService;

import java.time.Instant;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users/{userId}")
@Validated
public class PrivateRequestController {
    private final RequestService requestService;
    private final UserActionClient userActionController;

    @GetMapping("/requests")
    public List<ParticipationRequestDto> getAll(@PathVariable("userId") long userId) {
        return requestService.getAll(userId);
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto create(@PathVariable("userId") long userId,
                                          @RequestParam(value = "eventId") int eventId) {
        ParticipationRequestDto participationRequestDto = requestService.create(userId, eventId);
        userActionController.sendUserAction(eventId, userId, ActionTypeProto.ACTION_REGISTER, Instant.now());
        return participationRequestDto;
    }

    @PatchMapping("/requests/{requestId}/cancel")
    public ParticipationRequestDto cancel(@PathVariable("userId") long userId,
                                          @PathVariable("requestId") long requestId) {
        return requestService.cancel(userId, requestId);
    }

    @GetMapping("/events/{eventId}/requests")
    public List<ParticipationRequestDto> getRequestsOnUserEvent(@PathVariable("userId") long userId,
                                                                @PathVariable("eventId") long eventId) {
        return requestService.findRequestsOnUserEvent(userId, eventId);
    }

    @PatchMapping("/events/{eventId}/requests")
    public EventRequestStatusUpdateResult updateStatusRequestByUserAndEventId(@PathVariable("userId") long userId,
                                                                              @PathVariable("eventId") long eventId,
                                                                              @RequestBody EventRequestStatusUpdateRequest updateDto) {
        return requestService.updateStatus(new RequestParamsUpdate(userId, eventId, updateDto));
    }
}
