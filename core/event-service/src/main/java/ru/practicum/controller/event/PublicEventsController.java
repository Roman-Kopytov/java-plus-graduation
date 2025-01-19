package ru.practicum.controller.event;

import jakarta.ws.rs.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import ru.practicum.client.actions.UserActionClient;
import ru.practicum.client.recommendation.RecommendationClient;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.event.PublicEventRequestParams;
import ru.practicum.dto.event.Sort;
import ru.practicum.exeption.WrongDateException;
import ru.practicum.grpc.stats.action.ActionTypeProto;
import ru.practicum.grpc.stats.event.RecommendedEventProto;
import ru.practicum.service.event.EventService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@RestController
@RequiredArgsConstructor
@RequestMapping("/events")
public class PublicEventsController {
    private final EventService eventService;
    private final RecommendationClient recommendationController;
    private final UserActionClient userActionController;

    @GetMapping()
    public List<EventShortDto> getEventsPublic(@RequestParam(value = "text", required = false) String text,
                                               @RequestParam(value = "categories", required = false) List<Long> categories,
                                               @RequestParam(value = "paid", required = false) Boolean paid,
                                               @RequestParam(value = "rangeStart", required = false)
                                               @DateTimeFormat(pattern = ("yyyy-MM-dd HH:mm:ss")) LocalDateTime rangeStart,
                                               @RequestParam(value = "rangeEnd", required = false)
                                               @DateTimeFormat(pattern = ("yyyy-MM-dd HH:mm:ss")) LocalDateTime rangeEnd,
                                               @RequestParam(value = "onlyAvailable", defaultValue = "false") Boolean onlyAvailable,
                                               @RequestParam(value = "sort", required = false) Sort sort,
                                               @RequestParam(value = "from", defaultValue = "0") int from,
                                               @RequestParam(value = "size", defaultValue = "10") int size) {


        Map<String, LocalDateTime> ranges = validDate(rangeStart, rangeEnd);
        PublicEventRequestParams params = PublicEventRequestParams.builder()
                .text(text)
                .categories(categories)
                .paid(paid)
                .rangeStart(ranges.get("rangeStart"))
                .rangeEnd(ranges.get("rangeEnd"))
                .onlyAvailable(onlyAvailable)
                .sort(sort)
                .from(from)
                .size(size)
                .build();
        List<EventShortDto> all = eventService.getAll(params);
        return all;
    }

    @PutMapping("/{eventId}/like")
    public void addLikeToEvent(@RequestHeader("X-EWM-USER-ID") long userId, @PathVariable long eventId) {
        boolean wasVisited = eventService.checkEventVisitedByUser(eventId, userId);
        if (wasVisited) {
            userActionController.sendUserAction(eventId, userId, ActionTypeProto.ACTION_LIKE, Instant.now());
        } else {
            throw new BadRequestException();
        }
    }

    @GetMapping("/{eventId}")
    public EventFullDto getById(@PathVariable("eventId") long eventId, @RequestHeader("X-EWM-USER-ID") long userId) {
        EventFullDto event = eventService.getById(eventId);
        userActionController.sendUserAction(eventId, userId, ActionTypeProto.ACTION_VIEW, Instant.now());
        return event;
    }

    @GetMapping("/recommendations")
    public Stream<RecommendedEventProto> getRecommendations(@RequestHeader("X-EWM-USER-ID") int userId) {
        return recommendationController.getRecommendationsForUser(userId, 10);
    }


    private Map<String, LocalDateTime> validDate(LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        if (rangeEnd != null && rangeStart != null && rangeEnd.isBefore(rangeStart)) {
            throw new WrongDateException("Range end must be after range start");
        }
        LocalDateTime effectiveRangeStart = rangeStart != null ? rangeStart : LocalDateTime.now();
        LocalDateTime effectiveRangeEnd = rangeEnd != null ? rangeEnd : effectiveRangeStart.plusYears(200);
        return Map.of("rangeStart", effectiveRangeStart, "rangeEnd", effectiveRangeEnd);
    }

}
