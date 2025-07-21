package org.workswap.core.datasource.central.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

@Getter
@NoArgsConstructor
@Entity
public class Review {

    public Review(String text,
                  double rating,
                  User author,
                  Listing listing,
                  User profile) {
        this.text = text;
        this.rating = rating;
        this.author = author;
        this.listing = listing;
        this.profile = profile;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String text;
    private double rating;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "author_id")
    private User author;

    @ManyToOne
    @JoinColumn(name = "listing_id")
    private Listing listing;

    @ManyToOne
    @JoinColumn(name = "profile_id")
    private User profile;
}