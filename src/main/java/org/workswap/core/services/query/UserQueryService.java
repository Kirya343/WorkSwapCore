package org.workswap.core.services.query;

import java.util.List;

import org.springframework.security.oauth2.core.user.OAuth2User;
import org.workswap.datasource.central.model.User;

public interface UserQueryService {
    
    User findUser(String param);
    User findUserFromOAuth2(OAuth2User oauth2User);

    List<User> findAll();
    List<User> getRecentUsers(int count);
}
