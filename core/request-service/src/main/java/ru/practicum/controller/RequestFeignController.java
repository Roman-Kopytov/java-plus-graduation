package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.dto.request.EventCountByRequest;
import ru.practicum.feignclient.RequestClient;
import ru.practicum.service.RequestService;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/feign/request")
@RequiredArgsConstructor
public class RequestFeignController implements RequestClient {

    private final RequestService requestService;

    @Override
    @GetMapping("/event_with_count_confirmed")
    public List<EventCountByRequest> getEventIdAndCountRequest(Set<Long> eventIds) {
        return requestService.getEventIdAndCountRequest(eventIds);
    }

    @Override
    @GetMapping("/count_confirmed/{eventId}")
    public Integer countConfirmedRequest(Long eventId) {
        return requestService.countConfirmedRequest(eventId);
    }
}
