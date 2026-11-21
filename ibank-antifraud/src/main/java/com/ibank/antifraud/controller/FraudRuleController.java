package com.ibank.antifraud.controller;

import com.ibank.antifraud.controller.dto.FraudRuleRequest;
import com.ibank.antifraud.controller.dto.FraudRuleResponse;
import com.ibank.antifraud.mapper.FraudRuleMapper;
import com.ibank.antifraud.model.FraudRule;
import com.ibank.antifraud.model.FraudRuleType;
import com.ibank.antifraud.service.FraudRuleService;
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
@RequestMapping("/api/v1/antifraud/rules")
@RequiredArgsConstructor
@Tag(name = "Fraud Rules", description = "Fraud rule management: velocity, amount, geography, device, behavioral rules")
public class FraudRuleController {

    private final FraudRuleService fraudRuleService;
    private final FraudRuleMapper fraudRuleMapper;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Create a new fraud rule")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Rule created"),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<FraudRuleResponse> create(@Valid @RequestBody FraudRuleRequest request) {
        FraudRule rule = fraudRuleService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(fraudRuleMapper.toResponse(rule));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "Get fraud rule by ID")
    public ResponseEntity<FraudRuleResponse> getById(@PathVariable UUID id) {
        FraudRule rule = fraudRuleService.getById(id);
        return ResponseEntity.ok(fraudRuleMapper.toResponse(rule));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "List all fraud rules")
    public ResponseEntity<List<FraudRuleResponse>> listAll() {
        List<FraudRule> rules = fraudRuleService.findAll();
        return ResponseEntity.ok(rules.stream().map(fraudRuleMapper::toResponse).toList());
    }

    @GetMapping("/type/{ruleType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "List fraud rules by type")
    public ResponseEntity<List<FraudRuleResponse>> listByType(@PathVariable FraudRuleType ruleType) {
        List<FraudRule> rules = fraudRuleService.findByType(ruleType);
        return ResponseEntity.ok(rules.stream().map(fraudRuleMapper::toResponse).toList());
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "List active fraud rules ordered by priority")
    public ResponseEntity<List<FraudRuleResponse>> listActive() {
        List<FraudRule> rules = fraudRuleService.findActiveOrderByPriority();
        return ResponseEntity.ok(rules.stream().map(fraudRuleMapper::toResponse).toList());
    }

    @GetMapping("/applicable")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "Find applicable rules for a transaction context")
    public ResponseEntity<List<FraudRuleResponse>> findApplicable(
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String productCode,
            @RequestParam(required = false) String customerSegment) {
        List<FraudRule> rules = fraudRuleService.findApplicable(channel, productCode, customerSegment);
        return ResponseEntity.ok(rules.stream().map(fraudRuleMapper::toResponse).toList());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Update a fraud rule")
    public ResponseEntity<FraudRuleResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody FraudRuleRequest request) {
        FraudRule rule = fraudRuleService.update(id, request);
        return ResponseEntity.ok(fraudRuleMapper.toResponse(rule));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Activate a fraud rule")
    public ResponseEntity<Void> activate(@PathVariable UUID id) {
        fraudRuleService.activate(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Deactivate a fraud rule")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        fraudRuleService.deactivate(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a fraud rule")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        fraudRuleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}