package ru.practicum.client.recommendation;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.practicum.grpc.stats.collector.RecommendationsControllerGrpc;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "grpc.client.analyzer")
public class GrpcSimilarityConfig {
    private String address;

    private boolean enableKeepAlive;

    private boolean keepAliveWithoutCalls;

    private String negotiationType;

    @Bean(name = "customRecommendationClient")
    public RecommendationsControllerGrpc.RecommendationsControllerBlockingStub recommendationClient() {
        ManagedChannelBuilder<?> channelBuilder = ManagedChannelBuilder
                .forTarget(address)
                .keepAliveWithoutCalls(keepAliveWithoutCalls);
        if ("plaintext".equals(negotiationType)) {
            channelBuilder.usePlaintext();
        }
        ManagedChannel build = channelBuilder.build();
        return RecommendationsControllerGrpc.newBlockingStub(build);
    }
}