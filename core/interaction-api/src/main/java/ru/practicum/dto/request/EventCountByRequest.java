package ru.practicum.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class EventCountByRequest {
    Long eventId;
    Number count;

//    public EventCountByRequest(Long eventId, Long count) {
//        this.eventId = eventId;
//        this.count = count;
//    }

}