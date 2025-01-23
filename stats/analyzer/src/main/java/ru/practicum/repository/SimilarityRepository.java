package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.model.EventSimilarity;

import java.util.List;

public interface SimilarityRepository extends JpaRepository<EventSimilarity, Long> {
    List<EventSimilarity> getEventSimilaritiesByEventAIn(List<Long> listUserEventsWithAction);

    @Query("SELECT es FROM EventSimilarity es " +
            "WHERE es.eventA IN :listEvent OR es.eventB IN :listEvent"
    )
    List<EventSimilarity> getEventSimilarityByEventAOrEventBIn(@Param("listEvent") List<Long> listLastUserEventsWithAction);

    @Query("SELECT es FROM EventSimilarity es " +
            "WHERE es.eventA IN :listEvent OR es.eventB IN :listEvent " +
            "ORDER BY es.score desc " +
            "LIMIT :limit")
    List<EventSimilarity> getMostSimilarByEventAOrEventBIn(@Param("listEvent") List<Long> listLastUserEventsWithAction,
                                                           @Param("limit") long limit);
}
