package com.bank.payment_service.config;

import com.bank.payment_service.model.AccountSummary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.UUID;

@Slf4j
@Component
public class AccountClient {

    private WebClient webClient;

    public AccountClient(WebClient.Builder webClient){
        this.webClient=webClient
                .baseUrl("http://localhost:7071/api/v1/accounts")
                .build();
    }

    public AccountSummary getAccountDetails(UUID accountId){
        log.info("Fetching account details for accountId: {}", accountId);
        return webClient.get()
                .uri("/{accountId}", accountId)
                .retrieve()
                .bodyToMono(AccountSummary.class)
                .block();
    }
}
