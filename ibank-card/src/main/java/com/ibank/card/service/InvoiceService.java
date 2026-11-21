package com.ibank.card.service;

import com.ibank.card.event.InvoiceGeneratedEvent;
import com.ibank.card.event.InvoicePaidEvent;
import com.ibank.card.exception.CardNotFoundException;
import com.ibank.card.exception.InvoiceNotFoundException;
import com.ibank.card.mapper.InvoiceMapper;
import com.ibank.card.model.Card;
import com.ibank.card.model.CardStatus;
import com.ibank.card.model.Invoice;
import com.ibank.card.model.InvoiceInstallment;
import com.ibank.card.model.InvoiceStatus;
import com.ibank.card.model.InstallmentStatus;
import com.ibank.card.repository.CardRepository;
import com.ibank.card.repository.InvoiceInstallmentRepository;
import com.ibank.card.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceInstallmentRepository installmentRepository;
    private final CardRepository cardRepository;
    private final CardTransactionService transactionService;
    private final InvoiceMapper invoiceMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Invoice generateInvoice(UUID cardId, LocalDate referenceMonth) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException("Card not found: " + cardId));

        if (card.getStatus() != CardStatus.ACTIVE && card.getStatus() != CardStatus.BLOCKED) {
            throw new IllegalStateException("Cannot generate invoice for card with status: " + card.getStatus());
        }

        Optional<Invoice> existing = invoiceRepository.findByCardIdAndReferenceMonth(cardId, referenceMonth);
        if (existing.isPresent()) {
            return existing.get();
        }

        LocalDate openingDate = referenceMonth.withDayOfMonth(1);
        LocalDate closingDate = referenceMonth.with(TemporalAdjusters.lastDayOfMonth());
        LocalDate dueDate = closingDate.plusDays(10);

        List<CardTransactionService.CardTransaction> transactions = transactionService.findByCardIdAndCreatedAtBetween(
                cardId, openingDate.atStartOfDay(), closingDate.atTime(23, 59, 59)
        );

        BigDecimal totalAmount = transactions.stream()
                .filter(t -> t.getType() == com.ibank.card.model.TransactionType.PURCHASE ||
                        t.getType() == com.ibank.card.model.TransactionType.WITHDRAWAL ||
                        t.getType() == com.ibank.card.model.TransactionType.FEE ||
                        t.getType() == com.ibank.card.model.TransactionType.INTEREST)
                .map(CardTransactionService.CardTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal paidAmount = transactions.stream()
                .filter(t -> t.getType() == com.ibank.card.model.TransactionType.PAYMENT ||
                        t.getType() == com.ibank.card.model.TransactionType.REFUND)
                .map(CardTransactionService.CardTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Invoice invoice = Invoice.builder()
                .cardId(cardId)
                .referenceMonth(referenceMonth)
                .openingDate(openingDate)
                .closingDate(closingDate)
                .dueDate(dueDate)
                .status(InvoiceStatus.OPEN)
                .totalAmount(totalAmount)
                .paidAmount(paidAmount)
                .minimumPayment(calculateMinimumPayment(totalAmount))
                .interestAmount(BigDecimal.ZERO)
                .feeAmount(BigDecimal.ZERO)
                .previousBalance(BigDecimal.ZERO)
                .build();

        Invoice saved = invoiceRepository.save(invoice);

        if (transactions.stream().anyMatch(t -> t.getInstallments() != null && t.getInstallments() > 1)) {
            createInstallments(saved, transactions);
        }

        eventPublisher.publishEvent(new InvoiceGeneratedEvent(
                this, saved.getId(), cardId, card.getAccountId(),
                referenceMonth, totalAmount, dueDate
        ));

        log.info("Invoice generated: {} for card {} reference {}", saved.getId(), cardId, referenceMonth);
        return saved;
    }

    private BigDecimal calculateMinimumPayment(BigDecimal totalAmount) {
        BigDecimal minPercentage = new BigDecimal("0.15");
        BigDecimal minimum = totalAmount.multiply(minPercentage).setScale(2, RoundingMode.HALF_UP);
        return minimum.max(new BigDecimal("50.00"));
    }

    private void createInstallments(Invoice invoice, List<CardTransactionService.CardTransaction> transactions) {
        List<InvoiceInstallment> installments = new ArrayList<>();
        int installmentNumber = 1;

        for (CardTransactionService.CardTransaction tx : transactions) {
            if (tx.getInstallments() != null && tx.getInstallments() > 1) {
                BigDecimal installmentAmount = tx.getAmount()
                        .divide(BigDecimal.valueOf(tx.getInstallments()), 2, RoundingMode.HALF_UP);

                for (int i = 0; i < tx.getInstallments(); i++) {
                    LocalDate dueDate = invoice.getDueDate().plusMonths(i);
                    InvoiceInstallment installment = InvoiceInstallment.builder()
                            .invoiceId(invoice.getId())
                            .installmentNumber(installmentNumber++)
                            .totalInstallments(tx.getInstallments())
                            .dueDate(dueDate)
                            .amount(installmentAmount)
                            .paidAmount(BigDecimal.ZERO)
                            .status(InstallmentStatus.PENDING)
                            .build();
                    installments.add(installment);
                }
            }
        }

        if (!installments.isEmpty()) {
            installmentRepository.saveAll(installments);
        }
    }

    @Transactional
    public Invoice payInvoice(UUID invoiceId, BigDecimal amount, String paymentMethod, UUID paymentTransactionId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException("Invoice not found: " + invoiceId));

        if (invoice.getStatus() == InvoiceStatus.PAID || invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new IllegalStateException("Invoice already paid or cancelled");
        }

        BigDecimal remainingAmount = invoice.getTotalAmount().subtract(invoice.getPaidAmount());
        if (amount.compareTo(remainingAmount) > 0) {
            throw new IllegalArgumentException("Payment amount exceeds remaining balance");
        }

        invoice.setPaidAmount(invoice.getPaidAmount().add(amount));

        if (invoice.getPaidAmount().compareTo(invoice.getTotalAmount()) >= 0) {
            invoice.setStatus(InvoiceStatus.PAID);
            invoice.setPaidAt(LocalDateTime.now());
        } else {
            invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
        }

        invoice.setPaymentMethod(paymentMethod);
        invoice.setPaymentTransactionId(paymentTransactionId);

        Invoice saved = invoiceRepository.save(invoice);

        payInstallments(invoiceId, amount, paymentTransactionId);

        eventPublisher.publishEvent(new InvoicePaidEvent(
                this, invoiceId, invoice.getCardId(), invoice.getAccountId(),
                amount, paymentMethod
        ));

        log.info("Invoice paid: {} amount {}", invoiceId, amount);
        return saved;
    }

    private void payInstallments(UUID invoiceId, BigDecimal amount, UUID paymentTransactionId) {
        List<InvoiceInstallment> pending = installmentRepository.findByInvoiceIdAndStatus(invoiceId, InstallmentStatus.PENDING);
        BigDecimal remaining = amount;

        for (InvoiceInstallment installment : pending) {
            BigDecimal installmentRemaining = installment.getAmount().subtract(installment.getPaidAmount());
            if (remaining.compareTo(installmentRemaining) >= 0) {
                installment.setPaidAmount(installment.getAmount());
                installment.setStatus(InstallmentStatus.PAID);
                installment.setPaidAt(LocalDateTime.now());
                installment.setPaymentTransactionId(paymentTransactionId);
                remaining = remaining.subtract(installmentRemaining);
            } else {
                installment.setPaidAmount(installment.getPaidAmount().add(remaining));
                remaining = BigDecimal.ZERO;
            }
            installmentRepository.save(installment);
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
        }
    }

    @Transactional(readOnly = true)
    public Optional<Invoice> findById(UUID id) {
        return invoiceRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Invoice getById(UUID id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new InvoiceNotFoundException("Invoice not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Invoice> findByCardId(UUID cardId) {
        return invoiceRepository.findByCardId(cardId);
    }

    @Transactional(readOnly = true)
    public List<Invoice> findOpenInvoices(UUID cardId) {
        return invoiceRepository.findOpenInvoicesByCard(cardId, List.of(InvoiceStatus.OPEN, InvoiceStatus.PARTIALLY_PAID, InvoiceStatus.OVERDUE));
    }

    @Transactional
    public void closeInvoices() {
        LocalDate today = LocalDate.now();
        List<Invoice> openInvoices = invoiceRepository.findByDueDateBefore(today);
        for (Invoice invoice : openInvoices) {
            if (invoice.getStatus() == InvoiceStatus.OPEN || invoice.getStatus() == InvoiceStatus.PARTIALLY_PAID) {
                invoice.setStatus(InvoiceStatus.OVERDUE);
                invoiceRepository.save(invoice);
            }
        }
    }
}