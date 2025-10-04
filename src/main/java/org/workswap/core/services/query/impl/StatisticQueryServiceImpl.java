package org.workswap.core.services.query.impl;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.workswap.common.dto.analytic.OnlineStatsMetricsDTO;
import org.workswap.core.services.query.StatisticQueryService;
import org.workswap.datasource.central.model.Listing;
import org.workswap.datasource.central.model.User;
import org.workswap.datasource.central.repository.ResumeRepository;
import org.workswap.datasource.central.repository.UserRepository;
import org.workswap.datasource.central.repository.listing.ListingRepository;
import org.workswap.datasource.stats.model.ListingStatSnapshot;
import org.workswap.datasource.stats.model.OnlineStatSnapshot;
import org.workswap.datasource.stats.repository.ListingStatRepository;
import org.workswap.datasource.stats.repository.OnlineStatRepository;

import lombok.RequiredArgsConstructor;

@Service
@Profile({"production", "statistic"})
@RequiredArgsConstructor
public class StatisticQueryServiceImpl implements StatisticQueryService {

    private final ListingStatRepository listingStatRepository;
    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final ResumeRepository resumeRepository;
    private final OnlineStatRepository onlineStatRepository;
   
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

    public int getLastOnlineSnapshot() {
        OnlineStatSnapshot snapshot = onlineStatRepository.findFirstByOrderByTimestampDesc();
        return snapshot.getOnline();
    }

    public OnlineStatsMetricsDTO getMonthlyMetrics() {
        List<OnlineStatSnapshot> snapshots = onlineStatRepository.findByTimestampAfter(LocalDateTime.now().minusMonths(1));

        if (snapshots.isEmpty()) {
            return new OnlineStatsMetricsDTO(); // или кинуть exception
        }

        // Достаём список значений онлайна
        List<Integer> values = snapshots.stream()
                .map(OnlineStatSnapshot::getOnline)
                .sorted()
                .toList();

        IntSummaryStatistics stats = values.stream()
                .mapToInt(Integer::intValue)
                .summaryStatistics();

        int min = stats.getMin();
        int max = stats.getMax();
        double avg = stats.getAverage();

        // Медиана
        double median;
        int size = values.size();
        if (size % 2 == 0) {
            median = (values.get(size / 2 - 1) + values.get(size / 2)) / 2.0;
        } else {
            median = values.get(size / 2);
        }

        // p95
        int p95Index = (int) Math.ceil(0.95 * size) - 1;
        double p95 = values.get(Math.max(p95Index, 0));

        // Стандартное отклонение
        double variance = values.stream()
                .mapToDouble(v -> Math.pow(v - avg, 2))
                .sum() / size;
        double stdDev = Math.sqrt(variance);

        // Сумма человеко-часов (онлайн * время)
        // один снапшот = 15 секунд
        int totalUserSeconds = values.stream()
                .mapToInt(Integer::intValue)
                .sum() * 15;
        int totalUserHours = totalUserSeconds / 3600;

        Map<LocalDate, Double> avgByDay = snapshots.stream()
                .collect(Collectors.groupingBy(
                        snap -> snap.getTimestamp().toLocalDate(),
                        Collectors.averagingInt(OnlineStatSnapshot::getOnline)
                ));

        LocalDate peakDay = avgByDay.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        // 2. Час пика (по среднему за час суток)
        Map<Integer, Double> avgByHour = snapshots.stream()
                .collect(Collectors.groupingBy(
                        snap -> snap.getTimestamp().getHour(),
                        Collectors.averagingInt(OnlineStatSnapshot::getOnline)
                ));

        Integer peakHour = avgByHour.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        return new OnlineStatsMetricsDTO(
                min,
                max,
                avg,
                median,
                p95,
                stdDev,
                totalUserHours,
                peakDay,
                peakHour
        );
    }
}