package org.workswap.core.services.command;

import org.workswap.datasource.central.model.Listing;
import org.workswap.datasource.central.model.User;
import org.workswap.datasource.stats.model.StatSnapshot.IntervalType;

public interface StatisticCommandService {
    
    double calculateAverageRatingForUser(User user);
    double calculateAverageRatingForListing(Long listingId);

    void updateRatingForListing(Listing listing);
    void updateRatingForUser(User user);

    void cleanUpDuplicateListingsStat();

    void saveListingsStat(IntervalType intervalType);
}
