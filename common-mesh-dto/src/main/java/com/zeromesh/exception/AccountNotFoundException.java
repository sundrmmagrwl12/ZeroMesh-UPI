package com.zeromesh.exception;

/**
 * Thrown when an account lookup by UPI ID fails.
 * Maps to HTTP 404 NOT FOUND.
 */
public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String upiId) {
        super("Account not found with UPI ID: " + upiId);
    }
}
