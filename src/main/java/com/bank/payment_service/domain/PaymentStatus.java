package com.bank.payment_service.domain;

public enum PaymentStatus {

    INITIATED,

    VALIDATION_IN_PROGRESS,

    VALIDATED,

    PROCESSING,

    SUCCESS,

    FAILED,

    REVERSED,

    CANCELLED
}