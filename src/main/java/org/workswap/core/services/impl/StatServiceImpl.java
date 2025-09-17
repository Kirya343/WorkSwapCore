package org.workswap.core.services.impl;

import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.workswap.datasource.central.model.Listing;
import org.workswap.datasource.central.model.Review;
import org.workswap.datasource.central.model.User;
import org.workswap.datasource.central.repository.listing.ListingRepository;
import org.workswap.datasource.central.repository.ResumeRepository;
import org.workswap.datasource.central.repository.ReviewRepository;
import org.workswap.datasource.central.repository.UserRepository;
import org.workswap.datasource.stats.model.StatSnapshot;
import org.workswap.datasource.stats.model.StatSnapshot.IntervalType;
import org.workswap.datasource.stats.repository.StatsRepository;
import org.workswap.core.services.ReviewService;
import org.workswap.core.services.StatService;
import org.workswap.core.services.command.ListingCommandService;
import org.workswap.core.services.command.UserCommandService;
import org.workswap.core.services.query.ListingQueryService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Profile("production")
public class StatServiceImpl implements StatService {

    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final ResumeRepository resumeRepository;
    private final StatsRepository statsRepository;
    private final ReviewRepository reviewRepository;
    
    private final ListingQueryService listingQueryService;
    private final ListingCommandService listingCommandService;
    private final ReviewService reviewService;
    private final UserCommandService userCommandService;

    private static final Logger logger = LoggerFactory.getLogger(StatService.class);

    @Override
    public int getTotalViews(User user) {
        return user.getListings().stream()
                .mapToInt(Listing::getViews)
                .sum();
    }

    @Override
    public double getUserRating(User user) {
        updateRatingForUser(user);
        return user.getRating();
    }

    @Override
    public int getMonthlyListingStats(Long listingId, int daysBack, String metric) {
        LocalDateTime dateEnd = LocalDateTime.now().minusDays(daysBack);
        LocalDateTime dateStart = LocalDateTime.now().minusDays(30 + daysBack);

        return countStats(listingId, dateStart, dateEnd, metric);
    }

    @Override
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

    @Override
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

    @Override
    @Scheduled(fixedRate = 5 * 60 * 1000) // 5 минут (5 * 60 * 1000)
    public void create5minStatSnapshot() {
        saveStat(IntervalType.FIVE_MINUTES);
    }

    @Override
    @Scheduled(fixedRate = 60 * 60 * 1000) // 5 минут (5 * 60 * 1000)
    public void createHourStatSnapshot() {
        saveStat(IntervalType.HOURLY);
    }

    @Override
    @Scheduled(cron = "0 0 0 * * *", zone = "Europe/Helsinki")
    public void createDayStatSnapshot() {
        saveStat(IntervalType.DAILY);
    }

    @Override
    @Scheduled(cron = "0 0 0 * * SUN", zone = "Europe/Helsinki")
    public void createWeekStatSnapshot() {
        saveStat(IntervalType.WEEKLY);
    }

    @Override
    @Transactional
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanUpDuplicateSnapshots() {
        List<StatSnapshot> allSnapshots = statsRepository.findAll(Sort.by("listingId", "intervalType", "time"));

        logger.debug("Найдено снапшотов: {}", allSnapshots.size());

        Map<String, StatSnapshot> seenSnapshots = new HashMap<>();
        List<StatSnapshot> toDelete = new ArrayList<>();

        for (StatSnapshot snapshot : allSnapshots) {
            String key = snapshot.getListingId() + "-" +
                         snapshot.getViews() + "-" +
                         snapshot.getFavorites() + "-" +
                         snapshot.getRating() + "-" +
                         snapshot.getIntervalType();


            if (seenSnapshots.containsKey(key)) {
                toDelete.add(snapshot); // это повтор — добавляем в список на удаление
            } else {
                seenSnapshots.put(key, snapshot); // первый встретившийся снапшот
            }
        }

        statsRepository.deleteAll(toDelete);
        logger.debug("Удалено {} дубликатов статистики.", toDelete.size());
    }

