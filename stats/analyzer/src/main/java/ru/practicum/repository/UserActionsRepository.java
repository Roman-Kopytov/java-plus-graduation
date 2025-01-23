package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.model.RecommendedEventProtoWrap;
import ru.practicum.model.UserAction;

import java.util.List;

public interface UserActionsRepository extends JpaRepository<UserAction, Long> {
    List<UserAction> getUserActionByUserId(long userId);

    @Query("SELECT DISTINCT ua.eventId " +
            "FROM UserAction ua " +
            "WHERE ua.userId = :userId " +
            "ORDER BY ua.timestamp DESC " +
            "LIMIT :limit")
    List<Long> getLastUserEventsWithAction(@Param("userId") long userId, @Param("limit") long limit);

    @Query("SELECT DISTINCT ua.eventId FROM UserAction ua WHERE ua.userId = :userId")
    List<Long> getEventWithActionByUser(@Param("userId") long userId);

    @Query("SELECT new ru.practicum.model.RecommendedEventProtoWrap(ua.eventId, SUM(ua.maxWeight))  " +
            "FROM UserAction ua " +
            "WHERE ua.userId IN :userIds " +
            "GROUP BY ua.eventId")
    List<RecommendedEventProtoWrap> getUserActionByEventIdIn(@Param("userIds") List<Long> userIds);
}
