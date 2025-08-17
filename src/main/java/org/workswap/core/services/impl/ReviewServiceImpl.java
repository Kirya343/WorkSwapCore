package org.workswap.core.services.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.workswap.core.services.ListingService;
import org.workswap.core.services.ReviewService;
import org.workswap.core.services.UserService;
import org.workswap.datasource.central.model.Listing;
import org.workswap.datasource.central.model.Review;
import org.workswap.datasource.central.model.User;
import org.workswap.datasource.central.repository.ReviewRepository;

import lombok.RequiredArgsConstructor;

import java.util.List;

@Service
@RequiredArgsConstructor
@Profile("production")
public class ReviewServiceImpl implements ReviewService {

    private static final Logger logger = LoggerFactory.getLogger(ReviewService.class);

    private final ReviewRepository reviewRepository;
    private final ListingService listingService;
    private final UserService userService;

    // Метод для сохранения отзыва
    @Override
    public Review saveReview(Review review) {
        return reviewRepository.save(review); // Сохраняем отзыв в базе данных
    }

    // Метод для получения всех отзывов по ID объявления
    @Override
    public List<Review> getReviewsByListingId(Long listingId) {
        return reviewRepository.findByListingIdOrderByCreatedAtDesc(listingId); // Получаем отзывы для объявления
    }

    @Override
    public List<Review> getReviewsByProfileId(Long profileId) {
        return reviewRepository.findByProfileIdOrderByCreatedAtDesc(profileId); // Получаем отзывы для объявления
    }

    @Override
    public boolean hasUserReviewedListing(User user, Listing listing) {
        return reviewRepository.existsByAuthorAndListing(user, listing);
    }

    @Override
    public boolean hasUserReviewedProfile(User user, User profile) {
        return reviewRepository.existsByAuthorAndProfile(user, profile);
    }

    @Override
    public List<Review> getReviewsByListingIdWithAuthors(Long listingId) {
        return reviewRepository.findByListingIdWithAuthors(listingId);
    }

    @Override
    public List<Review> getReviewsByProfileIdWithAuthors(Long profileId) {
        return reviewRepository.findByProfileIdWithAuthors(profileId);
    }

    @Override
    public Review createReview(Long authorId, Long profileId, Long listingId, Double rating, String text) {
        User profile = null;
        Listing listing = null;
        if (profileId != null) {
            profile = userService.findUser(profileId.toString());
        } else if (listingId != null) {
            listing = listingService.findListing(listingId.toString());
            profile = listing.getAuthor();
        } else {
            logger.debug("Не было ни профиля, ни объявления");
            return null;
        }

        if (rating == null) {
            logger.debug("Рейтинг нулевой");
            return null;
        }

        // Получаем текущего пользователя
        User author = userService.findUser(authorId.toString());

        if (author == profile) {
            logger.debug("Автор объявления оставляет отзыв сам себе");
            return null;
        }

        // Проверяем, оставлял ли пользователь уже отзыв к этому объявлению
        boolean alreadyReviewed = hasUserReviewedListing(author, listing);

        if (alreadyReviewed) {
            logger.debug("Пользователь уже оставил отзыв на это объявление");
            return null;
        }

        // Создаем новый отзыв
        Review review = new Review(text, rating, author, listing, profile);

        saveReview(review);

        logger.debug("Отзыв сохранён");
        return review;
    }

    @Override
    @Transactional
    public void deleteReview(Review review) {
        reviewRepository.delete(review);
    }
}
