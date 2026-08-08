package com.bank.payment_service.model;

import com.bank.payment_service.domain.PaymentType;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRequest(
        UUID transferId,
        String sourceAccountId,
        String destinationAccountId,
        BigDecimal amount,
        String currency,
        PaymentType paymentType,
        String remarks
) {}
