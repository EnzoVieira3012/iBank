package com.ibank.api.controller;

import com.ibank.api.dto.pix.*;
import com.ibank.api.mapper.PixMapper;
import com.ibank.pix.client.MockDictClient;
import com.ibank.pix.model.PixKeyStatus;
import com.ibank.pix.model.PixKeyType;
import com.ibank.pix.model.QRCodeType;
import com.ibank.pix.service.PixKeyService;
import com.ibank.pix.service.PixTransactionService;
import com.ibank.pix.service.QRCodeService;
import com.ibank.account.repository.AccountRepository;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pix")
@Tag(name = "PIX", description = "PIX - Chaves, Pagamentos, QR Codes, Devoluções, Consulta DICT")
@RequiredArgsConstructor
@Slf4j
public class PixController {

    private final PixKeyService pixKeyService;
    private final PixTransactionService pixTransactionService;
    private final QRCodeService qrCodeService;
    private final MockDictClient mockDictClient;
    private final AccountRepository accountRepository;
    private final PixMapper pixMapper;

    // ==================== PIX KEYS ====================

    @PostMapping("/keys")
    @Operation(summary = "Cadastrar chave PIX", description = "Registra uma nova chave PIX (CPF, CNPJ, EMAIL, PHONE, RANDOM) no DICT", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Chave cadastrada",
            content = @Content(schema = @Schema(implementation = PixKeyResponse.class))),
        @ApiResponse(responseCode = "400", description = "Formato inválido"),
        @ApiResponse(responseCode = "409", description = "Chave já existe ou limite atingido"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "403", description = "Sem permissão")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    public ResponseEntity<PixKeyResponse> createKey(
            @Valid @RequestBody CreatePixKeyRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        log.info("Creating PIX key: {} for account {}", request.getKeyValue(), request.getAccountId());
        var command = pixMapper.toCreateCommand(request);
        PixKeyResponse response = pixMapper.toResponse(pixKeyService.createKey(command, idempotencyKey));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/keys/random")
    @Operation(summary = "Gerar chave aleatória", description = "Gera uma chave PIX aleatória (UUID) para a conta", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    public ResponseEntity<PixKeyResponse> createRandomKey(
            @RequestParam UUID accountId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        log.info("Creating random PIX key for account {}", accountId);
        PixKeyResponse response = pixMapper.toResponse(pixKeyService.createRandomKey(accountId, idempotencyKey));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/keys/{keyId}")
    @Operation(summary = "Consultar chave PIX por ID", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    public ResponseEntity<PixKeyResponse> getKey(@PathVariable UUID keyId) {
        log.info("Getting PIX key: {}", keyId);
        return ResponseEntity.ok(pixMapper.toResponse(pixKeyService.getKey(keyId)));
    }

    @GetMapping("/keys")
    @Operation(summary = "Listar chaves PIX da conta", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    public ResponseEntity<List<PixKeyResponse>> listKeys(
            @RequestParam UUID accountId,
            @RequestParam(required = false) PixKeyStatus status) {

        List<PixKeyResponse> keys = status != null ?
            pixKeyService.getKeysByAccount(accountId, status).stream().map(pixMapper::toResponse).toList() :
            pixKeyService.getKeysByAccount(accountId).stream().map(pixMapper::toResponse).toList();
        return ResponseEntity.ok(keys);
    }

    @PostMapping("/keys/{keyId}/block")
    @Operation(summary = "Bloquear chave PIX", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    public ResponseEntity<Void> blockKey(
            @PathVariable UUID keyId,
            @RequestParam String reason) {
        log.info("Blocking PIX key: {} - reason: {}", keyId, reason);
        pixKeyService.blockKey(keyId, reason);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/keys/{keyId}/unblock")
    @Operation(summary = "Desbloquear chave PIX", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    public ResponseEntity<Void> unblockKey(@PathVariable UUID keyId) {
        log.info("Unblocking PIX key: {}", keyId);
        pixKeyService.unblockKey(keyId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/keys/{keyId}")
    @Operation(summary = "Excluir chave PIX", description = "Remove a chave do DICT. Chave deve estar bloqueada.", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    public ResponseEntity<Void> deleteKey(@PathVariable UUID keyId) {
        log.info("Deleting PIX key: {}", keyId);
        pixKeyService.deleteKey(keyId);
        return ResponseEntity.noContent().build();
    }

    // ==================== DICT LOOKUP ====================

    @GetMapping("/dict/lookup")
    @Operation(summary = "Consultar chave no DICT (simulado)", description = "Simula consulta ao DICT do Bacen para obter informações da chave", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    public ResponseEntity<PixKeyLookupResponse> lookupDict(
            @RequestParam String keyValue) {
        log.info("Looking up key in DICT: {}", keyValue);
        var result = pixKeyService.lookupKeyInDict(keyValue);
        return ResponseEntity.ok(pixMapper.toLookupResponse(result));
    }

    // ==================== PIX PAYMENTS ====================

    @PostMapping("/pay")
    @Operation(summary = "Enviar PIX", description = "Realiza pagamento PIX usando chave do destinatário", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "PIX enviado",
            content = @Content(schema = @Schema(implementation = PixTransactionResponse.class))),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "404", description = "Chave ou conta não encontrada"),
        @ApiResponse(responseCode = "409", description = "Limite excedido, conta bloqueada, mesmo titular"),
        @ApiResponse(responseCode = "422", description = "Saldo insuficiente"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "403", description = "Sem permissão")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    public ResponseEntity<PixTransactionResponse> sendPix(
            @Valid @RequestBody SendPixRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        log.info("Sending PIX from account {} to key {} amount {}", 
            request.getPayerAccountId(), request.getPayeeKeyValue(), request.getAmount());
        var command = pixMapper.toSendCommand(request);
        PixTransactionResponse response = pixMapper.toResponse(pixTransactionService.sendPix(command, idempotencyKey));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/pay/qrcode")
    @Operation(summary = "Pagar QR Code PIX", description = "Realiza pagamento via QR Code (estático ou dinâmico)", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    public ResponseEntity<PixTransactionResponse> payQRCode(
            @Valid @RequestBody PayQRCodeRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        log.info("Paying QR Code from account {} amount {}", request.getPayerAccountId(), request.getAmount());
        var command = pixMapper.toPayQRCommand(request);
        PixTransactionResponse response = pixMapper.toResponse(pixTransactionService.payQRCode(command, idempotencyKey));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/refund")
    @Operation(summary = "Devolver PIX", description = "Solicita devolução total ou parcial de um PIX recebido", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    public ResponseEntity<PixTransactionResponse> refundPix(
            @Valid @RequestBody RefundPixRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        log.info("Refunding PIX: {} amount {}", request.getOriginalEndToEndId(), request.getAmount());
        var command = pixMapper.toRefundCommand(request);
        PixTransactionResponse response = pixMapper.toResponse(pixTransactionService.refundPix(command, idempotencyKey));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/transactions/{transactionId}")
    @Operation(summary = "Consultar transação PIX por ID", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    public ResponseEntity<PixTransactionResponse> getTransaction(@PathVariable UUID transactionId) {
        log.info("Getting PIX transaction: {}", transactionId);
        return ResponseEntity.ok(pixMapper.toResponse(pixTransactionService.getTransaction(transactionId)));
    }

    @GetMapping("/transactions/endtoend/{endToEndId}")
    @Operation(summary = "Consultar transação PIX por EndToEndId", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    public ResponseEntity<PixTransactionResponse> getTransactionByEndToEndId(@PathVariable String endToEndId) {
        log.info("Getting PIX transaction by EndToEndId: {}", endToEndId);
        return ResponseEntity.ok(pixMapper.toResponse(pixTransactionService.getTransactionByEndToEndId(endToEndId)));
    }

    @GetMapping("/transactions/sent")
    @Operation(summary = "Listar PIX enviados", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    public ResponseEntity<List<PixTransactionResponse>> getSentTransactions(
            @RequestParam UUID accountId) {
        return ResponseEntity.ok(
            pixTransactionService.getSentTransactions(accountId).stream()
                .map(pixMapper::toResponse).toList()
        );
    }

    @GetMapping("/transactions/received")
    @Operation(summary = "Listar PIX recebidos", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    public ResponseEntity<List<PixTransactionResponse>> getReceivedTransactions(
            @RequestParam UUID accountId) {
        return ResponseEntity.ok(
            pixTransactionService.getReceivedTransactions(accountId).stream()
                .map(pixMapper::toResponse).toList()
        );
    }

    // ==================== QR CODES ====================

    @PostMapping("/qrcode")
    @Operation(summary = "Gerar QR Code PIX", description = "Gera QR Code estático (chave) ou dinâmico (valor fixo, uso único)", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    public ResponseEntity<QRCodeResponse> generateQRCode(
            @Valid @RequestBody GenerateQRCodeRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        log.info("Generating QR Code type {} for account {}", request.getQrCodeType(), request.getAccountId());
        
        var account = accountRepository.findById(request.getAccountId())
            .orElseThrow(() -> new com.ibank.core.exception.NotFoundException("Account not found: " + request.getAccountId()));

        QRCodeResponse response;
        if (request.getQrCodeType() == QRCodeType.STATIC) {
            if (request.getPixKeyId() == null) {
                throw new IllegalArgumentException("Static QR Code requires pixKeyId");
            }
            var pixKey = pixKeyService.getKey(request.getPixKeyId());
            response = pixMapper.toResponse(qrCodeService.generateStaticQRCode(
                request.getAccountId(), request.getPixKeyId(), account.getHolderName(),
                account.getHolderCpf(), account.getAccountType().name(),
                account.getBranch(), account.getAccountNumber()
            ));
        } else {
            response = pixMapper.toResponse(qrCodeService.generateDynamicQRCode(
                request.getAccountId(), request.getPixKeyId(), 
                request.getAmount() != null ? request.getAmount() : com.ibank.core.util.Money.of(0),
                request.getDescription() != null ? request.getDescription() : "Pagamento PIX",
                request.getMaxUsage() != null ? request.getMaxUsage() : 1
            ));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/qrcode/{qrCodeId}")
    @Operation(summary = "Consultar QR Code", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    public ResponseEntity<QRCodeResponse> getQRCode(@PathVariable UUID qrCodeId) {
        return qrCodeService.getById(qrCodeId)
            .map(pixMapper::toResponse)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/qrcode/by-emv")
    @Operation(summary = "Consultar QR Code por EMV", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    public ResponseEntity<QRCodeResponse> getQRCodeByEmv(@RequestParam String emv) {
        return qrCodeService.getByEmv(emv)
            .map(pixMapper::toResponse)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/qrcode/account/{accountId}")
    @Operation(summary = "Listar QR Codes da conta", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    public ResponseEntity<org.springframework.data.domain.Page<QRCodeResponse>> listQRCodes(
            @PathVariable UUID accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        size = Math.min(size, 100);
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        return ResponseEntity.ok(
            qrCodeService.listByAccount(accountId, pageable).map(pixMapper::toResponse)
        );
    }
}