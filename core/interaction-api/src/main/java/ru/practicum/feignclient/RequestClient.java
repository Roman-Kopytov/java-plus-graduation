package ru.practicum.feignclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.dto.request.EventCountByRequest;

import java.util.List;

@FeignClient(name = "ewm-request-service")
public interface RequestClient {
    @GetMapping("/feign/request/event_with_count_confirmed")
    List<EventCountByRequest> getEventIdAndCountRequest(@RequestParam("eventIds") List<Long> eventIds);

    @GetMapping("/feign/request/count_confirmed/{eventId}")
    Integer countConfirmedRequest(@PathVariable long eventId);

    @GetMapping("/feign/request/participant/{eventId}/{userId}")
    Boolean isParticipant(@PathVariable long eventId, @PathVariable long userId);
}
