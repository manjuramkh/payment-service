package com.bank.payment_service.util;

public class PaymentAlreadyExistsException extends RuntimeException {

    public PaymentAlreadyExistsException(
            final String message) {
        super(message);
    }
}