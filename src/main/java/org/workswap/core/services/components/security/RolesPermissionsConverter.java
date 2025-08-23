package org.workswap.core.services.components.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.workswap.core.services.UserService;
import org.workswap.datasource.central.model.User;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RolesPermissionsConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserService userService;

    @Override
    public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();

        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles != null) {
            roles.forEach(role -> authorities.add(new SimpleGrantedAuthority(role)));
        }

        List<String> perms = jwt.getClaimAsStringList("permissions");
        if (perms != null) {
            perms.forEach(perm -> authorities.add(new SimpleGrantedAuthority(perm)));
        }
        
        User user = userService.findUser(jwt.getSubject());

        return new UserJwtAuthenticationToken(jwt, authorities, user);
    }
}
