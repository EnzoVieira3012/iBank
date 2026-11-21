package com.ibank.fee.controller;

import com.ibank.fee.controller.dto.InterestCalculationResponse;
import com.ibank.fee.controller.dto.InterestRateRequest;
import com.ibank.fee.controller.dto.InterestRateResponse;
import com.ibank.fee.mapper.InterestRateMapper;
import com.ibank.fee.model.InterestIndex;
import com.ibank.fee.model.InterestRate;
import com.ibank.fee.service.InterestCalculationService;
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
@RequestMapping("/api/v1/interest/rates")
@RequiredArgsConstructor
@Tag(name = "Interest Rates", description = "Interest rate management: market rates, spreads, effective rates")
public class InterestRateController {

    private final InterestCalculationService interestCalculationService;
    private final InterestRateMapper interestRateMapper;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Create or update an interest rate", description = "Upserts rate for index/date/product combination")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Rate created"),
        @ApiResponse(responseCode = "200", description = "Rate updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<InterestRateResponse> upsert(@Valid @RequestBody InterestRateRequest request) {
        InterestRate rate = interestCalculationService.upsertRate(request);
        return ResponseEntity.ok(interestRateMapper.toResponse(rate));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "Get interest rate by ID")
    public ResponseEntity<InterestRateResponse> getById(@PathVariable UUID id) {
        InterestRate rate = interestCalculationService.getById(id);
        return ResponseEntity.ok(interestRateMapper.toResponse(rate));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "List interest rates with filters")
    public ResponseEntity<List<InterestRateResponse>> list(
            @RequestParam(required = false) InterestIndex indexType,
            @RequestParam(required = false) String productCode,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            Pageable pageable) {
        List<InterestRate> rates;
        if (indexType != null && startDate != null && endDate != null) {
            rates = interestCalculationService.findByIndexTypeAndDateRange(indexType, startDate, endDate);
        } else if (indexType != null) {
            rates = interestCalculationService.findByIndexType(indexType);
        } else if (productCode != null) {
            rates = interestCalculationService.findByProductCode(productCode);
        } else {
            rates = interestCalculationService.findAll();
        }
        return ResponseEntity.ok(rates.stream().map(interestRateMapper::toResponse).toList());
    }

    @GetMapping("/latest")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "Get latest rate for index/date/product")
    public ResponseEntity<InterestRateResponse> getLatest(
            @RequestParam InterestIndex indexType,
            @RequestParam LocalDate date,
            @RequestParam(required = false) String productCode) {
        InterestRate rate = interestCalculationService.getLatestRate(indexType, date, productCode);
        return ResponseEntity.ok(interestRateMapper.toResponse(rate));
    }

    @PostMapping("/calculate/simple")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "Calculate simple interest", description = "Simulates simple interest calculation")
    public ResponseEntity<InterestCalculationResponse> calculateSimple(
            @RequestParam InterestIndex indexType,
            @RequestParam BigDecimal principal,
            @RequestParam Integer days,
            @RequestParam(required = false) String productCode,
            @RequestParam(required = false) LocalDate calculationDate) {
        LocalDate date = calculationDate != null ? calculationDate : LocalDate.now();
        InterestCalculationResponse response = interestCalculationService.calculateSimpleInterest(
                indexType, principal, days, productCode, date);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/calculate/compound")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "Calculate compound interest", description = "Simulates daily compound interest calculation")
    public ResponseEntity<InterestCalculationResponse> calculateCompound(
            @RequestParam InterestIndex indexType,
            @RequestParam BigDecimal principal,
            @RequestParam Integer days,
            @RequestParam(required = false) String productCode,
            @RequestParam(required = false) LocalDate calculationDate,
            @RequestParam(defaultValue = "365") Integer compoundingFrequency) {
        LocalDate date = calculationDate != null ? calculationDate : LocalDate.now();
        InterestCalculationResponse response = interestCalculationService.calculateCompoundInterest(
                indexType, principal, days, productCode, date, compoundingFrequency);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/accrue")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    @Operation(summary = "Accrue interest for an account/card/loan", description = "Creates interest accrual record")
    public ResponseEntity<InterestCalculationResponse> accrue(
            @RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) UUID cardId,
            @RequestParam(required = false) UUID loanId,
            @RequestParam BigDecimal principal,
            @RequestParam InterestIndex indexType,
            @RequestParam(required = false) String productCode,
            @RequestParam LocalDate accrualDate,
            @RequestParam Integer days) {
        // Simulate accrual and return calculation
        InterestCalculationResponse calc = interestCalculationService.calculateSimpleInterest(
                indexType, principal, days, productCode, accrualDate);
        interestCalculationService.accrueInterest(accountId, cardId, loanId, principal, indexType, productCode, accrualDate, days);
        return ResponseEntity.ok(calc);
    }

    @PostMapping("/capitalize")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    @Operation(summary = "Capitalize accrued interest up to a date")
    public ResponseEntity<Void> capitalize(
            @RequestParam UUID accountId,
            @RequestParam LocalDate upToDate) {
        interestCalculationService.capitalizeAccruals(accountId, upToDate);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/pay")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    @Operation(summary = "Mark loan interest accruals as paid")
    public ResponseEntity<Void> payLoanInterest(@RequestParam UUID loanId) {
        interestCalculationService.payAccruals(loanId);
        return ResponseEntity.ok().build();
    }
}