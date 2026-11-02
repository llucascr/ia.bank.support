package com.llucascr.identify_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @Size(max = 255) @Pattern(regexp = "(?s).*\\S.*") String fullName,
        @Email @Size(max = 255) @Pattern(regexp = "(?s).*\\S.*") String email
) {
    public UserUpdateRequest {
        fullName = fullName == null ? null : fullName.strip();
        email = email == null ? null : email.strip();
    }
}
