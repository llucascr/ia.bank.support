package com.llucascr.identify_service.controller;

import com.llucascr.identify_service.config.SecurityConfig;
import com.llucascr.identify_service.dto.*;
import com.llucascr.identify_service.exception.*;
import com.llucascr.identify_service.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, TokenService.class})
class UserControllerTests {
    @Autowired MockMvc mvc;
    @MockitoBean UserService service;
    @MockitoBean JwtDecoder jwtDecoder;
    @MockitoBean org.springframework.security.oauth2.jwt.JwtEncoder jwtEncoder;
    private final UUID id = UUID.randomUUID();

    private UserResponse response() {
        return UserResponse.builder().id(id).cpf("12345678901").fullName("Lucas")
                .email("lucas@example.com").role("CUSTOMER").status("ACTIVE").build();
    }

    @Test
    void allUserEndpointsRequireAuthentication() throws Exception {
        mvc.perform(get("/v1/users/me")).andExpect(status().isUnauthorized());
        mvc.perform(post("/v1/users").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createsUserWithoutExposingPassword() throws Exception {
        when(service.create(any())).thenReturn(response());
        mvc.perform(post("/v1/users").with(jwt()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"cpf":"12345678901","fullName":"Lucas","email":"lucas@example.com","password":"password123"}
                        """))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.password").doesNotExist()).andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void routesAllReadEndpoints() throws Exception {
        when(service.getById(id)).thenReturn(response());
        when(service.getCurrentUser(id)).thenReturn(CurrentUserResponse.builder().id(id).build());
        when(service.search("Lucas")).thenReturn(List.of(response()));
        mvc.perform(get("/v1/users/{id}", id).with(jwt())).andExpect(status().isOk());
        mvc.perform(get("/v1/users/me").with(jwt().jwt(token -> token.subject(id.toString()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id.toString()));
        mvc.perform(get("/v1/users/search").param("query", "Lucas").with(jwt()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id.toString()));
    }

    @Test
    void routesAllPatchEndpoints() throws Exception {
        when(service.update(eq(id), any())).thenReturn(response());
        when(service.completeEmail(id, "new@example.com")).thenReturn(response());
        when(service.activate(id)).thenReturn(response());
        when(service.deactivate(id)).thenReturn(response());
        mvc.perform(patch("/v1/users/{id}", id).with(jwt()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"fullName\":\"New name\"}")).andExpect(status().isOk());
        mvc.perform(patch("/v1/users/me/email").with(jwt().jwt(token -> token.subject(id.toString())))
                .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"new@example.com\"}"))
                .andExpect(status().isOk());
        mvc.perform(patch("/v1/users/{id}/activate", id).with(jwt())).andExpect(status().isOk());
        mvc.perform(patch("/v1/users/{id}/deactivate", id).with(jwt())).andExpect(status().isOk());
        verify(service).update(eq(id), any());
        verify(service).completeEmail(id, "new@example.com");
        verify(service).activate(id);
        verify(service).deactivate(id);
    }

    @Test
    void rejectsInvalidRequestsAndSubject() throws Exception {
        mvc.perform(post("/v1/users").with(jwt()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400));
        mvc.perform(patch("/v1/users/{id}", id).with(jwt()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"fullName\":\"   \"}")).andExpect(status().isBadRequest());
        mvc.perform(patch("/v1/users/me/email").with(jwt()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"invalid\"}")).andExpect(status().isBadRequest());
        mvc.perform(get("/v1/users/not-a-uuid").with(jwt())).andExpect(status().isBadRequest());
        mvc.perform(post("/v1/users").with(jwt()).contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/v1/users/search").with(jwt())).andExpect(status().isBadRequest());
        mvc.perform(get("/v1/users/me").with(jwt().jwt(token -> token.subject("invalid"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usesExistingNotFoundAndConflictResponses() throws Exception {
        when(service.getById(id)).thenThrow(new UserNotFoundException("User not found"));
        mvc.perform(get("/v1/users/{id}", id).with(jwt())).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));
        when(service.create(any())).thenThrow(new UserAlreadyExistsException("User already exists with this CPF"));
        mvc.perform(post("/v1/users").with(jwt()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"cpf":"12345678901","fullName":"Lucas","email":"lucas@example.com","password":"password123"}
                        """))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(409));
    }
}
