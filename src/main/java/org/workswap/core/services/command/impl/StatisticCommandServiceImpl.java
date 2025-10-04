package org.workswap.core.services.command.impl;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.workswap.common.dto.stat.OnlineStatSnapshotDTO;
import org.workswap.common.dto.stat.UsersStatSnapshotDTO;
import org.workswap.common.enums.IntervalType;
import org.workswap.core.services.ReviewService;
import org.workswap.core.services.command.ListingCommandService;
import org.workswap.core.services.command.StatisticCommandService;
import org.workswap.core.services.command.UserCommandService;
import org.workswap.core.services.query.ListingQueryService;
import org.workswap.datasource.central.model.Listing;
import org.workswap.datasource.central.model.Review;
import org.workswap.datasource.central.model.User;
import org.workswap.datasource.central.repository.ReviewRepository;
import org.workswap.datasource.central.repository.listing.ListingRepository;
import org.workswap.datasource.stats.model.ListingStatSnapshot;
import org.workswap.datasource.stats.model.ListingView;
import org.workswap.datasource.stats.model.OnlineStatSnapshot;
import org.workswap.datasource.stats.model.UsersStatSnapshot;
import org.workswap.datasource.stats.repository.ListingStatRepository;
import org.workswap.datasource.stats.repository.ListingViewRepository;
import org.workswap.datasource.stats.repository.OnlineStatRepository;
import org.workswap.datasource.stats.repository.UsersStatRepository;

import lombok.RequiredArgsConstructor;

@Service
@Profile({"production", "statistic"})
@RequiredArgsConstructor
public class StatisticCommandServiceImpl implements StatisticCommandService {
    
    private final ListingRepository listingRepository;
    private final ListingStatRepository listingStatRepository;
    private final ReviewRepository reviewRepository;
    private final ListingViewRepository listingViewRepository;
    private final OnlineStatRepository onlineStatRepository;
    private final UsersStatRepository usersStatRepository;

    private final ListingQueryService listingQueryService;
    private final ListingCommandService listingCommandService;
    private final ReviewService reviewService;
    private final UserCommandService userCommandService;

    private static final Logger logger = LoggerFactory.getLogger(StatisticCommandService.class);

    @Transactional
    public void cleanUpDuplicateListingsStat() {
        List<ListingStatSnapshot> allSnapshots = listingStatRepository.findAll(Sort.by("listingId", "intervalType", "time"));

        logger.debug("Найдено снапшотов: {}", allSnapshots.size());

        Map<String, ListingStatSnapshot> seenSnapshots = new HashMap<>();
        List<ListingStatSnapshot> toDelete = new ArrayList<>();

        for (ListingStatSnapshot snapshot : allSnapshots) {
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

        listingStatRepository.deleteAll(toDelete);
        logger.debug("Удалено {} дубликатов статистики.", toDelete.size());
    }

    @Transactional
    public void saveListingsStat(IntervalType intervalType) {
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
            long count = listingStatRepository.countRecentSnapshots(listing.getId(), intervalType, since);
            if (count > 0) {
                continue; // уже есть снапшот за этот период
            }

            ListingStatSnapshot stat = new ListingStatSnapshot(
                listing.getId(),
                listing.getViews(),
                listingRepository.countFavoritesByListingId(listing.getId()),
                listing.getRating(),
                intervalType
            );

            listingStatRepository.save(stat);
        }
    }

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

    public void updateRatingForListing(Listing listing) {
        double newListingRating = calculateAverageRatingForListing(listing.getId());
        listing.setRating(newListingRating);
        listingCommandService.save(listing);
        
        updateRatingForUser(listing.getAuthor());
    }

    public void updateRatingForUser(User user) {
        double newUserRating = calculateAverageRatingForUser(user);
        user.setRating(newUserRating);
        userCommandService.save(user);
    }

    public void addListingView(Long userId, Long listingId) {
        boolean alreadyExists = true;
        if (userId != null && listingId != null) {
            System.out.println("Айди объявления: " + listingId);
            System.out.println("Айди пользователя: " + userId);
            alreadyExists = listingViewRepository.existsByUserIdAndListingId(userId, listingId);

            System.out.println("Просмотр уже существует? " + alreadyExists);

            if (alreadyExists == false) {
                ListingView newView = new ListingView(userId, listingId);
                listingViewRepository.save(newView);
                Listing listing = listingQueryService.findListing(listingId.toString());
                int views = listing.getViews();
                listing.setViews(views + 1);
                listingRepository.save(listing);
            }
        }
    }

    public void saveOnlineStatSnapshot(OnlineStatSnapshotDTO dto) {
        OnlineStatSnapshot snapshot = new OnlineStatSnapshot(
            dto.getOnline(), 
            dto.getTimestamp());
        onlineStatRepository.save(snapshot);
    }

    public void saveUsersStatSnapshot(UsersStatSnapshotDTO dto) {
        UsersStatSnapshot snapshot = new UsersStatSnapshot(
            dto.getStandartsUsers() + dto.getTempUsers(),
            dto.getStandartsUsers(),
            dto.getTimestamp());
        usersStatRepository.save(snapshot);
    }
}
