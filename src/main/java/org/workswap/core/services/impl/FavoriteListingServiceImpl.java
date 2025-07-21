package org.workswap.core.services.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.workswap.core.datasource.central.model.FavoriteListing;
import org.workswap.core.datasource.central.model.Listing;
import org.workswap.core.datasource.central.model.User;
import org.workswap.core.datasource.central.repository.FavoriteListingRepository;
import org.workswap.core.services.FavoriteListingService;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteListingServiceImpl implements FavoriteListingService {

    private final FavoriteListingRepository favoriteListingRepository;

    @Override
    @Transactional
    public void toggleFavorite(User user, Listing listing) {
        if (favoriteListingRepository.existsByUserAndListing(user, listing)) {
            favoriteListingRepository.deleteByUserAndListing(user, listing);
        } else {
            FavoriteListing favoritelisting = new FavoriteListing(user, listing);
            favoriteListingRepository.save(favoritelisting);
        }
    }

    @Override
    public List<Listing> getFavoritesForUser(User user) {
        return favoriteListingRepository.findByUser(user).stream()
                .map(FavoriteListing::getListing)
                .collect(Collectors.toList());
    }

    @Override
    public void removeFromFavorites(User user, Listing listing) {
        if (favoriteListingRepository.existsByUserAndListing(user, listing)) {
            favoriteListingRepository.deleteByUserAndListing(user, listing);
        }
    }

    @Override
    public List<FavoriteListing> findByUser(User user) {
        return favoriteListingRepository.findByUser(user);
    }

    @Override
    public boolean isFavorite(User user, Listing listing) {
        return favoriteListingRepository.existsByUserAndListing(user, listing);
    }
}

