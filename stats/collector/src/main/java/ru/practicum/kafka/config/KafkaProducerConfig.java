package ru.practicum.kafka.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.Properties;

@Getter
@Setter
@Configuration
@RequiredArgsConstructor
public class KafkaProducerConfig {
    private final KafkaCollectorProperties kafkaProperties;

    @Bean
    public Producer<String, UserActionAvro> kafkaProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, kafkaProperties.getProducer().keySerializer);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, kafkaProperties.getProducer().valueSerializer);
        return new KafkaProducer<>(props);
    }
}
