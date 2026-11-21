package com.ibank.fee.controller;

import com.ibank.fee.controller.dto.FeeConfigRequest;
import com.ibank.fee.controller.dto.FeeConfigResponse;
import com.ibank.fee.mapper.FeeConfigMapper;
import com.ibank.fee.model.FeeConfig;
import com.ibank.fee.model.FeeType;
import com.ibank.fee.service.FeeConfigService;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fees/configs")
@RequiredArgsConstructor
@Tag(name = "Fee Configurations", description = "Fee configuration management: pricing tables, fee rules, tax settings")
public class FeeConfigController {

    private final FeeConfigService feeConfigService;
    private final FeeConfigMapper feeConfigMapper;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Create a new fee configuration")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Fee config created"),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<FeeConfigResponse> create(@Valid @RequestBody FeeConfigRequest request) {
        FeeConfig config = feeConfigService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(feeConfigMapper.toResponse(config));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "Get fee configuration by ID")
    public ResponseEntity<FeeConfigResponse> getById(@PathVariable UUID id) {
        FeeConfig config = feeConfigService.getById(id);
        return ResponseEntity.ok(feeConfigMapper.toResponse(config));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "List all fee configurations")
    public ResponseEntity<List<FeeConfigResponse>> listAll() {
        List<FeeConfig> configs = feeConfigService.findAll();
        return ResponseEntity.ok(configs.stream().map(feeConfigMapper::toResponse).toList());
    }

    @GetMapping("/type/{feeType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "List fee configurations by type")
    public ResponseEntity<List<FeeConfigResponse>> listByType(@PathVariable FeeType feeType) {
        List<FeeConfig> configs = feeConfigService.findByType(feeType);
        return ResponseEntity.ok(configs.stream().map(feeConfigMapper::toResponse).toList());
    }

    @GetMapping("/type/{feeType}/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "List active fee configurations by type")
    public ResponseEntity<List<FeeConfigResponse>> listActiveByType(@PathVariable FeeType feeType) {
        List<FeeConfig> configs = feeConfigService.findActiveByType(feeType);
        return ResponseEntity.ok(configs.stream().map(feeConfigMapper::toResponse).toList());
    }

    @GetMapping("/product/{productCode}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "List fee configurations by product code")
    public ResponseEntity<List<FeeConfigResponse>> listByProduct(@PathVariable String productCode) {
        List<FeeConfig> configs = feeConfigService.findByProductCode(productCode);
        return ResponseEntity.ok(configs.stream().map(feeConfigMapper::toResponse).toList());
    }

    @GetMapping("/applicable")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "Find applicable fee configs for a scenario")
    public ResponseEntity<List<FeeConfigResponse>> findApplicable(
            @RequestParam FeeType feeType,
            @RequestParam LocalDate date,
            @RequestParam(required = false) String productCode,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String customerSegment) {
        List<FeeConfig> configs = feeConfigService.findApplicable(feeType, date, productCode, channel, customerSegment);
        return ResponseEntity.ok(configs.stream().map(feeConfigMapper::toResponse).toList());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Update a fee configuration")
    public ResponseEntity<FeeConfigResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody FeeConfigRequest request) {
        FeeConfig config = feeConfigService.update(id, request);
        return ResponseEntity.ok(feeConfigMapper.toResponse(config));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Activate a fee configuration")
    public ResponseEntity<Void> activate(@PathVariable UUID id) {
        feeConfigService.activate(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Deactivate a fee configuration")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        feeConfigService.deactivate(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a fee configuration")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        feeConfigService.delete(id);
        return ResponseEntity.noContent().build();
    }
}