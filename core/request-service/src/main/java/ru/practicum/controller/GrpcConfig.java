package ru.practicum.controller;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.practicum.grpc.stats.collector.UserActionControllerGrpc;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "analyzer.grpc.client.hub-router")
public class GrpcConfig {
    private String address;

    private boolean enableKeepAlive;

    private boolean keepAliveWithoutCalls;

    private String negotiationType;


    @Bean
    public UserActionControllerGrpc.UserActionControllerBlockingStub userActionsClient() {
        ManagedChannelBuilder<?> channelBuilder = ManagedChannelBuilder
                .forTarget(address)
                .keepAliveWithoutCalls(keepAliveWithoutCalls);
        if ("plaintext".equals(negotiationType)) {
            channelBuilder.usePlaintext();
        }
        ManagedChannel build = channelBuilder.build();
        return UserActionControllerGrpc.newBlockingStub(build);
    }
}