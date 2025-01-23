package ru.practicum;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.kafka.config.KafkaProperties;
import ru.practicum.service.UserActionsService;

import java.time.Duration;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserActionsProcessor implements Runnable {
    private final Consumer<String, UserActionAvro> consumer;
    private final KafkaProperties kafkaProperties;
    private final UserActionsService userActionsService;
    private static long POLL_DURATION;

    @PostConstruct
    private void init() {
        POLL_DURATION = kafkaProperties.getPollDuration();
    }

    @Override
    public void run() {
        log.info("UserActions processor started");
        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));
        try {
            consumer.subscribe(List.of(kafkaProperties.getEvents().getTopic()));
            while (true) {
                ConsumerRecords<String, UserActionAvro> records = consumer.poll(Duration.ofMillis(POLL_DURATION));
                for (ConsumerRecord<String, UserActionAvro> record : records) {
                    sendToService(record);
                }
                consumer.commitSync();
            }
        } catch (WakeupException e) {
            log.info("Consumer wakeup triggered, shutting down gracefully.");
        } catch (Exception e) {
            log.error("Ошибка во время обработки событий от хаба", e);
        } finally {
            log.info("Закрываем консьюмер");
            consumer.close();
        }
    }

    private void sendToService(ConsumerRecord<String, UserActionAvro> record) {
        UserActionAvro event = record.value();
        userActionsService.saveAction(event);
    }
}
