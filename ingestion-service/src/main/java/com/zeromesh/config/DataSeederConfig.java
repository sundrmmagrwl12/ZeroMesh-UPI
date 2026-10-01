package com.zeromesh.config;

import com.zeromesh.model.Account;
import com.zeromesh.repository.AccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
public class DataSeederConfig {

    // Seeds demo accounts on first startup — skips if accounts already exist
    @Bean
    public CommandLineRunner seedAccounts(AccountRepository accountRepository) {
        return args -> {
            if (accountRepository.count() == 0) {
                accountRepository.save(new Account("sundram@upi", "Sundram", new BigDecimal("2000.00")));
                accountRepository.save(new Account("rahul@upi",   "Rahul",   new BigDecimal("1000.00")));
                accountRepository.save(new Account("priya@upi",   "Priya",   new BigDecimal("1500.00")));
                System.out.println("[ZeroMesh] Seeded 3 accounts into DB.");
            }
        };
    }
}
