package ru.practicum.service;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.controller.UserActionHandlerImpl;
import ru.practicum.grpc.stats.action.UserActionProto;
import ru.practicum.grpc.stats.collector.UserActionControllerGrpc;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class UserActionController extends UserActionControllerGrpc.UserActionControllerImplBase {
    private final UserActionHandlerImpl userActionHandler;

    @Override
    public void collectUserAction(UserActionProto request, StreamObserver<Empty> responseObserver) {
        log.info("Received request: {}", request);
        try {
            userActionHandler.handle(request);
            log.info("Successfully processed request for user action: {}", request.getActionType());
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Failed to process request for user action: {}", request.getActionType(), e);
            responseObserver.onError(new StatusRuntimeException(Status.fromThrowable(e)));
        }
    }
}

