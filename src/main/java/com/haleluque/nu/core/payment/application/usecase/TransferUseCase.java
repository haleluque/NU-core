package com.haleluque.nu.core.payment.application.usecase;

import com.haleluque.nu.core.payment.application.port.in.TransferCommand;
import com.haleluque.nu.core.payment.application.port.in.TransferDetailResult;
import com.haleluque.nu.core.payment.application.port.in.TransferPort;
import com.haleluque.nu.core.payment.application.port.in.TransferResult;
import com.haleluque.nu.core.payment.application.port.out.TransactionRepository;
import com.haleluque.nu.core.payment.domain.model.Transaction;
import com.haleluque.nu.core.payment.domain.service.TransferService;
import com.haleluque.nu.core.payment.infrastructure.adapter.output.messaging.PaymentEventProducer;
import com.haleluque.nu.core.payment.infrastructure.adapter.output.messaging.dto.TransactionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Use case: orchestrates transfer and starts the risk flow (saga).
 */
public final class TransferUseCase implements TransferPort {

    private static final Logger log = LoggerFactory.getLogger(TransferUseCase.class);

    private final TransferService transferService;
    private final TransactionRepository transactionRepository;
    private final PaymentEventProducer paymentEventProducer;

    public TransferUseCase(TransferService transferService,
                           TransactionRepository transactionRepository,
                           PaymentEventProducer paymentEventProducer) {
        this.transferService = transferService;
        this.transactionRepository = transactionRepository;
        this.paymentEventProducer = paymentEventProducer;
    }

    @Override
    public TransferResult transfer(TransferCommand command) {
        transferService.transfer(
                command.originAccountId(),
                command.destinationAccountId(),
                command.amount()
        );

        UUID transactionId = UUID.randomUUID();
        Transaction transaction = new Transaction(
                transactionId,
                command.originAccountId(),
                command.destinationAccountId(),
                command.amount(),
                "TRANSFER",
                Transaction.PENDING,
                Instant.now()
        );
        transactionRepository.save(transaction);

        log.info("Starting risk validation for transaction: {}", transactionId);

        TransactionEvent event = new TransactionEvent(
                transactionId.toString(),
                command.originAccountId(),
                command.amount(),
                Transaction.PENDING
        );
        paymentEventProducer.sendTransactionEvent(event);

        return new TransferResult(
                transactionId,
                command.originAccountId(),
                command.destinationAccountId(),
                "PENDING_RISK"
        );
    }

    @Override
    public Optional<TransferDetailResult> getById(UUID transferId) {
        return transactionRepository.findById(transferId)
                .map(tx -> new TransferDetailResult(
                        tx.id(),
                        tx.accountId(),
                        tx.destinationAccountId(),
                        tx.amount(),
                        tx.status(),
                        tx.createdAt()
                ));
    }
}
