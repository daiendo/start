package com.daiend.muriox.auth;

import java.util.List;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    private final UserAuthorityService userAuthorityService;

    public CurrentUserJwtAuthenticationConverter(UserAuthorityService userAuthorityService) {
        this.userAuthorityService = userAuthorityService;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        CurrentUser currentUser = new CurrentUser(
                userId,
                jwt.getClaimAsString("sid"));

        List<SimpleGrantedAuthority> authorities =
                userAuthorityService.findPermissionCodes(userId)
                        .stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList();

        return new JwtAuthenticationToken(
                jwt,
                currentUser,
                authorities);
    }

}
