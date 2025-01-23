package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class SimilarityCalcService {
    private final Map<Long, Map<Long, Double>> userEventWeights = new HashMap<>();
    private final Map<Long, Double> eventWeightSums = new HashMap<>();
    private final Map<Long, Map<Long, Double>> minWeightsSums = new HashMap<>();


    public Set<Long> getEventIds() {
        return userEventWeights.keySet();
    }

    public double calculateSimilarity(long eventA, long eventB) {
        double sMin = minWeightsSums
                .getOrDefault(eventA, new HashMap<>())
                .getOrDefault(eventB, 0.0);

        double sA = eventWeightSums.getOrDefault(eventA, 0.0);
        double sB = eventWeightSums.getOrDefault(eventB, 0.0);
        if (sA == 0 || sB == 0) {
            return 0.0;
        }
        return (sMin == 0) ? 0.0 : sMin / (Math.sqrt(sA) * Math.sqrt(sB));
    }


    public void updateUserAction(UserActionAvro action) {
        long eventId = action.getEventId();
        long userId = action.getUserId();
        double newWeight = calculateWeight(action.getActionType().toString());

        userEventWeights.putIfAbsent(eventId, new HashMap<>());
        eventWeightSums.putIfAbsent(eventId, 0.0);

        Map<Long, Double> userWeights = userEventWeights.get(eventId);
        double oldWeight = userWeights.getOrDefault(userId, 0.0);

        if (newWeight > oldWeight) {
            double oldSum = eventWeightSums.get(eventId);
            eventWeightSums.put(eventId, oldSum - oldWeight * oldWeight + newWeight * newWeight);

            userWeights.put(userId, newWeight);

            updateMinWeights(userId, eventId, oldWeight, newWeight);
        }
    }


    private void updateMinWeights(long userId, long eventId, double oldWeight, double newWeight) {
        for (Map.Entry<Long, Map<Long, Double>> entry : userEventWeights.entrySet()) {
            long otherEventId = entry.getKey();
            if (eventId == otherEventId) continue;

            double otherWeight = entry.getValue().getOrDefault(userId, 0.0);
            if (otherWeight > 0) {


                double oldMinSum = minWeightsSums
                        .computeIfAbsent(eventId, e -> new HashMap<>())
                        .getOrDefault(otherEventId, 0.0);

                double newMinSum = oldMinSum
                        - Math.min(oldWeight, otherWeight)
                        + Math.min(newWeight, otherWeight);

                minWeightsSums.get(eventId).put(otherEventId, newMinSum);
            }
        }
    }


    private double calculateWeight(String actionType) {
        return switch (actionType) {
            case "VIEW" -> 0.4;
            case "REGISTER" -> 0.8;
            case "LIKE" -> 1.0;
            default -> 0.0;
        };
    }
}
