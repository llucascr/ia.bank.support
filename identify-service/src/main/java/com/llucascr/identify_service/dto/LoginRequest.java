package com.llucascr.identify_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginRequest(
        @NotBlank @Pattern(regexp = "[0-9]{11}") String cpf,
        @NotBlank String password
) {}
