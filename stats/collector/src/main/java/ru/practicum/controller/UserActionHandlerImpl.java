package ru.practicum.controller;

import com.google.protobuf.Timestamp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.kafka.config.KafkaCollectorProperties;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.service.KafkaActionProducer;
import ru.practicum.grpc.stats.action.ActionTypeProto;
import ru.practicum.grpc.stats.action.UserActionProto;

import java.time.Instant;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserActionHandlerImpl implements UserActionHandler {

    private final KafkaActionProducer producer;
    private final KafkaCollectorProperties kafkaProperties;

    @Override
    public void handle(UserActionProto userAction) {
        UserActionAvro userActionAvro = mapToAvro(userAction);
        String topic = kafkaProperties.getUserActionsTopic();

        log.info("Отправка события в топик {}", topic);
        producer.send(topic, userAction.getEventId(), userActionAvro);
    }

    private UserActionAvro mapToAvro(UserActionProto userAction) {
        Timestamp timestamp = userAction.getTimestamp();
        Instant instant = Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
        return UserActionAvro.newBuilder()
                .setEventId(userAction.getEventId())
                .setUserId(userAction.getUserId())
                .setActionType(mapActionTypeToAvro(userAction.getActionType()))
                .setTimestamp(instant)
                .build();
    }

    private ActionTypeAvro mapActionTypeToAvro(ActionTypeProto actionType) {
        return ActionTypeAvro.valueOf(actionType.name());
    }
}
