package ru.practicum.controller;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.service.SimilarityService;
import ru.practicum.grpc.stats.collector.RecommendationsControllerGrpc;
import ru.practicum.grpc.stats.event.InteractionsCountRequestProto;
import ru.practicum.grpc.stats.event.RecommendedEventProto;
import ru.practicum.grpc.stats.event.SimilarEventsRequestProto;
import ru.practicum.grpc.stats.event.UserPredictionsRequestProto;

import java.util.List;

@GrpcService
@Slf4j
@RequiredArgsConstructor
public class SimilarityServer extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {
    private final SimilarityService similarityService;

    @Override
    public void getRecommendationsForUser(UserPredictionsRequestProto request, StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            log.info("Method getRecommendationsForUser request received");
            List<RecommendedEventProto> recommendedEvents = similarityService.generateRecommendationsForUser(request);
            for (RecommendedEventProto event : recommendedEvents) {
                responseObserver.onNext(event);
            }
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(new StatusRuntimeException(
                    Status.INTERNAL
                            .withDescription(e.getLocalizedMessage())
                            .withCause(e)
            ));
        }
    }

    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request, StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            log.info("Method getSimilarEvents request received");
            List<RecommendedEventProto> recommendedEvents = similarityService.generateRecommendationsForUserByEvent(request);
            for (RecommendedEventProto event : recommendedEvents) {
                responseObserver.onNext(event);
            }
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(new StatusRuntimeException(
                    Status.INTERNAL
                            .withDescription(e.getLocalizedMessage())
                            .withCause(e)
            ));
        }
    }

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request, StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            log.info("Method getInteractionsCount request received");
            List<RecommendedEventProto> recommendedEvents = similarityService.getInteractionsCount(request);
            for (RecommendedEventProto event : recommendedEvents) {
                responseObserver.onNext(event);
            }
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(new StatusRuntimeException(
                    Status.INTERNAL
                            .withDescription(e.getLocalizedMessage())
                            .withCause(e)
            ));
        }
    }


}
