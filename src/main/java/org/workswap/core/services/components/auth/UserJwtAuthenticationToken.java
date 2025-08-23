package org.workswap.core.services.components.auth;

import java.util.Collection;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.workswap.datasource.central.model.User;

public class UserJwtAuthenticationToken extends AbstractAuthenticationToken {

    private final User user;
    private final Jwt jwt;

    public UserJwtAuthenticationToken(Jwt jwt, Collection<GrantedAuthority> authorities, User user) {
        super(authorities);
        this.jwt = jwt;
        this.user = user;
        super.setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return jwt.getTokenValue();
    }

    @Override
    public Object getPrincipal() {
        return user;
    }

    public Jwt getJwt() {
        return jwt;
    }
}
