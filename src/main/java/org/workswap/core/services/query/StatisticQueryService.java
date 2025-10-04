package org.workswap.core.services.query;

import java.util.Locale;
import java.util.Map;

import org.workswap.common.dto.analytic.OnlineStatsMetricsDTO;
import org.workswap.datasource.central.model.User;

public interface StatisticQueryService {
    
    int getTotalViews(User user);

    Map<String, Object> getSiteStats(Locale locale);
    Map<String, Object> getUserStats(User user, Locale locale);

    int getMonthlyListingStats(Long listingId, int daysBack, String metric);

    int getLastOnlineSnapshot();
    OnlineStatsMetricsDTO getMonthlyMetrics();
}
