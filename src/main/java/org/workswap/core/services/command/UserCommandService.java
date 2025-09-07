package org.workswap.core.services.command;

import java.util.Map;

import org.springframework.security.oauth2.core.user.OAuth2User;
import org.workswap.datasource.central.model.User;

public interface UserCommandService {

    void registerUserFromOAuth2(OAuth2User oauth2User);

    void deleteUserFromOAuth2(OAuth2User oauth2User);
    void deleteUser(User user);
    
    User save(User user);

    void modifyUserParam(User user, Map<String, Object> updates);
    
}
