package ru.practicum.feignclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.dto.event.EventRatingDto;

import java.util.List;

@FeignClient(name = "ewm-rating-service")
public interface RatingClient {

    @GetMapping("/feign/rating/count")
    List<EventRatingDto> countEventsRating(@RequestParam("eventIds") List<Long> eventIds);

    @GetMapping("/feign/rating/count/{eventId}")
    Integer countEventRating(@PathVariable long eventId);
}
