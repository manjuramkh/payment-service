package com.bank.payment_service.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;


@Entity
@Table(
        name = "Payments",
        indexes = {
                @Index(name = "idx_payment_reference", columnList = "payment_reference"),
                @Index(name = "idx_transfer_id", columnList = "transfer_id"),
                @Index(name = "idx_sender_account", columnList = "sender_account"),
                @Index(name = "idx_receiver_account", columnList = "receiver_account"),
                @Index(name = "idx_status", columnList = "status")
        }
)
@Getter
@Setter
@EqualsAndHashCode(of = "id")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String paymentReference;

    @Column(nullable = false)
    private UUID transferId;

    @Column(name = "source_account_id", nullable = false)
    private UUID senderAccount;

    @Column(name = "destination_account_id", nullable = false)
    private UUID receiverAccount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column
    @Enumerated(EnumType.STRING)
    private PaymentType paymentType;

    @Column
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Column
    private String channel;

    @Column(length = 255)
    private String remarks;

    @Column
    private Instant createdAt;

    @Column
    private Instant updatedAt;

}
