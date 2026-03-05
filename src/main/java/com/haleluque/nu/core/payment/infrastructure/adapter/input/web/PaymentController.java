package com.haleluque.nu.core.payment.infrastructure.adapter.input.web;

import com.haleluque.nu.core.payment.application.port.in.TransferCommand;
import com.haleluque.nu.core.payment.application.port.in.TransferPort;
import com.haleluque.nu.core.payment.application.port.in.TransferResult;
import com.haleluque.nu.core.payment.infrastructure.adapter.input.web.dto.TransferDetailResponse;
import com.haleluque.nu.core.payment.infrastructure.adapter.input.web.dto.TransferRequest;
import com.haleluque.nu.core.payment.infrastructure.adapter.input.web.dto.TransferResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST input adapter: exposes the transfer port via HTTP.
 */
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final TransferPort transferPort;

    public PaymentController(TransferPort transferPort) {
        this.transferPort = transferPort;
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody TransferRequest request) {
        TransferCommand command = new TransferCommand(
                request.originAccountId(),
                request.destinationAccountId(),
                request.amount()
        );
        TransferResult result = transferPort.transfer(command);
        TransferResponse response = new TransferResponse(
                result.transferId(),
                result.originAccountId(),
                result.destinationAccountId(),
                result.status()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/transfers/{transferId}")
    public ResponseEntity<TransferDetailResponse> getTransferById(@PathVariable UUID transferId) {
        return transferPort.getById(transferId)
                .map(r -> ResponseEntity.ok(new TransferDetailResponse(
                        r.transferId(),
                        r.originAccountId(),
                        r.destinationAccountId(),
                        r.amount(),
                        r.status(),
                        r.createdAt()
                )))
                .orElse(ResponseEntity.notFound().build());
    }
}
