package com.daiend.muriox.security;

import com.daiend.muriox.auth.TokenService;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class ActiveSessionJwtValidator implements OAuth2TokenValidator<Jwt> {
    private static final OAuth2Error INVALID_TOKEN =
            new OAuth2Error(
                    "invalid_token",
                    "登录会话已失效",
                    null);
    private final TokenService tokenService;
    public ActiveSessionJwtValidator(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {

    String subject = jwt.getSubject();
    String sessionId = jwt.getClaimAsString("sid");

        if (subject == null || sessionId == null) {
            return OAuth2TokenValidatorResult.failure(INVALID_TOKEN);
        }
        try {
            Long userId = Long.valueOf(subject);

            if (tokenService.isSessionActive(userId, sessionId)) {
                return OAuth2TokenValidatorResult.success();
            }
        } catch (NumberFormatException exception){
            return OAuth2TokenValidatorResult.failure(INVALID_TOKEN);
        }

        return OAuth2TokenValidatorResult.failure(INVALID_TOKEN);
    }
}
