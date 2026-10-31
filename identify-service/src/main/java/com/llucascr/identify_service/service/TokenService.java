package com.llucascr.identify_service.service;

import com.llucascr.identify_service.dto.AuthResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

@Service
public class TokenService {
    private final JwtEncoder encoder;
    private final long expiresIn;

    public TokenService(JwtEncoder encoder, @Value("${jwt.expiration-seconds:3600}") long expiresIn) {
        if (expiresIn <= 0) {
            throw new IllegalArgumentException("JWT expiration must be positive");
        }
        this.encoder = encoder;
        this.expiresIn = expiresIn;
    }

    public AuthResponse issue(UUID userId) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        var claims = JwtClaimsSet.builder().subject(userId.toString())
                .issuedAt(now).expiresAt(now.plusSeconds(expiresIn)).build();
        var header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AuthResponse(token, "Bearer", expiresIn);
    }

    public UUID getUserId(Jwt jwt) {
        try {
            if (jwt == null || jwt.getSubject() == null) {
                throw new IllegalArgumentException();
            }
            UUID id = UUID.fromString(jwt.getSubject());
            if (!id.toString().equalsIgnoreCase(jwt.getSubject())) {
                throw new IllegalArgumentException();
            }
            return id;
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid user token");
        }
    }
}
