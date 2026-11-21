package com.ibank.card.controller;

import com.ibank.card.controller.dto.InvoiceResponse;
import com.ibank.card.mapper.InvoiceMapper;
import com.ibank.card.model.Invoice;
import com.ibank.card.model.InvoiceStatus;
import com.ibank.card.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cards/{cardId}/invoices")
@RequiredArgsConstructor
@Tag(name = "Card Invoices", description = "Invoice management: generation, payment, installments")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final InvoiceMapper invoiceMapper;

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    @Operation(summary = "Generate invoice for a reference month")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Invoice generated or retrieved"),
        @ApiResponse(responseCode = "404", description = "Card not found")
    })
    public ResponseEntity<InvoiceResponse> generateInvoice(
            @PathVariable UUID cardId,
            @RequestParam LocalDate referenceMonth) {
        Invoice invoice = invoiceService.generateInvoice(cardId, referenceMonth);
        return ResponseEntity.ok(invoiceMapper.toResponse(invoice));
    }

    @GetMapping("/{invoiceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "Get invoice by ID")
    public ResponseEntity<InvoiceResponse> getInvoice(
            @PathVariable UUID cardId,
            @PathVariable UUID invoiceId) {
        Invoice invoice = invoiceService.getById(invoiceId);
        if (!invoice.getCardId().equals(cardId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(invoiceMapper.toResponse(invoice));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "List all invoices for a card")
    public ResponseEntity<List<InvoiceResponse>> listInvoices(@PathVariable UUID cardId) {
        List<Invoice> invoices = invoiceService.findByCardId(cardId);
        return ResponseEntity.ok(invoices.stream().map(invoiceMapper::toResponse).toList());
    }

    @GetMapping("/open")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "List open/overdue invoices for a card")
    public ResponseEntity<List<InvoiceResponse>> listOpenInvoices(@PathVariable UUID cardId) {
        List<Invoice> invoices = invoiceService.findOpenInvoices(cardId);
        return ResponseEntity.ok(invoices.stream().map(invoiceMapper::toResponse).toList());
    }

    @PostMapping("/{invoiceId}/pay")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "Pay an invoice")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Invoice paid"),
        @ApiResponse(responseCode = "400", description = "Invalid amount or invoice already paid"),
        @ApiResponse(responseCode = "404", description = "Invoice not found")
    })
    public ResponseEntity<InvoiceResponse> payInvoice(
            @PathVariable UUID cardId,
            @PathVariable UUID invoiceId,
            @RequestParam BigDecimal amount,
            @RequestParam String paymentMethod,
            @RequestParam UUID paymentTransactionId) {
        Invoice invoice = invoiceService.payInvoice(invoiceId, amount, paymentMethod, paymentTransactionId);
        return ResponseEntity.ok(invoiceMapper.toResponse(invoice));
    }
}