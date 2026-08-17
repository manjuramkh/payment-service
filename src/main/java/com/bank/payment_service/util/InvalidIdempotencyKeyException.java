package com.bank.payment_service.util;

public class InvalidIdempotencyKeyException
        extends RuntimeException {

    public InvalidIdempotencyKeyException(
            final String message) {
        super(message);
    }
}
