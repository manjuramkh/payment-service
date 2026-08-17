package com.bank.payment_service.config;

import com.bank.payment_service.domain.PaymentStatus;
import com.bank.payment_service.model.AccountResponse;
import com.bank.payment_service.model.AccountSummary;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.List;
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

    @CircuitBreaker(
            name = "accountService",
            fallbackMethod = "getAccountDetailsFallback"
    )
    public List<AccountSummary> getAccountDetails(){
        return webClient.get()
                .retrieve()
                .bodyToMono(AccountSummary.class)
                .map(accountSummary -> List.of(accountSummary))
                .block();
    }

    @CircuitBreaker(
            name = "accountService",
            fallbackMethod = "getAccountFallback"
    )
    public AccountResponse getAccount(UUID accountId) {
        log.info("Fetching account details for accountId: {}", accountId);
        return webClient.get()
                .uri("/{accountId}", accountId)
                .retrieve()
                .bodyToMono(AccountResponse.class)
                .block();
    }

    private AccountSummary getAccountFallback(UUID accountId, Throwable throwable) {
        log.error("Error fetching account for accountId: {}. Error: {}", accountId, throwable.getMessage());
        // Return a default or empty AccountResponse object
        return new AccountSummary(
                null,
                "SERVICE_UNAVAILABLE",
                "Account service is currently unavailable. Please try again later."
        );
    }

    private AccountSummary getAccountDetailsFallback(UUID accountId, Throwable throwable) {
        log.error("Error fetching account details for accountId: {}. Error: {}", accountId, throwable.getMessage());
        // Return a default or empty AccountSummary object
        return new AccountSummary(
                null,
                "SERVICE_UNAVAILABLE",
                "Account service is currently unavailable. Please try again later."
                );
    }
}
