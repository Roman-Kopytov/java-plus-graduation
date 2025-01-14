package ru.practicum.config;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties("spring.kafka")
public class KafkaProperties {
    private String bootstrapServers;
    private ProducerProperties producer;
    private ConsumerProperties consumer;

    @Getter
    @Setter
    public static class ProducerProperties {
        private String keySerializer;
        private String valueSerializer;
        private String topic;
    }

    @Getter
    @Setter
    public static class ConsumerProperties {
        private String groupId;
        private String keyDeserializer;
        private String valueDeserializer;
        private String topic;
        private int pollDuration;
        private int messageFixTime;
    }
}
