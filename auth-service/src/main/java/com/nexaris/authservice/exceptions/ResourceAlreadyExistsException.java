package com.nexaris.authservice.exceptions;

/**
 * Exception levée lors de tentative de création avec une ressource existante (ex: email déjà utilisé)
 */
public class ResourceAlreadyExistsException extends RuntimeException {
    public ResourceAlreadyExistsException(String message) {
        super(message);
    }

    public ResourceAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
