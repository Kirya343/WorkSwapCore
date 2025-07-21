package org.workswap.core.datasource.central.model.listingModels;

import org.workswap.core.datasource.central.model.Listing;

import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity
public class Image {

    public Image(String path,
                 Listing listing) {
        this.path = path;
        this.listing = listing;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String path;

    @ManyToOne
    @JoinColumn(name = "listing_id")
    private Listing listing;
}