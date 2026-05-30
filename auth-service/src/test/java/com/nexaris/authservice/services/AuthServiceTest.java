package com.nexaris.authservice.services;

import com.nexaris.authservice.config.JwtTokenProvider;
import com.nexaris.authservice.dtos.LoginRequest;
import com.nexaris.authservice.dtos.LoginResponse;
import com.nexaris.authservice.dtos.UserRegistrationRequest;
import com.nexaris.authservice.entities.*;
import com.nexaris.authservice.exceptions.InvalidCredentialsException;
import com.nexaris.authservice.exceptions.ResourceNotFoundException;
import com.nexaris.authservice.exceptions.UnauthorizedException;
import com.nexaris.authservice.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PrivilegeRepository privilegeRepository;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private CountryRepository countryRepository;
    @Mock
    private LanguageRepository languageRepository;
    @Mock
    private GlobalSettingRepository settingsRepository;
    @Mock
    private NotificationClient notificationClient;
    @Mock
    private OrgRightsClient orgRightsClient;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private Role userRole;
    private Country testCountry;
    private Language testLanguage;

    @BeforeEach
    void setUp() {
        testCountry = new Country(10, "BE", "Belgique", "+32");
        testLanguage = new Language(10, "FR", "Français");
        userRole = new Role(10, "ROLE_USER", null);

        testUser = new User();
        testUser.setId(10);
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setEnabled(true);
        testUser.setCountry(testCountry);
        testUser.setLanguage(testLanguage);
        testUser.setRoles(Set.of(userRole));
    }

    @Test
    void register_ShouldCreateNewUser_WhenValidRequest() {
        // Given
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setEmail("new@example.com");
        request.setPassword("password123");
        request.setFirstName("New");
        request.setLastName("User");
        request.setFkCountry(1);
        request.setFkLanguage(1);

        when(settingsRepository.isRegistrationEnabled()).thenReturn(true);
        when(countryRepository.findByCode("BE")).thenReturn(Optional.of(testCountry));
        when(languageRepository.findByCode("FR")).thenReturn(Optional.of(testLanguage));
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        
        // Create expected user for return
        User expectedUser = new User();
        expectedUser.setId(11);
        expectedUser.setEmail("new@example.com");
        expectedUser.setPassword("encodedPassword");
        expectedUser.setFirstName("New");
        expectedUser.setLastName("User");
        expectedUser.setEnabled(true);
        expectedUser.setCountry(testCountry);
        expectedUser.setLanguage(testLanguage);
        expectedUser.setRoles(Set.of(userRole));
        
        when(userRepository.save(any(User.class))).thenReturn(expectedUser);

        // When
        User result = authService.register(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("new@example.com");
        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode("password123");
    }

    @Test
    void login_ShouldReturnLoginResponse_WhenCredentialsValid() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtTokenProvider.generateToken(10, "test@example.com", List.of("ROLE_USER"))).thenReturn("accessToken");
        when(jwtTokenProvider.generateRefreshToken(10)).thenReturn("refreshToken");
        when(jwtTokenProvider.getExpirationTimeInSeconds()).thenReturn(900L);

        // When
        LoginResponse response = authService.login(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("accessToken");
        assertThat(response.getRefreshToken()).isEqualTo("refreshToken");
        assertThat(response.getExpiresIn()).isEqualTo(900L);
        verify(userRepository).findByEmail("test@example.com");
        verify(passwordEncoder).matches("password123", "encodedPassword");
    }

    @Test
    void login_ShouldThrowInvalidCredentialsException_WhenUserNotFound() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("nonexistent@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Identifiants incorrects");
    }

    @Test
    void login_ShouldThrowUnauthorizedException_WhenUserDisabled() {
        // Given
        testUser.setEnabled(false);
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When & Then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Compte désactivé. Veuillez contacter l'administration.");
    }

    @Test
    void login_ShouldThrowInvalidCredentialsException_WhenPasswordIncorrect() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrongpassword");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpassword", "encodedPassword")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Identifiants incorrects");
    }

    @Test
    void deactivateUser_ShouldDisableUser_WhenUserExists() {
        // Given
        when(userRepository.findById(10)).thenReturn(Optional.of(testUser));

        // When
        authService.deactivateUser(10);

        // Then
        assertThat(testUser.isEnabled()).isFalse();
        // @Transactional uses dirty checking, so save() is not explicitly called
    }

    @Test
    void deactivateUser_ShouldThrowResourceNotFoundException_WhenUserNotFound() {
        // Given
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.deactivateUser(999))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Utilisateur non trouvé avec l'ID : 999");
    }

    @Test
    void anonymizeUser_ShouldClearRolesAndPurgeOrgRights_WhenUserExists() {
        when(userRepository.findById(10)).thenReturn(Optional.of(testUser));

        authService.anonymizeUser(10);

        assertThat(testUser.isEnabled()).isFalse();
        assertThat(testUser.getRoles()).isEmpty();
        verify(orgRightsClient).purgeUserRights(10);
    }

    @Test
    void changePassword_ShouldUpdatePassword_WhenCurrentPasswordCorrect() {
        when(userRepository.findById(10)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldpassword", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newStrongP@ssw0rd")).thenReturn("encodedNewPassword");

        authService.changePassword(10, "oldpassword", "newStrongP@ssw0rd");

        assertThat(testUser.getPassword()).isEqualTo("encodedNewPassword");
    }

    @Test
    void changePassword_ShouldThrowInvalidCredentialsException_WhenCurrentPasswordInvalid() {
        when(userRepository.findById(10)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpassword", "encodedPassword")).thenReturn(false);

        assertThatThrownBy(() -> authService.changePassword(10, "wrongpassword", "newpassword123"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Le mot de passe actuel est incorrect");
    }

    @Test
    void logout_ShouldRevokeToken() {
        String token = "jwt-token";

        authService.logout(token);

        verify(jwtTokenProvider).revokeToken(token);
    }
}