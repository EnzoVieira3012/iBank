package com.ibank.limit.controller;

import com.ibank.limit.controller.dto.LimitValidationRequest;
import com.ibank.limit.controller.dto.LimitValidationResponse;
import com.ibank.limit.service.LimitValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/limits/validate")
@RequiredArgsConstructor
@Tag(name = "Limit Validation", description = "Real-time limit validation for transactions")
public class LimitValidationController {

    private final LimitValidationService limitValidationService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "Validate transaction against limits", description = "Checks if transaction would exceed any limits or be blocked")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Validation result"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "403", description = "Limit exceeded or transaction blocked")
    })
    public ResponseEntity<LimitValidationResponse> validate(@Valid @RequestBody LimitValidationRequest request) {
        LimitValidationResponse response = limitValidationService.validate(request);
        if (!response.isAllowed()) {
            return ResponseEntity.status(403).body(response);
        }
        return ResponseEntity.ok(response);
    }
}