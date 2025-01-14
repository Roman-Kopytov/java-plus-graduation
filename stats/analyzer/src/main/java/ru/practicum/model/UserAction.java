package ru.practicum.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserAction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "userId", nullable = false)
    private long userId;


    @Column(name = "eventId", nullable = false)
    private long eventId;

    @Column(name = "actionType", nullable = false)
    @Enumerated(EnumType.STRING)
    private ActionType actionType;

    @Column(name = "maxWeight", nullable = false)
    private double maxWeight;
//тут не знаю какой подход применить, либо хранить в бд
//    либо доставать через Enum или через метод с switch
//     в идеале, что бы коэфициенты в одном месте были записаны и сервис Aggregator еще мог к ним доступ иметь
//    если в одном месте поменяется, то и в другом.

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

}
