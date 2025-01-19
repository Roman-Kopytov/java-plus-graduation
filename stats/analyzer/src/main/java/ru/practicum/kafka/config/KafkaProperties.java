package ru.practicum.kafka.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties("spring.kafka.consumer")
public class KafkaProperties {
    private String bootstrapServers;
    private long pollDuration;
    private int messageFixTime;
    private Actions actions;
    private Events events;

    @Getter
    @Setter
    public static class ConsumerFields {
        private String bootstrapServers;
        private String groupId;
        private String keyDeserializer;
        private String valueDeserializer;
        private String topic;
    }

    @Getter
    @Setter
    public static class Actions extends ConsumerFields {
    }

    @Getter
    @Setter
    public static class Events extends ConsumerFields {
    }
}
