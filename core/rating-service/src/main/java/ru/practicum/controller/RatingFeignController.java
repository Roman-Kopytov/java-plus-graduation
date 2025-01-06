package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.event.EventRatingDto;
import ru.practicum.feignclient.RatingClient;
import ru.practicum.service.RatingService;

import java.util.List;

@RestController
@RequestMapping("/feign/rating")
@RequiredArgsConstructor
public class RatingFeignController implements RatingClient {
    private final RatingService ratingService;

    @Override
    @GetMapping("/count")
    public List<EventRatingDto> countEventsRating(@RequestBody List<Long> eventIds) {
        return ratingService.getEventsRating(eventIds);
    }

    @GetMapping("/count/{eventId}")
    @Override
    public Integer countEventRating(@PathVariable long eventId) {
        return ratingService.getEventRating(eventId);
    }
}
