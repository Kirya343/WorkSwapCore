package org.workswap.core.services.command;

import java.util.Map;

import org.springframework.security.oauth2.core.user.OAuth2User;
import org.workswap.datasource.central.model.User;

public interface UserCommandService {

    //старый метод регистрации(не используется)
    void registerUserFromOAuth2(OAuth2User oauth2User);

    User registerUser(User user);

    //старый метод удаления пользователя из oauth(не используется)
    void deleteUserFromOAuth2(OAuth2User oauth2User);

    //универсальный метод удаления, работает и используется
    void deleteUser(User user);
    
    User save(User user);

    void modifyUserParam(User user, Map<String, Object> updates);
}
