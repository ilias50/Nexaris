package com.nexaris.authservice.exceptions;

/**
 * Exception levée quand une opération n'est pas autorisée
 * Ex: compte désactivé ou utilisateur suspendu
 */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}
