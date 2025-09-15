package org.workswap.core.services.query.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.workswap.common.dto.listing.CatalogListingDTO;
import org.workswap.common.dto.listing.ImageDTO;
import org.workswap.common.dto.listing.ListingDTO;
import org.workswap.common.dto.listing.ListingTranslationDTO;
import org.workswap.common.enums.SearchModelParamType;
import org.workswap.core.services.CategoryService;
import org.workswap.core.services.LocationService;
import org.workswap.core.services.components.ServiceUtils;
import org.workswap.core.services.mapping.ListingMappingService;
import org.workswap.core.services.query.ListingQueryService;
import org.workswap.core.services.util.ListingLocalizationService;
import org.workswap.datasource.central.model.Listing;
import org.workswap.datasource.central.model.User;
import org.workswap.datasource.central.model.listingModels.Category;
import org.workswap.datasource.central.model.listingModels.Location;
import org.workswap.datasource.central.repository.listing.ListingRepository;
import org.workswap.datasource.central.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Profile("production")
public class ListingQueryServiceImpl implements ListingQueryService {

    private static final Logger logger = LoggerFactory.getLogger(ListingQueryService.class);

    private final ListingRepository listingRepository;
    private final UserRepository userRepository;

    private final ServiceUtils serviceUtils;

    private final CategoryService categoryService;
    private final LocationService locationService;
    private final ListingMappingService mappingService;
    private final ListingLocalizationService localizationService;

    private Listing findListingFromRepostirory(String param, SearchModelParamType paramType) {
        switch (paramType) {
            case ID:
                return listingRepository.findById(Long.parseLong(param)).orElse(null);
            default:
                throw new IllegalArgumentException("Unknown param type: " + paramType);
        }
    }

    public Listing findListing(String param) {
        SearchModelParamType paramType = serviceUtils.detectParamType(param);
        return findListingFromRepostirory(param, paramType);
    }

    public List<Listing> getRecentListings(int count) {
        Pageable pageable = PageRequest.of(0, count);
        return listingRepository.findAllByOrderByCreatedAtDesc(pageable).getContent();
    }

    public List<Listing> findListingsByUser(User user) {
        return listingRepository.findByAuthor(user);
    }

    public List<Listing> findMyListings(User user) {
        return listingRepository.findByAuthorAndTemporary(user, false);
    }

    public List<Listing> findActiveListingsByUser(User user) {
        return listingRepository.findByAuthorAndActiveTrue(user);
    } 

    public List<Listing> getAllActiveListings() {
        return listingRepository.findListByActiveTrue(); // Предполагая, что у вас есть поле `active` в сущности
    }

    public List<Listing> getAllListings() {
        return listingRepository.findAll(); // Просто получаем все объявления
    }

    public List<Listing> findActiveByCommunity(String community) {
        List<Listing> listings = listingRepository.findByCommunityAndActiveTrue(community.toLowerCase());
        logger.debug("Найдены активные объявления: " + listings.size());
        return listings;
    }

    public List<Listing> findActiveByCommunities(List<String> communities) {
        List<Listing> listings = listingRepository.findByCommunitiesInAndActiveTrue(communities);
        logger.debug("Найдены активные объявления: " + listings.size());

        return listings;
    }

    public List<Listing> findDrafts(User user) {
        return listingRepository.findByAuthorAndTemporary(user, true); 
    }

    public List<Listing> findByCategory(Category category) {
        List<Category> categories = categoryService.getAllDescendants(category);
        List<Listing> listings = listingRepository.findByCategories(categories);
        logger.debug("Найдены объявления: " + listings.size());
        return listings;
    }

    public List<Listing> findByLocation(Location location) {
        List<Location> locations = locationService.getAllDescendants(location);
        List<Listing> listings = listingRepository.findByLocations(locations);
        logger.debug("Найдены объявления: " + listings.size());
        return listings;
    }

    public List<Listing> findFavoritesListingsByUser(User user) {
        return new ArrayList<>(userRepository.findById(user.getId()).orElse(null).getFavoriteListings());
    }

    public boolean isFavorite(User user, Listing listing) {
        return user.getFavoriteListings().contains(listing);
    }

