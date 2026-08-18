package com.bank.payment_service.service;

import com.bank.payment_service.config.AccountClient;
import com.bank.payment_service.domain.IdempotencyRecord;
import com.bank.payment_service.domain.IdempotencyStatus;
import com.bank.payment_service.domain.Payment;
import com.bank.payment_service.domain.PaymentStatus;
import com.bank.payment_service.model.AccountResponse;
import com.bank.payment_service.model.AccountSummary;
import com.bank.payment_service.model.PaymentDTO;
import com.bank.payment_service.model.PaymentRequest;
import com.bank.payment_service.model.PaymentResponse;
import com.bank.payment_service.repos.IdempotencyRepository;
import com.bank.payment_service.repos.PaymentRepository;
import com.bank.payment_service.util.IdempotencyConflictException;
import com.bank.payment_service.util.InvalidIdempotencyKeyException;
import com.bank.payment_service.util.NotFoundException;
import com.bank.payment_service.util.PaymentAlreadyExistsException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final AccountClient accountClient;
    private final IdempotencyRepository idempotencyRepository;
    private final RequestHashService requestHashService;


    // =========================================================
    // GET ALL PAYMENTS
    // =========================================================

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<PaymentResponse> getAllPayments() {

        List<Payment> payments = paymentRepository.findAll(
                Sort.by(
                        Sort.Direction.DESC,
                        "createdAt"
                )
        );

        if (payments.isEmpty()) {
            throw new NotFoundException(
                    "There are no payments."
            );
        }

        return payments.stream()
                .map(this::mapToPaymentResponse)
                .toList();
    }


    // =========================================================
    // GET PAYMENT BY ID
    // =========================================================

    @Transactional(Transactional.TxType.SUPPORTS)
    public PaymentResponse getPayment(final UUID id) {

        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Payment not found with id: " + id
                        )
                );

        return mapToPaymentResponse(payment);
    }


    // =========================================================
    // CREATE PAYMENT
    // =========================================================

    @Transactional
    public PaymentResponse create(
            final PaymentRequest paymentRequest,
            final String idempotencyKey
    ) {

        // -----------------------------------------------------
        // 1. Validate Idempotency-Key
        // -----------------------------------------------------

        validateIdempotencyKey(idempotencyKey);


        // -----------------------------------------------------
        // 2. Validate Payment Request
        // -----------------------------------------------------

        validatePaymentRequest(paymentRequest);


        // -----------------------------------------------------
        // 3. Generate request hash
        // -----------------------------------------------------

        String requestHash =
                requestHashService.generateHash(paymentRequest);


        // -----------------------------------------------------
        // 4. Check existing idempotency record
        // -----------------------------------------------------

        var existingRecord =
                idempotencyRepository
                        .findByIdempotencyKey(idempotencyKey);

        if (existingRecord.isPresent()) {

            return handleExistingIdempotencyRecord(
                    existingRecord.get(),
                    requestHash
            );
        }


        // -----------------------------------------------------
        // 5. Check whether transfer already has payment
        // -----------------------------------------------------

        var existingPayment =
                paymentRepository.findByTransferId(
                        paymentRequest.transferId()
                );

        if (existingPayment.isPresent()) {

            throw new PaymentAlreadyExistsException(
                    "Payment already exists for transfer: "
                            + paymentRequest.transferId()
            );
        }


        // -----------------------------------------------------
        // 6. Create Idempotency Record
        // -----------------------------------------------------

        Instant now = Instant.now();

        IdempotencyRecord idempotencyRecord =
                new IdempotencyRecord();

        idempotencyRecord.setId(UUID.randomUUID());
        idempotencyRecord.setIdempotencyKey(idempotencyKey);
        idempotencyRecord.setRequestHash(requestHash);
        idempotencyRecord.setStatus(
                IdempotencyStatus.PROCESSING
        );
        idempotencyRecord.setCreatedAt(now);

        try {

            idempotencyRepository.saveAndFlush(
                    idempotencyRecord
            );

        } catch (Exception ex) {

            /*
             * Another concurrent request may have inserted
             * the same idempotency key.
             *
             * Re-read the record and return the existing
             * payment if it now exists.
             */

            var concurrentRecord =
                    idempotencyRepository
                            .findByIdempotencyKey(
                                    idempotencyKey
                            );

            if (concurrentRecord.isPresent()) {

                return handleExistingIdempotencyRecord(
                        concurrentRecord.get(),
                        requestHash
                );
            }

            throw ex;
        }


        // -----------------------------------------------------
        // 7. Create Payment
        // -----------------------------------------------------

        Payment payment = new Payment();

        payment.setId(UUID.randomUUID());

        payment.setPaymentReference(
                generatePaymentReference()
        );

        payment.setTransferId(
                paymentRequest.transferId()
        );

        payment.setSenderAccount(
                paymentRequest.sourceAccountId()
        );

        payment.setReceiverAccount(
                paymentRequest.destinationAccountId()
        );

        payment.setAmount(
                paymentRequest.amount()
        );

        payment.setCurrency(
                paymentRequest.currency()
        );

        payment.setPaymentType(
                paymentRequest.paymentType()
        );

        payment.setStatus(
                PaymentStatus.INITIATED
        );

        payment.setRemarks(
                paymentRequest.remarks()
        );

        payment.setCreatedAt(now);
        payment.setUpdatedAt(now);


        // -----------------------------------------------------
        // 8. Save Payment
        // -----------------------------------------------------

        paymentRepository.save(payment);


        // -----------------------------------------------------
        // 9. Update Idempotency Record
        // -----------------------------------------------------

        idempotencyRecord.setPaymentId(
                payment.getId()
        );

        idempotencyRecord.setStatus(
                IdempotencyStatus.COMPLETED
        );

        idempotencyRepository.save(
                idempotencyRecord
        );


        // -----------------------------------------------------
        // 10. Return Response
        // -----------------------------------------------------

        return mapToPaymentResponse(payment);
    }


    // =========================================================
    // HANDLE EXISTING IDEMPOTENCY RECORD
    // =========================================================

    private PaymentResponse handleExistingIdempotencyRecord(
            final IdempotencyRecord record,
            final String requestHash
    ) {

        // -----------------------------------------------------
        // Different request with same idempotency key
        // -----------------------------------------------------

        if (!record.getRequestHash().equals(requestHash)) {

            throw new IdempotencyConflictException(
                    "Idempotency-Key was already used "
                            + "with a different request."
            );
        }


        // -----------------------------------------------------
        // Request is still being processed
        // -----------------------------------------------------

        if (record.getStatus() ==
                IdempotencyStatus.PROCESSING) {

            throw new IdempotencyConflictException("A request with this Idempotency-Key is already being processed.");
        }


        // -----------------------------------------------------
        // Payment should exist for COMPLETED request
        // -----------------------------------------------------

        if (record.getPaymentId() == null) {

            throw new NotFoundException(
                    "Payment ID is missing from idempotency record."
            );
        }


        // -----------------------------------------------------
        // Retrieve original payment
        // -----------------------------------------------------

        Payment payment =
                paymentRepository.findById(
                                record.getPaymentId()
                        )
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Payment associated with "
                                                + "Idempotency-Key was not found."
                                )
                        );


        // -----------------------------------------------------
        // Return original payment response
        // -----------------------------------------------------

        return mapToPaymentResponse(payment);
    }


    // =========================================================
    // PAYMENT RESPONSE MAPPER
    // =========================================================

    private PaymentResponse mapToPaymentResponse(
            final Payment payment
    ) {

        AccountSummary sourceAccount =
                getAccountSummary(
                        payment.getSenderAccount()
                );

        AccountSummary destinationAccount =
                getAccountSummary(
                        payment.getReceiverAccount()
                );


        return new PaymentResponse(
                payment.getId(),
                payment.getPaymentReference(),
                payment.getTransferId(),
                sourceAccount,
                destinationAccount,
                payment.getAmount(),
                payment.getCurrency(),
                payment.getPaymentType(),
                payment.getStatus(),
                payment.getRemarks(),
                payment.getChannel(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }


    // =========================================================
    // ACCOUNT SUMMARY
    // =========================================================

    private AccountSummary getAccountSummary(
            final UUID accountId
    ) {

        if (accountId == null) {
            return null;
        }

        AccountResponse account =
                accountClient.getAccount(accountId);

        if (account == null) {
            throw new NotFoundException(
                    "Account not found: " + accountId
            );
        }

        return new AccountSummary(
                account.getAccountId(),
                account.getAccountNumber(),
                account.getAccountHolderName()
        );
    }


    // =========================================================
    // VALIDATE IDEMPOTENCY KEY
    // =========================================================

    private void validateIdempotencyKey(
            final String idempotencyKey
    ) {

        if (idempotencyKey == null ||
                idempotencyKey.isBlank()) {

            throw new InvalidIdempotencyKeyException(
                    "Idempotency-Key header is required."
            );
        }

        if (idempotencyKey.length() > 100) {

            throw new InvalidIdempotencyKeyException(
                    "Idempotency-Key must not exceed "
                            + "100 characters."
            );
        }

        if (!idempotencyKey.matches(
                "^[a-zA-Z0-9._:-]+$"
        )) {

            throw new InvalidIdempotencyKeyException(
                    "Idempotency-Key contains invalid characters."
            );
        }
    }


    // =========================================================
    // VALIDATE PAYMENT REQUEST
    // =========================================================

    private void validatePaymentRequest(
            final PaymentRequest request
    ) {

        if (request == null) {

            throw new IllegalArgumentException(
                    "Payment request cannot be null."
            );
        }

        if (request.transferId() == null) {

            throw new IllegalArgumentException(
                    "Transfer ID is required."
            );
        }

        if (request.sourceAccountId() == null) {

            throw new IllegalArgumentException(
                    "Source account ID is required."
            );
        }

        if (request.destinationAccountId() == null) {

            throw new IllegalArgumentException(
                    "Destination account ID is required."
            );
        }

        if (request.amount() == null ||
                request.amount().signum() <= 0) {

            throw new IllegalArgumentException(
                    "Payment amount must be greater than zero."
            );
        }

        if (request.currency() == null ||
                request.currency().isBlank()) {

            throw new IllegalArgumentException(
                    "Currency is required."
            );
        }

        if (request.paymentType() == null) {

            throw new IllegalArgumentException(
                    "Payment type is required."
            );
        }

        if (request.sourceAccountId().equals(
                request.destinationAccountId()
        )) {

            throw new IllegalArgumentException(
                    "Source and destination accounts "
                            + "cannot be the same."
            );
        }
    }


    // =========================================================
    // PAYMENT REFERENCE
    // =========================================================

    private String generatePaymentReference() {

        return "PAY-" +
                UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 12)
                        .toUpperCase();
    }


    // =========================================================
    // EXISTING CRUD
    // =========================================================

    public List<PaymentDTO> findAll() {

        final List<Payment> payments =
                paymentRepository.findAll(
                        Sort.by("id")
                );

        return payments.stream()
                .map(payment ->
                        mapToDTO(
                                payment,
                                new PaymentDTO()
                        )
                )
                .toList();
    }


    public void update(
            final UUID id,
            final PaymentDTO paymentDTO
    ) {

        final Payment payment =
                paymentRepository.findById(id)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Payment not found: "
                                                + id
                                )
                        );

        mapToEntity(
                paymentDTO,
                payment
        );

        payment.setUpdatedAt(
                Instant.now()
        );

        paymentRepository.save(payment);
    }


    public void delete(final UUID id) {

        final Payment payment =
                paymentRepository.findById(id)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Payment not found: "
                                                + id
                                )
                        );

        paymentRepository.delete(payment);
    }


    // =========================================================
    // DTO MAPPING
    // =========================================================

    private PaymentDTO mapToDTO(
            final Payment payment,
            final PaymentDTO paymentDTO
    ) {

        paymentDTO.setId(
                payment.getId()
        );

        paymentDTO.setPaymentReference(
                payment.getPaymentReference()
        );

        paymentDTO.setTransferId(
                payment.getTransferId()
        );

        paymentDTO.setSenderAccount(
                payment.getSenderAccount()
        );

        paymentDTO.setReceiverAccount(
                payment.getReceiverAccount()
        );

        paymentDTO.setAmount(
                payment.getAmount()
        );

        paymentDTO.setCurrency(
                payment.getCurrency()
        );

        paymentDTO.setPaymentType(
                payment.getPaymentType()
        );

        paymentDTO.setStatus(
                payment.getStatus()
        );

        paymentDTO.setCreatedAt(
                payment.getCreatedAt()
        );

        paymentDTO.setUpdatedAt(
                payment.getUpdatedAt()
        );

        return paymentDTO;
    }


    private Payment mapToEntity(
            final PaymentDTO paymentDTO,
            final Payment payment
    ) {

        payment.setPaymentReference(
                paymentDTO.getPaymentReference()
        );

        payment.setTransferId(
                paymentDTO.getTransferId()
        );

        payment.setSenderAccount(
                paymentDTO.getSenderAccount()
        );

        payment.setReceiverAccount(
                paymentDTO.getReceiverAccount()
        );

        payment.setAmount(
                paymentDTO.getAmount()
        );

        payment.setCurrency(
                paymentDTO.getCurrency()
        );

        payment.setPaymentType(
                paymentDTO.getPaymentType()
        );

        payment.setStatus(
                paymentDTO.getStatus()
        );

        return payment;
    }


    // =========================================================
    // PAYMENT REFERENCE EXISTS
    // =========================================================

    public boolean paymentReferenceExists(
            final String paymentReference
    ) {

        return paymentRepository
                .existsByPaymentReferenceIgnoreCase(
                        paymentReference
                );
    }
}