package org.workswap.core.services.util.impl;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.workswap.config.LocalisationConfig.LanguageUtils;
import org.workswap.core.services.util.ListingLocalizationService;
import org.workswap.datasource.central.model.Listing;
import org.workswap.datasource.central.model.listingModels.ListingTranslation;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Profile("production")
public class ListingLocalizationServiceImpl implements ListingLocalizationService {

    public List<Listing> localizeListings(List<Listing> listings, Locale locale) {
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
