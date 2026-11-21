package com.ibank.antifraud.controller;

import com.ibank.antifraud.controller.dto.FraudAlertRequest;
import com.ibank.antifraud.controller.dto.FraudAlertResponse;
import com.ibank.antifraud.mapper.FraudAlertMapper;
import com.ibank.antifraud.model.AlertSeverity;
import com.ibank.antifraud.model.AlertStatus;
import com.ibank.antifraud.model.FraudAlert;
import com.ibank.antifraud.service.FraudAlertService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/antifraud/alerts")
@RequiredArgsConstructor
@Tag(name = "Fraud Alerts", description = "Fraud alert management: review, assign, escalate, resolve")
public class FraudAlertController {

    private final FraudAlertService alertService;
    private final FraudAlertMapper alertMapper;

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "Get fraud alert by ID")
    public ResponseEntity<FraudAlertResponse> getById(@PathVariable UUID id) {
        FraudAlert alert = alertService.getById(id);
        return ResponseEntity.ok(alertMapper.toResponse(alert));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "List all fraud alerts")
    public ResponseEntity<List<FraudAlertResponse>> listAll() {
        List<FraudAlert> alerts = alertService.findAll();
        return ResponseEntity.ok(alerts.stream().map(alertMapper::toResponse).toList());
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "List alerts for a customer")
    public ResponseEntity<List<FraudAlertResponse>> listByCustomer(@PathVariable UUID customerId) {
        List<FraudAlert> alerts = alertService.findByCustomerId(customerId);
        return ResponseEntity.ok(alerts.stream().map(alertMapper::toResponse).toList());
    }

    @GetMapping("/account/{accountId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "List alerts for an account")
    public ResponseEntity<List<FraudAlertResponse>> listByAccount(@PathVariable UUID accountId) {
        List<FraudAlert> alerts = alertService.findByAccountId(accountId);
        return ResponseEntity.ok(alerts.stream().map(alertMapper::toResponse).toList());
    }

    @GetMapping("/card/{cardId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "List alerts for a card")
    public ResponseEntity<List<FraudAlertResponse>> listByCard(@PathVariable UUID cardId) {
        List<FraudAlert> alerts = alertService.findByCardId(cardId);
        return ResponseEntity.ok(alerts.stream().map(alertMapper::toResponse).toList());
    }

    @GetMapping("/transaction/{transactionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "List alerts for a transaction")
    public ResponseEntity<List<FraudAlertResponse>> listByTransaction(@PathVariable UUID transactionId) {
        List<FraudAlert> alerts = alertService.findByTransactionId(transactionId);
        return ResponseEntity.ok(alerts.stream().map(alertMapper::toResponse).toList());
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "List alerts by status")
    public ResponseEntity<List<FraudAlertResponse>> listByStatus(@PathVariable AlertStatus status) {
        List<FraudAlert> alerts = alertService.findByStatus(status);
        return ResponseEntity.ok(alerts.stream().map(alertMapper::toResponse).toList());
    }

    @GetMapping("/severity/{severity}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "List alerts by severity")
    public ResponseEntity<List<FraudAlertResponse>> listBySeverity(@PathVariable AlertSeverity severity) {
        List<FraudAlert> alerts = alertService.findBySeverity(severity);
        return ResponseEntity.ok(alerts.stream().map(alertMapper::toResponse).toList());
    }

    @GetMapping("/assigned/{assignedTo}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "List alerts assigned to a user")
    public ResponseEntity<List<FraudAlertResponse>> listByAssigned(@PathVariable UUID assignedTo) {
        List<FraudAlert> alerts = alertService.findByAssignedTo(assignedTo);
        return ResponseEntity.ok(alerts.stream().map(alertMapper::toResponse).toList());
    }

    @GetMapping("/rule/{ruleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "List alerts triggered by a rule")
    public ResponseEntity<List<FraudAlertResponse>> listByRule(@PathVariable UUID ruleId) {
        List<FraudAlert> alerts = alertService.findByRuleId(ruleId);
        return ResponseEntity.ok(alerts.stream().map(alertMapper::toResponse).toList());
    }

    @GetMapping("/open")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    @Operation(summary = "List all open alerts")
    public ResponseEntity<List<FraudAlertResponse>> listOpen() {
        List<FraudAlert> alerts = alertService.findOpenAlerts();
        return ResponseEntity.ok(alerts.stream().map(alertMapper::toResponse).toList());
    }

    @GetMapping("/my-open")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    @Operation(summary = "List open alerts assigned to current user")
    public ResponseEntity<List<FraudAlertResponse>> listMyOpen(@RequestParam UUID assignedTo) {
        List<FraudAlert> alerts = alertService.findAssignedOpenAlerts(assignedTo);
        return ResponseEntity.ok(alerts.stream().map(alertMapper::toResponse).toList());
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    @Operation(summary = "Assign alert to a user")
    public ResponseEntity<FraudAlertResponse> assign(
            @PathVariable UUID id,
            @RequestParam UUID assignedTo) {
        FraudAlert alert = alertService.assignAlert(id, assignedTo);
        return ResponseEntity.ok(alertMapper.toResponse(alert));
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    @Operation(summary = "Review an alert")
    public ResponseEntity<FraudAlertResponse> review(
            @PathVariable UUID id,
            @RequestParam UUID reviewedBy,
            @RequestParam AlertStatus status,
            @RequestParam(required = false) String resolution) {
        FraudAlert alert = alertService.reviewAlert(id, reviewedBy, status, resolution);
        return ResponseEntity.ok(alertMapper.toResponse(alert));
    }

    @PostMapping("/{id}/false-positive")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    @Operation(summary = "Mark alert as false positive")
    public ResponseEntity<FraudAlertResponse> markFalsePositive(
            @PathVariable UUID id,
            @RequestParam UUID reviewedBy,
            @RequestParam String reason) {
        FraudAlert alert = alertService.markFalsePositive(id, reviewedBy, reason);
        return ResponseEntity.ok(alertMapper.toResponse(alert));
    }

    @PostMapping("/{id}/escalate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Escalate an alert")
    public ResponseEntity<FraudAlertResponse> escalate(
            @PathVariable UUID id,
            @RequestParam UUID escalatedTo) {
        FraudAlert alert = alertService.escalateAlert(id, escalatedTo);
        return ResponseEntity.ok(alertMapper.toResponse(alert));
    }

    @PostMapping("/auto-close")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    @Operation(summary = "Auto-close old alerts")
    public ResponseEntity<Void> autoClose(@RequestParam java.time.LocalDateTime olderThan) {
        alertService.autoCloseAlerts(olderThan);
        return ResponseEntity.ok().build();
    }
}