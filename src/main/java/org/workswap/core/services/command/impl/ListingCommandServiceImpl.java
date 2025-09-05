package org.workswap.core.services.command.impl;

import java.util.ArrayList;
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
import org.workswap.core.services.query.ListingQueryService;
import org.workswap.datasource.central.model.Listing;
import org.workswap.datasource.central.model.User;
import org.workswap.datasource.central.model.chat.Chat;
import org.workswap.datasource.central.model.listingModels.Category;
import org.workswap.datasource.central.model.listingModels.ListingTranslation;
import org.workswap.datasource.central.model.listingModels.Location;
import org.workswap.datasource.central.repository.CategoryRepository;
import org.workswap.datasource.central.repository.ListingRepository;
import org.workswap.datasource.central.repository.ListingTranslationRepository;
import org.workswap.datasource.central.repository.LocationRepository;
import org.workswap.datasource.central.repository.UserRepository;
import org.workswap.datasource.central.repository.chat.ChatRepository;
import org.workswap.datasource.stats.model.StatSnapshot;
import org.workswap.datasource.stats.repository.StatsRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Profile("production")
public class ListingCommandServiceImpl implements ListingCommandService {

    private static final Logger logger = LoggerFactory.getLogger(ListingCommandService.class);

    private final ListingQueryService queryService;

    private final ListingTranslationRepository listingTranslationRepository;
    private final ListingRepository listingRepository;
    private final ChatRepository chatRepository;
    private final StatsRepository statsRepository;
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
        clearStatSnapshots(listing);

        logger.debug("Удаляем объявление");
        listingRepository.delete(listing);
    }

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

    public void modifyListingParam(Long id, Map<String, Object> updates) {
        Listing listing = queryService.findListing(id.toString());

        if (listing != null) {
            updates.forEach((key, value) -> {
                switch (key) {
                    case "translation":
                        if (value instanceof Map) {
                            Map<?, ?> rawMap = (Map<?, ?>) value;

                            List<String> communities = new ArrayList<>();

                            rawMap.forEach((lang, v) -> {
                                String language = (String) lang;
                                if (v instanceof Map) {
                                    Map<?, ?> vMap = (Map<?, ?>) v;
                                    String title = (String) vMap.get("title");
                                    String description = (String) vMap.get("description");

                                    ListingTranslation translation = listingTranslationRepository.findByListingAndLanguage(listing, language);

                                    communities.add(language);
                                    if (translation == null) {
                                        translation = new ListingTranslation(language, title, description, listing);
                                    } else {
                                        translation.setTitle(title);
                                        translation.setDescription(description);
                                    }
                                    listingTranslationRepository.save(translation);
                                }
                                listing.setCommunities(communities);
                            });
                        }
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
                    case "mainImageUrl":
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
}
