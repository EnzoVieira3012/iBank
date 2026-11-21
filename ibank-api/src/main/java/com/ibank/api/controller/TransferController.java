package com.ibank.api.controller;

import com.ibank.api.dto.transfer.*;
import com.ibank.api.mapper.TransferMapper;
import com.ibank.transfer.model.TransferStatus;
import com.ibank.transfer.model.TransferType;
import com.ibank.transfer.service.TransferService;
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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transfers")
@Tag(name = "Transfers", description = "Transferências - TED, Internas, Agendadas, Cancelamento")
@RequiredArgsConstructor
@Slf4j
public class TransferController {

    private final TransferService transferService;
    private final TransferMapper transferMapper;

    @PostMapping("/internal")
    @Operation(summary = "Transferência interna", description = "Transferência instantânea entre contas do mesmo banco", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Transferência realizada",
            content = @Content(schema = @Schema(implementation = TransferResponse.class))),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "404", description = "Conta não encontrada"),
        @ApiResponse(responseCode = "409", description = "Conta bloqueada, mesmo titular, idempotency duplicada"),
        @ApiResponse(responseCode = "422", description = "Saldo insuficiente"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "403", description = "Sem permissão")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    public ResponseEntity<TransferResponse> createInternalTransfer(
            @Valid @RequestBody CreateInternalTransferRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        log.info("Creating internal transfer from {} to {} amount {}", 
            request.getPayerAccountId(), request.getPayeeAccountId(), request.getAmount());
        
        var command = transferMapper.toInternalCommand(request);
        TransferResponse response = transferMapper.toResponse(transferService.createInternalTransfer(command, idempotencyKey));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/ted")
    @Operation(summary = "Transferência TED", description = "TED para outro banco (válida até 17h)", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "TED realizada",
            content = @Content(schema = @Schema(implementation = TransferResponse.class))),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "404", description = "Conta não encontrada"),
        @ApiResponse(responseCode = "409", description = "Após 17h use agendada, conta bloqueada, idempotency duplicada"),
        @ApiResponse(responseCode = "422", description = "Saldo insuficiente"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "403", description = "Sem permissão")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    public ResponseEntity<TransferResponse> createTEDTransfer(
            @Valid @RequestBody CreateTEDTransferRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        log.info("Creating TED transfer from {} to {} ({}) amount {}", 
            request.getPayerAccountId(), request.getPayeeName(), request.getPayeeBankName(), request.getAmount());
        
        var command = transferMapper.toTEDCommand(request);
        TransferResponse response = transferMapper.toResponse(transferService.createTEDTransfer(command, idempotencyKey));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/schedule")
    @Operation(summary = "Agendar transferência", description = "Agenda transferência interna ou TED para data/hora futura", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Transferência agendada",
            content = @Content(schema = @Schema(implementation = TransferResponse.class))),
        @ApiResponse(responseCode = "400", description = "Data no passado ou inválida"),
        @ApiResponse(responseCode = "404", description = "Conta não encontrada"),
        @ApiResponse(responseCode = "409", description = "Conta bloqueada, idempotency duplicada"),
        @ApiResponse(responseCode = "422", description = "Saldo insuficiente para bloqueio"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "403", description = "Sem permissão")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    public ResponseEntity<TransferResponse> scheduleTransfer(
            @Valid @RequestBody ScheduleTransferRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        log.info("Scheduling {} transfer from {} for {}", 
            request.getTransferType(), request.getPayerAccountId(), request.getScheduledAt());
        
        var command = transferMapper.toScheduleCommand(request);
        TransferResponse response = transferMapper.toResponse(transferService.scheduleTransfer(command, idempotencyKey));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{transferId}")
    @Operation(summary = "Consultar transferência por ID", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    public ResponseEntity<TransferResponse> getTransfer(@PathVariable UUID transferId) {
        log.info("Getting transfer: {}", transferId);
        return ResponseEntity.ok(transferMapper.toResponse(transferService.getTransfer(transferId)));
    }

    @GetMapping("/endtoend/{endToEndId}")
    @Operation(summary = "Consultar transferência por EndToEndId", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    public ResponseEntity<TransferResponse> getTransferByEndToEndId(@PathVariable String endToEndId) {
        log.info("Getting transfer by EndToEndId: {}", endToEndId);
        return ResponseEntity.ok(transferMapper.toResponse(transferService.getTransferByEndToEndId(endToEndId)));
    }

    @GetMapping("/idempotency/{idempotencyKey}")
    @Operation(summary = "Consultar transferência por Idempotency-Key", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    public ResponseEntity<TransferResponse> getTransferByIdempotencyKey(@PathVariable String idempotencyKey) {
        log.info("Getting transfer by IdempotencyKey: {}", idempotencyKey);
        return ResponseEntity.ok(transferMapper.toResponse(transferService.getTransferByIdempotencyKey(idempotencyKey)));
    }

    @GetMapping
    @Operation(summary = "Listar transferências com filtros", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    public ResponseEntity<Page<TransferResponse>> listTransfers(
            @Parameter(description = "Filtrar por conta (pagador ou recebedor)") @RequestParam(required = false) UUID accountId,
            @Parameter(description = "Filtrar por status") @RequestParam(required = false) TransferStatus status,
            @Parameter(description = "Filtrar por tipo") @RequestParam(required = false) TransferType transferType,
            @Parameter(description = "Data início") @RequestParam(required = false) OffsetDateTime startDate,
            @Parameter(description = "Data fim") @RequestParam(required = false) OffsetDateTime endDate,
            @Parameter(description = "Página (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamanho da página (máx 100)") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Campo para ordenação") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Direção da ordenação") @RequestParam(defaultValue = "desc") String sortDir) {

        size = Math.min(size, 100);
        Sort.Direction direction = Sort.Direction.fromString(sortDir.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<TransferResponse> result = transferService.findWithFilters(accountId, status, transferType, startDate, endDate, pageable)
            .map(transferMapper::toResponse);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/sent")
    @Operation(summary = "Listar transferências enviadas", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    public ResponseEntity<List<TransferResponse>> getSentTransfers(
            @RequestParam UUID accountId) {
        return ResponseEntity.ok(
            transferService.getSentTransfers(accountId).stream()
                .map(transferMapper::toResponse).toList()
        );
    }

    @GetMapping("/received")
    @Operation(summary = "Listar transferências recebidas", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    public ResponseEntity<List<TransferResponse>> getReceivedTransfers(
            @RequestParam UUID accountId) {
        return ResponseEntity.ok(
            transferService.getReceivedTransfers(accountId).stream()
                .map(transferMapper::toResponse).toList()
        );
    }

    @PostMapping("/{transferId}/cancel")
    @Operation(summary = "Cancelar transferência", description = "Cancela transferência pendente, agendada ou em processamento", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Transferência cancelada"),
        @ApiResponse(responseCode = "404", description = "Transferência não encontrada"),
        @ApiResponse(responseCode = "409", description = "Transferência não pode ser cancelada (já finalizada)"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "403", description = "Sem permissão")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    public ResponseEntity<Void> cancelTransfer(
            @PathVariable UUID transferId,
            @Valid @RequestBody CancelTransferRequest request) {

        log.info("Cancelling transfer: {} - reason: {}", transferId, request.getReason());
        var command = transferMapper.toCancelCommand(request);
        transferService.cancelTransfer(transferId, command);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/execute-scheduled")
    @Operation(summary = "Executar transferências agendadas", description = "Executa transferências agendadas cujo horário chegou (job interno)", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> executeScheduledTransfers() {
        log.info("Executing scheduled transfers");
        transferService.executeScheduledTransfers();
        return ResponseEntity.noContent().build();
    }
}