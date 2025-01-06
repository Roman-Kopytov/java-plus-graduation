package ru.practicum.feign;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.practicum.stat.EndpointHitDTO;
import ru.practicum.stat.StatsParamsEncode;
import ru.practicum.stat.ViewStatsDTO;

import java.util.List;

@FeignClient(name = "ewm-stats-server")
public interface StatsClient {


    @PostMapping("/hit")
    EndpointHitDTO saveStats(@RequestBody EndpointHitDTO hitDto);

    @CircuitBreaker(name = "myCircuitBreaker", fallbackMethod = "getDefaultStats")
    @GetMapping("/stats")
    List<ViewStatsDTO> getStats(@SpringQueryMap StatsParamsEncode params);

    default List<ViewStatsDTO> getDefaultStats(@SpringQueryMap StatsParamsEncode params, Throwable throwable) {

        return List.of();
    }
}
