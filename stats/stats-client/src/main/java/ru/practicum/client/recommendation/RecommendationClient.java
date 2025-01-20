package ru.practicum.client.recommendation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import ru.practicum.grpc.stats.collector.RecommendationsControllerGrpc;
import ru.practicum.grpc.stats.event.InteractionsCountRequestProto;
import ru.practicum.grpc.stats.event.RecommendedEventProto;
import ru.practicum.grpc.stats.event.SimilarEventsRequestProto;
import ru.practicum.grpc.stats.event.UserPredictionsRequestProto;

import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service
@Slf4j
@RequiredArgsConstructor
public class RecommendationClient {
    @GrpcClient("analyzer")
    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub client;

    public Stream<RecommendedEventProto> getSimilarEvents(int eventId, int userId, int maxResults) {
        try {
            SimilarEventsRequestProto request = SimilarEventsRequestProto.newBuilder()
                    .setEventId(eventId)
                    .setUserId(userId)
                    .setMaxResults(maxResults)
                    .build();
            Iterator<RecommendedEventProto> iterator = client.getSimilarEvents(request);

            return asStream(iterator);
        } catch (Exception e) {
            log.error("Error while fetching similar events: eventId={}, userId={}, maxResults={}", eventId, userId, maxResults, e);
            return Stream.empty();
        }
    }

    public Stream<RecommendedEventProto> getRecommendationsForUser(int userId, int maxResults) {
        try {
            UserPredictionsRequestProto request = UserPredictionsRequestProto.newBuilder()
                    .setUserId(userId)
                    .setMaxResults(maxResults)
                    .build();
            Iterator<RecommendedEventProto> iterator = client.getRecommendationsForUser(request);

            return asStream(iterator);
        } catch (Exception e) {
            log.error("Error while fetching recommendations for user: userId={}, maxResults={}", userId, maxResults, e);
            return Stream.empty();
        }
    }

    public Stream<RecommendedEventProto> getInteractionsCount(List<Long> eventId) {
        try {
            InteractionsCountRequestProto request = InteractionsCountRequestProto.newBuilder()
                    .addAllEventId(eventId)
                    .build();
            Iterator<RecommendedEventProto> iterator = client.getInteractionsCount(request);

            return asStream(iterator);
        } catch (Exception e) {
            log.error("Error while fetching interactions count", e);
            return Stream.empty();
        }
    }

    private Stream<RecommendedEventProto> asStream(Iterator<RecommendedEventProto> iterator) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED),
                false
        );
    }
}
