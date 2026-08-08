package com.bank.payment_service.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
public class AccountClient {

    private WebClient webClient;

    public AccountClient(WebClient.Builder webClient){
        this.webClient=webClient
                .baseUrl("http://localhost:7071/api/v1/accounts")
                .build();
    }
}
