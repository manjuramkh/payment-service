package com.bank.payment_service.model;

import java.util.UUID;

public record AccountSummary(

        UUID accountId,
        String accountNumber,
        String accountHolderName
) { }
