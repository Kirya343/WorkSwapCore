package org.workswap.core.services.query.impl;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.workswap.common.enums.SearchModelParamType;
import org.workswap.core.services.components.ServiceUtils;
import org.workswap.core.services.query.UserQueryService;
import org.workswap.datasource.central.model.User;
import org.workswap.datasource.central.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Profile("production")
public class UserQueryServiceImpl implements UserQueryService {

    private final UserRepository userRepository;

    private final ServiceUtils serviceUtils;

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

    public User findUser(String param) {
        SearchModelParamType paramType = serviceUtils.detectParamType(param);
        return findUserFromRepostirory(param, paramType);
    }

    public User findUserFromOAuth2(OAuth2User oauth2User) {
        User user = findUser(oauth2User.getAttribute("email"));
        return user;
    }

    public List<User> findAll() {
        return userRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    public List<User> getRecentUsers(int count) {
        return userRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, count)).getContent();
    }
}
