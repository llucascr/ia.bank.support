package com.llucascr.identify_service.service;

import com.llucascr.identify_service.domain.entity.User;
import com.llucascr.identify_service.domain.enums.Role;
import com.llucascr.identify_service.domain.enums.Status;
import com.llucascr.identify_service.dto.*;
import com.llucascr.identify_service.exception.UserAlreadyExistsException;
import com.llucascr.identify_service.exception.UserNotFoundException;
import com.llucascr.identify_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public CurrentUserResponse getCurrentUser(UUID id) {
        User user = findUser(id);
        return CurrentUserResponse.builder().id(user.getId()).cpf(user.getCpf())
                .fullName(user.getFullName()).email(user.getEmail()).role(user.getRole())
                .status(user.getStatus()).createdAt(user.getCreatedAt()).updatedAt(user.getUpdatedAt()).build();
    }

    @Transactional
    public UserResponse completeEmail(UUID id, String email) {
        User user = findUser(id);
        changeEmail(user, email);
        return response(save(user));
    }

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        if (userRepository.existsByCpf(request.cpf())) {
            throw new UserAlreadyExistsException("User already exists with this CPF");
        }
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new UserAlreadyExistsException("User already exists with this email");
        }

        User user = User.builder()
                .cpf(request.cpf())
                .fullName(request.fullName().strip())
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.CUSTOMER.name())
                .status(Status.ACTIVE.name())
                .build();

        return response(save(user));
    }

    public UserResponse getById(UUID id) {
        return response(findUser(id));
    }

    public List<UserResponse> search(String query) {
        if (query == null || query.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Search query must not be blank");
        }
        String term = query.strip();
        String pattern = "%" + term.replace("\\", "\\\\").replace("%", "\\%")
                .replace("_", "\\_") + "%";
        return userRepository.search(term, pattern).stream().map(this::response).toList();
    }

    @Transactional
    public UserResponse update(UUID id, UserUpdateRequest request) {
        User user = findUser(id);
        user.setFullName(request.fullName() == null ? user.getFullName() : request.fullName().strip());
        if (request.email() != null) {
            changeEmail(user, request.email());
        }
        return response(save(user));
    }

    @Transactional
    public UserResponse activate(UUID id) {
        return changeStatus(id, Status.ACTIVE);
    }

    @Transactional
    public UserResponse deactivate(UUID id) {
        return changeStatus(id, Status.BLOCKED);
    }

    private UserResponse changeStatus(UUID id, Status status) {
        User user = findUser(id);
        user.setStatus(status.name());
        return response(save(user));
    }

    private User findUser(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    private void changeEmail(User user, String email) {
        String normalized = normalizeEmail(email);
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(normalized, user.getId())) {
            throw new UserAlreadyExistsException("User already exists with this email");
        }
        user.setEmail(normalized);
    }

    private String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    private User save(User user) {
        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            Throwable cause = exception;
            while (cause != null) {
                if (cause instanceof org.hibernate.exception.ConstraintViolationException violation) {
                    if ("uk_tb_user_cpf".equals(violation.getConstraintName())) {
                        throw new UserAlreadyExistsException("User already exists with this CPF");
                    }
                    if ("uk_tb_user_email".equals(violation.getConstraintName())) {
                        throw new UserAlreadyExistsException("User already exists with this email");
                    }
                }
                cause = cause.getCause();
            }
            throw exception;
        }
    }

    private UserResponse response(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .cpf(user.getCpf())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
