package org.workswap.core.services;

import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.workswap.datasource.central.model.Listing;
import org.workswap.datasource.central.model.User;
import org.workswap.datasource.central.model.DTOs.ListingDTO;
import org.workswap.datasource.central.model.listingModels.Category;
import org.workswap.datasource.central.model.listingModels.Location;

public interface ListingService {

    //
    // Получение единичного объявления
    //
    
    // Универсальный метод поиска
    Listing findListing(String param);

    Listing getListingByIdWithAuthorAndReviews(Long id);

    //
    // Получение списка объявлений(неограниченый) (List)
    //

    // Получение по юзеру
    List<Listing> findListingsByUser(User user);
    List<Listing> findActiveListingsByUser(User user);

    // Все объявления
    List<Listing> getAllActiveListings();
    List<Listing> getAllListings();

    // Последние объявления
    List<Listing> getRecentListings(int count);

    // Для сортировки объявлений
    List<Listing> findByCategory(Category category);
    List<Listing> findByLocation(Location location);
    List<Listing> findActiveByCommunity(String community);
    List<Listing> searchListings(String searchQuery);

    // Методы для локализации объявлений пользователя в аккаунте
    List<Listing> localizeAccountListings(User user, Locale locale);
    List<Listing> localizeActiveAccountListings(User user, Locale locale);
    List<Listing> localizeFavoriteListings(User user, Locale locale);
    List<Listing> localizeCatalogListings(List<Listing> listings, Locale locale);

    List<Listing> findSortedListings(Category category, Location location, String searchQuery, boolean hasReviews, List<String> languages);

    //
    // Получение списка объявлений(ограниченый) (Page)
    //

    Page<Listing> findListingsByCategory(String category, Pageable pageable);
    Page<Listing> findActiveListingsByCategory(String category, Pageable pageable);
    Page<Listing> findPageOfSortedListings(Category category, String sortBy, Pageable pageable, Location location, String searchQuery, boolean hasReviews, List<String> languages);

    //
    // Методы обработки объявлений
    //

    void deleteListing(Long id);
    void save(Listing listing);
    void localizeListing(Listing listing, Locale locale);

    void toggleFavorite(User user, Listing listing);
    void addListingToFavorite(User user, Listing listing);
    void removeListingFromFavorite(User user, Listing listing);
    boolean isFavorite(User user, Listing listing);

    // Методы с возвратом
    Listing saveAndReturn(Listing listing);

    // Конвертация в дто для чата
    ListingDTO convertToDTO(Listing listing, Locale locale);
}
