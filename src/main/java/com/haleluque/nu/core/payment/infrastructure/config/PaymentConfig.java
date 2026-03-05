package com.haleluque.nu.core.payment.infrastructure.config;

import com.haleluque.nu.core.payment.application.port.out.AccountRepository;
import com.haleluque.nu.core.payment.application.port.out.TransactionRepository;
import com.haleluque.nu.core.payment.application.port.in.TransferPort;
import com.haleluque.nu.core.payment.application.usecase.TransferUseCase;
import com.haleluque.nu.core.payment.domain.service.TransferService;
import com.haleluque.nu.core.payment.infrastructure.adapter.output.messaging.PaymentEventProducer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Hexagon bean configuration (ports and use cases).
 */
@Configuration
public class PaymentConfig {

    @Bean
    public TransferService transferService(AccountRepository accountRepository) {
        return new TransferService(accountRepository);
    }

    @Bean
    public TransferPort transferPort(TransferService transferService,
                                    TransactionRepository transactionRepository,
                                    PaymentEventProducer paymentEventProducer) {
        return new TransferUseCase(transferService, transactionRepository, paymentEventProducer);
    }
}
