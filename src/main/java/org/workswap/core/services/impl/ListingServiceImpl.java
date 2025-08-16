package org.workswap.core.services.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
import org.workswap.config.LocalisationConfig.LanguageUtils;
import org.workswap.datasource.central.model.Listing;
import org.workswap.datasource.central.model.User;
import org.workswap.common.dto.ListingDTO;
import org.workswap.datasource.central.model.chat.Chat;
import org.workswap.common.enums.SearchModelParamType;
import org.workswap.datasource.central.model.listingModels.Category;
import org.workswap.datasource.central.model.listingModels.ListingTranslation;
import org.workswap.datasource.central.model.listingModels.Location;
import org.workswap.datasource.central.repository.chat.ChatRepository;
import org.workswap.datasource.central.repository.ListingRepository;
import org.workswap.datasource.central.repository.UserRepository;
import org.workswap.datasource.stats.model.StatSnapshot;
import org.workswap.datasource.stats.repository.StatsRepository;
import org.workswap.core.services.CategoryService;
import org.workswap.core.services.ListingService;
import org.workswap.core.services.LocationService;
import org.workswap.core.services.components.ServiceUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ListingServiceImpl implements ListingService {

    private final ListingRepository listingRepository;
    private final ChatRepository chatRepository;
    private final CategoryService categoryService;
    private final LocationService locationService;
    private final UserRepository userRepository;
    private final ServiceUtils serviceUtils;
    private final StatsRepository statsRepository;

    private static final Logger logger = LoggerFactory.getLogger(ListingService.class);
    
    private Listing findListingFromRepostirory(String param, SearchModelParamType paramType) {
        switch (paramType) {
            case ID:
                return listingRepository.findById(Long.parseLong(param)).orElse(null);
            default:
                throw new IllegalArgumentException("Unknown param type: " + paramType);
        }
    }

    @Override
    public Listing findListing(String param) {
        SearchModelParamType paramType = serviceUtils.detectParamType(param);
        return findListingFromRepostirory(param, paramType);
    }

    @Override
    public Page<Listing> findListingsByCategory(String category, Pageable pageable) {
        return listingRepository.findByCategory(category, pageable);
    }

    @Override
    public List<Listing> getRecentListings(int count) {
        Pageable pageable = PageRequest.of(0, count);
        return listingRepository.findAllByOrderByCreatedAtDesc(pageable).getContent();
    }


    @Override
    public List<Listing> findListingsByUser(User user) {
        return listingRepository.findByAuthor(user);
    }

    @Override
    public List<Listing> findActiveListingsByUser(User user) {
        return listingRepository.findByAuthorAndActiveTrue(user);
    } 

    @Override
    public void save(Listing listing) {
        listingRepository.save(listing);
    }
    
    @Override
    public Listing saveAndReturn(Listing listing) {
        return listingRepository.save(listing);
    }

    @Override
    public Page<Listing> findActiveListingsByCategory(String category, Pageable pageable) {
        return listingRepository.findActiveByCategory(category, pageable);
    }

    @Override
    public List<Listing> getAllActiveListings() {
        return listingRepository.findListByActiveTrue(); // Предполагая, что у вас есть поле `active` в сущности
    }

    @Override
    public List<Listing> getAllListings() {
        return listingRepository.findAll(); // Просто получаем все объявления
    }

    // Новый метод с JOIN FETCH (оптимизированный)
    @Override
    public Listing getListingByIdWithAuthorAndReviews(Long id) {
        return listingRepository.findByIdWithAuthorAndReviews(id).orElse(null);
    }

    @Override
    @Transactional
    public void deleteListing(Listing listing) {

        logger.debug("Начинаем удаление объявления {}", listing.getId());

        // Обнуляем связь listing у всех chat
        List<Chat> chats = chatRepository.findAllByListing(listing);

        logger.debug("Обнуляем свясь с объявлением у чатов");

        for (Chat chat : chats) {
            chat.setListing(null);
        }
        chatRepository.saveAll(chats);

        logger.debug("Чистим статистику объявления");

        clearStatSnapshots(listing);

        logger.debug("Удаляем объявление");

        // Теперь можно удалить объявление
        listingRepository.delete(listing);
    }

    @Override
    public List<Listing> findByCategory(Category category) {
        List<Category> categories = categoryService.getAllDescendants(category);
        List<Listing> listings = listingRepository.findByCategories(categories);
        logger.debug("Найдены объявления: " + listings.size());
        return listings;
    }

    @Override
    public List<Listing> findByLocation(Location location) {
        List<Location> locations = locationService.getAllDescendants(location);
        List<Listing> listings = listingRepository.findByLocations(locations);
        logger.debug("Найдены объявления: " + listings.size());
        return listings;
    }

    @Override
    public List<Listing> findActiveByCommunity(String community) {
        List<Listing> listings = listingRepository.findByCommunityAndActiveTrue(community.toLowerCase());
        logger.debug("Найдены активные объявления: " + listings.size());
        return listings;
    }

    @Override
    public List<Listing> findActiveByCommunities(List<String> communities) {
        List<Listing> listings = listingRepository.findByCommunitiesInAndActiveTrue(communities);
        logger.debug("Найдены активные объявления: " + listings.size());
        return listings;
    }

    @Override
    public Page<Listing> findPageOfSortedListings(Category category, String sortBy, Pageable pageable, Location location, String searchQuery, boolean hasReviews, List<String> languages) {

        List<Listing> filteredListings = findSortedListings(category, location, searchQuery, hasReviews, languages);
        logger.debug("[GetListingsSorted] Языки для поиска: " + languages);

        // Выбор компаратора для сортировки
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

        // Применяем сортировку
        filteredListings.sort(comparator);

        // Пагинация
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filteredListings.size());

        List<Listing> pageContent = (start <= end) ? filteredListings.subList(start, end) : List.of();
        logger.debug("Отфильтрованные и отсортированные объявления: " + pageContent.size());

        return new PageImpl<>(pageContent, pageable, filteredListings.size());
    }

    @Override
    public List<Listing> findSortedListings(Category category, Location location, String searchQuery, boolean hasReviews, List<String> languages) {

        List<Listing> listings = findActiveByCommunities(languages);

        logger.debug("Объявления по языку: " + listings.size());

        // Фильтруем по категории

        if (category != null) {
            listings.removeIf(listing -> listing.getCategory() != category);
        }

        logger.debug("Объявления прошли фильтр категории: " + listings.size());

        // Фильтруем по локации
        if (location != null) {
            if (location.isCity()) {
                // выбрали город -> оставляем только этот город
                listings.removeIf(listing -> 
                    !listing.getLocation().equals(location)
                );
            } else {
                // выбрали страну -> оставляем объявления из этой страны или её городов
                listings.removeIf(listing -> {
                    Location listingLocation = listing.getLocation();
                    return !(listingLocation.equals(location) ||
                            (listingLocation.getCountry() != null && listingLocation.getCountry().equals(location)));
                });
            }
        }

        logger.debug("Объявления прошли фильтр локации: " + listings.size());

        // Фильтруем по поиску

        if (searchQuery != null && !searchQuery.isBlank()) {
            List<Listing> searchResults = searchListings(searchQuery);
            listings.retainAll(searchResults); // оставляем только те, которые есть и там, и там
        }

        logger.debug("Объявления прошли фильтр поиска: " + listings.size());

        // Фильтруем по наличию отзывов

        if (hasReviews) {
            listings.removeIf(listing -> listing.getRating() == 0);
        }

        logger.debug("Объявления прошли фильтр наличия отзывов: " + listings.size());

        return listings;
    }

    @Override
    public List<Listing> searchListings(String searchQuery) {
        String query = "%" + searchQuery.toLowerCase() + "%";
        
        return listingRepository.searchAllFields(query);
    }

    @Override
    public List<Listing> localizeAccountListings(User user, Locale locale) {
        List<Listing> listings = findListingsByUser(user);
        logger.debug("Got locale: " + locale);

        for (Listing listing : listings) {
            localizeListing(listing, locale);
        }

        logger.debug("Объявлений: " + listings.size());
        return listings;
    }

    @Override
    public List<Listing> localizeActiveAccountListings(User user, Locale locale) {
        List<Listing> listings = findActiveListingsByUser(user);
        logger.debug("Got locale: " + locale);

        for (Listing listing : listings) {
            localizeListing(listing, locale);
        }

        return listings;
    }

    @Override
    public List<Listing> localizeFavoriteListings(User user, Locale locale) {
        List<Listing> favorites = new ArrayList<>(user.getFavoriteListings());

        for (Listing listing : favorites) {
            localizeListing(listing, locale);
        }

        return favorites;
    }

    @Override
    public List<Listing> localizeCatalogListings(List<Listing> listings, Locale locale) {
        for (Listing listing : listings) {
            localizeListing(listing, locale);
        }

        return listings;
    }

    @Override
    public void localizeListing(Listing listing, Locale locale) {
        String lang = locale.getLanguage();
        Map<String, ListingTranslation> translations = listing.getTranslations();

        ListingTranslation selected = translations.get(lang);

        // fallback, если нужного языка нет
        if (selected == null || isBlank(selected.getTitle()) || isBlank(selected.getDescription())) {
            // Приоритет: fi > ru > en
            for (String fallbackLang : LanguageUtils.SUPPORTED_LANGUAGES) {
                selected = translations.get(fallbackLang);
                if (selected != null && !isBlank(selected.getTitle()) && !isBlank(selected.getDescription())) {
                    break;
                }
            }
        }

        if (selected != null) {
            listing.setLocalizedTitle(safe(selected.getTitle()));
            listing.setLocalizedDescription(safe(selected.getDescription()));
        } else {
            listing.setLocalizedTitle(null);
            listing.setLocalizedDescription(null);
        }
    }

    private String safe(String value) {
    return (value != null && !value.isBlank()) ? value : null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @Override
    @Transactional
    public ListingDTO convertToDTO(Listing listing, Locale locale) {
        if (listing == null) {
            return null;
        }

        localizeListing(listing, locale);

        ListingDTO dto = new ListingDTO(
            listing.getId(),
            listing.getAuthor().getId(),
            listing.getLocalizedTitle(),
            listing.getLocalizedDescription(),
            listing.getPrice(),
            listing.getPriceType().getDisplayName(),
            listing.getCategory().getName(),
            listing.getCategory().getId(),
            listing.getLocation().getFullName(),
            listing.getLocation().getId(),
            listing.getRating(),
            listing.getViews(),
            listing.getCreatedAt(),
            listing.isActive(),
            listing.getImagePath(),
            listing.isTestMode()
        );

        return dto;
    }

    @Override
    @Transactional
    public void toggleFavorite(User user, Listing listing) {
        Set<Listing> favorites = user.getFavoriteListings();
        if (favorites.contains(listing)) {
            favorites.remove(listing);
        } else {
            favorites.add(listing);
        }
        userRepository.save(user); // чтобы изменения сохранились в базе
    }

    @Override
    public void addListingToFavorite(User user, Listing listing) {
        user.getFavoriteListings().add(listing);
        userRepository.save(user);
    }

    @Override
    public void removeListingFromFavorite(User user, Listing listing) {
        user.getFavoriteListings().remove(listing);
        userRepository.save(user);
    }

    @Override
    public boolean isFavorite(User user, Listing listing) {
        return user.getFavoriteListings().contains(listing);
    }

    @Override
    @Transactional
    public void clearStatSnapshots(Listing listing) {
        logger.debug("Чистим статистику объявления {}", listing.getId());

        List<StatSnapshot> snapshots = statsRepository.findAllByListingId(listing.getId());

        if (!snapshots.isEmpty()) {
            logger.debug("Чистим снапшоты");
            for (StatSnapshot snapshot : snapshots) {
                statsRepository.delete(snapshot);
            } 
        } else {
            logger.debug("У объявления не было снапшотов статистики");
        }
    }
}