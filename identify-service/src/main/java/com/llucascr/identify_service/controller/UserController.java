package com.llucascr.identify_service.controller;

import com.llucascr.identify_service.controller.swagger.UserControllerSwagger;
import com.llucascr.identify_service.dto.*;
import com.llucascr.identify_service.service.TokenService;
import com.llucascr.identify_service.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/users")
public class UserController implements UserControllerSwagger {

    private final UserService userService;
    private final TokenService tokenService;

    @GetMapping("/me")
    public CurrentUserResponse getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        return userService.getCurrentUser(tokenService.getUserId(jwt));
    }

    @PatchMapping("/me/email")
    public UserResponse completeEmail(@Valid @RequestBody UserEmailCompletionRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return userService.completeEmail(tokenService.getUserId(jwt), request.email());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody UserCreateRequest request) {
        return userService.create(request);
    }

    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable UUID id) {
        return userService.getById(id);
    }

    @GetMapping("/search")
    public List<UserResponse> search(@RequestParam String query) {
        return userService.search(query);
    }

    @PatchMapping("/{id}")
    public UserResponse update(@PathVariable UUID id, @Valid @RequestBody UserUpdateRequest request) {
        return userService.update(id, request);
    }

    @PatchMapping("/{id}/activate")
    public UserResponse activate(@PathVariable UUID id) {
        return userService.activate(id);
    }

    @PatchMapping("/{id}/deactivate")
    public UserResponse deactivate(@PathVariable UUID id) {
        return userService.deactivate(id);
    }
}
