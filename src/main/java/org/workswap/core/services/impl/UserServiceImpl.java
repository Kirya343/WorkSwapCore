package org.workswap.core.services.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.workswap.core.datasource.central.model.User;
import org.workswap.core.datasource.central.model.enums.Role;
import org.workswap.core.datasource.central.model.enums.SearchModelParamType;
import org.workswap.core.datasource.central.repository.UserRepository;
import org.workswap.core.services.UserService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
 
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
        SearchModelParamType paramType = detectParamType(param);
        return findUserFromRepostirory(param, paramType);
    }

    private SearchModelParamType detectParamType(String param) {
        if (param == null || param.isBlank()) {
            throw new IllegalArgumentException("Search parameter cannot be null or empty");
        }

        if (param.length() >= 15 && param.matches("^[a-zA-Z0-9._-]+$")) {
            return SearchModelParamType.SUB;
        }

        if (param.matches("^\\d+$")) {
            return SearchModelParamType.ID;
        }

        if (param.contains("@")) {
            return SearchModelParamType.EMAIL;
        }

        return SearchModelParamType.NAME;
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

    @Override
    public void deleteById(Long id) {
        User user = findUser(id.toString());
        userRepository.delete(user);
    }
}

