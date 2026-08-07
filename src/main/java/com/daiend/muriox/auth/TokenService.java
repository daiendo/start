package com.daiend.muriox.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.daiend.muriox.config.JwtProperties;

@Service
public class TokenService {

    private static final String SESSION_PREFIX = "auth:session:";

    private final JwtProperties jwtProperties;
    private final JwtEncoder jwtEncoder;
    private final StringRedisTemplate stringRedisTemplate;

    public TokenService(JwtProperties jwtProperties, JwtEncoder jwtEncoder,StringRedisTemplate stringRedisTemplate) {
        this.jwtProperties = jwtProperties;
        this.jwtEncoder = jwtEncoder;
        this.stringRedisTemplate = stringRedisTemplate;

    }

    public IssuedToken createToken(Long id) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(
                jwtProperties.accessTokenTtl());
        String sessionId = UUID.randomUUID().toString();
        String sessionKey = sessionKey(sessionId);
        Duration sessionDuration = jwtProperties.accessTokenTtl();
        String tokenId = UUID.randomUUID().toString();

        JwtClaimsSet claimsSet = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .subject(id.toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .id(tokenId)
                .claim("sid", sessionId)
                .build();

        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).type("JWT").build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claimsSet)).getTokenValue();

        stringRedisTemplate.opsForValue().set(
                sessionKey,
                id.toString(),
                sessionDuration);

        return new IssuedToken(
                token,
                jwtProperties.accessTokenTtl().toSeconds());
    }

    public boolean isSessionActive(Long userId, String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return false;
        }

        String storedUserId = stringRedisTemplate.opsForValue()
                .get(sessionKey(sessionId));

        return userId.toString().equals(storedUserId);
    }

    public void  revokeSession(String sessionId) {
        stringRedisTemplate.delete(sessionKey(sessionId));
    }

    private String sessionKey(String sessionId) {
        return SESSION_PREFIX + sessionId;
    }


}
