package org.workswap.core.services.command;

import org.workswap.common.enums.IntervalType;
import org.workswap.datasource.central.model.Listing;
import org.workswap.datasource.central.model.User;

public interface StatisticCommandService {
    
    double calculateAverageRatingForUser(User user);
    double calculateAverageRatingForListing(Long listingId);

    void updateRatingForListing(Listing listing);
    void updateRatingForUser(User user);

    void cleanUpDuplicateListingsStat();

    void saveListingsStat(IntervalType intervalType);

    void addListingView(Long listingId, Long userId);
}
