package ru.practicum.dto.compilation;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.dto.event.EventShortDto;

import java.util.Collection;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CompilationDto {
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    private Collection<EventShortDto> events;

    private boolean pinned;

    private String title;
}
