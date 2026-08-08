package com.bank.payment_service.model;

import com.bank.payment_service.domain.PaymentStatus;
import com.bank.payment_service.domain.PaymentType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID paymentId,

        String paymentReference,

        UUID transferId,

        AccountSummary sourceAccount,

        AccountSummary destinationAccount,

        BigDecimal amount,

        String currency,

        PaymentType paymentType,

        PaymentStatus status,

        String remarks,

        String channel,

        Instant createdAt,

//        Instant processedAt,

        Instant completedAt
) { }
