package ru.practicum.service;

import com.querydsl.core.types.dsl.BooleanExpression;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.event.EventRatingDto;
import ru.practicum.dto.event.EventRequestDto;
import ru.practicum.dto.event.EventState;
import ru.practicum.dto.rating.RatingDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.exeption.ConflictException;
import ru.practicum.exeption.NotFoundException;
import ru.practicum.feignclient.EventClient;
import ru.practicum.feignclient.UserClient;
import ru.practicum.mapper.RatingMapper;
import ru.practicum.model.QRating;
import ru.practicum.model.Rating;
import ru.practicum.repository.RatingRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RatingServiceImpl implements RatingService {

    private final RatingRepository ratingRepository;
    private final EventClient eventClient;
    private final UserClient userClient;
    private final RatingMapper ratingMapper;

    @Override
    public List<RatingDto> getAllById(long userId, int from, int size) {
        QRating rating = QRating.rating;
        List<BooleanExpression> conditions = new ArrayList<>();
        conditions.add(rating.userId.eq(userId));
        BooleanExpression finalCondition = conditions.stream()
                .reduce(BooleanExpression::and)
                .orElse(null);

        PageRequest pageRequest = PageRequest.of(from / size, size);

        List<Rating> ratings = ratingRepository.findAll(finalCondition, pageRequest).getContent();

        return ratings.stream()
                .map(ratingMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void addRating(long userId, long eventId, boolean isLike) {
        UserShortDto user = getUser(userId);
        EventRequestDto event = getEvent(eventId);

        if (event.getInitiatorId() == (userId)) {
            throw new ConflictException("The initiator of the event can't add a request to participate in his event");
        }

        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("Cannot react because the event is not published");
        }

        Optional<Rating> existingRating = ratingRepository.findByUserIdAndEventId(user.getId(), event.getId());

        if (existingRating.isPresent()) {
            Rating rating = existingRating.get();
            if (rating.getIsLike() == isLike) {
                throw new ConflictException((isLike ? "Like" : "Dislike") + " already exists");
            } else {
                rating.setIsLike(isLike);
                ratingMapper.toDto(ratingRepository.save(rating));
            }
        } else {
            // Если нет реакции, создаем новую
            Rating rating = Rating.builder()
                    .created(LocalDateTime.now())
                    .userId(user.getId())
                    .eventId(event.getId())
                    .isLike(isLike)
                    .build();
            ratingMapper.toDto(ratingRepository.save(rating));
        }
    }

    @Transactional
    public void removeRating(long userId, long eventId, boolean isLike) {
        UserShortDto user = getUser(userId);
        EventRequestDto event = getEvent(eventId);

        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("Cannot remove reaction because the event is not published");
        }

        Rating rating = getRating(user.getId(), event.getId());

        if (rating.getIsLike() == isLike) {
            ratingRepository.delete(rating);
        } else {
            throw new ConflictException("No " + (isLike ? "like" : "dislike") + " found to remove");
        }
    }

    @Override
    public List<EventRatingDto> getEventsRating(List<Long> eventIds) {
        return ratingRepository.countEventsRating(eventIds);
    }

    @Override
    public Integer getEventRating(long eventId) {
        int likes = Optional.of(ratingRepository.countLikesByEvent(eventId)).orElse(0);
        int dislikes = Optional.of(ratingRepository.countDislikesByEvent(eventId)).orElse(0);
        return likes - dislikes;
    }

    private UserShortDto getUser(long userId) {
        try {
            return userClient.getUserById(userId);
        } catch (FeignException e) {
            if (e.status() == 404) {
                log.warn("User not found");
            } else {
                log.warn("Feign error: " + e.status(), e);
            }
            throw e;
        }
    }

    private EventRequestDto getEvent(long eventId) {
        try {
            return eventClient.getEventById(eventId);
        } catch (FeignException e) {
            if (e.status() == 404) {
                log.warn("User not found");
            } else {
                log.warn("Feign error: " + e.status(), e);
            }
            throw e;
        }
    }

    private Rating getRating(long userId, long eventId) {
        return ratingRepository.findByUserIdAndEventId(userId, eventId)
                .orElseThrow(() -> new NotFoundException("Rating not found"));
    }
}
