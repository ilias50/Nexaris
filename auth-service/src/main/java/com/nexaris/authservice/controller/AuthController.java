package com.nexaris.authservice.controller;

import com.nexaris.authservice.dtos.LoginRequest;
import com.nexaris.authservice.dtos.LoginResponse;
import com.nexaris.authservice.dtos.CountryOptionResponse;
import com.nexaris.authservice.dtos.PasswordChangeRequest;
import com.nexaris.authservice.dtos.UserRegistrationRequest;
import com.nexaris.authservice.dtos.UserResponse;
import com.nexaris.authservice.entities.User;
import com.nexaris.authservice.services.AuthService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    // Injection par constructeur (Meilleure pratique)
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody UserRegistrationRequest request) {


        User newUser = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Utilisateur " + newUser.getFirstName() + " " + newUser.getLastName() + " créé avec succès !");
    }

    @GetMapping("/registration-enabled")
    public ResponseEntity<Map<String, Boolean>> getRegistrationStatus() {
        return ResponseEntity.ok(Map.of("enabled", authService.isRegistrationEnabled()));
    }

    @GetMapping("/countries")
    public ResponseEntity<List<CountryOptionResponse>> getCountries() {
        return ResponseEntity.ok(authService.getCountries());
    }

    @PostMapping("/login")
    @RateLimiter(name = "login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PutMapping("/user/{id}")
    public ResponseEntity<String> update(@PathVariable Integer id,
                                         @RequestBody UserRegistrationRequest request,
                                         Authentication authentication) {
        authService.ensureSelfOrAdmin(authentication, id);
        User updatedUser = authService.updateUser(id, request);
        return ResponseEntity.ok("Utilisateur " + updatedUser.getFirstName() + " " + updatedUser.getLastName() + " mis à jour !");
    }

    @DeleteMapping("/user/{id}")
    public ResponseEntity<String> deactivate(@PathVariable Integer id, Authentication authentication) {
        authService.ensureSelfOrAdmin(authentication, id);
        authService.deactivateUser(id);
        return ResponseEntity.ok("Compte désactivé avec succès");
    }

    @PatchMapping("/user/{id}/anonymize")
    public ResponseEntity<String> anonymize(@PathVariable Integer id, Authentication authentication) {
        authService.ensureSelfOrAdmin(authentication, id);
        authService.anonymizeUser(id);
        return ResponseEntity.ok("Données de l'utilisateur anonymisées conformément au RGPD.");
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body("En-tête Authorization manquant ou invalide");
        }

        String token = authHeader.substring(7);
        authService.logout(token);
        return ResponseEntity.ok("Déconnexion effectuée");
    }

    @PostMapping("/user/{id}/password")
    public ResponseEntity<String> changePassword(@PathVariable Integer id,
                                                 @Valid @RequestBody PasswordChangeRequest request,
                                                 Authentication authentication) {
        authService.ensureSelfOrAdmin(authentication, id);
        authService.changePassword(id, request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok("Mot de passe mis à jour avec succès");
    }

    @GetMapping("/user/{id}")
    public ResponseEntity<UserResponse> getUserProfile(@PathVariable Integer id, Authentication authentication) {
        authService.ensureSelfOrAdmin(authentication, id);
        return ResponseEntity.ok(authService.getUserResponse(id));
    }

    @GetMapping("/internal/user/{id}")
    public ResponseEntity<UserResponse> getInternalUserProfile(@PathVariable Integer id) {
        return ResponseEntity.ok(authService.getUserResponse(id));
    }

    @PostMapping(value = "/user/{id}/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadProfileImage(@PathVariable Integer id,
                                                                  @RequestPart("file") MultipartFile file,
                                                                  Authentication authentication) {
        authService.ensureSelfOrAdmin(authentication, id);
        String url = authService.uploadProfileImage(id, file);
        return ResponseEntity.ok(Map.of("profileImageUrl", url));
    }

    @GetMapping("/user/{id}/profile-image")
    public ResponseEntity<Resource> getProfileImage(@PathVariable Integer id) {
        Resource image = authService.getProfileImage(id);
        String mimeType = authService.getProfileImageContentType(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mimeType))
                .body(image);
    }

    @DeleteMapping("/user/{id}/profile-image")
    public ResponseEntity<String> deleteProfileImage(@PathVariable Integer id, Authentication authentication) {
        authService.ensureSelfOrAdmin(authentication, id);
        authService.deleteProfileImage(id);
        return ResponseEntity.ok("Photo de profil supprimée");
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers(Authentication authentication) {
        authService.ensureAdmin(authentication);
        return ResponseEntity.ok(authService.findAllUserResponses());
    }

    /**
     * Vérifie la validité d'un token JWT.
     * Les exceptions de validation sont gérées par le GlobalExceptionHandler.
     */
    @PostMapping("/verify-token")
    public ResponseEntity<Map<String, Object>> verifyToken(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(Map.of("valid", false, "message", "Format d'entête invalide"));
        }

        String token = authHeader.substring(7);
        if (authService.validateToken(token)) {
            Integer userId = authService.getUserIdFromToken(token);
            return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "userId", userId,
                    "roles", authService.getUserRolesFromToken(token)
            ));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("valid", false, "message", "Token invalide ou expiré"));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<LoginResponse> refreshToken(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().build();
        }

        String token = authHeader.substring(7);
        // On valide le token actuel avant d'en générer un nouveau
        if (authService.validateToken(token)) {
            Integer userId = authService.getUserIdFromToken(token);
            return ResponseEntity.ok(authService.refreshAccessToken(userId));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

}