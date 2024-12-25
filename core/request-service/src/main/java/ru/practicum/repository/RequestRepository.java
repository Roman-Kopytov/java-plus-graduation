package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import ru.practicum.dto.request.EventCountByRequest;
import ru.practicum.dto.request.RequestStatus;
import ru.practicum.model.Request;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface RequestRepository extends JpaRepository<Request, Long>, QuerydslPredicateExecutor<Request> {

    List<Request> findByEventIdAndRequesterId(long eventId, long userId);

    Optional<Request> findByEventIdAndRequesterIdAndStatus(long eventId, long userId, RequestStatus status);


    List<Request> findByRequesterId(long userId);

//    @Query(value = "SELECT new ru.practicum.dto.request.EventCountByRequest(r.eventId, COUNT(r.id)) " +
//            "FROM Request r " +
//            "WHERE r.eventId IN ?1 AND r.status = 'CONFIRMED' " +
//            "GROUP BY r.eventId " +
//            "GROUP BY e.id, e.participantLimit " +
//            "HAVING COUNT(r.id) >= e.participantLimit")
//    List<EventCountByRequest> findConfirmedRequestWithLimitCheck(Set<Long> eventIds);

    @Query(value = "SELECT new ru.practicum.dto.request.EventCountByRequest(r.eventId, COUNT(r.id)) " +
            "FROM Request r " +
            "WHERE r.eventId IN ?1 AND r.status = 'CONFIRMED' " +
            "GROUP BY r.eventId")
    List<EventCountByRequest> getEventIdAndCountRequest(Set<Long> eventIds);


    @Query(value = "SELECT (COUNT(r.id)>=?2) " +
            "FROM Request r " +
            "WHERE r.eventId = ?1 AND r.status= 'CONFIRMED'")
    boolean isParticipantLimitReached(long eventId, int limit);

    @Query(value = "SELECT COUNT(r.id) AS count " +
            "FROM Request r " +
            "WHERE r.eventId = ?1 AND r.status = 'CONFIRMED'")
    Integer countConfirmedRequest(long eventId);

    List<Request> findByEventId(long eventId);
}