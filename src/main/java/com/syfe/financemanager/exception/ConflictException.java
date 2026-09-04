package com.syfe.financemanager.exception;

/** Thrown for state conflicts such as duplicate usernames or category names. Mapped to HTTP 409. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
