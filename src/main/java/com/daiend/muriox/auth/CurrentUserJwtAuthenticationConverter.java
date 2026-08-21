package com.daiend.muriox.auth;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CurrentUserJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    private final UserAuthorityService userAuthorityService;
    private final TokenService tokenService;

    public CurrentUserJwtAuthenticationConverter(UserAuthorityService userAuthorityService, TokenService tokenService) {
        this.userAuthorityService = userAuthorityService;
        this.tokenService = tokenService;
    }

    @Override
    public AbstractAuthenticationToken convert(
            Jwt jwt) {

        Long userId =
                Long.valueOf(
                        jwt.getSubject());

        String sessionId =
                jwt.getClaimAsString("sid");

        UserSessionState sessionState =
                tokenService.getSessionState(
                        userId,
                        sessionId);

        /*
         * ACTIVE 以外的状态都按受限会话处理。
         * 即使 Session 在校验与转换之间被撤销，
         * 也不能加载正常权限。
         */
        boolean mustChangePassword =
                sessionState
                        != UserSessionState.ACTIVE;

        CurrentUser currentUser =
                new CurrentUser(
                        userId,
                        sessionId,
                        mustChangePassword);

        List<SimpleGrantedAuthority> authorities =
                mustChangePassword
                        ? List.of()
                        : userAuthorityService
                        .findPermissionCodes(userId)
                        .stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList();

        return new JwtAuthenticationToken(
                jwt,
                currentUser,
                authorities);
    }

}
