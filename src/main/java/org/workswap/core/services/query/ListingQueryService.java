package org.workswap.core.services.query;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.workswap.common.dto.ImageDTO;
import org.workswap.common.dto.ListingDTO;
import org.workswap.common.dto.ListingTranslationDTO;
import org.workswap.datasource.central.model.Listing;
import org.workswap.datasource.central.model.User;
import org.workswap.datasource.central.model.listingModels.Category;
import org.workswap.datasource.central.model.listingModels.Location;

public interface ListingQueryService {

    Listing findListing(String param);

    Page<Listing> findListingsByCategory(String category, Pageable pageable);
    Page<Listing> findActiveListingsByCategory(String category, Pageable pageable);

    List<Listing> getRecentListings(int count);
    List<Listing> findListingsByUser(User user);
    List<Listing> findMyListings(User user);
    List<Listing> findActiveListingsByUser(User user);
    List<Listing> getAllActiveListings();
    List<Listing> getAllListings();
    List<Listing> findActiveByCommunity(String community);
    List<Listing> findActiveByCommunities(List<String> communities);
    List<Listing> findDrafts(User user);
    List<Listing> findByCategory(Category category);
    List<Listing> findByLocation(Location location);
    List<Listing> findFavoritesListingsByUser(User user);
    List<Listing> searchListings(String searchQuery);

    boolean isFavorite(User user, Listing listing);

    Page<Listing> findPageOfSortedListings(
        Category category, 
        String sortBy, 
        Pageable pageable, 
        Location location, 
        String searchQuery, 
        boolean hasReviews, 
        List<String> languages);

    List<Listing> findSortedListings(
        Category category, 
        Location location, 
        String searchQuery, 
        boolean hasReviews, 
        List<String> languages);

    List<ListingDTO> getSortedCatalogDto(
        User user, 
        String location, 
        String lang,
        int page,
        String category,
        String sortBy,
        String searchQuery,
        boolean hasReviews);

    ListingDTO getListingDTO(Long id, String locale);
    
    List<ListingDTO> getDrafts(User user, String locale);
    List<ListingDTO> getListingDtosByUser(Long id, String locale);
    List<ListingDTO> getRecentListings(int amount, String locale);
    List<ListingDTO> getFavorites(User user, String locale);
    List<Map<String, ListingTranslationDTO>> getTranslations(Long id);
    List<ImageDTO> getImages(Long id);
}