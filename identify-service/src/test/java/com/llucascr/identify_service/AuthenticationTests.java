package com.llucascr.identify_service;

import com.jayway.jsonpath.JsonPath;
import com.llucascr.identify_service.domain.entity.User;
import com.llucascr.identify_service.dto.AuthResponse;
import com.llucascr.identify_service.dto.UserCreateRequest;
import com.llucascr.identify_service.repository.UserRepository;
import com.llucascr.identify_service.service.AuthService;
import com.llucascr.identify_service.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@ActiveProfiles("auth-test")
class AuthenticationTests {
    static final String CPF = "12345678901";
    static final String PASSWORD = "password123";
    static final String REGISTRATION = """
            {"cpf":"12345678901","fullName":"Lucas","email":"LUCAS@example.com","password":"password123"}
            """;

    @Autowired RestTestClient http;
    @Autowired AuthService auth;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwords;
    @Autowired JwtDecoder decoder;
    @Autowired JwtEncoder encoder;
    @MockitoSpyBean TokenService tokens;

    @DynamicPropertySource
    static void jwtKeys(DynamicPropertyRegistry registry) {
        TestJwtKeys.registerProperties(registry);
    }

    @BeforeEach
    void cleanDatabase() {
        users.deleteAllInBatch();
        users.flush();
    }

    private AuthResponse registerDirectly() {
        return auth.register(new UserCreateRequest(CPF, "Lucas", "LUCAS@example.com", PASSWORD));
    }

    private EntityExchangeResult<String> post(String path, String body) {
        return http.post().uri(path).contentType(MediaType.APPLICATION_JSON).body(body)
                .exchange().expectBody(String.class).returnResult();
    }

    private EntityExchangeResult<String> get(String path) {
        return http.get().uri(path).exchange().expectBody(String.class).returnResult();
    }

    private EntityExchangeResult<String> get(String path, String token) {
        return http.get().uri(path).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange().expectBody(String.class).returnResult();
    }

    private static void assertStatus(int expected, EntityExchangeResult<String> response) {
        assertEquals(expected, response.getStatus().value(), response.getResponseBody());
    }

    private static String json(EntityExchangeResult<String> response, String path) {
        return JsonPath.read(response.getResponseBody(), path);
    }

    private static void assertJsonEquals(Object expected, EntityExchangeResult<String> response, String path) {
        assertEquals(expected, JsonPath.read(response.getResponseBody(), path));
    }

    private static void assertJsonFieldMissing(EntityExchangeResult<String> response, String field) {
        assertFalse(JsonPath.<java.util.Map<String, Object>>read(response.getResponseBody(), "$").containsKey(field));
    }

    @Test
    void registerAndLoginIssueSignedTokensAcceptedByProtectedRoute() {
        var registration = post("/v1/auth/register", REGISTRATION);
        assertStatus(201, registration);
        assertJsonEquals("Bearer", registration, "$.tokenType");
        assertJsonEquals(3600, registration, "$.expiresIn");
        assertNotNull(json(registration, "$.accessToken"));
        assertJsonFieldMissing(registration, "password");
        assertJsonFieldMissing(registration, "passwordHash");

        User user = users.findByCpf(CPF).orElseThrow();
        assertEquals("lucas@example.com", user.getEmail());
        assertEquals("CUSTOMER", user.getRole());
        assertEquals("ACTIVE", user.getStatus());
        assertNotEquals(PASSWORD, user.getPasswordHash());
        assertTrue(passwords.matches(PASSWORD, user.getPasswordHash()));

        var login = post("/v1/auth/login", """
                {"cpf":"12345678901","password":"password123"}
                """);
        assertStatus(200, login);
        assertNotNull(json(login, "$.accessToken"));
        assertJsonEquals("Bearer", login, "$.tokenType");
        assertJsonEquals(3600, login, "$.expiresIn");

        for (var result : List.of(registration, login)) {
            String token = json(result, "$.accessToken");
            var jwt = decoder.decode(token);
            assertEquals(user.getId().toString(), jwt.getSubject());
            assertEquals("RS256", jwt.getHeaders().get("alg"));
            assertEquals(3600, jwt.getExpiresAt().getEpochSecond() - jwt.getIssuedAt().getEpochSecond());
            var me = get("/v1/users/me", token);
            assertStatus(200, me);
            assertJsonEquals(user.getId().toString(), me, "$.id");
            assertJsonFieldMissing(me, "passwordHash");
        }
    }

    @Test
    void unknownCpfWrongPasswordAndBlockedAccountReturnSameError() {
        assertInvalidLogin(CPF, PASSWORD);
        postAndAssertStatus("/v1/auth/register", REGISTRATION, 201);
        assertInvalidLogin(CPF, "wrong");
        User user = users.findByCpf(CPF).orElseThrow();
        user.setStatus("BLOCKED");
        users.saveAndFlush(user);
        assertInvalidLogin(CPF, PASSWORD);
    }

    private void assertInvalidLogin(String cpf, String password) {
        var response = post("/v1/auth/login", """
                {"cpf":"%s","password":"%s"}
                """.formatted(cpf, password));
        assertStatus(401, response);
        assertJsonEquals(401, response, "$.code");
        assertJsonEquals("Invalid credentials", response, "$.message");
    }

    private void postAndAssertStatus(String path, String body, int status) {
        assertStatus(status, post(path, body));
    }

    @Test
    void validatesRequestsAndReportsDuplicateCpfAndEmail() {
        for (String path : new String[]{"/v1/auth/login", "/v1/auth/register"}) {
            postAndAssertStatus(path, "{}", 400);
            postAndAssertStatus(path, "{", 400);
        }
        postAndAssertStatus("/v1/auth/login", """
                {"cpf":"123","password":"x"}
                """, 400);
        postAndAssertStatus("/v1/auth/register", REGISTRATION.replace("password123", "short"), 400);
        postAndAssertStatus("/v1/auth/register", REGISTRATION.replace("LUCAS@example.com", "invalid"), 400);
        postAndAssertStatus("/v1/auth/register", REGISTRATION, 201);
        for (String body : new String[]{REGISTRATION, REGISTRATION.replace(CPF, "98765432100")}) {
            var response = post("/v1/auth/register", body);
            assertStatus(409, response);
            assertJsonEquals(409, response, "$.code");
        }
    }

    @Test
    void rejectsMissingExpiredAndIncorrectlySignedTokens() {
        assertStatus(401, get("/v1/users/me"));
        String expired = encoder.encode(JwtEncoderParameters.from(
                org.springframework.security.oauth2.jwt.JwsHeader.with(SignatureAlgorithm.RS256).build(),
                JwtClaimsSet.builder().subject(UUID.randomUUID().toString())
                        .issuedAt(Instant.now().minusSeconds(7200))
                        .expiresAt(Instant.now().minusSeconds(3600)).build())).getTokenValue();
        String valid = registerDirectly().accessToken();
        String[] parts = valid.split("\\.");
        char first = parts[2].charAt(0);
        String tampered = parts[0] + "." + parts[1] + "." + (first == 'A' ? "B" : "A") + parts[2].substring(1);
        for (String token : new String[]{expired, tampered}) {
            assertStatus(401, get("/v1/users/me", token));
        }
    }

    @Test
    void rollsBackRegistrationWhenTokenIssuanceFails() {
        doThrow(new IllegalStateException("Signing unavailable")).when(tokens).issue(any());
        assertThrows(IllegalStateException.class, this::registerDirectly);
        assertFalse(users.existsByCpf(CPF));
    }
}
