package org.workswap.core.services.mapping.impl;

import java.util.Locale;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.workswap.common.dto.listing.CatalogListingDTO;
import org.workswap.common.dto.listing.ListingDTO;
import org.workswap.common.enums.PriceType;
import org.workswap.core.services.mapping.ListingMappingService;
import org.workswap.core.services.util.ListingLocalizationService;
import org.workswap.datasource.central.model.Listing;
import org.workswap.datasource.central.model.listingModels.Category;
import org.workswap.datasource.central.model.listingModels.Location;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Profile("production")
public class ListingMappingServiceImpl implements ListingMappingService {

    private final ListingLocalizationService listingLocalizationService;

    @Transactional
    public ListingDTO convertToDTO(Listing listing, Locale locale) {
        if (listing == null) {
            return null;
        }

        PriceType priceType = listing.getPriceType();
        Category cat = listing.getCategory();
        Location loc = listing.getLocation();

        listingLocalizationService.localizeListing(listing, locale);

        ListingDTO dto = new ListingDTO(
            listing.getId(),
            listing.getAuthor().getId(),
            listing.getLocalizedTitle(),
            listing.getLocalizedDescription(),
            listing.getPrice(),
            priceType != null ? priceType.getDisplayName() : null,
            cat != null ? cat.getName() : null,
            cat != null ? cat.getId() : null,
            loc != null ? loc.getFullName() : null,
            loc != null ? loc.getId() : null,
            listing.getRating(),
            listing.getViews(),
            listing.getCreatedAt(),
            listing.isActive(),
            listing.getImagePath(),
            listing.isTestMode(),
            listing.isTemporary()
        );

        return dto;
    }

    public CatalogListingDTO convertToCatalogDTO(Listing listing, Locale locale) {
        
        if (listing == null) {
            return null;
        }

        PriceType priceType = listing.getPriceType();
        Category cat = listing.getCategory();
        Location loc = listing.getLocation();

        listingLocalizationService.localizeListing(listing, locale);

        CatalogListingDTO dto = new CatalogListingDTO(
            listing.getId(),
            listing.getLocalizedTitle(),
            listing.getLocalizedDescription(),
            listing.getPrice(),
            priceType != null ? priceType.getDisplayName() : null,
            cat != null ? cat.getName() : null,
            loc != null ? loc.getFullName() : null,
            listing.getRating(),
            listing.getImagePath()
        );

        return dto;
    }
}
