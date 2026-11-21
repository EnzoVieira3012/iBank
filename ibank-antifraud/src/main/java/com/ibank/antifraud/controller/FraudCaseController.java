package com.ibank.antifraud.controller;

import com.ibank.antifraud.controller.dto.FraudCaseRequest;
import com.ibank.antifraud.controller.dto.FraudCaseResponse;
import com.ibank.antifraud.mapper.FraudCaseMapper;
import com.ibank.antifraud.model.CaseStatus;
import com.ibank.antifraud.model.FraudCase;
import com.ibank.antifraud.service.FraudCaseService;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/antifraud/cases")
@RequiredArgsConstructor
@Tag(name = "Fraud Cases", description = "Fraud case management: investigation, resolution, SAR filing")
public class FraudCaseController {

    private final FraudCaseService caseService;
    private final FraudCaseMapper caseMapper;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    @Operation(summary = "Create a new fraud case")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Case created"),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<FraudCaseResponse> create(@Valid @RequestBody FraudCaseRequest request) {
        FraudCase fraudCase = caseService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(caseMapper.toResponse(fraudCase));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "Get fraud case by ID")
    public ResponseEntity<FraudCaseResponse> getById(@PathVariable UUID id) {
        FraudCase fraudCase = caseService.getById(id);
        return ResponseEntity.ok(caseMapper.toResponse(fraudCase));
    }

    @GetMapping("/number/{caseNumber}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "Get fraud case by case number")
    public ResponseEntity<FraudCaseResponse> getByCaseNumber(@PathVariable String caseNumber) {
        Optional<FraudCase> fraudCase = caseService.findByCaseNumber(caseNumber);
        return fraudCase.map(c -> ResponseEntity.ok(caseMapper.toResponse(c)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "List all fraud cases")
    public ResponseEntity<List<FraudCaseResponse>> listAll() {
        List<FraudCase> cases = caseService.findAll();
        return ResponseEntity.ok(cases.stream().map(caseMapper::toResponse).toList());
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "List cases for a customer")
    public ResponseEntity<List<FraudCaseResponse>> listByCustomer(@PathVariable UUID customerId) {
        List<FraudCase> cases = caseService.findByCustomerId(customerId);
        return ResponseEntity.ok(cases.stream().map(caseMapper::toResponse).toList());
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "List cases by status")
    public ResponseEntity<List<FraudCaseResponse>> listByStatus(@PathVariable CaseStatus status) {
        List<FraudCase> cases = caseService.findByStatus(status);
        return ResponseEntity.ok(cases.stream().map(caseMapper::toResponse).toList());
    }

    @GetMapping("/assigned/{assignedTo}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "List cases assigned to a user")
    public ResponseEntity<List<FraudCaseResponse>> listByAssigned(@PathVariable UUID assignedTo) {
        List<FraudCase> cases = caseService.findByAssignedTo(assignedTo);
        return ResponseEntity.ok(cases.stream().map(caseMapper::toResponse).toList());
    }

    @GetMapping("/open")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    @Operation(summary = "List all open cases")
    public ResponseEntity<List<FraudCaseResponse>> listOpen() {
        List<FraudCase> cases = caseService.findOpenCases();
        return ResponseEntity.ok(cases.stream().map(caseMapper::toResponse).toList());
    }

    @GetMapping("/my-open")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    @Operation(summary = "List open cases assigned to current user")
    public ResponseEntity<List<FraudCaseResponse>> listMyOpen(@RequestParam UUID assignedTo) {
        List<FraudCase> cases = caseService.findAssignedOpenCases(assignedTo);
        return ResponseEntity.ok(cases.stream().map(caseMapper::toResponse).toList());
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Assign case to an investigator")
    public ResponseEntity<FraudCaseResponse> assign(
            @PathVariable UUID id,
            @RequestParam UUID assignedTo) {
        FraudCase fraudCase = caseService.assignCase(id, assignedTo);
        return ResponseEntity.ok(caseMapper.toResponse(fraudCase));
    }

    @PatchMapping("/{id}/investigation")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    @Operation(summary = "Update investigation notes")
    public ResponseEntity<FraudCaseResponse> updateInvestigation(
            @PathVariable UUID id,
            @RequestParam UUID investigatorId,
            @RequestParam String notes) {
        FraudCase fraudCase = caseService.updateInvestigation(id, investigatorId, notes);
        return ResponseEntity.ok(caseMapper.toResponse(fraudCase));
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    @Operation(summary = "Resolve a case")
    public ResponseEntity<FraudCaseResponse> resolve(
            @PathVariable UUID id,
            @RequestParam UUID resolvedBy,
            @RequestParam CaseStatus resolution,
            @RequestParam String resolutionNotes) {
        FraudCase fraudCase = caseService.resolveCase(id, resolvedBy, resolution, resolutionNotes);
        return ResponseEntity.ok(caseMapper.toResponse(fraudCase));
    }

    @PostMapping("/{id}/sar")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "File SAR for a case")
    public ResponseEntity<FraudCaseResponse> fileSar(
            @PathVariable UUID id,
            @RequestParam UUID filedBy,
            @RequestParam String sarReference) {
        FraudCase fraudCase = caseService.fileSar(id, filedBy, sarReference);
        return ResponseEntity.ok(caseMapper.toResponse(fraudCase));
    }

    @PostMapping("/{id}/escalate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Escalate a case")
    public ResponseEntity<FraudCaseResponse> escalate(
            @PathVariable UUID id,
            @RequestParam UUID escalatedTo) {
        FraudCase fraudCase = caseService.escalateCase(id, escalatedTo);
        return ResponseEntity.ok(caseMapper.toResponse(fraudCase));
    }

    @GetMapping("/sar/count")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Count SARs filed in a period")
    public ResponseEntity<Long> countSarFiled(
            @RequestParam LocalDate start,
            @RequestParam LocalDate end) {
        long count = caseService.countSarFiledInPeriod(start, end);
        return ResponseEntity.ok(count);
    }
}