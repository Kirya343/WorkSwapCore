package org.workswap.core.services.command.impl;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.workswap.common.enums.UserStatus;
import org.workswap.core.services.RoleService;
import org.workswap.core.services.command.ListingCommandService;
import org.workswap.core.services.command.UserCommandService;
import org.workswap.core.services.impl.ChatServiceImpl;
import org.workswap.core.services.query.UserQueryService;
import org.workswap.datasource.central.model.Listing;
import org.workswap.datasource.central.model.Review;
import org.workswap.datasource.central.model.User;
import org.workswap.datasource.central.model.chat.Chat;
import org.workswap.datasource.central.model.chat.ChatParticipant;
import org.workswap.datasource.central.model.listingModels.Location;
import org.workswap.datasource.central.model.user.Role;
import org.workswap.datasource.central.model.user.UserSettings;
import org.workswap.datasource.central.repository.LocationRepository;
import org.workswap.datasource.central.repository.ReviewRepository;
import org.workswap.datasource.central.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Profile("production")
public class UserCommandServiceImpl implements UserCommandService {

    private static final Logger logger = LoggerFactory.getLogger(UserCommandService.class);

    private final UserQueryService queryService;
    private final RoleService roleService;
    private final ChatServiceImpl chatService;
    private final ListingCommandService listingCommandService;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final LocationRepository locationRepository;

    @Transactional
    public void registerUserFromOAuth2(OAuth2User oauth2User) {

        // Проверяем, существует ли пользователь с таким email
        User existingUser = queryService.findUser(oauth2User.getAttribute("email"));

        if (existingUser != null) {
            throw new RuntimeException("Пользователь с таким email уже зарегистрирован.");
        }

        Role role = roleService.getRoleByName("USER");

        Set<Role> roles = new HashSet<>();

        roles.add(role);
        // Создаем нового пользователя
        User newUser = new User(oauth2User.getAttribute("name"),
                                oauth2User.getAttribute("email"),
                                oauth2User.getAttribute("sub"),
                                oauth2User.getAttribute("picture"),
                                roles,
                                true);

        // Сохраняем нового пользователя
        newUser = save(newUser);
    }

    public User registerUser(User user) {
        user.setStatus(UserStatus.ACTIVE);
        user.setTermsAccepted(true);
        user.setTermsAcceptanceDate(LocalDateTime.now());

        return save(user);
    }

    @Transactional
    public void deleteUserFromOAuth2(OAuth2User oauth2User) {
        User user = queryService.findUser(oauth2User.getAttribute("email"));
        deleteUser(user);
    }

    @Transactional
    public void deleteUser(User user) {
        try {
            if (user == null) {
                throw new RuntimeException("Пользователя не зарегистрировано.");
            }

            logger.debug("Пользователь {} найден, начинаем удаление", user.getId());

            // Чистка разговоров

            Set<ChatParticipant> chatParticipants = user.getChatParticipants();
            logger.debug("> Чистка чатов");

            if (!chatParticipants.isEmpty()) {
                for (ChatParticipant chatParticipant : chatParticipants) {
                    Chat chat = chatParticipant.getChat();
                    user.getChatParticipants().remove(chatParticipant);
                    logger.debug(">> Удаление чата {}", chat.getId());
                    chatService.deleteChat(chat);
                }
            } else {
                logger.debug(">> У пользователя не найдено чатов");
            }

            // Чистка объявлений
            List<Listing> listings = user.getListings();
            logger.debug("> Чистка объявлений");

            if (!listings.isEmpty()) {
                for (Listing listing : listings) {
                    logger.debug(">> Удаление объявления {}", listing.getId());
                    user.getListings().remove(listing);
                    listingCommandService.delete(listing);
                }
            } else {
                logger.debug(">> У пользователя не найдено объявлений");
            }

            // Чистка отзывов
            List<Review> reviews = user.getReviews();
            logger.debug("> Чистка отзывов");

            if (!reviews.isEmpty()) {
                for (Review review : reviews) {
                    logger.debug(">> Удаление отзыва {}", review.getId());
                    user.getReviews().remove(review);
                    reviewRepository.delete(review);
                }
            } else {
                logger.debug(">> У пользователя не найдено отзывов");
            }

            // Удаление пользователя
            try {
                logger.debug("Удаление пользователя {}", user.getId());
                userRepository.deleteById(user.getId());
            } catch (Exception e) {
                logger.error("Ошибка при удалении пользователя {}: {}", user.getId(), e.getMessage(), e);
                throw new RuntimeException("Ошибка при удалении пользователя", e);
            }

        } catch (Exception e) {
            logger.error("Не удалось удалить пользователя через OAuth2: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка при удалении пользователя через OAuth2", e);
        }
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    public void modifyUserParam(User user, Map<String, Object> updates) {

        UserSettings settings = user.getSettings();

        if (user != null) {
            updates.forEach((key, value) -> {
                switch (key) {
                    case "name":
                        user.setName((String) value);
                        break;
                    case "phone":
                        user.setPhone((String) value);
                        break;
                    case "phoneVisible":
                        settings.setPhoneVisible((Boolean) value);
                        break;
                    case "emailVisible":
                        settings.setEmailVisible((Boolean) value);
                        break;
                    case "avatarType":
                        settings.setAvatarType((String) value);
                        break;
                    case "avatarUrl":
                        user.setAvatarUrl((String) value);
                        break;
                    case "languages":
                        user.setLanguages((List<String>) value);
                        break;
                    case "location":
                        if (value != null) {
                            Long locId = ((Number) value).longValue(); // безопасно для Integer и Long
                            Location loc = locationRepository.findById(locId).orElse(null);
                            user.setLocation(loc);
                        }
                        break;
                    case "bio":
                        user.setBio((String) value);
                        break;
                }
            });
        }

        save(user);
    }
}
