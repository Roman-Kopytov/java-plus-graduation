package ru.practicum.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties("spring.kafka")
public class KafkaProperties {
    private String bootstrapServers;
    private Producer producer;
    @Value("${collector.kafka.topics.userActions}")
    private String userActionsTopic;

    @Getter
    @Setter
    public static class Producer {
        String keySerializer;
        String valueSerializer;
    }
}