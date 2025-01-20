package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaActionProducer {
    private final Producer<String, UserActionAvro> kafkaProducer;

    public void send(String topic, UserActionAvro action) {
        ProducerRecord<String, UserActionAvro> record =
                new ProducerRecord<>(topic, null, Instant.now().getEpochSecond(), null, action);
        log.info("Отправка события в топик {} с EventId: {}", topic, action.getEventId());
        kafkaProducer.send(record, (metadata, exception) -> {
            if (exception != null) {
                log.error("Ошибка при отправке сообщения в Kafka, topic: {}", topic, exception);
            } else {
                log.info("Сообщение успешно отправлено в Kafka, topic: {}, partition: {}, offset: {}",
                        metadata.topic(), metadata.partition(), metadata.offset());
            }
        });
    }
}
