package org.workswap.core.services;

import org.springframework.security.oauth2.core.user.OAuth2User;
import org.workswap.datasource.central.model.User;

import java.util.List;

public interface UserService {

    // Получение списка пользователей по пораметрам
    List<User> findAll();
    List<User> getRecentUsers(int count);

    // Поиск пользователя по пораметрам
    User findUserFromOAuth2(OAuth2User oauth2User);
    User findUser(String param);

    // Регистрирация пользователя из OAuth2
    void registerUserFromOAuth2(OAuth2User oauth2User);

    // Управление пользователями
    User save(User user);
    void deleteById(Long id);
}