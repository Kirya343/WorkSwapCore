package org.workswap.core.datasource.central.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.workswap.core.datasource.central.model.FavoriteListing;
import org.workswap.core.datasource.central.model.Listing;
import org.workswap.core.datasource.central.model.User;

import java.util.List;

public interface FavoriteListingRepository extends JpaRepository<FavoriteListing, Long> {
    List<FavoriteListing> findByUser(User user);
    boolean existsByUserAndListing(User user, Listing listing);
    void deleteByUserAndListing(User user, Listing listing);
}
