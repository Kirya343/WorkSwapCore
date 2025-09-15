package org.workswap.core.services.query;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.workswap.common.dto.listing.CatalogListingDTO;
import org.workswap.common.dto.listing.ImageDTO;
import org.workswap.common.dto.listing.ListingDTO;
import org.workswap.common.dto.listing.ListingTranslationDTO;
import org.workswap.datasource.central.model.Listing;
import org.workswap.datasource.central.model.User;
import org.workswap.datasource.central.model.listingModels.Category;
import org.workswap.datasource.central.model.listingModels.Location;

public interface ListingQueryService {

    Listing findListing(String param);

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
    List<Listing> localizeAccountListings(User user, Locale locale);
    List<Listing> localizeActiveAccountListings(User user, Locale locale);
    List<Listing> localizeFavoriteListings(User user, Locale locale);

    boolean isFavorite(User user, Listing listing);

    List<CatalogListingDTO> getSortedCatalogDto(
        User user, 
        String location, 
        String lang,
        int page,
        Long categoryId,
        String sortBy,
        String searchQuery,
        boolean hasReviews);

    ListingDTO getListingDTO(Long id, String locale);
    
    List<ListingDTO> getDrafts(User user, String locale);
    List<ListingDTO> getListingDtosByUser(Long id, String locale);
    List<ListingDTO> getRecentListings(int amount, String locale);
    List<ListingDTO> getFavorites(User user, String locale);
    Map<String, ListingTranslationDTO> getTranslations(Long id);
    List<ImageDTO> getImages(Long id);
}