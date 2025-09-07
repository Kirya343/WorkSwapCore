package org.workswap.core.services.util;

import java.util.List;
import java.util.Locale;

import org.workswap.datasource.central.model.Listing;

public interface ListingLocalizationService {

    List<Listing> localizeListings(List<Listing> listings, Locale locale);

    void localizeListing(Listing listing, Locale locale);
}
