package ru.practicum.service;

import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.yandex.practicum.grpc.stats.event.InteractionsCountRequestProto;
import ru.yandex.practicum.grpc.stats.event.RecommendedEventProto;
import ru.yandex.practicum.grpc.stats.event.SimilarEventsRequestProto;
import ru.yandex.practicum.grpc.stats.event.UserPredictionsRequestProto;

import java.util.List;

public interface SimilarityService {

    void saveSimilarity(EventSimilarityAvro eventSimilarityAvro);

    List<RecommendedEventProto> generateRecommendationsForUser(UserPredictionsRequestProto userRequest);

    List<RecommendedEventProto> generateRecommendationsForUserByEvent(SimilarEventsRequestProto eventRequest);

    List<RecommendedEventProto> getInteractionsCount(InteractionsCountRequestProto countRequest);
}
