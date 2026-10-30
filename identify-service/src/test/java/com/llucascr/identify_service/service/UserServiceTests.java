package com.llucascr.identify_service.service;

import com.llucascr.identify_service.domain.entity.User;
import com.llucascr.identify_service.dto.*;
import com.llucascr.identify_service.exception.*;
import com.llucascr.identify_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserServiceTests {

    private final UserRepository repository = mock(UserRepository.class);
    private final PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
    private final UserService service = new UserService(repository, encoder);
    private final UUID id = UUID.randomUUID();
    private User user;

    @BeforeEach
    void setup() {
        user = User.builder().id(id).cpf("12345678901").fullName("Lucas").email("old@example.com")
                .passwordHash("original-hash").role("CUSTOMER").status("ACTIVE").build();
        lenient().when(repository.findById(id)).thenReturn(Optional.of(user));
        lenient().when(repository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsCustomerWithEncodedPasswordAndNormalizedEmail() {
        UserResponse response = service.create(new UserCreateRequest("12345678901", " Lucas ", " LUCAS@example.com ", "password123"));
        var captor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(repository).saveAndFlush(captor.capture());
        assertTrue(encoder.matches("password123", captor.getValue().getPasswordHash()));
        assertEquals("lucas@example.com", response.email());
        assertEquals("Lucas", response.fullName());
        assertEquals("CUSTOMER", response.role());
        assertEquals("ACTIVE", response.status());
    }

    @Test
    void rejectsDuplicateCpfAndEmail() {
        var request = new UserCreateRequest("12345678901", "Lucas", "new@example.com", "password123");
        when(repository.existsByCpf(request.cpf())).thenReturn(true);
        assertThrows(UserAlreadyExistsException.class, () -> service.create(request));
        when(repository.existsByCpf(request.cpf())).thenReturn(false);
        when(repository.existsByEmailIgnoreCase(request.email())).thenReturn(true);
        assertThrows(UserAlreadyExistsException.class, () -> service.create(request));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void missingUserUsesExistingException() {
        assertThrows(UserNotFoundException.class, () -> service.getById(UUID.randomUUID()));
    }

    @Test
    void updatesOnlyProvidedFields() {
        UserResponse response = service.update(id, new UserUpdateRequest("New name", null));
        assertEquals("New name", response.fullName());
        assertEquals("old@example.com", response.email());
        assertEquals("original-hash", user.getPasswordHash());
        assertEquals("12345678901", user.getCpf());
    }

    @Test
    void changesOwnEmailAndRejectsAnotherUsersEmail() {
        assertEquals("new@example.com", service.completeEmail(id, " NEW@example.com ").email());
        when(repository.existsByEmailIgnoreCaseAndIdNot("taken@example.com", id)).thenReturn(true);
        assertThrows(UserAlreadyExistsException.class, () -> service.completeEmail(id, "taken@example.com"));
        assertEquals("new@example.com", user.getEmail());
    }

    @Test
    void repeatedStatusChangesAreAllowed() {
        assertEquals("BLOCKED", service.deactivate(id).status());
        assertEquals("BLOCKED", service.deactivate(id).status());
        assertEquals("ACTIVE", service.activate(id).status());
        assertEquals("ACTIVE", service.activate(id).status());
    }

    @Test
    void currentUserReturnsProfile() {
        assertEquals(id, service.getCurrentUser(id).id());
    }

    @Test
    void searchReturnsResultsOrEmptyListAndRejectsBlankTerm() {
        when(repository.search("Lucas", "%Lucas%")).thenReturn(List.of(user));
        assertEquals(1, service.search(" Lucas ").size());
        assertTrue(service.search("missing").isEmpty());
        assertThrows(ResponseStatusException.class, () -> service.search(" "));
    }

    @Test
    void searchTreatsSqlWildcardsAsLiteralCharacters() {
        service.search("a_%");
        verify(repository).search("a_%", "%a\\_\\%%");
    }
}
