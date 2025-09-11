package org.workswap.core.services.mapping;

import java.util.Locale;

import org.workswap.common.dto.listing.CatalogListingDTO;
import org.workswap.common.dto.listing.ListingDTO;
import org.workswap.datasource.central.model.Listing;

public interface ListingMappingService {

    ListingDTO convertToDTO(Listing listing, Locale locale);
    CatalogListingDTO convertToCatalogDTO(Listing listing, Locale locale);
}
