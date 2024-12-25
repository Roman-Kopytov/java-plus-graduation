package ru.practicum.controller.event;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.dto.event.EventRequestDto;
import ru.practicum.feignclient.EventClient;
import ru.practicum.service.event.EventService;

@RestController
@RequestMapping("/feign/events")
@RequiredArgsConstructor
public class EventFeignController implements EventClient {
    private final EventService eventService;

    @Override
    @GetMapping("/{eventId}")
    public EventRequestDto getEventById(long eventId) {
        return eventService.getByIdForRequest(eventId);
    }

    @Override
    @GetMapping("/{eventId}/initiator/{userId}")
    public EventRequestDto getEventByIdAndInitiator(long eventId, long userId) {
        return eventService.getByIdAndInitiatorId(eventId, userId);
    }
}
