package org.workswap.core.services.util;

import java.util.List;
import java.util.Locale;

import org.workswap.datasource.central.model.Listing;
import org.workswap.datasource.central.model.User;

public interface ListingLocalizationService {

    List<Listing> localizeAccountListings(User user, Locale locale);
    List<Listing> localizeActiveAccountListings(User user, Locale locale);
    List<Listing> localizeFavoriteListings(User user, Locale locale);
    List<Listing> localizeCatalogListings(List<Listing> listings, Locale locale);

    void localizeListing(Listing listing, Locale locale);
}
