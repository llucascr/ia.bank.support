package com.llucascr.identify_service.controller.swagger;

import com.llucascr.identify_service.dto.*;
import com.llucascr.identify_service.exception.CustomErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.oauth2.jwt.Jwt;
import java.util.List;
import java.util.UUID;

@Tag(name = "Users", description = "User registration, profile and status management")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = CustomErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content(schema = @Schema(implementation = CustomErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "CPF or email already registered", content = @Content(schema = @Schema(implementation = CustomErrorResponse.class)))
})
public interface UserControllerSwagger {
    @Operation(summary = "Get the authenticated user's profile")
    CurrentUserResponse getCurrentUser(@Parameter(hidden = true) Jwt jwt);

    @Operation(summary = "Change the authenticated user's email")
    UserResponse completeEmail(UserEmailCompletionRequest request, @Parameter(hidden = true) Jwt jwt);

    @Operation(summary = "Create a user", responses = @ApiResponse(responseCode = "201", description = "User created"))
    UserResponse create(UserCreateRequest request);

    @Operation(summary = "Get a user by UUID")
    UserResponse getById(UUID id);

    @Operation(summary = "Search users by name, email or exact CPF")
    List<UserResponse> search(String query);

    @Operation(summary = "Partially update a user's name and email")
    UserResponse update(UUID id, UserUpdateRequest request);

    @Operation(summary = "Activate a user")
    UserResponse activate(UUID id);

    @Operation(summary = "Block a user")
    UserResponse deactivate(UUID id);
}
