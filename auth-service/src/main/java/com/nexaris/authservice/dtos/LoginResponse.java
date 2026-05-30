package com.nexaris.authservice.dtos;

/**
 * Réponse retournée lors d'une connexion réussie
 * Contient le token JWT et les informations de base de l'utilisateur
 */
public class LoginResponse {
    private String token;
    private String refreshToken;
    private Long expiresIn; // en secondes
    private UserResponse user;

    // Constructeur personnalisé
    public LoginResponse(String token, String refreshToken, Long expiresIn, UserResponse user) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.user = user;
    }

    // Constructeur vide
    public LoginResponse() {}

    // Getters et Setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public UserResponse getUser() {
        return user;
    }

    public void setUser(UserResponse user) {
        this.user = user;
    }
}
