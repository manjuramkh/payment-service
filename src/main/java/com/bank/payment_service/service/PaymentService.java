package com.bank.payment_service.service;

import com.bank.payment_service.domain.Payment;
import com.bank.payment_service.model.AccountSummary;
import com.bank.payment_service.model.PaymentDTO;
import com.bank.payment_service.model.PaymentRequest;
import com.bank.payment_service.model.PaymentResponse;
import com.bank.payment_service.repos.PaymentRepository;
import com.bank.payment_service.util.NotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;


@Service
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(final PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
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

    public String create(final PaymentRequest paymentRequest) {
//        paymentRepository.fin

        return null;
    }

    public List<PaymentResponse> getAllPayments(){
        List<Payment> payments = paymentRepository.findAll();
        if (payments.isEmpty()){
            throw new NotFoundException("There are No Payments.");
        }

        List<PaymentResponse> paymentResponses = new ArrayList<>();

        for (Payment payment : payments){
             PaymentResponse paymentResponse = new PaymentResponse(
                     payment.getId(),
                     payment.getPaymentReference(),
                     payment.getTransferId(),
                     new AccountSummary(
                             payment.getSenderAccount(),
                             null,
                             null
                     ),
                     new AccountSummary(
                             payment.getReceiverAccount(),
                             null,
                             null
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
