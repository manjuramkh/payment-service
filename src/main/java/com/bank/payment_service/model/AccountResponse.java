package com.bank.payment_service.model;

import com.bank.payment_service.domain.PaymentStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountResponse {

    private UUID accountId;
    private String accountNumber;
    private String accountHolderName;
    private String accountType;
    private BigDecimal balance;
    private String accountStatus;

    // Used only for fallback information
    private String message;
    private String errorCode;
    private String status;
    private PaymentStatus paymentStatus;
    private LocalDate date;
}