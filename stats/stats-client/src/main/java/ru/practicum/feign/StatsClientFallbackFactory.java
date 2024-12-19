package ru.practicum.feign;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.practicum.StatsServerUnavailable;
import ru.practicum.stat.EndpointHitDTO;
import ru.practicum.stat.StatsParamsEncode;
import ru.practicum.stat.ViewStatsDTO;

import java.util.List;

@Component
public class StatsClientFallbackFactory implements FallbackFactory<StatsClient> {

    @Override
    public StatsClient create(Throwable cause) {
        return new StatsClient() {

            @Override
            public EndpointHitDTO saveStats(EndpointHitDTO hitDto) {
                throw new StatsServerUnavailable("Fallback response: сервис временно недоступен");
            }

            @Override
            public List<ViewStatsDTO> getStats(StatsParamsEncode params) {
                throw new StatsServerUnavailable("Fallback response: сервис временно недоступен");
            }
        };
    }
}
