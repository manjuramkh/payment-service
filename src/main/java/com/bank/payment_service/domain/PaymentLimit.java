package com.bank.payment_service.domain;


import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "payment_limits",
        indexes = {
                @Index(name = "idx_customer_id", columnList = "customer_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentLimit {

    @Id
    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "daily_limit", nullable = false, precision = 19, scale = 2)
    private BigDecimal dailyLimit;

    @Column(name = "monthly_limit", nullable = false, precision = 19, scale = 2)
    private BigDecimal monthlyLimit;

    @Column(name = "used_today", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal usedToday = BigDecimal.ZERO;

    @Column(name = "used_month", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal usedMonth = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
