package com.llucascr.identify_service.controller.swagger;

import com.llucascr.identify_service.dto.*;
import com.llucascr.identify_service.exception.CustomErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Authentication", description = "CPF and password authentication")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = CustomErrorResponse.class))),
    @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content(schema = @Schema(implementation = CustomErrorResponse.class)))
})
public interface AuthControllerSwagger {
    @Operation(summary = "Authenticate with CPF and password", responses = @ApiResponse(responseCode = "200", description = "Authenticated"))
    AuthResponse login(LoginRequest request);

    @Operation(summary = "Register and authenticate", responses = {
        @ApiResponse(responseCode = "201", description = "Registered and authenticated"),
        @ApiResponse(responseCode = "409", description = "CPF or email already registered", content = @Content(schema = @Schema(implementation = CustomErrorResponse.class)))
    })
    AuthResponse register(UserCreateRequest request);
}
