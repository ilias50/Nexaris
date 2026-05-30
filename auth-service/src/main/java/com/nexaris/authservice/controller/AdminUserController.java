package com.nexaris.authservice.controller;

import com.nexaris.authservice.dtos.UserRegistrationRequest;
import com.nexaris.authservice.entities.User;
import com.nexaris.authservice.services.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/admin")
@PreAuthorize("hasRole('ADMIN')") // Toute la classe est réservée aux admins
public class AdminUserController {

    private final AuthService authService;

    public AdminUserController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/users")
    public ResponseEntity<String> createUser(@Valid @RequestBody UserRegistrationRequest request) {
        User createdUser = authService.registerByAdmin(request);
        return ResponseEntity.ok("Utilisateur " + createdUser.getFirstName() + " " + createdUser.getLastName() + " créé avec succès !");
    }

    @PostMapping("/{id}/roles/{roleName}")
    public User assignRole(@PathVariable Integer id, @PathVariable String roleName) {
        return authService.addRoleToUser(id, roleName);
    }

    @DeleteMapping("/{id}/roles/{roleName}")
    public User revokeRole(@PathVariable Integer id, @PathVariable String roleName) {
        return authService.removeRoleFromUser(id, roleName);
    }

    @PatchMapping("/registration-toggle")
    public ResponseEntity<String> toggleRegistration(@RequestParam boolean enabled) {
        authService.updateRegistrationStatus(enabled);
        return ResponseEntity.ok("Statut de l'inscription mis à jour : " + (enabled ? "OUVERT" : "FERMÉ"));
    }


}
