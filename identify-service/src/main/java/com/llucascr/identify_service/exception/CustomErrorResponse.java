package com.llucascr.identify_service.exception;

import java.time.Instant;

public record CustomErrorResponse(
        int code,
        String message,
        Instant timestamp,
        String path
) {
}
