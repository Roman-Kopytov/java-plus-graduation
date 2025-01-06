package ru.practicum.dto.location;

import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LocationDto {

    private long id;
    private float lat;
    private float lon;
}
