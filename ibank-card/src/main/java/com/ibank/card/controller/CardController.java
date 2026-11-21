package com.ibank.card.controller;

import com.ibank.card.controller.dto.CardRequest;
import com.ibank.card.controller.dto.CardResponse;
import com.ibank.card.mapper.CardMapper;
import com.ibank.card.model.Card;
import com.ibank.card.model.CardStatus;
import com.ibank.card.service.CardService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cards")
@RequiredArgsConstructor
@Tag(name = "Cards", description = "Card management: creation, activation, blocking, limits, features")
public class CardController {

    private final CardService cardService;
    private final CardMapper cardMapper;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "Create a new card", description = "Creates a new credit/debit/multiple/prepaid card for an account")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Card created",
            content = @Content(schema = @Schema(implementation = CardResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "409", description = "Card already exists")
    })
    public ResponseEntity<CardResponse> createCard(@Valid @RequestBody CardRequest request) {
        Card card = cardMapper.toEntity(request);
        card.setAccountId(UUID.fromString(request.getAccountId()));
        Card created = cardService.createCard(card);
        return ResponseEntity.status(HttpStatus.CREATED).body(cardMapper.toResponse(created));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "Get card by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Card found"),
        @ApiResponse(responseCode = "404", description = "Card not found")
    })
    public ResponseEntity<CardResponse> getCard(@PathVariable UUID id) {
        Card card = cardService.getById(id);
        return ResponseEntity.ok(cardMapper.toResponse(card));
    }

    @GetMapping("/account/{accountId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "List cards by account")
    public ResponseEntity<List<CardResponse>> getCardsByAccount(@PathVariable UUID accountId) {
        List<Card> cards = cardService.findByAccountId(accountId);
        return ResponseEntity.ok(cards.stream().map(cardMapper::toResponse).toList());
    }

    @GetMapping("/account/{accountId}/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "List cards by account and status")
    public ResponseEntity<List<CardResponse>> getCardsByAccountAndStatus(
            @PathVariable UUID accountId,
            @PathVariable CardStatus status) {
        List<Card> cards = cardService.findByAccountIdAndStatus(accountId, status);
        return ResponseEntity.ok(cards.stream().map(cardMapper::toResponse).toList());
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "Activate a pending card")
    public ResponseEntity<CardResponse> activateCard(@PathVariable UUID id) {
        Card card = cardService.activateCard(id);
        return ResponseEntity.ok(cardMapper.toResponse(card));
    }

    @PostMapping("/{id}/block")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "Block a card")
    public ResponseEntity<CardResponse> blockCard(
            @PathVariable UUID id,
            @RequestParam String reason) {
        Card card = cardService.blockCard(id, reason);
        return ResponseEntity.ok(cardMapper.toResponse(card));
    }

    @PostMapping("/{id}/unblock")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "Unblock a blocked card")
    public ResponseEntity<CardResponse> unblockCard(@PathVariable UUID id) {
        Card card = cardService.unblockCard(id);
        return ResponseEntity.ok(cardMapper.toResponse(card));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "Cancel a card")
    public ResponseEntity<CardResponse> cancelCard(
            @PathVariable UUID id,
            @RequestParam String reason) {
        Card card = cardService.cancelCard(id, reason);
        return ResponseEntity.ok(cardMapper.toResponse(card));
    }

    @PostMapping("/{id}/report-lost")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "Report card as lost")
    public ResponseEntity<CardResponse> reportLost(@PathVariable UUID id) {
        Card card = cardService.reportLost(id);
        return ResponseEntity.ok(cardMapper.toResponse(card));
    }

    @PostMapping("/{id}/report-stolen")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "Report card as stolen")
    public ResponseEntity<CardResponse> reportStolen(@PathVariable UUID id) {
        Card card = cardService.reportStolen(id);
        return ResponseEntity.ok(cardMapper.toResponse(card));
    }

    @PatchMapping("/{id}/limits")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    @Operation(summary = "Update card limits")
    public ResponseEntity<CardResponse> updateLimits(
            @PathVariable UUID id,
            @RequestParam(required = false) java.math.BigDecimal creditLimit,
            @RequestParam(required = false) java.math.BigDecimal dailyLimit,
            @RequestParam(required = false) java.math.BigDecimal monthlyLimit) {
        Card card = cardService.updateLimits(id, creditLimit, dailyLimit, monthlyLimit);
        return ResponseEntity.ok(cardMapper.toResponse(card));
    }

    @PatchMapping("/{id}/features")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "Update card features")
    public ResponseEntity<CardResponse> updateFeatures(
            @PathVariable UUID id,
            @RequestParam(required = false) Boolean contactless,
            @RequestParam(required = false) Boolean online,
            @RequestParam(required = false) Boolean international) {
        Card card = cardService.updateFeatures(id, contactless, online, international);
        return ResponseEntity.ok(cardMapper.toResponse(card));
    }
}