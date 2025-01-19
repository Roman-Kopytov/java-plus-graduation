package ru.practicum.client.actions;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.practicum.grpc.stats.collector.UserActionControllerGrpc;

@Getter
@Setter
@Configuration
@EnableConfigurationProperties(GrpcActionsConfig.class)
@ConfigurationProperties(prefix = "grpc.client.collector")
public class GrpcActionsConfig {
    private String address;

    private boolean enableKeepAlive;

    private boolean keepAliveWithoutCalls;

    private String negotiationType;


    @Bean(name = "customUserActionClient")
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