package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.model.EventSimilarity;
import ru.practicum.model.RecommendedEventProtoWrap;
import ru.practicum.model.UserAction;
import ru.practicum.repository.SimilarityRepository;
import ru.practicum.repository.UserActionsRepository;
import ru.practicum.grpc.stats.event.InteractionsCountRequestProto;
import ru.practicum.grpc.stats.event.RecommendedEventProto;
import ru.practicum.grpc.stats.event.SimilarEventsRequestProto;
import ru.practicum.grpc.stats.event.UserPredictionsRequestProto;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class SimilarityServiceImpl implements SimilarityService {
    private final SimilarityRepository similarityRepository;
    private final UserActionsRepository userActionsRepository;

    @Override
    public void saveSimilarity(EventSimilarityAvro eventSimilarityAvro) {
        EventSimilarity similarity = EventSimilarity.builder()
                .eventA(eventSimilarityAvro.getEventA())
                .eventB(eventSimilarityAvro.getEventB())
                .score(eventSimilarityAvro.getScore())
                .timestamp(eventSimilarityAvro.getTimestamp())
                .build();
        similarityRepository.save(similarity);
    }

    @Override
    public List<RecommendedEventProto> generateRecommendationsForUser(UserPredictionsRequestProto userRequest) {
        long userId = userRequest.getUserId();
        long maxResults = userRequest.getMaxResults();
        long limit = 100;

        List<Long> lastUserEventsWithAction = userActionsRepository.getLastUserEventsWithAction(userId, limit);
        List<Long> eventWithActionByUser = userActionsRepository.getEventWithActionByUser(userId);
        List<UserAction> userActionByUserId = userActionsRepository.getUserActionByUserId(userId);

        List<EventSimilarity> predictedEvents = findSimilarEvents(lastUserEventsWithAction, eventWithActionByUser, maxResults);
        List<RecommendedEventProto> recommendedEventProtos = mapToUniqueRecommendedEvent(predictedEvents, eventWithActionByUser);
        List<Long> predictedEventsId = recommendedEventProtos.stream().map(RecommendedEventProto::getEventId).toList();

        long k = 3;
        List<EventSimilarity> similarityWithPredicted = similarityRepository.getEventSimilarityByEventAOrEventBIn(predictedEventsId);
        Map<Long, List<EventSimilarity>> predictedToTopKSimilarities = predictedEventsId.stream()
                .collect(Collectors.toMap(
                        predicted -> predicted,
                        predicted -> similarityWithPredicted.stream()
                                .filter(es -> (es.getEventA() == predicted || es.getEventB() == predicted)
                                        && eventWithActionByUser.contains(es.getEventA()) && eventWithActionByUser.contains(es.getEventB()))
                                .sorted(Comparator.comparingDouble(EventSimilarity::getScore).reversed())
                                .limit(k)
                                .toList()
                ));

        Map<Long, Double> userRatings = userActionByUserId.stream()
                .collect(Collectors.toMap(UserAction::getEventId, UserAction::getMaxWeight));
        Map<Long, Double> marksForPredicted = calculatePredictedScores(predictedToTopKSimilarities, userRatings);

        recommendedEventProtos.forEach(proto -> {
            Double score = marksForPredicted.getOrDefault(proto.getEventId(), 0.0);
            proto = RecommendedEventProto.newBuilder(proto).setScore(score).build();
        });

        return recommendedEventProtos;
    }

    @Override
    public List<RecommendedEventProto> generateRecommendationsForUserByEvent(SimilarEventsRequestProto eventRequest) {
        long userId = eventRequest.getUserId();
        long eventId = eventRequest.getEventId();
        long maxResults = eventRequest.getMaxResults();

        List<Long> eventWithActionByUser = userActionsRepository.getEventWithActionByUser(userId);
        List<EventSimilarity> similarEvents = findSimilarEvents(List.of(eventId), eventWithActionByUser, maxResults);
        return mapToUniqueRecommendedEvent(similarEvents, eventWithActionByUser);
    }

    private List<EventSimilarity> findSimilarEvents(List<Long> targetEvents, List<Long> eventWithActionByUser, long maxResults) {
        return similarityRepository.getEventSimilarityByEventAOrEventBIn(targetEvents).stream()
                .filter(es -> !(eventWithActionByUser.contains(es.getEventA()) && eventWithActionByUser.contains(es.getEventB())))
                .sorted(Comparator.comparingDouble(EventSimilarity::getScore).reversed())
                .limit(maxResults)
                .toList();
    }

    @Override
    public List<RecommendedEventProto> getInteractionsCount(InteractionsCountRequestProto countRequest) {
        List<Long> eventIds = countRequest.getEventIdList();
        List<RecommendedEventProtoWrap> userActionByEventIdIn = userActionsRepository.getUserActionByEventIdIn(eventIds);
        return userActionByEventIdIn.stream()
                .map(wr -> RecommendedEventProto.newBuilder()
                        .setEventId(wr.evenId())
                        .setScore(wr.score())
                        .build())
                .toList();

    }

    private List<RecommendedEventProto> mapToUniqueRecommendedEvent(List<EventSimilarity> eventSimilarities,
                                                                    List<Long> eventWithActionByUser) {
        return eventSimilarities.stream()
                .flatMap(es -> Stream.of(
                        !eventWithActionByUser.contains(es.getEventA())
                                ? RecommendedEventProto.newBuilder().setEventId(es.getEventA()).setScore(es.getScore()).build()
                                : null,
                        !eventWithActionByUser.contains(es.getEventB())
                                ? RecommendedEventProto.newBuilder().setEventId(es.getEventB()).setScore(es.getScore()).build()
                                : null
                ))
                .filter(Objects::nonNull)
                .toList();
    }

    private Map<Long, Double> calculatePredictedScores(Map<Long, List<EventSimilarity>> predictedToTopKSimilarities,
                                                       Map<Long, Double> userRatings) {
        return predictedToTopKSimilarities.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            double weightedSum = entry.getValue().stream()
                                    .filter(es -> userRatings.containsKey(es.getEventA()) || userRatings.containsKey(es.getEventB()))
                                    .mapToDouble(es -> {
                                        Long relatedEventId = es.getEventA() == entry.getKey() ? es.getEventB() : es.getEventA();
                                        return userRatings.getOrDefault(relatedEventId, 0.0) * es.getScore();
                                    }).sum();
                            double similaritySum = entry.getValue().stream()
                                    .mapToDouble(EventSimilarity::getScore)
                                    .sum();
                            return similaritySum > 0 ? weightedSum / similaritySum : 0.0;
                        }
                ));
    }

}
