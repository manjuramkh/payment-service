package com.bank.payment_service.service;

import com.bank.payment_service.model.PaymentRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

@Component
public class RequestHashService {

    private final ObjectMapper objectMapper;

    public RequestHashService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String generateHash(PaymentRequest request) {

        try {
            String json = objectMapper.writeValueAsString(request);

            return DigestUtils.md5DigestAsHex(json.getBytes());

        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Unable to generate request hash", e);
        }
    }
}
