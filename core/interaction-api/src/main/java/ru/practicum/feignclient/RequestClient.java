package ru.practicum.feignclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.dto.request.EventCountByRequest;

import java.util.List;
import java.util.Set;

@FeignClient(name = "ewm-request-service")
public interface RequestClient {
    @GetMapping("/feign/request/event_with_count_confirmed")
    List<EventCountByRequest> getEventIdAndCountRequest(Set<Long> eventIds);

    @GetMapping("/feign/request/count_confirmed/{eventId}")
    Integer countConfirmedRequest(@PathVariable Long eventId);

}