    public List<CatalogListingDTO> getSortedCatalogDto(
        User user, 
        String location, 
        String lang,
        int page,
        Long categoryId,
        String sortBy,
        String searchQuery,
        boolean hasReviews
    ) {
        List<String> languages = new ArrayList<>();

        if (user != null) {
            languages = user.getLanguages();
        }

        if (!languages.contains(lang)) {
            languages.add(lang);
        }

        List<Category> categories = categoryService.getAllDescendantsById(categoryId);

        Comparator<Listing> comparator;
        switch (sortBy) {
            case "price":
                comparator = Comparator.comparing(Listing::getPrice, Comparator.nullsLast(Comparator.naturalOrder()));
                break;
            case "rating":
                comparator = Comparator.comparing(Listing::getRating, Comparator.nullsLast(Comparator.reverseOrder()));
                break;
            case "popularity":
                comparator = Comparator.comparing(Listing::getViews, Comparator.nullsLast(Comparator.reverseOrder()));
                break;
            case "date":
            default:
                comparator = Comparator.comparing(Listing::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
                break;
        }

        List<Listing> foundListings = listingRepository.findListings(categories, location, searchQuery, hasReviews, languages, page, 12);

        foundListings.sort(comparator);

        List<CatalogListingDTO> listings = new ArrayList<>();
        for(Listing l : foundListings) {
            listings.add(mappingService.convertToCatalogDTO(l, Locale.of(lang)));
        }

        return listings;
    }

    public List<ListingDTO> getDrafts(User user, String locale) {
        return findDrafts(user)
            .stream()
            .map(listing -> mappingService.convertToDTO(listing, Locale.of(locale)))
            .collect(Collectors.toList());
    }

    public List<ListingDTO> getRecentListings(int amount, String locale) {
        return getRecentListings(amount)
            .stream()
            .map(listing -> mappingService.convertToDTO(listing, Locale.of(locale)))
            .collect(Collectors.toList());
    }

    public List<ListingDTO> getListingDtosByUser(Long id, String locale) {
        User user = userRepository.findById(id).orElse(null);

        return findListingsByUser(user)
            .stream()
            .map(listing -> mappingService.convertToDTO(listing, Locale.of(locale)))
            .collect(Collectors.toList());
    }

    public List<ListingDTO> getFavorites(User user, String locale) {
        return findFavoritesListingsByUser(user)
            .stream()
            .map(listing -> mappingService.convertToDTO(listing, Locale.of(locale)))
            .collect(Collectors.toList());
    }

    public Map<String, ListingTranslationDTO> getTranslations(Long id) {
        Listing listing = listingRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Listing not found"));
        
        return listing.getTranslations().entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> new ListingTranslationDTO(
                    entry.getValue().getTitle(), 
                    entry.getValue().getDescription()
                )
            ));
    }

    public List<ImageDTO> getImages(Long id) {
        return findListing(id.toString())
            .getImages()
            .stream()
            .map(image -> new ImageDTO(image.getId(), id, image.getPath()))
            .collect(Collectors.toList());
    }

    public List<Listing> localizeAccountListings(User user, Locale locale) {
        List<Listing> listings = findListingsByUser(user);
        logger.debug("Got locale: " + locale);

        for (Listing listing : listings) {
            localizationService.localizeListing(listing, locale);
        }

        logger.debug("Объявлений: " + listings.size());
        return listings;
    }

    public List<Listing> localizeActiveAccountListings(User user, Locale locale) {
        List<Listing> listings = findActiveListingsByUser(user);
        logger.debug("Got locale: " + locale);

        for (Listing listing : listings) {
            localizationService.localizeListing(listing, locale);
        }

        return listings;
    }

    public List<Listing> localizeFavoriteListings(User user, Locale locale) {
        List<Listing> favorites = findFavoritesListingsByUser(user);

        for (Listing listing : favorites) {
            localizationService.localizeListing(listing, locale);
        }

        return favorites;
    }

    public ListingDTO getListingDTO(Long id, String locale) {
        return mappingService.convertToDTO(findListing(id.toString()), Locale.of(locale));
    }
}