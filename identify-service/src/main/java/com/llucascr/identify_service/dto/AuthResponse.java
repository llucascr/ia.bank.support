package com.llucascr.identify_service.dto;

public record AuthResponse(String accessToken, String tokenType, long expiresIn) {}
