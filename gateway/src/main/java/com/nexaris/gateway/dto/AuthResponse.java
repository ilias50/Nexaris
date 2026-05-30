package com.nexaris.gateway.dto;

import java.util.List;

public class AuthResponse {
    private boolean valid;
    private Integer userId;
    private List<String> roles;
    private String message;

    // Constructeurs
    public AuthResponse() {}
    public AuthResponse(boolean valid, Integer userId, String message) {
        this.valid = valid;
        this.userId = userId;
        this.message = message;
    }

    // Getters et Setters (Écris-les explicitement !)
    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}