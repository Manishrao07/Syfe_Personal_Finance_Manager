package com.syfe.financemanager.exception;

/** Thrown when a user attempts to access or modify another user's resource. Mapped to HTTP 403. */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
