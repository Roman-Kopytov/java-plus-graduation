package ru.practicum.stat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StatsParamsEncode {
    private String start;
    private String end;
    private List<String> uris;
    private Boolean unique;
}