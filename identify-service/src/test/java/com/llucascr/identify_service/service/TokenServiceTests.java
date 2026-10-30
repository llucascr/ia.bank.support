package com.llucascr.identify_service.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class TokenServiceTests {
    private final TokenService service = new TokenService();

    @Test
    void extractsUuidSubject() {
        UUID id = UUID.randomUUID();
        assertEquals(id, service.getUserId(jwt(id.toString())));
    }

    @Test
    void rejectsMissingOrMalformedSubject() {
        for (String subject : new String[] {"", "invalid", "1-1-1-1-1"}) {
            var exception = assertThrows(ResponseStatusException.class, () -> service.getUserId(jwt(subject)));
            assertEquals(401, exception.getStatusCode().value());
        }
        assertThrows(ResponseStatusException.class, () -> service.getUserId(null));
        assertThrows(ResponseStatusException.class, () -> service.getUserId(
                Jwt.withTokenValue("token").header("alg", "RS256").claim("scope", "users").build()));
    }

    private Jwt jwt(String subject) {
        return Jwt.withTokenValue("token").header("alg", "RS256").subject(subject).build();
    }
}
