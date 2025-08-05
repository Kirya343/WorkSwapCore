package org.workswap.core.services.impl;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.workswap.datasource.central.model.Listing;
import org.workswap.datasource.central.model.Review;
import org.workswap.datasource.central.model.User;
import org.workswap.datasource.central.model.chat.Chat;
import org.workswap.datasource.central.model.chat.ChatParticipant;
import org.workswap.datasource.central.model.enums.Role;
import org.workswap.datasource.central.model.enums.SearchModelParamType;
import org.workswap.datasource.central.repository.ReviewRepository;
import org.workswap.datasource.central.repository.UserRepository;
import org.workswap.core.services.ListingService;
import org.workswap.core.services.UserService;
import org.workswap.core.services.components.ServiceUtils;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final ServiceUtils serviceUtils;
    private final ChatServiceImpl chatService;
    private final ListingService listingService;
    private final ReviewRepository reviewRepository;
 
    private User findUserFromRepostirory(String param, SearchModelParamType paramType) {
        switch (paramType) {
            case ID:
                return userRepository.findById(Long.parseLong(param)).orElse(null);
            case EMAIL:
                return userRepository.findByEmail(param).orElse(null);
            case NAME:
                return userRepository.findByName(param).orElse(null); // если есть такой метод
            case SUB:
                return userRepository.findBySub(param).orElse(null);
            default:
                throw new IllegalArgumentException("Unknown param type: " + paramType);
        }
    }

    @Override
    public User findUser(String param) {
        SearchModelParamType paramType = serviceUtils.detectParamType(param);
        return findUserFromRepostirory(param, paramType);
    }

    @Override
    public User findUserFromOAuth2(OAuth2User oauth2User) {
        User user = findUser(oauth2User.getAttribute("email"));
        return user;
    }

    @Override
    @Transactional
    public void registerUserFromOAuth2(OAuth2User oauth2User) {

        // Проверяем, существует ли пользователь с таким email
        User existingUser = findUser(oauth2User.getAttribute("email"));

        if (existingUser != null) {
            throw new RuntimeException("Пользователь с таким email уже зарегистрирован.");
        }

        // Создаем нового пользователя
        User newUser = new User(oauth2User.getAttribute("name"),
                                oauth2User.getAttribute("email"),
                                oauth2User.getAttribute("sub"),
                                oauth2User.getAttribute("picture"),
                                Role.USER,
                                true);

        // Сохраняем нового пользователя
        newUser = userRepository.save(newUser);

    }

    @Override
    @Transactional
    public void deleteUserFromOAuth2(OAuth2User oauth2User) {
        User user = findUser(oauth2User.getAttribute("email"));
        deleteUser(user);
    }

    @Override
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
                    listingService.deleteListing(listing);
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

    @Override
    public List<User> findAll() {
        return userRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Override
    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }

    @Override
    public List<User> getRecentUsers(int count) {
        return userRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, count)).getContent();
    }
}

