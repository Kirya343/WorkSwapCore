package org.workswap.core.services.query.impl;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.workswap.core.services.query.StatisticQueryService;
import org.workswap.datasource.central.model.Listing;
import org.workswap.datasource.central.model.User;
import org.workswap.datasource.central.repository.ResumeRepository;
import org.workswap.datasource.central.repository.UserRepository;
import org.workswap.datasource.central.repository.listing.ListingRepository;
import org.workswap.datasource.stats.model.ListingStatSnapshot;
import org.workswap.datasource.stats.repository.ListingStatRepository;

import lombok.RequiredArgsConstructor;

@Service
@Profile({"production", "statistic"})
@RequiredArgsConstructor
public class StatisticQueryServiceImpl implements StatisticQueryService {

    private final ListingStatRepository listingStatRepository;
    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final ResumeRepository resumeRepository;
   
    public int getTotalViews(User user) {
        return user.getListings().stream()
                .mapToInt(Listing::getViews)
                .sum();
    }

    public int getMonthlyListingStats(Long listingId, int daysBack, String metric) {
        LocalDateTime dateEnd = LocalDateTime.now().minusDays(daysBack);
        LocalDateTime dateStart = LocalDateTime.now().minusDays(30 + daysBack);

        return countStats(listingId, dateStart, dateEnd, metric);
    }

    public Map<String, Object> getSiteStats(Locale locale) {
        Map<String, Object> stats = new HashMap<>();

        // Получаем реальные данные из репозиториев
        long usersCount = userRepository.count();
        long resumesCount = resumeRepository.countByPublishedTrue();
        long activeListingsCount = listingRepository.findListByActiveTrue().size();
        long totalViews = listingRepository.findAll().stream()
                .mapToInt(Listing::getViews)
                .sum();

        // Форматируем числа в зависимости от локали
        NumberFormat numberFormat = NumberFormat.getInstance(locale);

        stats.put("usersCount", numberFormat.format(usersCount));
        stats.put("listingsCount", numberFormat.format(activeListingsCount));
        stats.put("viewsCount", numberFormat.format(totalViews));
        stats.put("resumesCount", numberFormat.format(resumesCount));

        // Пока используем фиктивные данные для сделок
        stats.put("dealsCount", "2,000+");

        return stats;
    }

    public Map<String, Object> getUserStats(User user, Locale locale) {
        Map<String, Object> stats = new HashMap<>();

        // Получаем статистику пользователя
        int totalViews = getTotalViews(user);
        int totalResponses = 0;
        int completedDeals = 0;

        // Форматируем числа в зависимости от локали
        NumberFormat numberFormat = NumberFormat.getInstance(locale);

        stats.put("totalViews", numberFormat.format(totalViews));
        stats.put("totalResponses", numberFormat.format(totalResponses));
        stats.put("completedDeals", numberFormat.format(completedDeals));

        return stats;
    }

    private int countStats(Long listingId, LocalDateTime dateStart, LocalDateTime dateEnd, String metric) {
        ListingStatSnapshot statMin = listingStatRepository.findMinByMetric(listingId, dateStart, dateEnd, null, metric);
        ListingStatSnapshot statMax = listingStatRepository.findMaxByMetric(listingId, dateStart, dateEnd, null, metric);

        if (statMin == null || statMax == null) {
            return 0;
        }

        switch (metric) {
            case "views":
                return statMax.getViews() - statMin.getViews();
            case "favorites":
                return statMax.getFavorites() - statMin.getFavorites();
            case "rating":
                // Assuming rating is a double, you may want to return (int) or change return type
                return (int) (statMax.getRating() - statMin.getRating());
            default:
                throw new IllegalArgumentException("Unknown metric: " + metric);
        }
    }
}