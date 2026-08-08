package com.bank.payment_service.rest;

import com.bank.payment_service.model.PaymentRequest;
import com.bank.payment_service.model.PaymentResponse;
import com.bank.payment_service.service.PaymentService;

import java.util.List;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@RestController
@RequestMapping(value = "/api/v1/payments", produces = MediaType.APPLICATION_JSON_VALUE)
public class PaymentResource {

    private final PaymentService paymentService;

    public PaymentResource(final PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/payment")
    public ResponseEntity<String> createPayment(@RequestBody PaymentRequest paymentRequest) throws Exception {
        String msg = paymentService.create(paymentRequest);
        log.info("Payment has been created.");
        return new ResponseEntity<>(msg, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPayment(UUID id){
        PaymentResponse paymentResponse =  paymentService.get(id);
        log.info("Payment details of id: "+paymentResponse.paymentId());
        return new ResponseEntity<>(paymentResponse, HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<List<PaymentResponse>> getAllPayments(){
        List<PaymentResponse> allPayments = paymentService.getAllPayments();
        return new ResponseEntity<>(allPayments, HttpStatus.OK);
    }

    @GetMapping("/{paymentId}/status")
    public String paymentStatus(){

        return null;
    }

    @GetMapping("/{paymentId}/timeline")
    public String paymentTimeline(){

        return null;
    }

}
