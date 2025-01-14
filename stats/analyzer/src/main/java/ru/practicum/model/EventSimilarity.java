package ru.practicum.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventSimilarity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "eventA", nullable = false)
    private long eventA;

    @Column(name = "eventB", nullable = false)
    private long eventB;

    @Column(name = "score", nullable = false)
    private double score;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

}
