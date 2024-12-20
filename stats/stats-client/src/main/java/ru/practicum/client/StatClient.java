package ru.practicum.client;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.feign.StatsClient;
import ru.practicum.stat.EndpointHitDTO;
import ru.practicum.stat.StatsParams;
import ru.practicum.stat.StatsParamsEncode;
import ru.practicum.stat.ViewStatsDTO;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;


@Slf4j
@Component
@RequiredArgsConstructor
public class StatClient {

    private final StatsClient statsClient;

    public void saveStats(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        String uri = request.getRequestURI();
        EndpointHitDTO dto = EndpointHitDTO.builder()
                .app("ewm-main-service")
                .ip(ip)
                .uri(uri)
                .timestamp(LocalDateTime.now())
                .build();

        statsClient.saveStats(dto);
    }

    public List<ViewStatsDTO> getStats(StatsParams params) {
        StatsParamsEncode paramsEncode = new StatsParamsEncode();
        paramsEncode.setUnique(params.getUnique());
        paramsEncode.setUris(params.getUris());
        paramsEncode.setEnd(encodeDate(params.getEnd()));
        paramsEncode.setStart(encodeDate(params.getStart()));
        return statsClient.getStats(paramsEncode);
    }

    private String encodeDate(LocalDateTime date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return date.format(formatter);
    }
}

