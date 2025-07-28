package org.workswap.core.services;

import java.util.List;

import org.workswap.datasource.central.model.listingModels.FavoriteListing;
import org.workswap.datasource.central.model.Listing;
import org.workswap.datasource.central.model.User;

public interface FavoriteListingService {

    void toggleFavorite(User user, Listing listing);

    List<Listing> getFavoritesForUser(User user);

    List<FavoriteListing> findByUser(User user);

    boolean isFavorite(User user, Listing listing);

    void deleteFavorite(User user, Listing listing);
}
