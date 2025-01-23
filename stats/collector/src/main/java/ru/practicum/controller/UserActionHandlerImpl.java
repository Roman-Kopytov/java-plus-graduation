package ru.practicum.controller;

import com.google.protobuf.Timestamp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.grpc.stats.action.ActionTypeProto;
import ru.practicum.grpc.stats.action.UserActionProto;
import ru.practicum.kafka.config.KafkaCollectorProperties;
import ru.practicum.service.KafkaActionProducer;

import java.time.Instant;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserActionHandlerImpl implements UserActionHandler {

    private final KafkaActionProducer producer;
    private final KafkaCollectorProperties kafkaProperties;

    @Override
    public void handle(UserActionProto userAction) {
        try {
            log.info("Начало обработки действия пользователя: {}", userAction);

            UserActionAvro userActionAvro = mapToAvro(userAction);
            String topic = kafkaProperties.getUserActionsTopic();

            producer.send(topic, userActionAvro);

            log.info("Событие успешно отправлено в топик {}", topic);
        } catch (IllegalArgumentException e) {
            log.error("Ошибка при преобразовании UserActionProto в Avro: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Ошибка при обработке действия пользователя: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка при обработке действия пользователя", e);
        }
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
        switch (actionType) {
            case ACTION_VIEW:
                return ActionTypeAvro.VIEW;
            case ACTION_REGISTER:
                return ActionTypeAvro.REGISTER;
            case ACTION_LIKE:
                return ActionTypeAvro.LIKE;
            default:
                throw new IllegalArgumentException("Unknown ActionTypeProto: " + actionType);
        }
    }
}
