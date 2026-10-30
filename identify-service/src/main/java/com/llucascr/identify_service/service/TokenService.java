package com.llucascr.identify_service.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

@Service
public class TokenService {
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
