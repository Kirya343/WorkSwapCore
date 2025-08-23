package org.workswap.core.services.components.security;

import java.util.Set;

import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.workswap.datasource.central.model.User;
import org.workswap.datasource.central.model.user.Role;
import org.workswap.core.services.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Profile("production")
public class RoleCheckService {

    private final UserService userService;

    public void checkRoles(Model model, OAuth2User oauth2User) {
        User user = userService.findUserFromOAuth2(oauth2User);
        Set<Role> roles = user.getRoles();

        for (Role role : roles) {
            String roleName = role.getName().toUpperCase(); // Пример: "ADMIN"
            model.addAttribute("role" + capitalize(roleName.toLowerCase()), true);
        }
    }

    public boolean hasRole(Set<Role> roles, String roleName) {
        return roles.stream()
                .anyMatch(role -> role.getName().equalsIgnoreCase(roleName));
    }

    private String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
