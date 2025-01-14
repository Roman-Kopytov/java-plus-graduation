package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import ru.practicum.dto.request.EventCountByRequest;
import ru.practicum.dto.request.RequestStatus;
import ru.practicum.model.Request;

import java.util.List;

public interface RequestRepository extends JpaRepository<Request, Long>, QuerydslPredicateExecutor<Request> {

    List<Request> findByEventIdAndRequesterId(long eventId, long userId);

    List<Request> findByRequesterId(long userId);

    @Query("SELECT new ru.practicum.dto.request.EventCountByRequest(e.eventId, " +
            "(SELECT COUNT(r.id) FROM Request r WHERE r.eventId = e.eventId AND r.status = 'CONFIRMED')) " +
            "FROM Request e WHERE e.eventId IN :eventIds GROUP BY e.eventId")
    List<EventCountByRequest> getEventIdAndCountRequest(@Param("eventIds") List<Long> eventIds);


    @Query(value = "SELECT (COUNT(r.id)>=?2) " +
            "FROM Request r " +
            "WHERE r.eventId = ?1 AND r.status= 'CONFIRMED'")
    boolean isParticipantLimitReached(long eventId, int limit);

    boolean existsByRequesterIdAndEventIdAndStatus(long userId, long eventId, RequestStatus status);

    @Query(value = "SELECT COUNT(r.id) AS count " +
            "FROM Request r " +
            "WHERE r.eventId = :eventId AND r.status = :status")
    Integer countRequests(@Param("eventId") long eventId, @Param("status") RequestStatus status);

    List<Request> findByEventId(long eventId);
}