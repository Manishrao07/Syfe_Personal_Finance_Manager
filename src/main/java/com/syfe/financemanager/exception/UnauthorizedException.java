package com.syfe.financemanager.exception;

/** Thrown for authentication failures such as bad login credentials. Mapped to HTTP 401. */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
