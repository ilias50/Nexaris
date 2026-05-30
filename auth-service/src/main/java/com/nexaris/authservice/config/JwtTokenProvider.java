package com.nexaris.authservice.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Composant responsable de la génération, validation et extraction des informations des JWT
 */
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationInMs;

    @Value("${jwt.refresh-expiration}")
    private long jwtRefreshExpirationInMs;

    private final Set<String> revokedTokens = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public void revokeToken(String token) {
        if (token != null && !token.isBlank()) {
            revokedTokens.add(token);
        }
    }

    public boolean isTokenRevoked(String token) {
        return token == null || revokedTokens.contains(token);
    }

    /**
     * Génère un JWT token pour un utilisateur
     * @param userId l'ID de l'utilisateur
     * @param email l'email de l'utilisateur
     * @return le token JWT signé
     */
    public String generateToken(Integer userId, String email, java.util.List<String> roles) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * Génère un refresh token pour un utilisateur
     * @param userId l'ID de l'utilisateur
     * @return le refresh token JWT signé
     */
    public String generateRefreshToken(Integer userId) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtRefreshExpirationInMs);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("type", "REFRESH")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * Valide un token JWT
     * @param token le token à valider
     * @return true si le token est valide, false sinon
     */
    public boolean validateToken(String token) {
        if (isTokenRevoked(token)) {
            return false;
        }
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parse(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    /**
     * Extrait l'ID utilisateur d'un token JWT
     * @param token le token
     * @return l'ID utilisateur
     */
    public Integer getUserIdFromToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return Integer.parseInt(claims.getSubject());
    }

    /**
     * Extrait l'email d'un token JWT
     * @param token le token
     * @return l'email utilisateur
     */
    public String getEmailFromToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.get("email", String.class);
    }

    /**
     * Vérifie si un token est expiré
     * @param token le token
     * @return true si expiré, false sinon
     */
    public boolean isTokenExpired(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return claims.getExpiration().before(new Date());
        } catch (JwtException e) {
            return true;
        }
    }

    /**
     * Extrait les rôles d'un token JWT
     * @param token le token
     * @return la liste des rôles, ou un rôle USER par défaut
     */
    public java.util.List<String> getRolesFromToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Object rolesClaim = claims.get("roles");
            if (rolesClaim instanceof java.util.List<?> rolesList) {
                return rolesList.stream()
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .toList();
            }
        } catch (JwtException e) {
            // Rien, on retourne un rôle par défaut après
        }
        return java.util.List.of("ROLE_USER");
    }

    /**
     * Retourne le temps d'expiration en secondes
     * @return expirationInMs / 1000
     */
    public long getExpirationTimeInSeconds() {
        return jwtExpirationInMs / 1000;
    }
}
