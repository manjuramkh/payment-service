package com.bank.payment_service.model;

import com.bank.payment_service.domain.PaymentStatus;
import com.bank.payment_service.domain.PaymentType;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class PaymentDTO {

    private UUID id;

    @NotNull
    @Size(max = 255)
    private String paymentReference;

    @NotNull
    private UUID transferId;

    @NotNull
    private UUID senderAccount;

    private UUID receiverAccount;

    @NotNull
    @Digits(integer = 10, fraction = 2)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Schema(type = "string", example = "92.08")
    private BigDecimal amount;

    @Size(max = 255)
    private String currency;

    @Size(max = 255)
    private PaymentType paymentType;

    @Size(max = 255)
    private PaymentStatus status;

    private Instant createdAt;

    private Instant updatedAt;

}
