package com.llucascr.identify_service.dto;

import lombok.Builder;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record UserResponse(UUID id, String cpf, String fullName, String email,
        String role, String status, LocalDateTime createdAt, LocalDateTime updatedAt) {
}
