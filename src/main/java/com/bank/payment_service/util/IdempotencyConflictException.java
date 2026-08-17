package com.bank.payment_service.util;

public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException(
            final String message) {
        super(message);
    }
}