package com.nexaris.authservice.exceptions;

/**
 * Exception levée quand une ressource utilisateur n'existe pas
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
