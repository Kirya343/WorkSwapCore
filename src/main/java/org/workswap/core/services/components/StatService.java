package org.workswap.core.services.components;

import java.util.Locale;
import java.util.Map;

import org.workswap.core.datasource.main.model.Listing;
import org.workswap.core.datasource.main.model.User;

public interface StatService {
    int getTotalViews(User user);
    double getAverageRating(User user);

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
