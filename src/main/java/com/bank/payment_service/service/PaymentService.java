package com.bank.payment_service.service;

import com.bank.payment_service.config.AccountClient;
import com.bank.payment_service.domain.Payment;
import com.bank.payment_service.domain.PaymentStatus;
import com.bank.payment_service.model.AccountSummary;
import com.bank.payment_service.model.PaymentDTO;
import com.bank.payment_service.model.PaymentRequest;
import com.bank.payment_service.model.PaymentResponse;
import com.bank.payment_service.repos.PaymentRepository;
import com.bank.payment_service.util.NotFoundException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;


@Service
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final AccountClient accountClient;

    public PaymentService(final PaymentRepository paymentRepository, final AccountClient accountClient) {
        this.paymentRepository = paymentRepository;
        this.accountClient= accountClient;
    }

    public List<PaymentDTO> findAll() {
        final List<Payment> payments = paymentRepository.findAll(Sort.by("id"));
        return payments.stream()
                .map(payment -> mapToDTO(payment, new PaymentDTO()))
                .toList();
    }

    public PaymentResponse get(final UUID id) {
        return null;
    }

    public String create(final PaymentRequest paymentRequest) throws Exception {
        Optional<Payment> existingPayment = paymentRepository.findById(paymentRequest.transferId());

        if (existingPayment.isPresent()){
            throw new Exception("Payment with transfer ID already exists.");
        }

        Payment payment = new Payment();
        
        String paymentReference = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        while (paymentReferenceExists(paymentReference)) {
            paymentReference = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }

        payment.setPaymentReference(paymentReference);
        payment.setTransferId(paymentRequest.transferId());
        payment.setSenderAccount(paymentRequest.sourceAccountId());
        payment.setReceiverAccount(paymentRequest.destinationAccountId());
        payment.setAmount(paymentRequest.amount());
        payment.setCurrency(paymentRequest.currency());
        payment.setPaymentType(paymentRequest.paymentType());
        payment.setStatus(PaymentStatus.INITIATED);
        payment.setChannel(paymentRequest.paymentType().toString());
        payment.setRemarks(paymentRequest.remarks());
        payment.setCreatedAt(Instant.now());
        payment.setUpdatedAt(Instant.now());
        
        paymentRepository.save(payment);
        
        return paymentReference;
    }

    public List<PaymentResponse> getAllPayments(){
        List<Payment> payments = paymentRepository.findAll();
        if (payments.isEmpty()){
            throw new NotFoundException("There are No Payments.");
        }

//        accountClient.getAccountDetails(payments.stream()
//                                                .map(Payment::getSenderAccount));

        List<PaymentResponse> paymentResponses = new ArrayList<>();

        for (Payment payment : payments){
            AccountSummary accountSummary1 = accountClient.getAccountDetails(payment.getSenderAccount());
            AccountSummary accountSummary2 = accountClient.getAccountDetails(payment.getReceiverAccount());
            PaymentResponse paymentResponse = new PaymentResponse(
                     payment.getId(),
                     payment.getPaymentReference(),
                     payment.getTransferId(),
                     new AccountSummary(
                             accountSummary1.accountId(),
                             accountSummary1.accountNumber(),
                             accountSummary1.accountHolderName()
                     ),
                     new AccountSummary(
                             accountSummary2.accountId(),
                             accountSummary2.accountNumber(),
                             accountSummary2.accountHolderName()
                     ),
                     payment.getAmount(),
                     payment.getCurrency(),
                     payment.getPaymentType(),
                     payment.getStatus(),
                     payment.getRemarks(),
                     payment.getChannel(),
                     payment.getCreatedAt(),
                     payment.getUpdatedAt()
             );

             paymentResponses.add(paymentResponse);
        }

        return paymentResponses;
    }

    public void update(final UUID id, final PaymentDTO paymentDTO) {
        final Payment payment = paymentRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        mapToEntity(paymentDTO, payment);
        paymentRepository.save(payment);
    }

    public void delete(final UUID id) {
        final Payment payment = paymentRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        paymentRepository.delete(payment);
    }

    private PaymentDTO mapToDTO(final Payment payment, final PaymentDTO paymentDTO) {
        paymentDTO.setId(payment.getId());
        paymentDTO.setPaymentReference(payment.getPaymentReference());
        paymentDTO.setTransferId(payment.getTransferId());
        paymentDTO.setSenderAccount(payment.getSenderAccount());
        paymentDTO.setReceiverAccount(payment.getReceiverAccount());
        paymentDTO.setAmount(payment.getAmount());
        paymentDTO.setCurrency(payment.getCurrency());
        paymentDTO.setPaymentType(payment.getPaymentType());
        paymentDTO.setStatus(payment.getStatus());
        paymentDTO.setCreatedAt(payment.getCreatedAt());
        paymentDTO.setUpdatedAt(payment.getUpdatedAt());
        return paymentDTO;
    }

    private Payment mapToEntity(final PaymentDTO paymentDTO, final Payment payment) {
        payment.setPaymentReference(paymentDTO.getPaymentReference());
        payment.setTransferId(paymentDTO.getTransferId());
        payment.setSenderAccount(paymentDTO.getSenderAccount());
        payment.setReceiverAccount(paymentDTO.getReceiverAccount());
        payment.setAmount(paymentDTO.getAmount());
        payment.setCurrency(paymentDTO.getCurrency());
        payment.setPaymentType(paymentDTO.getPaymentType());
        payment.setStatus(paymentDTO.getStatus());
        payment.setCreatedAt(paymentDTO.getCreatedAt());
        payment.setUpdatedAt(paymentDTO.getUpdatedAt());
        return payment;
    }

    public boolean paymentReferenceExists(final String paymentReference) {
        return paymentRepository.existsByPaymentReferenceIgnoreCase(paymentReference);
    }

}
