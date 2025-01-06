package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.request.EventCountByRequest;
import ru.practicum.feignclient.RequestClient;
import ru.practicum.service.RequestService;

import java.util.List;

@RestController
@RequestMapping("/feign/request")
@RequiredArgsConstructor
public class RequestFeignController implements RequestClient {

    private final RequestService requestService;

    @Override
    @GetMapping("/event_with_count_confirmed")
    public List<EventCountByRequest> getEventIdAndCountRequest(@RequestParam("eventIds") List<Long> eventIds) {
        return requestService.getEventIdAndCountRequest(eventIds);
    }

    @Override
    @GetMapping("/count_confirmed/{eventId}")
    public Integer countConfirmedRequest(@PathVariable Long eventId) {
        return requestService.countConfirmedRequest(eventId);
    }
}
