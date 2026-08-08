package com.bank.payment_service.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;


@Configuration
@EntityScan("com.bank.payment_service.domain")
@EnableJpaRepositories("com.bank.payment_service.repos")
@EnableTransactionManagement
public class DomainConfig {
}
