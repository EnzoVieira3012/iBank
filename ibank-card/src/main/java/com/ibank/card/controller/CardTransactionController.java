package com.ibank.card.controller;

import com.ibank.card.controller.dto.CardTransactionRequest;
import com.ibank.card.controller.dto.CardTransactionResponse;
import com.ibank.card.mapper.CardTransactionMapper;
import com.ibank.card.model.CardTransaction;
import com.ibank.card.model.TransactionStatus;
import com.ibank.card.model.TransactionType;
import com.ibank.card.service.CardTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cards/{cardId}/transactions")
@RequiredArgsConstructor
@Tag(name = "Card Transactions", description = "Card transaction management: authorize, capture, settle, reverse, refund")
public class CardTransactionController {

    private final CardTransactionService transactionService;
    private final CardTransactionMapper transactionMapper;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "Authorize a transaction", description = "Authorizes a purchase, withdrawal, or payment transaction")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Transaction authorized"),
        @ApiResponse(responseCode = "400", description = "Invalid request or insufficient limit"),
        @ApiResponse(responseCode = "404", description = "Card not found"),
        @ApiResponse(responseCode = "409", description = "Duplicate idempotency key")
    })
    public ResponseEntity<CardTransactionResponse> authorize(
            @PathVariable UUID cardId,
            @Valid @RequestBody CardTransactionRequest request) {
        request.setCardId(cardId.toString());
        CardTransaction transaction = transactionMapper.toEntity(request);
        CardTransaction authorized = transactionService.authorize(transaction);
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionMapper.toResponse(authorized));
    }

    @GetMapping("/{transactionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "Get transaction by ID")
    public ResponseEntity<CardTransactionResponse> getTransaction(
            @PathVariable UUID cardId,
            @PathVariable UUID transactionId) {
        CardTransaction transaction = transactionService.getById(transactionId);
        if (!transaction.getCardId().equals(cardId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(transactionMapper.toResponse(transaction));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "List transactions for a card")
    public ResponseEntity<List<CardTransactionResponse>> listTransactions(
            @PathVariable UUID cardId,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate,
            Pageable pageable) {
        List<CardTransaction> transactions;
        if (status != null) {
            transactions = transactionService.findByCardIdAndStatus(cardId, status);
        } else if (type != null) {
            transactions = transactionService.findByCardIdAndType(cardId, type);
        } else if (startDate != null && endDate != null) {
            transactions = transactionService.findByCardIdAndCreatedAtBetween(cardId, startDate, endDate);
        } else {
            transactions = transactionService.findByCardId(cardId);
        }
        return ResponseEntity.ok(transactions.stream().map(transactionMapper::toResponse).toList());
    }

    @PostMapping("/{transactionId}/capture")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    @Operation(summary = "Capture an authorized transaction")
    public ResponseEntity<CardTransactionResponse> capture(
            @PathVariable UUID cardId,
            @PathVariable UUID transactionId) {
        CardTransaction transaction = transactionService.capture(transactionId);
        return ResponseEntity.ok(transactionMapper.toResponse(transaction));
    }

    @PostMapping("/{transactionId}/settle")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    @Operation(summary = "Settle a captured transaction")
    public ResponseEntity<CardTransactionResponse> settle(
            @PathVariable UUID cardId,
            @PathVariable UUID transactionId) {
        CardTransaction transaction = transactionService.settle(transactionId);
        return ResponseEntity.ok(transactionMapper.toResponse(transaction));
    }

    @PostMapping("/{transactionId}/reverse")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    @Operation(summary = "Reverse an authorized/captured transaction")
    public ResponseEntity<CardTransactionResponse> reverse(
            @PathVariable UUID cardId,
            @PathVariable UUID transactionId,
            @RequestParam String reason) {
        CardTransaction transaction = transactionService.reverse(transactionId, reason);
        return ResponseEntity.ok(transactionMapper.toResponse(transaction));
    }

    @PostMapping("/{transactionId}/refund")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    @Operation(summary = "Refund a settled transaction")
    public ResponseEntity<CardTransactionResponse> refund(
            @PathVariable UUID cardId,
            @PathVariable UUID transactionId,
            @RequestParam String reason) {
        CardTransaction transaction = transactionService.refund(transactionId, reason);
        return ResponseEntity.ok(transactionMapper.toResponse(transaction));
    }

    @PostMapping("/{transactionId}/decline")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    @Operation(summary = "Decline a pending transaction")
    public ResponseEntity<CardTransactionResponse> decline(
            @PathVariable UUID cardId,
            @PathVariable UUID transactionId,
            @RequestParam String reason) {
        CardTransaction transaction = transactionService.decline(transactionId, reason);
        return ResponseEntity.ok(transactionMapper.toResponse(transaction));
    }
}