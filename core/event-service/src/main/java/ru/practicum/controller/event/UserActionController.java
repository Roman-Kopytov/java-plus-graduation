package ru.practicum.controller.event;

import com.google.protobuf.Timestamp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.grpc.stats.action.ActionTypeProto;
import ru.yandex.practicum.grpc.stats.action.UserActionProto;
import ru.yandex.practicum.grpc.stats.collector.UserActionControllerGrpc;

import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserActionController {
    @GrpcClient("collector")
    private UserActionControllerGrpc.UserActionControllerBlockingStub client;

    public void sendUserAction(long eventId, long userId, ActionTypeProto actionType, Instant instant) {
        Timestamp timestamp = Timestamp.newBuilder().setNanos(instant.getNano()).build();
        UserActionProto request = UserActionProto.newBuilder()
                .setEventId(eventId)
                .setUserId(userId)
                .setActionType(actionType)
                .setTimestamp(timestamp)
                .build();
        client.collectUserAction(request);

    }
}
