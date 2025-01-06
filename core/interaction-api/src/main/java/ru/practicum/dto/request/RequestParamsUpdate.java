package ru.practicum.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestParamsUpdate {

    private long userId;
    private long eventId;
    private EventRequestStatusUpdateRequest dto;

}

