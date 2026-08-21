package com.daiend.muriox.auth;

import com.daiend.muriox.config.JwtProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class TokenService {

    private static final String USER_SESSION_PREFIX =
            "auth:user:";
    private final JwtProperties jwtProperties;
    private final JwtEncoder jwtEncoder;
    private final StringRedisTemplate stringRedisTemplate;

    public TokenService(JwtProperties jwtProperties, JwtEncoder jwtEncoder, StringRedisTemplate stringRedisTemplate) {
        this.jwtProperties = jwtProperties;
        this.jwtEncoder = jwtEncoder;
        this.stringRedisTemplate = stringRedisTemplate;

    }

    public IssuedToken createToken(Long userId, boolean mustChangePassword) {
        UserSessionState sessionState =
                mustChangePassword
                        ? UserSessionState
                        .PASSWORD_CHANGE_REQUIRED
                        : UserSessionState.ACTIVE;
        Instant issuedAt = Instant.now();

        Instant expiresAt =
                issuedAt.plus(
                        jwtProperties.accessTokenTtl());

        String sessionId =
                UUID.randomUUID().toString();

        Duration sessionDuration =
                jwtProperties.accessTokenTtl();

        String tokenId =
                UUID.randomUUID().toString();

        JwtClaimsSet claimsSet =
                JwtClaimsSet.builder()
                        .issuer(jwtProperties.issuer())
                        .subject(userId.toString())
                        .issuedAt(issuedAt)
                        .expiresAt(expiresAt)
                        .id(tokenId)
                        .claim("sid", sessionId)
                        .build();

        JwsHeader header =
                JwsHeader
                        .with(SignatureAlgorithm.RS256)
                        .type("JWT")
                        .build();

        String token =
                jwtEncoder.encode(
                                JwtEncoderParameters.from(
                                        header,
                                        claimsSet))
                        .getTokenValue();

        String sessionKey =
                sessionKey(userId, sessionId);

        String sessionIndexKey =
                sessionIndexKey(userId);

        stringRedisTemplate
                .opsForValue()
                .set(
                        sessionKey,
                        sessionState.name(),
                        sessionDuration);

        stringRedisTemplate
                .opsForSet()
                .add(
                        sessionIndexKey,
                        sessionId);

        stringRedisTemplate.expire(
                sessionIndexKey,
                sessionDuration);

        return new IssuedToken(
                token,
                sessionDuration.toSeconds());
    }

    public boolean isSessionActive(
            Long userId,
            String sessionId) {

        return getSessionState(
                userId,
                sessionId) != null;
    }

    public UserSessionState getSessionState(
            Long userId,
            String sessionId) {

        if (userId == null
                || sessionId == null
                || sessionId.isBlank()) {

            return null;
        }

        String value =
                stringRedisTemplate
                        .opsForValue()
                        .get(
                                sessionKey(
                                        userId,
                                        sessionId));

        if (value == null) {
            return null;
        }

        try {
            return UserSessionState.valueOf(
                    value);
        } catch (IllegalArgumentException exception) {
            stringRedisTemplate.delete(
                    sessionKey(
                            userId,
                            sessionId));

            stringRedisTemplate
                    .opsForSet()
                    .remove(
                            sessionIndexKey(userId),
                            sessionId);

            return null;
        }
    }


    public void revokeSession(
            Long userId,
            String sessionId) {

        if (userId == null
                || sessionId == null
                || sessionId.isBlank()) {

            return;
        }

        stringRedisTemplate.delete(
                sessionKey(
                        userId,
                        sessionId));

        stringRedisTemplate
                .opsForSet()
                .remove(
                        sessionIndexKey(userId),
                        sessionId);
    }

    public void revokeAllSessions(Long userId) {
        if (userId == null) {
            return;
        }

        String sessionIndexKey =
                sessionIndexKey(userId);

        Set<String> sessionIds =
                stringRedisTemplate
                        .opsForSet()
                        .members(sessionIndexKey);

        if (sessionIds == null
                || sessionIds.isEmpty()) {

            stringRedisTemplate.delete(
                    sessionIndexKey);

            return;
        }

        List<String> sessionKeys =
                sessionIds.stream()
                        .map(sessionId ->
                                sessionKey(
                                        userId,
                                        sessionId))
                        .toList();

        stringRedisTemplate.delete(
                sessionKeys);

        stringRedisTemplate.delete(
                sessionIndexKey);
    }

    public boolean requiresPasswordChange(
            Long userId,
            String sessionId) {

        return getSessionState(
                userId,
                sessionId)
                == UserSessionState
                .PASSWORD_CHANGE_REQUIRED;
    }

    private String sessionKey(
            Long userId,
            String sessionId) {

        return USER_SESSION_PREFIX
                + "{"
                + userId
                + "}"
                + ":session:"
                + sessionId;
    }

    private String sessionIndexKey(Long userId) {
        return USER_SESSION_PREFIX
                + "{"
                + userId
                + "}"
                + ":sessions";
    }


}
