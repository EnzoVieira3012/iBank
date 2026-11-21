package com.ibank.api.controller;

import com.ibank.account.model.AccountType;
import com.ibank.account.service.AccountService;
import com.ibank.api.dto.account.*;
import com.ibank.api.mapper.AccountMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "Accounts", description = "Contas correntes e poupança - CRUD completo, saldos, bloqueio, encerramento")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final AccountService accountService;
    private final AccountMapper accountMapper;

    @PostMapping
    @Operation(
        summary = "Abrir nova conta",
        description = "Cria uma nova conta corrente ou poupança. Requer role ADMIN, MANAGER ou OPERATOR.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Conta criada com sucesso",
            content = @Content(schema = @Schema(implementation = AccountResponse.class))),
        @ApiResponse(responseCode = "400", description = "Dados inválidos",
            content = @Content(schema = @Schema(implementation = com.ibank.api.exception.ProblemDetail.class))),
        @ApiResponse(responseCode = "409", description = "CPF já possui conta do mesmo tipo",
            content = @Content(schema = @Schema(implementation = com.ibank.api.exception.ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Não autenticado",
            content = @Content(schema = @Schema(implementation = com.ibank.api.exception.ProblemDetail.class))),
        @ApiResponse(responseCode = "403", description = "Sem permissão",
            content = @Content(schema = @Schema(implementation = com.ibank.api.exception.ProblemDetail.class)))
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    public ResponseEntity<AccountResponse> openAccount(
            @Valid @RequestBody OpenAccountRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        log.info("Opening account for CPF: {} type: {}", request.getHolderCpf(), request.getAccountType());

        var command = accountMapper.toOpenCommand(request);
        AccountService.AccountSummaryDTO summary = accountService.openAccount(command, idempotencyKey);

        AccountResponse response = accountMapper.toResponse(
            accountService.getAccount(summary.id())
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Consultar conta por ID",
        description = "Retorna detalhes completos da conta. CLIENT só acessa suas próprias contas.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Conta encontrada",
            content = @Content(schema = @Schema(implementation = AccountResponse.class))),
        @ApiResponse(responseCode = "404", description = "Conta não encontrada",
            content = @Content(schema = @Schema(implementation = com.ibank.api.exception.ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "403", description = "Acesso negado - conta de outro cliente")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable UUID id) {
        log.info("Getting account: {}", id);
        var summary = accountService.getSummary(id);
        return ResponseEntity.ok(accountMapper.toSummaryResponse(
            accountService.getAccount(id)
        ));
    }

    @GetMapping
    @Operation(
        summary = "Listar contas com filtros e paginação",
        description = "Lista contas com filtros opcionais. ADMIN/MANAGER/OPERATOR veem todas, CLIENT só as suas.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de contas",
            content = @Content(schema = @Schema(implementation = AccountListResponse.class))),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "403", description = "Sem permissão")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    public ResponseEntity<AccountListResponse> listAccounts(
            @Parameter(description = "Filtrar por CPF do titular") @RequestParam(required = false) String cpf,
            @Parameter(description = "Filtrar por status") @RequestParam(required = false) com.ibank.core.model.AccountStatus status,
            @Parameter(description = "Filtrar por tipo de conta") @RequestParam(required = false) AccountType accountType,
            @Parameter(description = "Filtrar por agência") @RequestParam(required = false) String branch,
            @Parameter(description = "Número da página (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamanho da página (máx 100)") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Campo para ordenação") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Direção da ordenação") @RequestParam(defaultValue = "desc") String sortDir) {

        log.info("Listing accounts with filters: cpf={}, status={}, type={}, branch={}", cpf, status, accountType, branch);

        size = Math.min(size, 100);
        Sort.Direction direction = Sort.Direction.fromString(sortDir.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<AccountService.AccountSummaryDTO> result = accountService.listAccounts(cpf, status, accountType, branch, pageable);

        AccountListResponse response = AccountListResponse.builder()
            .content(result.getContent().stream()
                .map(accountMapper::toSummaryResponse)
                .toList())
            .page(result.getNumber())
            .size(result.getSize())
            .totalElements(result.getTotalElements())
            .totalPages(result.getTotalPages())
            .first(result.isFirst())
            .last(result.isLast())
            .empty(result.isEmpty())
            .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/summary")
    @Operation(
        summary = "Resumo da conta",
        description = "Retorna resumo leve da conta (sem limites e configurações detalhadas).",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    public ResponseEntity<AccountSummaryResponse> getAccountSummary(@PathVariable UUID id) {
        log.info("Getting account summary: {}", id);
        var summary = accountService.getSummary(id);
        return ResponseEntity.ok(accountMapper.toSummaryResponse(
            accountService.getAccount(id)
        ));
    }

    @GetMapping("/{id}/balance")
    @Operation(
        summary = "Consultar saldo detalhado",
        description = "Retorna saldo, disponível, bloqueado, limite de cheque especial e usado.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    public ResponseEntity<AccountBalanceResponse> getBalance(@PathVariable UUID id) {
        log.info("Getting balance for account: {}", id);
        var balance = accountService.getBalance(id);
        return ResponseEntity.ok(accountMapper.toBalanceResponse(balance));
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "Atualizar informações da conta",
        description = "Atualiza dados cadastrais e configurações da conta. Não altera CPF ou tipo de conta.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Conta atualizada",
            content = @Content(schema = @Schema(implementation = AccountResponse.class))),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "404", description = "Conta não encontrada"),
        @ApiResponse(responseCode = "409", description = "Limite de cheque especial não permitido para este tipo"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "403", description = "Sem permissão")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    public ResponseEntity<AccountResponse> updateAccount(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAccountRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        log.info("Updating account: {}", id);
        var command = accountMapper.toUpdateCommand(request);
        AccountService.AccountSummaryDTO summary = accountService.updateAccount(id, command);

        AccountResponse response = accountMapper.toResponse(
            accountService.getAccount(summary.id())
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/block")
    @Operation(
        summary = "Bloquear conta",
        description = "Bloqueia a conta impedindo movimentações. Requer justificativa.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Conta bloqueada com sucesso"),
        @ApiResponse(responseCode = "400", description = "Conta já encerrada"),
        @ApiResponse(responseCode = "404", description = "Conta não encontrada"),
        @ApiResponse(responseCode = "409", description = "Conta já bloqueada"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "403", description = "Sem permissão")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    public ResponseEntity<Void> blockAccount(
            @PathVariable UUID id,
            @Valid @RequestBody BlockAccountRequest request) {

        log.info("Blocking account: {} - reason: {}", id, request.getReason());
        var command = accountMapper.toBlockCommand(request);
        accountService.blockAccount(id, command);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/unblock")
    @Operation(
        summary = "Desbloquear conta",
        description = "Remove o bloqueio da conta permitindo movimentações novamente.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Conta desbloqueada com sucesso"),
        @ApiResponse(responseCode = "404", description = "Conta não encontrada"),
        @ApiResponse(responseCode = "409", description = "Conta não está bloqueada"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "403", description = "Sem permissão")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    public ResponseEntity<Void> unblockAccount(@PathVariable UUID id) {
        log.info("Unblocking account: {}", id);
        accountService.unblockAccount(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/close")
    @Operation(
        summary = "Encerrar conta",
        description = "Encerra definitivamente a conta. Saldo deve ser zero e sem cheque especial pendente.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Conta encerrada com sucesso"),
        @ApiResponse(responseCode = "400", description = "Conta com saldo não-zero ou cheque especial pendente"),
        @ApiResponse(responseCode = "404", description = "Conta não encontrada"),
        @ApiResponse(responseCode = "409", description = "Conta já encerrada"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "403", description = "Sem permissão")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    public ResponseEntity<Void> closeAccount(
            @PathVariable UUID id,
            @Valid @RequestBody CloseAccountRequest request) {

        log.info("Closing account: {} - reason: {}", id, request.getReason());
        var command = accountMapper.toCloseCommand(request);
        accountService.closeAccount(id, command);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/by-cpf/{cpf}")
    @Operation(
        summary = "Listar contas por CPF",
        description = "Retorna todas as contas de um CPF específico.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    public ResponseEntity<AccountListResponse> getAccountsByCpf(@PathVariable String cpf) {
        log.info("Getting accounts for CPF: {}", cpf);
        var accounts = accountService.getAccountsByCpf(cpf);
        return ResponseEntity.ok(AccountListResponse.builder()
            .content(accounts.stream()
                .map(accountMapper::toSummaryResponse)
                .toList())
            .page(0)
            .size(accounts.size())
            .totalElements(accounts.size())
            .totalPages(1)
            .first(true)
            .last(true)
            .empty(accounts.isEmpty())
            .build());
    }

    @GetMapping("/number/{accountNumber}")
    @Operation(
        summary = "Buscar conta por número",
        description = "Busca conta pelo número completo (ex: 12345678-9).",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    public ResponseEntity<AccountResponse> getAccountByNumber(@PathVariable String accountNumber) {
        log.info("Getting account by number: {}", accountNumber);
        var account = accountService.getAccountByNumber(accountNumber);
        return ResponseEntity.ok(accountMapper.toResponse(account));
    }
}