    @Transactional
    private void saveStat(IntervalType intervalType) {
        Duration checkWindow;

        // Устанавливаем окно времени для каждого интервала
        switch (intervalType) {
            case FIVE_MINUTES -> checkWindow = Duration.ofMinutes(4);
            case HOURLY -> checkWindow = Duration.ofMinutes(50);
            case DAILY -> checkWindow = Duration.ofHours(20);
            case WEEKLY -> checkWindow = Duration.ofDays(6); // для недельного (можно менять)
            default -> throw new IllegalArgumentException("Unknown interval: " + intervalType);
        }

        LocalDateTime since = LocalDateTime.now().minus(checkWindow);
        List<Listing> listings = listingRepository.findAll();

        for (Listing listing : listings) {
            long count = statsRepository.countRecentSnapshots(listing.getId(), intervalType, since);
            if (count > 0) {
                continue; // уже есть снапшот за этот период
            }

            StatSnapshot stat = new StatSnapshot();
            stat.setViews(listing.getViews());
            stat.setRating(listing.getRating());
            stat.setListingId(listing.getId());
            stat.setFavorites(listingRepository.countFavoritesByListingId(listing.getId()));
            stat.setIntervalType(intervalType);
            statsRepository.save(stat);
        }
    }

    private int countStats(Long listingId, LocalDateTime dateStart, LocalDateTime dateEnd, String metric) {
        StatSnapshot statMin = statsRepository.findMinByMetric(listingId, dateStart, dateEnd, null, metric);
        StatSnapshot statMax = statsRepository.findMaxByMetric(listingId, dateStart, dateEnd, null, metric);

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

    @Override
    public double calculateAverageRatingForListing(Long listingId) {
        List<Review> reviews = reviewRepository.findByListingIdOrderByCreatedAtDesc(listingId);

        if (reviews.isEmpty()) {
            return 0;  // Если нет отзывов, возвращаем 0
        }

        double totalRating = 0;
        for (Review review : reviews) {
            totalRating += review.getRating();
        }

        return totalRating / reviews.size();  // Средний рейтинг
    }

    @Override
    public double calculateAverageRatingForUser(User user) {
        List<Listing> listings = listingQueryService.findListingsByUser(user);  // Получаем все объявления пользователя

        List<Review> listingReviews = listings.stream()
            .flatMap(listing -> reviewService.getReviewsByListingId(listing.getId()).stream())
            .toList();

        List<Review> profileReviews = reviewService.getReviewsByProfileId(user.getId());

        List<Review> allReviews = new ArrayList<>();
        allReviews.addAll(listingReviews);
        allReviews.addAll(profileReviews);

        if (listings.isEmpty()) {
            return 0;  // Если у пользователя нет объявлений, возвращаем 0
        }

        double totalRating = 0;
        int totalReviews = 0;

        for (Review review : allReviews) {
            double rating = review.getRating();  // Получаем рейтинг для каждого объявления

            totalRating += rating;
            totalReviews++;
        }

        // Если есть хотя бы одно объявление с рейтингом больше 1, считаем средний рейтинг
        if (totalReviews > 0) {
        double average = totalRating / totalReviews;
        return Math.round(average * 10.0) / 10.0;  // округление до 1 знака после запятой
        } else {
            return 0.0;
        }
    }

    @Override
    public void updateRatingForListing(Listing listing) {
        double newListingRating = calculateAverageRatingForListing(listing.getId());
        listing.setRating(newListingRating);
        listingCommandService.save(listing);
        
        updateRatingForUser(listing.getAuthor());
    }

    @Override
    public void updateRatingForUser(User user) {
        double newUserRating = calculateAverageRatingForUser(user);
        user.setRating(newUserRating);
        userCommandService.save(user);
    }
}
