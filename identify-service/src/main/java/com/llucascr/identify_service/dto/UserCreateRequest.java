package com.llucascr.identify_service.dto;

import jakarta.validation.constraints.*;

public record UserCreateRequest(
        @NotBlank @Pattern(regexp = "[0-9]{11}") String cpf,
        @NotBlank @Size(max = 255) String fullName,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8) String password
) {
    public UserCreateRequest {
        fullName = fullName == null ? null : fullName.strip();
        email = email == null ? null : email.strip();
    }
}
