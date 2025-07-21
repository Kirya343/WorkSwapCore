package org.workswap.core.services;

import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.workswap.core.datasource.central.model.Listing;
import org.workswap.core.datasource.central.model.Review;
import org.workswap.core.datasource.central.model.User;
import org.workswap.core.datasource.central.repository.ReviewRepository;

import lombok.RequiredArgsConstructor;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ListingService listingService;
    private final UserService userService;

    // Метод для сохранения отзыва
    public Review saveReview(Review review) {
        return reviewRepository.save(review); // Сохраняем отзыв в базе данных
    }

    // Метод для получения всех отзывов по ID объявления
    public List<Review> getReviewsByListingId(Long listingId) {
        return reviewRepository.findByListingIdOrderByCreatedAtDesc(listingId); // Получаем отзывы для объявления
    }

    public List<Review> getReviewsByProfileId(Long profileId) {
        return reviewRepository.findByProfileIdOrderByCreatedAtDesc(profileId); // Получаем отзывы для объявления
    }

    public boolean hasUserReviewedListing(User user, Listing listing) {
        return reviewRepository.existsByAuthorAndListing(user, listing);
    }

    public boolean hasUserReviewedProfile(User user, User profile) {
        return reviewRepository.existsByAuthorAndProfile(user, profile);
    }

    public List<Review> getReviewsByListingIdWithAuthors(Long listingId) {
        return reviewRepository.findByListingIdWithAuthors(listingId);
    }

    public List<Review> getReviewsByProfileIdWithAuthors(Long profileId) {
        return reviewRepository.findByProfileIdWithAuthors(profileId);
    }

    public void createReview(OAuth2User oauth2User, Long profileId, Long listingId, Double rating, String text) {
        User profile = null;
        Listing listing = null;
        if (profileId != null) {
            profile = userService.findUser(profileId.toString());
        } else if (listingId != null) {
            listing = listingService.getListingById(listingId);
            profile = listing.getAuthor();
        } else {
            return;
        }

        // Получаем текущего пользователя
        User user = userService.findUserFromOAuth2(oauth2User);

        if (user == profile) {
            return;
        }

        // Проверяем, оставлял ли пользователь уже отзыв к этому объявлению
        boolean alreadyReviewed = hasUserReviewedListing(user, listing);
        if (alreadyReviewed) {
            return;
        }

        // Создаем новый отзыв
        Review review = new Review(text, rating, user, listing, profile);

        saveReview(review);
    }
}
