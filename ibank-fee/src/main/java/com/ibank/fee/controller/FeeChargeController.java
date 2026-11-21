package com.ibank.fee.controller;

import com.ibank.fee.controller.dto.FeeCalculationResponse;
import com.ibank.fee.controller.dto.FeeChargeRequest;
import com.ibank.fee.controller.dto.FeeChargeResponse;
import com.ibank.fee.mapper.FeeChargeMapper;
import com.ibank.fee.model.FeeCharge;
import com.ibank.fee.model.FeeChargeStatus;
import com.ibank.fee.model.FeeType;
import com.ibank.fee.service.FeeCalculationService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fees/charges")
@RequiredArgsConstructor
@Tag(name = "Fee Charges", description = "Fee charge management: calculate, charge, pay, waive fees")
public class FeeChargeController {

    private final FeeCalculationService feeCalculationService;
    private final FeeChargeMapper feeChargeMapper;

    @PostMapping("/calculate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "Calculate fee for a scenario", description = "Simulates fee calculation without creating a charge")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Fee calculated"),
        @ApiResponse(responseCode = "404", description = "No fee config found")
    })
    public ResponseEntity<FeeCalculationResponse> calculate(
            @RequestParam FeeType feeType,
            @RequestParam BigDecimal baseAmount,
            @RequestParam(required = false) String productCode,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String customerSegment,
            @RequestParam(required = false) LocalDate date) {
        LocalDate calcDate = date != null ? date : LocalDate.now();
        FeeCalculationResponse response = feeCalculationService.calculateFee(
                feeType, baseAmount, productCode, channel, customerSegment, calcDate);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    @Operation(summary = "Create a fee charge", description = "Charges a fee to an account/card/loan/transfer")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Fee charged"),
        @ApiResponse(responseCode = "400", description = "Invalid request or duplicate idempotency key"),
        @ApiResponse(responseCode = "404", description = "Fee config not found")
    })
    public ResponseEntity<FeeChargeResponse> charge(@Valid @RequestBody FeeChargeRequest request) {
        FeeCharge charge = feeCalculationService.chargeFee(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(feeChargeMapper.toResponse(charge));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "Get fee charge by ID")
    public ResponseEntity<FeeChargeResponse> getById(@PathVariable UUID id) {
        FeeCharge charge = feeCalculationService.getById(id);
        return ResponseEntity.ok(feeChargeMapper.toResponse(charge));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "List fee charges with filters")
    public ResponseEntity<List<FeeChargeResponse>> list(
            @RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) UUID cardId,
            @RequestParam(required = false) UUID loanId,
            @RequestParam(required = false) UUID transferId,
            @RequestParam(required = false) FeeType feeType,
            @RequestParam(required = false) FeeChargeStatus status,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            Pageable pageable) {
        List<FeeCharge> charges;
        if (accountId != null) {
            charges = feeCalculationService.findByAccountId(accountId);
        } else if (cardId != null) {
            charges = feeCalculationService.findByCardId(cardId);
        } else if (loanId != null) {
            charges = feeCalculationService.findByLoanId(loanId);
        } else if (transferId != null) {
            charges = feeCalculationService.findByTransferId(transferId);
        } else if (feeType != null) {
            charges = feeCalculationService.findByFeeType(feeType);
        } else if (status != null) {
            charges = feeCalculationService.findByStatus(status);
        } else if (startDate != null && endDate != null) {
            charges = feeCalculationService.findByChargeDateBetween(startDate, endDate);
        } else {
            charges = feeCalculationService.findAll();
        }
        return ResponseEntity.ok(charges.stream().map(feeChargeMapper::toResponse).toList());
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    @Operation(summary = "List overdue fee charges")
    public ResponseEntity<List<FeeChargeResponse>> listOverdue(
            @RequestParam(required = false) LocalDate asOfDate) {
        LocalDate date = asOfDate != null ? asOfDate : LocalDate.now();
        List<FeeCharge> charges = feeCalculationService.findOverdueFees(date);
        return ResponseEntity.ok(charges.stream().map(feeChargeMapper::toResponse).toList());
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    @Operation(summary = "Mark fee charge as paid")
    public ResponseEntity<FeeChargeResponse> pay(
            @PathVariable UUID id,
            @RequestParam UUID paymentTransactionId) {
        FeeCharge charge = feeCalculationService.payFee(id, paymentTransactionId);
        return ResponseEntity.ok(feeChargeMapper.toResponse(charge));
    }

    @PostMapping("/{id}/waive")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Waive a fee charge")
    public ResponseEntity<FeeChargeResponse> waive(@PathVariable UUID id) {
        FeeCharge charge = feeCalculationService.waiveFee(id);
        return ResponseEntity.ok(feeChargeMapper.toResponse(charge));
    }
}