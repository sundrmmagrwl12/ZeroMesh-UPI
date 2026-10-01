package com.zeromesh.exception;

import java.math.BigDecimal;

/**
 * Thrown when an account has insufficient balance to complete a payment instruction.
 * Maps to HTTP 422 UNPROCESSABLE ENTITY.
 */
public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(String upiId, BigDecimal balance, BigDecimal requestedAmount) {
        super(String.format("Account '%s' has insufficient balance. Available: ₹%s, Required: ₹%s",
                upiId, balance, requestedAmount));
    }
}
