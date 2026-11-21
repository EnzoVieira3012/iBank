package com.ibank.antifraud.controller;

import com.ibank.antifraud.controller.dto.RiskAssessmentResponse;
import com.ibank.antifraud.controller.dto.RiskScoreRequest;
import com.ibank.antifraud.controller.dto.RiskScoreResponse;
import com.ibank.antifraud.mapper.RiskScoreMapper;
import com.ibank.antifraud.model.RiskLevel;
import com.ibank.antifraud.model.RiskScore;
import com.ibank.antifraud.service.RiskScoringService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/antifraud/risk")
@RequiredArgsConstructor
@Tag(name = "Risk Scoring", description = "Real-time risk assessment and scoring for transactions")
public class RiskScoringController {

    private final RiskScoringService riskScoringService;
    private final RiskScoreMapper riskScoreMapper;

    @PostMapping("/assess")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "Assess transaction risk in real-time", description = "Evaluates transaction against fraud rules and returns risk score with recommended action")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Risk assessment completed"),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<RiskAssessmentResponse> assessRisk(@Valid @RequestBody RiskScoreRequest request) {
        RiskAssessmentResponse response = riskScoringService.assessRisk(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "Get risk scores for a customer")
    public ResponseEntity<List<RiskScoreResponse>> getByCustomer(@PathVariable UUID customerId) {
        List<RiskScore> scores = riskScoringService.findByCustomerId(customerId);
        return ResponseEntity.ok(scores.stream().map(riskScoreMapper::toResponse).toList());
    }

    @GetMapping("/transaction/{transactionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "Get risk score for a transaction")
    public ResponseEntity<RiskScoreResponse> getByTransaction(@PathVariable UUID transactionId) {
        Optional<RiskScore> score = riskScoringService.findLatestByTransactionId(transactionId);
        return score.map(s -> ResponseEntity.ok(riskScoreMapper.toResponse(s)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/customer/{customerId}/average")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "Get average risk score for a customer over a period")
    public ResponseEntity<Double> getAverageScore(
            @PathVariable UUID customerId,
            @RequestParam LocalDateTime since) {
        Double avg = riskScoringService.getAverageScore(customerId, since);
        return ResponseEntity.ok(avg != null ? avg : 0.0);
    }
}