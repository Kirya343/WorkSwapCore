package org.workswap.core.datasource.stats.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class StatSnapshot  {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long listingId;
    
    public enum IntervalType {
        FIVE_MINUTES,
        HOURLY,
        DAILY,
        WEEKLY
    }

    private int views;
    private int favorites;
    private double rating;

    @Enumerated(EnumType.STRING)
    private IntervalType intervalType;

    @CreationTimestamp
    private LocalDateTime time;

}
