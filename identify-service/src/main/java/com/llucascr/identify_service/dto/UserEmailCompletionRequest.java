package com.llucascr.identify_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserEmailCompletionRequest(@NotBlank @Email @Size(max = 255) String email) {
    public UserEmailCompletionRequest {
        email = email == null ? null : email.strip();
    }
}
