package com.bank.payment_service.repos;

import com.bank.payment_service.domain.Payment;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;


public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    boolean existsByPaymentReferenceIgnoreCase(String paymentReference);

    Optional<Payment> findByTransferId(UUID transferId);
}
