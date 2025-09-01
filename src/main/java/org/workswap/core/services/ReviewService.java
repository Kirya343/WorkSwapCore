package org.workswap.core.services;

import java.util.List;

import org.workswap.common.dto.ReviewDTO;
import org.workswap.datasource.central.model.Listing;
import org.workswap.datasource.central.model.Review;
import org.workswap.datasource.central.model.User;

public interface ReviewService {

    Review saveReview(Review review);
    List<Review> getReviewsByListingId(Long listingId);
    List<Review> getReviewsByProfileId(Long profileId);
    List<Review> getReviewsByListingIdWithAuthors(Long listingId);
    List<Review> getReviewsByProfileIdWithAuthors(Long profileId);

    boolean hasUserReviewedListing(User user, Listing listing);
    boolean hasUserReviewedProfile(User user, User profile);

    Review createReview(Long authorId, Long profileId, Long listingId, Double rating, String text);
    void deleteReview(Review review);

    ReviewDTO convertToDTO(Review review);
}
