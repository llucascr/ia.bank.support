package com.llucascr.identify_service.controller;

import com.llucascr.identify_service.controller.swagger.AuthControllerSwagger;
import com.llucascr.identify_service.dto.*;
import com.llucascr.identify_service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/auth")
public class AuthController implements AuthControllerSwagger {
    private final AuthService authService;

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody UserCreateRequest request) {
        return authService.register(request);
    }
}
