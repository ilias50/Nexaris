package com.nexaris.authservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexaris.authservice.dtos.LoginRequest;
import com.nexaris.authservice.dtos.LoginResponse;
import com.nexaris.authservice.dtos.PasswordChangeRequest;
import com.nexaris.authservice.dtos.UserRegistrationRequest;
import com.nexaris.authservice.entities.User;
import com.nexaris.authservice.services.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void register_ShouldReturn201_WhenValidRequest() {
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setFirstName("Test");
        request.setLastName("User");
        request.setFkCountry(1);
        request.setFkLanguage(1);

        User createdUser = new User();
        createdUser.setId(14);
        createdUser.setEmail("test@example.com");
        createdUser.setFirstName("Test");
        createdUser.setLastName("User");

        when(authService.register(any(UserRegistrationRequest.class))).thenReturn(createdUser);

        ResponseEntity<String> response = authController.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo("Utilisateur Test User créé avec succès !");
    }

    @Test
    void login_ShouldReturn200_WhenValidCredentials() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        LoginResponse expected = new LoginResponse("token123", "refreshToken123", 900L, null);
        when(authService.login(any(LoginRequest.class))).thenReturn(expected);

        ResponseEntity<LoginResponse> response = authController.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
    }

    @Test
    void verifyToken_ShouldReturnBadRequest_WhenAuthorizationHeaderMissing() {
        ResponseEntity<?> response = authController.verifyToken(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void verifyToken_ShouldReturnOk_WhenTokenValid() {
        String token = "Bearer valid-token";

        when(authService.validateToken("valid-token")).thenReturn(true);
        when(authService.getUserIdFromToken("valid-token")).thenReturn(1);

        ResponseEntity<?> response = authController.verifyToken(token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((java.util.Map<?, ?>) response.getBody()).get("valid")).isEqualTo(true);
    }

    @Test
    void logout_ShouldReturnOk_WhenTokenProvided() {
        String token = "Bearer valid-token";

        ResponseEntity<String> response = authController.logout(token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("Déconnexion effectuée");
        verify(authService).logout("valid-token");
    }

    @Test
    void changePassword_ShouldReturnOk_WhenRequestValid() {
        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setCurrentPassword("old1234");
        request.setNewPassword("new123456");
        Authentication authentication = mock(Authentication.class);

        ResponseEntity<String> response = authController.changePassword(10, request, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("Mot de passe mis à jour avec succès");
        verify(authService).ensureSelfOrAdmin(authentication, 10);
        verify(authService).changePassword(10, "old1234", "new123456");
    }
}
