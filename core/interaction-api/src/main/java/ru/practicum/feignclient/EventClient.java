package ru.practicum.feignclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.dto.event.EventRequestDto;

@FeignClient(name = "ewm-event-service")
public interface EventClient {

    @GetMapping("/feign/events/{eventId}")
    EventRequestDto getEventById(@PathVariable long eventId);

    @GetMapping("/feign/events/{eventId}/initiator{userId}")
    EventRequestDto getEventByIdAndInitiator(@PathVariable long eventId,
                                             @PathVariable long userId);

}
