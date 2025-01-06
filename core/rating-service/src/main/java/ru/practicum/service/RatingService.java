package ru.practicum.service;

import ru.practicum.dto.event.EventRatingDto;
import ru.practicum.dto.rating.RatingDto;

import java.util.List;

public interface RatingService {
    List<RatingDto> getAllById(long userId, int from, int size);

    void addRating(long userId, long eventId, boolean isLike);

    void removeRating(long userId, long eventId, boolean isLike);

    List<EventRatingDto> getEventsRating(List<Long> eventIds);

    Integer getEventRating(long eventId);
}
