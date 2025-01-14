package ru.practicum;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.practicum.config.KafkaProperties;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.service.SimilarityService;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventsSimilarityProcessor implements Runnable {
    private final Consumer<String, EventSimilarityAvro> consumer;
    private final KafkaProperties kafkaProperties;
    private final SimilarityService similarityService;
    private static long POLL_DURATION;

    @PostConstruct
    private void init() {
        POLL_DURATION = kafkaProperties.getPollDuration();
    }

    @Override
    public void run() {
        log.info("EventsSimilarity processor started");
        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));
        try {
            consumer.subscribe(List.of(kafkaProperties.getEvents().getTopic()));
            while (true) {
                ConsumerRecords<String, EventSimilarityAvro> records = consumer.poll(Duration.ofMillis(POLL_DURATION));
                for (ConsumerRecord<String, EventSimilarityAvro> record : records) {
                    sendToService(record);
                }
                consumer.commitSync();
            }
        } catch (WakeupException e) {
        } catch (Exception e) {
            log.error("Ошибка во время обработки событий от хаба", e);
        } finally {
            log.info("Закрываем консьюмер");
            consumer.close();
        }
    }

    private void sendToService(ConsumerRecord<String, EventSimilarityAvro> record) {
        EventSimilarityAvro event = record.value();
        similarityService.saveSimilarity(event);
    }
}
