package org.workswap.core.datasource.central.model.listingModels;

import org.workswap.core.datasource.central.model.Listing;

import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity
public class ListingTranslation {

    public ListingTranslation(String language,
                              String title,
                              String description,
                              Listing listing) {
        this.language = language;
        this.title = title;
        this.description = description;
        this.listing = listing;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String language;

    private String title;
    @Column(length = 2000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id")
    private Listing listing;
}