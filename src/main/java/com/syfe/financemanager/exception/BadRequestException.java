package com.syfe.financemanager.exception;

/** Thrown when a request violates a business rule (invalid category reference, default-category
 * mutation attempt, etc). Mapped to HTTP 400. */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
