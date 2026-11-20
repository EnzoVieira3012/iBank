package com.ibank.api.controller;

import com.ibank.api.dto.AccountRequest;
import com.ibank.api.dto.AccountResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "Accounts", description = "Contas correntes e poupança")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    @PostMapping
    @Operation(summary = "Abrir nova conta", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Conta criada",
            content = @Content(schema = @Schema(implementation = AccountResponse.class))),
        @ApiResponse(responseCode = "400", description = "Dados inválidos",
            content = @Content(schema = @Schema(implementation = com.ibank.api.exception.ProblemDetail.class))),
        @ApiResponse(responseCode = "409", description = "CPF já possui conta",
            content = @Content(schema = @Schema(implementation = com.ibank.api.exception.ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Não autenticado",
            content = @Content(schema = @Schema(implementation = com.ibank.api.exception.ProblemDetail.class)))
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    public ResponseEntity<AccountResponse> openAccount(
            @Valid @RequestBody AccountRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        log.info("Opening account for CPF: {}", request.getCpf());

        AccountResponse response = AccountResponse.builder()
            .id(UUID.randomUUID())
            .accountNumber("12345678-" + (int)(Math.random() * 9))
            .accountType(request.getAccountType())
            .holderName(request.getHolderName())
            .cpf(request.getCpf())
            .balance(java.math.BigDecimal.ZERO)
            .status("ACTIVE")
            .createdAt(java.time.OffsetDateTime.now())
            .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar conta por ID", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Conta encontrada",
            content = @Content(schema = @Schema(implementation = AccountResponse.class))),
        @ApiResponse(responseCode = "404", description = "Conta não encontrada",
            content = @Content(schema = @Schema(implementation = com.ibank.api.exception.ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Não autenticado",
            content = @Content(schema = @Schema(implementation = com.ibank.api.exception.ProblemDetail.class)))
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable UUID id) {
        log.info("Getting account: {}", id);

        AccountResponse response = AccountResponse.builder()
            .id(id)
            .accountNumber("12345678-9")
            .accountType("CURRENT")
            .holderName("João Silva")
            .cpf("123.456.789-00")
            .balance(new java.math.BigDecimal("1500.00"))
            .status("ACTIVE")
            .createdAt(java.time.OffsetDateTime.now().minusDays(30))
            .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Listar contas", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de contas",
            content = @Content(schema = @Schema(implementation = AccountResponse.class))),
        @ApiResponse(responseCode = "401", description = "Não autenticado",
            content = @Content(schema = @Schema(implementation = com.ibank.api.exception.ProblemDetail.class)))
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    public ResponseEntity<List<AccountResponse>> listAccounts(
            @RequestParam(required = false) String cpf,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Listing accounts with filters: cpf={}, status={}", cpf, status);

        AccountResponse account = AccountResponse.builder()
            .id(UUID.randomUUID())
            .accountNumber("12345678-9")
            .accountType("CURRENT")
            .holderName("João Silva")
            .cpf("123.456.789-00")
            .balance(new java.math.BigDecimal("1500.00"))
            .status("ACTIVE")
            .createdAt(java.time.OffsetDateTime.now().minusDays(30))
            .build();

        return ResponseEntity.ok(List.of(account));
    }

    @PostMapping("/{id}/block")
    @Operation(summary = "Bloquear conta", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Conta bloqueada"),
        @ApiResponse(responseCode = "404", description = "Conta não encontrada",
            content = @Content(schema = @Schema(implementation = com.ibank.api.exception.ProblemDetail.class))),
        @ApiResponse(responseCode = "400", description = "Conta já bloqueada",
            content = @Content(schema = @Schema(implementation = com.ibank.api.exception.ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Não autenticado",
            content = @Content(schema = @Schema(implementation = com.ibank.api.exception.ProblemDetail.class)))
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    public ResponseEntity<Void> blockAccount(
            @PathVariable UUID id,
            @RequestBody @Valid BlockAccountRequest request) {
        log.info("Blocking account: {} - reason: {}", id, request.getReason());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/unblock")
    @Operation(summary = "Desbloquear conta", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Conta desbloqueada"),
        @ApiResponse(responseCode = "404", description = "Conta não encontrada"),
        @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    public ResponseEntity<Void> unblockAccount(@PathVariable UUID id) {
        log.info("Unblocking account: {}", id);
        return ResponseEntity.noContent().build();
    }
}