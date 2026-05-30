package com.nexaris.authservice.config;

import com.nexaris.authservice.exceptions.InvalidCredentialsException;
import com.nexaris.authservice.exceptions.ResourceAlreadyExistsException;
import com.nexaris.authservice.exceptions.ResourceNotFoundException;
import com.nexaris.authservice.exceptions.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @Test
    void handleInvalidCredentialsException_ShouldReturn401() {
        // Given
        InvalidCredentialsException exception = new InvalidCredentialsException("Identifiants incorrects");

        // When
        ResponseEntity<?> response = exceptionHandler.handleInvalidCredentials(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body.get("status")).isEqualTo(401);
        assertThat(body.get("message")).isEqualTo("Identifiants incorrects");
        assertThat(body.get("success")).isEqualTo(false);
    }

    @Test
    void handleUnauthorizedException_ShouldReturn403() {
        // Given
        UnauthorizedException exception = new UnauthorizedException("Accès non autorisé");

        // When
        ResponseEntity<?> response = exceptionHandler.handleUnauthorized(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body.get("status")).isEqualTo(403);
        assertThat(body.get("message")).isEqualTo("Accès non autorisé");
    }

    @Test
    void handleResourceNotFoundException_ShouldReturn404() {
        // Given
        ResourceNotFoundException exception = new ResourceNotFoundException("Utilisateur non trouvé");

        // When
        ResponseEntity<?> response = exceptionHandler.handleResourceNotFound(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body.get("status")).isEqualTo(404);
        assertThat(body.get("message")).isEqualTo("Utilisateur non trouvé");
    }

    @Test
    void handleResourceAlreadyExistsException_ShouldReturn409() {
        // Given
        ResourceAlreadyExistsException exception = new ResourceAlreadyExistsException("Utilisateur existe déjà");

        // When
        ResponseEntity<?> response = exceptionHandler.handleResourceAlreadyExists(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body.get("status")).isEqualTo(409);
        assertThat(body.get("message")).isEqualTo("Utilisateur existe déjà");
    }

    @Test
    void handleGenericException_ShouldReturn500() {
        // Given
        Exception exception = new RuntimeException("Erreur interne");

        // When
        ResponseEntity<?> response = exceptionHandler.handleGenericException(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body.get("status")).isEqualTo(500);
        assertThat(body.get("message")).isEqualTo("Une erreur interne s'est produite");
    }
}