package ru.practicum.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.practicum.stat.EndpointHitDTO;
import ru.practicum.stat.StatsParamsEncode;
import ru.practicum.stat.ViewStatsDTO;

import java.util.List;

@FeignClient(name = "ewm-stats-server", fallbackFactory = StatsClientFallbackFactory.class)
public interface StatsClient {

    @PostMapping("/hit")
    EndpointHitDTO saveStats(@RequestBody EndpointHitDTO hitDto);

    @GetMapping("/stats")
    List<ViewStatsDTO> getStats(@SpringQueryMap StatsParamsEncode params);
}
