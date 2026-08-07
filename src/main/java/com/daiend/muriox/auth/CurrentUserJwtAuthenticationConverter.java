package com.daiend.muriox.auth;

import java.util.List;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        CurrentUser currentUser = new CurrentUser(
                Long.valueOf(jwt.getSubject()),
                jwt.getClaimAsString("sid"));

        return new JwtAuthenticationToken(
                jwt,
                currentUser,
                List.of());
    }

}
