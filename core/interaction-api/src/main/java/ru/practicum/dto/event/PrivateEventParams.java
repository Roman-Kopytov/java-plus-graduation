package ru.practicum.dto.event;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PrivateEventParams {
    long userId;
    int from;
    int size;
}
