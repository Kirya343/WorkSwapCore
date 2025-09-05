package org.workswap.core.services.util.impl;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.workswap.config.LocalisationConfig.LanguageUtils;
import org.workswap.core.services.query.ListingQueryService;
import org.workswap.core.services.util.ListingLocalizationService;
import org.workswap.datasource.central.model.Listing;
import org.workswap.datasource.central.model.User;
import org.workswap.datasource.central.model.listingModels.ListingTranslation;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Profile("production")
public class ListingLocalizationServiceImpl implements ListingLocalizationService {

    private static final Logger logger = LoggerFactory.getLogger(ListingLocalizationService.class);

    private final ListingQueryService listingQueryService;

    public List<Listing> localizeAccountListings(User user, Locale locale) {
        List<Listing> listings = listingQueryService.findListingsByUser(user);
        logger.debug("Got locale: " + locale);

        for (Listing listing : listings) {
            localizeListing(listing, locale);
        }

        logger.debug("Объявлений: " + listings.size());
        return listings;
    }

    public List<Listing> localizeActiveAccountListings(User user, Locale locale) {
        List<Listing> listings = listingQueryService.findActiveListingsByUser(user);
        logger.debug("Got locale: " + locale);

        for (Listing listing : listings) {
            localizeListing(listing, locale);
        }

        return listings;
    }

    public List<Listing> localizeFavoriteListings(User user, Locale locale) {
        List<Listing> favorites = listingQueryService.findFavoritesListingsByUser(user);

        for (Listing listing : favorites) {
            localizeListing(listing, locale);
        }

        return favorites;
    }

    public List<Listing> localizeCatalogListings(List<Listing> listings, Locale locale) {
        for (Listing listing : listings) {
            localizeListing(listing, locale);
        }

        return listings;
    }

    public void localizeListing(Listing listing, Locale locale) {
        String lang = locale.getLanguage();
        Map<String, ListingTranslation> translations = listing.getTranslations();

        ListingTranslation selected = translations.get(lang);

        // fallback, если нужного языка нет
        if (selected == null || isBlank(selected.getTitle()) || isBlank(selected.getDescription())) {
            // Приоритет: fi > ru > en
            for (String fallbackLang : LanguageUtils.SUPPORTED_LANGUAGES) {
                selected = translations.get(fallbackLang);
                if (selected != null && !isBlank(selected.getTitle()) && !isBlank(selected.getDescription())) {
                    break;
                }
            }
        }

        if (selected != null) {
            listing.setLocalizedTitle(safe(selected.getTitle()));
            listing.setLocalizedDescription(safe(selected.getDescription()));
        } else {
            listing.setLocalizedTitle(null);
            listing.setLocalizedDescription(null);
        }
    }

    private String safe(String value) {
        return (value != null && !value.isBlank()) ? value : null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
