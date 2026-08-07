package com.daiend.muriox.auth;

import java.time.Instant;
import java.util.UUID;

import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.daiend.muriox.config.JwtProperties;

@Service
public class TokenService {

    private final JwtProperties jwtProperties;
    private final JwtEncoder jwtEncoder;

    public TokenService(JwtProperties jwtProperties, JwtEncoder jwtEncoder) {
        this.jwtProperties = jwtProperties;
        this.jwtEncoder = jwtEncoder;

    }

    public IssuedToken createToken(Long id) {
        Instant issueAt = Instant.now();
        Instant expiresAt = issueAt.plus(
                jwtProperties.accessTokenTtl());
        String sessionId = UUID.randomUUID().toString();
        String tokenId = UUID.randomUUID().toString();

        JwtClaimsSet claimsSet = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .subject(id.toString())
                .issuedAt(issueAt)
                .expiresAt(expiresAt)
                .id(tokenId)
                .claim("sid", sessionId)
                .build();

        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).type("JWT").build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claimsSet)).getTokenValue();

        return new IssuedToken(
                token,
                jwtProperties.accessTokenTtl().toSeconds());
    }
}
