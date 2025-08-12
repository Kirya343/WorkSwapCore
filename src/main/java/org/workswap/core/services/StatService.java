package org.workswap.core.services;

import java.util.Locale;
import java.util.Map;

import org.workswap.datasource.central.model.Listing;
import org.workswap.datasource.central.model.User;

public interface StatService {
    int getTotalViews(User user);
    double getUserRating(User user);

    Map<String, Object> getSiteStats(Locale locale);
    Map<String, Object> getUserStats(User user, Locale locale);

    int getMonthlyListingStats(Long listingId, int daysBack, String metric);

    double calculateAverageRatingForUser(User user);
    double calculateAverageRatingForListing(Long listingId);

    void updateRatingForListing(Listing listing);
    void updateRatingForUser(User user);

    void cleanUpDuplicateSnapshots();

    void create5minStatSnapshot();
    void createHourStatSnapshot();
    void createDayStatSnapshot();
    void createWeekStatSnapshot();
}
