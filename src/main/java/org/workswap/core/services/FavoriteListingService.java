package org.workswap.core.services;

import java.util.List;

import org.workswap.core.datasource.central.model.FavoriteListing;
import org.workswap.core.datasource.central.model.Listing;
import org.workswap.core.datasource.central.model.User;

public interface FavoriteListingService {

    void toggleFavorite(User user, Listing listing);

    List<Listing> getFavoritesForUser(User user);

    void removeFromFavorites(User user, Listing listing);

    List<FavoriteListing> findByUser(User user);

    boolean isFavorite(User user, Listing listing);
}
