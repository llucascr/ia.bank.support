package com.llucascr.identify_service.service;

import com.llucascr.identify_service.domain.entity.User;
import com.llucascr.identify_service.domain.enums.Status;
import com.llucascr.identify_service.dto.*;
import com.llucascr.identify_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final TokenService tokenService;

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByCpf(request.cpf())
                .orElseThrow(this::invalidCredentials);
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())
                || !Status.ACTIVE.name().equals(user.getStatus())) {
            throw invalidCredentials();
        }
        return tokenService.issue(user.getId());
    }

    @Transactional
    public AuthResponse register(UserCreateRequest request) {
        UserResponse user = userService.create(request);
        return tokenService.issue(user.id());
    }

    private ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }
}
