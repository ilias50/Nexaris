package com.nexaris.authservice.exceptions;

/**
 * Exception levée lors d'une tentative de connexion avec des identifiants incorrects
 */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }

    public InvalidCredentialsException(String message, Throwable cause) {
        super(message, cause);
    }
}
