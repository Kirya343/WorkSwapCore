package org.workswap.core.services.components;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.workswap.datasource.central.model.User;
import org.workswap.core.exceptions.UserNotRegisteredException;
import org.workswap.core.services.StatService;
import org.workswap.core.services.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final StatService statService;
    private final RoleCheckService roleCheckService;

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private void addAuthenticationAttributes(Model model, OAuth2User oauth2User, User user) {
        if (oauth2User != null) {
            double rating = statService.getUserRating(user);

            roleCheckService.checkRoles(model, oauth2User);

            model.addAttribute("isAuthenticated", true);
            
            logger.debug("Пользователь прошёл авторизацию: {}", user.getName());
            model.addAttribute("user", user);
            model.addAttribute("rating", rating);
        } else {
            model.addAttribute("isAuthenticated", false);
        }
    }

    public void validateAndAddAuthentication(Model model, OAuth2User oauth2User) {
        if (oauth2User == null) {
            addAuthenticationAttributes(model, null, null);
            return;
        }

        User user = userService.findUserFromOAuth2(oauth2User);
        if (user == null) {
            throw new UserNotRegisteredException("User not found in the system");
        }

        addAuthenticationAttributes(model, oauth2User, user);
    }
}

