package com.nexaris.authservice.dtos;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DtoValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void loginRequest_ShouldValidate_WhenValidData() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        // When
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    void loginRequest_ShouldFailValidation_WhenEmailInvalid() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("invalid-email");
        request.setPassword("password123");

        // When
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Le format de l'email est invalide");
    }

    @Test
    void loginRequest_ShouldFailValidation_WhenEmailBlank() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("");
        request.setPassword("password123");

        // When
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("L'email est obligatoire");
    }

    @Test
    void loginRequest_ShouldFailValidation_WhenPasswordBlank() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("");

        // When
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        // Then
        // Deux violations: @NotBlank et @Size
        assertThat(violations).hasSize(2);
        assertThat(violations).anyMatch(v -> v.getMessage().equals("Le mot de passe est obligatoire"));
        assertThat(violations).anyMatch(v -> v.getMessage().equals("Le mot de passe doit faire au moins 8 caractères"));
    }

    @Test
    void userRegistrationRequest_ShouldValidate_WhenValidData() {
        // Given
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setFirstName("Test");
        request.setLastName("User");
        request.setFkCountry(1);
        request.setFkLanguage(1);

        // When
        Set<ConstraintViolation<UserRegistrationRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    void userRegistrationRequest_ShouldFailValidation_WhenEmailInvalid() {
        // Given
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setEmail("invalid-email");
        request.setPassword("password123");
        request.setFirstName("Test");
        request.setLastName("User");
        request.setFkCountry(1);
        request.setFkLanguage(1);

        // When
        Set<ConstraintViolation<UserRegistrationRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Le format de l'email est invalide");
    }

    @Test
    void userRegistrationRequest_ShouldFailValidation_WhenPasswordTooShort() {
        // Given
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setEmail("test@example.com");
        request.setPassword("12345"); // Less than 8 characters
        request.setFirstName("Test");
        request.setLastName("User");
        request.setFkCountry(1);
        request.setFkLanguage(1);

        // When
        Set<ConstraintViolation<UserRegistrationRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Le mot de passe doit faire au moins 8 caractères");
    }

    @Test
    void userRegistrationRequest_ShouldFailValidation_WhenFirstNameBlank() {
        // Given
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setFirstName("");
        request.setLastName("User");
        request.setFkCountry(1);
        request.setFkLanguage(1);

        // When
        Set<ConstraintViolation<UserRegistrationRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Le prénom est obligatoire");
    }

    @Test
    void userRegistrationRequest_ShouldFailValidation_WhenLastNameBlank() {
        // Given
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setFirstName("Test");
        request.setLastName("");
        request.setFkCountry(1);
        request.setFkLanguage(1);

        // When
        Set<ConstraintViolation<UserRegistrationRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Le nom est obligatoire");
    }
}