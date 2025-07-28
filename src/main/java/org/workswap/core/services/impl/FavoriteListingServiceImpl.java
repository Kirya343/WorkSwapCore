package org.workswap.core.services.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.workswap.datasource.central.model.listingModels.FavoriteListing;
import org.workswap.datasource.central.model.Listing;
import org.workswap.datasource.central.model.User;
import org.workswap.datasource.central.repository.FavoriteListingRepository;
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
            deleteFavorite(user, listing);
            System.out.println("Объявление уже было в избранных пользователя " + user.getName());
        } else {
            FavoriteListing favoritelisting = new FavoriteListing(user, listing);
            System.out.println("Пользователь " + user.getName() + " добавил объявление в избранное");
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
    public List<FavoriteListing> findByUser(User user) {
        return favoriteListingRepository.findByUser(user);
    }

    @Override
    public boolean isFavorite(User user, Listing listing) {
        return favoriteListingRepository.existsByUserAndListing(user, listing);
    }

    @Override
    @Transactional
    public void deleteFavorite(User user, Listing listing) {
        FavoriteListing favorite = favoriteListingRepository.findByUserAndListing(user, listing);
        System.out.println("Удаляем из избранных: " + favorite.getId());
        favoriteListingRepository.deleteById(favorite.getId());
        favoriteListingRepository.flush();
        System.out.println("Метод удаления сработал: " + favorite.getId());

        if (isFavorite(user, listing)) {
            System.out.println("Удаление не удалось");
        } else {
            System.out.println("Успешно удалено");
        }
    }
}

