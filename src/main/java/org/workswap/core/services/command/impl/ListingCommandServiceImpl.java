package org.workswap.core.services.command.impl;

import java.nio.file.AccessDeniedException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.workswap.common.enums.PriceType;
import org.workswap.core.services.command.ListingCommandService;
import org.workswap.datasource.central.model.Listing;
import org.workswap.datasource.central.model.User;
import org.workswap.datasource.central.model.chat.Chat;
import org.workswap.datasource.central.model.listingModels.Category;
import org.workswap.datasource.central.model.listingModels.ListingTranslation;
import org.workswap.datasource.central.model.listingModels.Location;
import org.workswap.datasource.central.repository.CategoryRepository;
import org.workswap.datasource.central.repository.listing.ListingRepository;
import org.workswap.datasource.central.repository.LocationRepository;
import org.workswap.datasource.central.repository.UserRepository;
import org.workswap.datasource.central.repository.chat.ChatRepository;
import org.workswap.datasource.stats.model.ListingStatSnapshot;
import org.workswap.datasource.stats.repository.ListingStatRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Profile({"production", "statistic"})
public class ListingCommandServiceImpl implements ListingCommandService {

    private static final Logger logger = LoggerFactory.getLogger(ListingCommandService.class);

    private final ListingRepository listingRepository;
    private final ChatRepository chatRepository;
    private final ListingStatRepository listingStatRepository;
    private final UserRepository userRepository;
    private final LocationRepository locationRepository;
    private final CategoryRepository categoryRepository;

    public void save(Listing listing) {
        logger.debug("Сохраняем объявление: {}", listing.getId());
        listingRepository.save(listing);
    }

    public Listing saveAndReturn(Listing listing) {
        return listingRepository.save(listing);
    }

    @Transactional
    public void delete(Listing listing) {

        logger.debug("Начинаем удаление объявления {}", listing.getId());
        List<Chat> chats = chatRepository.findAllByListing(listing);

        logger.debug("Обнуляем свясь с объявлением у чатов");
        for (Chat chat : chats) {
            chat.setListing(null);
        }
        chatRepository.saveAll(chats);

        logger.debug("Чистим статистику объявления");
        clearListingStatSnapshots(listing);

        logger.debug("Удаляем объявление");
        listingRepository.delete(listing);
    }

    @Transactional
    public void clearListingStatSnapshots(Listing listing) {
        logger.debug("Чистим статистику объявления {}", listing.getId());
        List<ListingStatSnapshot> snapshots = listingStatRepository.findAllByListingId(listing.getId());

        if (!snapshots.isEmpty()) {
            logger.debug("Чистим снапшоты batch-ом");
            // Получаем ID для batch удаления
            List<Long> snapshotIds = snapshots.stream()
                    .map(ListingStatSnapshot::getId)
                    .toList();
            
            listingStatRepository.deleteAllByIdInBatch(snapshotIds);
        } else {
            logger.debug("У объявления не было снапшотов статистики");
        }
    }

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

    public void addListingToFavorite(User user, Listing listing) {
        user.getFavoriteListings().add(listing);
        userRepository.save(user);
    }

    public void removeListingFromFavorite(User user, Listing listing) {
        user.getFavoriteListings().remove(listing);
        userRepository.save(user);
    }

    public void modifyListingParam(User user, Long id, Map<String, Object> updates) throws AccessDeniedException {
        Listing listing = listingRepository.findById(id).orElse(null);

        if (listing.getAuthor().getId() != user.getId()) {
            throw new AccessDeniedException("Это не ваше объявление!");
        }
        
        if (listing != null) {
            updates.forEach((key, value) -> {
                switch (key) {
                    case "translation":
                        updateListingTranslations(listing, (Map<String, Object>) value);
                        break;
                    case "price":
                        if (value != null) {
                            Double price;
                            if (value instanceof Number) {
                                price = ((Number) value).doubleValue();
                            } else {
                                price = Double.parseDouble(value.toString());
                            }
                            listing.setPrice(price);
                        }
                        break;
                    case "mainImage":
                        listing.setImagePath((String) value);
                        break;
                    case "priceType":
                        listing.setPriceType(PriceType.valueOf((String) value));
                        break;
                    case "active":
                        listing.setActive((Boolean) value);
                        break;
                    case "testMode":
                        listing.setTestMode((Boolean) value);
                        break;
                    case "location":
                        if (value != null) {
                            Long locId = ((Number) value).longValue(); // безопасно для Integer и Long
                            Location loc = locationRepository.findById(locId).orElse(null);
                            listing.setLocation(loc);
                        }
                        break;
                    case "category":
                        if (value != null) {
                            Long catId = ((Number) value).longValue(); // безопасно для Integer и Long
                            Category cat = categoryRepository.findById(catId).orElse(null);
                            listing.setCategory(cat);
                        }
                        break;
                }
            });
        }

        save(listing);
    }

    @Transactional
    private void updateListingTranslations(Listing listing, Map<String, Object> translationsMap) {
        Map<String, ListingTranslation> currentTranslations = listing.getTranslations();
        Set<String> newLanguages = new HashSet<>();

        // Обновляем или создаем переводы поэлементно
        for (Map.Entry<String, Object> entry : translationsMap.entrySet()) {
            String lang = entry.getKey();
            Object v = entry.getValue();

            if (!(v instanceof Map)) continue;
            Map<?, ?> vMap = (Map<?, ?>) v;

            String title = (String) vMap.get("title");
            String description = (String) vMap.get("description");
            newLanguages.add(lang);

            if (currentTranslations.containsKey(lang)) {
                // Обновляем существующий
                ListingTranslation translation = currentTranslations.get(lang);
                translation.setTitle(title);
                translation.setDescription(description);
            } else {
                // Создаем новый
                ListingTranslation translation = new ListingTranslation(lang, title, description, listing);
                currentTranslations.put(lang, translation);
            }
        }

        // Удаляем отсутствующие переводы (orphanRemoval сработает автоматически)
        currentTranslations.keySet().removeIf(lang -> !newLanguages.contains(lang));

        // Обновляем список языков
        listing.setCommunities(new ArrayList<>(newLanguages));
        
        listingRepository.save(listing);
    }
}