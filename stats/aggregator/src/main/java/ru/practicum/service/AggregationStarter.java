package ru.practicum.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.practicum.config.KafkaAggregatorProperties;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AggregationStarter {

    private static long PERIOD_OF_MESSAGE_FIX;
    private static long POLL_DURATION;
    private final KafkaAggregatorProperties kafkaProperties;
    private final Consumer<String, UserActionAvro> consumer;
    private final Producer<String, EventSimilarityAvro> producer;
    private final SimilarityCalcService similarityCalcService;
    private static final Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();


    @PostConstruct
    private void init() {
        POLL_DURATION = kafkaProperties.getConsumer().getPollDuration();
        PERIOD_OF_MESSAGE_FIX = kafkaProperties.getConsumer().getMessageFixTime();
    }

    public void start() {
        log.info("Aggregation started");
        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));
        try {
            consumer.subscribe(List.of(kafkaProperties.getConsumer().getTopic()));
            while (true) {
                ConsumerRecords<String, UserActionAvro> records = consumer.poll(Duration.ofMillis(POLL_DURATION));
                long count = 0;
                for (ConsumerRecord<String, UserActionAvro> record : records) {
                    handleRecord(record);
                    manageOffsets(record, count, consumer);
                    count++;
                }
                consumer.commitAsync();
            }
        } catch (WakeupException | InterruptedException ignores) {

        } catch (Exception e) {
            log.error("Ошибка во время обработки событий от датчиков", e);
        } finally {
            try {
                producer.flush();
                consumer.commitSync(currentOffsets);
            } finally {
                log.info("Закрываем консьюмер");
                consumer.close();
                log.info("Закрываем продюсер");
                producer.close();
            }
        }
    }

    private static void manageOffsets(ConsumerRecord<String, UserActionAvro> record,
                                      long count, Consumer<String, UserActionAvro> consumer) {

        currentOffsets.put(
                new TopicPartition(record.topic(), record.partition()),
                new OffsetAndMetadata(record.offset() + 1)
        );

        if (count % PERIOD_OF_MESSAGE_FIX == 0) {
            consumer.commitAsync(currentOffsets, (offsets, exception) -> {
                if (exception != null) {
                    log.warn("Ошибка во время фиксации оффсетов: {}", offsets, exception);
                }
            });
        }
    }

    private void handleRecord(ConsumerRecord<String, UserActionAvro> record) throws InterruptedException {
        UserActionAvro action = record.value();
        log.info("Обработка записи с EventId: {}", action.getEventId());
        try {
            similarityCalcService.updateUserAction(action);
            long eventId = action.getEventId();

            log.info("Начинаем вычисление схожести для события EventId: {}", eventId);

            for (long otherEventId : similarityCalcService.getEventIds()) {
                if (eventId != otherEventId) {
                    double similarity = similarityCalcService.calculateSimilarity(eventId, otherEventId);

                    long eventA = Math.min(eventId, otherEventId);
                    long eventB = Math.max(eventA, otherEventId);

                    EventSimilarityAvro eventSimilarityAvro = EventSimilarityAvro.newBuilder()
                            .setEventA(eventA)
                            .setEventB(eventB)
                            .setScore(similarity)
                            .setTimestamp(action.getTimestamp())
                            .build();

                    log.info("Отправка схожести для событий {} и {} с коэффициентом: {}", eventA, eventB, similarity);
                    send(kafkaProperties.getConsumer().getTopic(), eventSimilarityAvro);
                }
            }
        } catch (Exception e) {
            // Логируем ошибку, если она произошла
            log.error("Ошибка при обработке записи с EventId: {}", action.getEventId(), e);
            throw new InterruptedException("Ошибка при обработке записи: " + e.getMessage());
        }
        log.info("Обработка записи с EventId: {} завершена", action.getEventId());
    }


    private void send(String topic, EventSimilarityAvro snapshot) {
        ProducerRecord<String, EventSimilarityAvro> record =
                new ProducerRecord<>(topic, null, snapshot.getTimestamp().getEpochSecond(), null, snapshot);

        producer.send(record, (metadata, exception) -> {
            if (exception != null) {
                log.error("Ошибка при отправке сообщения в Kafka, topic: {}", topic, exception);
            } else {
                log.info("Сообщение успешно отправлено в Kafka, topic: {}, partition: {}, offset: {}",
                        metadata.topic(), metadata.partition(), metadata.offset());
            }
        });
    }
}
