package com.ibank.limit.controller;

import com.ibank.limit.controller.dto.CustomerLimitRequest;
import com.ibank.limit.controller.dto.CustomerLimitResponse;
import com.ibank.limit.mapper.CustomerLimitMapper;
import com.ibank.limit.model.CustomerLimit;
import com.ibank.limit.model.LimitScope;
import com.ibank.limit.model.LimitType;
import com.ibank.limit.service.CustomerLimitService;
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
@RequestMapping("/api/v1/limits")
@RequiredArgsConstructor
@Tag(name = "Customer Limits", description = "Customer limit management: transaction limits, velocity controls, usage tracking")
public class CustomerLimitController {

    private final CustomerLimitService limitService;
    private final CustomerLimitMapper limitMapper;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Create a new customer limit")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Limit created"),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<CustomerLimitResponse> create(@Valid @RequestBody CustomerLimitRequest request) {
        CustomerLimit limit = limitService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(limitMapper.toResponse(limit));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "Get limit by ID")
    public ResponseEntity<CustomerLimitResponse> getById(@PathVariable UUID id) {
        CustomerLimit limit = limitService.getById(id);
        return ResponseEntity.ok(limitMapper.toResponse(limit));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "List all limits")
    public ResponseEntity<List<CustomerLimitResponse>> listAll() {
        List<CustomerLimit> limits = limitService.findAll();
        return ResponseEntity.ok(limits.stream().map(limitMapper::toResponse).toList());
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "List limits for a customer")
    public ResponseEntity<List<CustomerLimitResponse>> listByCustomer(@PathVariable UUID customerId) {
        List<CustomerLimit> limits = limitService.findByCustomerId(customerId);
        return ResponseEntity.ok(limits.stream().map(limitMapper::toResponse).toList());
    }

    @GetMapping("/customer/{customerId}/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "List active limits for a customer")
    public ResponseEntity<List<CustomerLimitResponse>> listActiveByCustomer(@PathVariable UUID customerId) {
        List<CustomerLimit> limits = limitService.findActiveByCustomerId(customerId);
        return ResponseEntity.ok(limits.stream().map(limitMapper::toResponse).toList());
    }

    @GetMapping("/account/{accountId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "List limits for an account")
    public ResponseEntity<List<CustomerLimitResponse>> listByAccount(@PathVariable UUID accountId) {
        List<CustomerLimit> limits = limitService.findByAccountId(accountId);
        return ResponseEntity.ok(limits.stream().map(limitMapper::toResponse).toList());
    }

    @GetMapping("/card/{cardId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "List limits for a card")
    public ResponseEntity<List<CustomerLimitResponse>> listByCard(@PathVariable UUID cardId) {
        List<CustomerLimit> limits = limitService.findByCardId(cardId);
        return ResponseEntity.ok(limits.stream().map(limitMapper::toResponse).toList());
    }

    @GetMapping("/type/{limitType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "List limits by type")
    public ResponseEntity<List<CustomerLimitResponse>> listByType(@PathVariable LimitType limitType) {
        List<CustomerLimit> limits = limitService.findByLimitType(limitType);
        return ResponseEntity.ok(limits.stream().map(limitMapper::toResponse).toList());
    }

    @GetMapping("/scope/{scope}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "List limits by scope")
    public ResponseEntity<List<CustomerLimitResponse>> listByScope(@PathVariable LimitScope scope) {
        List<CustomerLimit> limits = limitService.findByScope(scope);
        return ResponseEntity.ok(limits.stream().map(limitMapper::toResponse).toList());
    }

    @GetMapping("/applicable")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "Find applicable limits for a customer/account/card")
    public ResponseEntity<List<CustomerLimitResponse>> findApplicable(
            @RequestParam UUID customerId,
            @RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) UUID cardId) {
        List<CustomerLimit> limits = limitService.findApplicableLimits(customerId, accountId, cardId);
        return ResponseEntity.ok(limits.stream().map(limitMapper::toResponse).toList());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Update a limit")
    public ResponseEntity<CustomerLimitResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody CustomerLimitRequest request) {
        CustomerLimit limit = limitService.update(id, request);
        return ResponseEntity.ok(limitMapper.toResponse(limit));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Activate a limit")
    public ResponseEntity<Void> activate(@PathVariable UUID id) {
        limitService.activate(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Deactivate a limit")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        limitService.deactivate(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a limit")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        limitService.delete(id);
        return ResponseEntity.noContent().build();
    }
}