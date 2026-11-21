package com.ibank.transfer.service;

import com.ibank.account.model.Account;
import com.ibank.account.model.AccountStatus;
import com.ibank.account.repository.AccountRepository;
import com.ibank.account.service.AccountService;
import com.ibank.core.exception.BusinessException;
import com.ibank.core.exception.InsufficientFundsException;
import com.ibank.core.exception.NotFoundException;
import com.ibank.core.util.Money;
import com.ibank.transfer.model.Transfer;
import com.ibank.transfer.model.TransferStatus;
import com.ibank.transfer.model.TransferType;
import com.ibank.transfer.repository.TransferRepository;
import com.ibank.transfer.event.TransferCreatedEvent;
import com.ibank.transfer.event.TransferCompletedEvent;
import com.ibank.transfer.event.TransferCancelledEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TransferService {

    private final TransferRepository transferRepository;
    private final AccountRepository accountRepository;
    private final AccountService accountService;
    private final ApplicationEventPublisher eventPublisher;

    private static final int TED_CUTOFF_HOUR = 17;
    private static final ZoneId BRAZIL_ZONE = ZoneId.of("America/Sao_Paulo");

    public Transfer createInternalTransfer(CreateInternalTransferCommand command, String idempotencyKey) {
        if (transferRepository.existsByIdempotencyKey(idempotencyKey)) {
            throw BusinessException.conflict("IDEMPOTENCY_KEY_EXISTS", 
                "Transfer with this idempotency key already exists");
        }

        Account payerAccount = accountRepository.findById(command.payerAccountId())
            .orElseThrow(() -> new NotFoundException("Payer account not found: " + command.payerAccountId()));

        validateActiveAccount(payerAccount);

        Account payeeAccount = accountRepository.findById(command.payeeAccountId())
            .orElseThrow(() -> new NotFoundException("Payee account not found: " + command.payeeAccountId()));

        validateActiveAccount(payeeAccount);

        if (payerAccount.getId().equals(payeeAccount.getId())) {
            throw BusinessException.badRequest("SAME_ACCOUNT", "Cannot transfer to same account");
        }

        Money totalAmount = command.amount().add(command.feeAmount() != null ? command.feeAmount() : Money.ZERO);
        Money availableBalance = payerAccount.getEffectiveAvailableBalance();
        if (availableBalance.isLessThan(totalAmount)) {
            throw new InsufficientFundsException(availableBalance, totalAmount);
        }

        String endToEndId = generateEndToEndId("INT");

        Transfer transfer = Transfer.builder()
            .idempotencyKey(idempotencyKey)
            .endToEndId(endToEndId)
            .transferType(TransferType.INTERNAL)
            .status(TransferStatus.PROCESSING)
            .amount(command.amount())
            .description(command.description())
            .payerAccountId(payerAccount.getId())
            .payerName(payerAccount.getHolderName())
            .payerCpf(payerAccount.getHolderCpf())
            .payerBranch(payerAccount.getBranch())
            .payerAccountNumber(payerAccount.getAccountNumber())
            .payeeAccountId(payeeAccount.getId())
            .payeeName(payeeAccount.getHolderName())
            .payeeCpf(payeeAccount.getHolderCpf())
            .payeeBranch(payeeAccount.getBranch())
            .payeeAccountNumber(payeeAccount.getAccountNumber())
            .feeAmount(command.feeAmount())
            .build();

        Transfer saved = transferRepository.save(transfer);

        try {
            accountService.debit(payerAccount.getId(), totalAmount,
                "Transferência interna para " + payeeAccount.getHolderName() + " (" + command.description() + ")");
            accountService.credit(payeeAccount.getId(), command.amount(),
                "Transferência interna de " + payerAccount.getHolderName() + " (" + command.description() + ")");

            if (command.feeAmount() != null && command.feeAmount().isPositive()) {
                // Fee will be collected by fee engine
            }

            saved.setStatus(TransferStatus.COMPLETED);
            saved.setExecutedAt(OffsetDateTime.now());
            transferRepository.save(saved);

            eventPublisher.publishEvent(new TransferCompletedEvent(
                saved.getId(), saved.getEndToEndId(), saved.getTransferType(),
                payerAccount.getId(), payeeAccount.getId(), command.amount(), 
                command.feeAmount() != null ? command.feeAmount() : Money.ZERO
            ));

            log.info("Internal transfer completed: {} from {} to {}", 
                endToEndId, payerAccount.getAccountNumber(), payeeAccount.getAccountNumber());

        } catch (Exception e) {
            saved.setStatus(TransferStatus.FAILED);
            saved.setFailedAt(OffsetDateTime.now());
            saved.setFailureReason(e.getMessage());
            transferRepository.save(saved);
            throw e;
        }

        eventPublisher.publishEvent(new TransferCreatedEvent(
            saved.getId(), saved.getEndToEndId(), saved.getTransferType(),
            payerAccount.getId(), payeeAccount.getId(), command.amount(), idempotencyKey
        ));

        return saved;
    }

    public Transfer createTEDTransfer(CreateTEDTransferCommand command, String idempotencyKey) {
        if (transferRepository.existsByIdempotencyKey(idempotencyKey)) {
            throw BusinessException.conflict("IDEMPOTENCY_KEY_EXISTS", 
                "Transfer with this idempotency key already exists");
        }

        Account payerAccount = accountRepository.findById(command.payerAccountId())
            .orElseThrow(() -> new NotFoundException("Payer account not found: " + command.payerAccountId()));

        validateActiveAccount(payerAccount);

        // Validate TED cutoff time (17:00 Brazil time)
        OffsetDateTime now = OffsetDateTime.now(BRAZIL_ZONE);
        if (now.getHour() >= TED_CUTOFF_HOUR) {
            throw BusinessException.conflict("TED_CUTOFF_EXCEEDED", 
                "TED transfers only allowed before 17:00. Current time: " + now.toLocalTime() + 
                ". Use scheduled transfer for after hours.");
        }

        Money totalAmount = command.amount().add(command.feeAmount() != null ? command.feeAmount() : Money.ZERO);
        Money availableBalance = payerAccount.getEffectiveAvailableBalance();
        if (availableBalance.isLessThan(totalAmount)) {
            throw new InsufficientFundsException(availableBalance, totalAmount);
        }

        String endToEndId = generateEndToEndId("TED");

        Transfer transfer = Transfer.builder()
            .idempotencyKey(idempotencyKey)
            .endToEndId(endToEndId)
            .transferType(TransferType.TED)
            .status(TransferStatus.PROCESSING)
            .amount(command.amount())
            .description(command.description())
            .payerAccountId(payerAccount.getId())
            .payerName(payerAccount.getHolderName())
            .payerCpf(payerAccount.getHolderCpf())
            .payerBranch(payerAccount.getBranch())
            .payerAccountNumber(payerAccount.getAccountNumber())
            .payerBankIspb("12345678")
            .payeeName(command.payeeName())
            .payeeCpf(command.payeeCpf())
            .payeeBranch(command.payeeBranch())
            .payeeAccountNumber(command.payeeAccountNumber())
            .payeeBankIspb(command.payeeBankIspb())
            .payeeBankName(command.payeeBankName())
            .feeAmount(command.feeAmount())
            .build();

        Transfer saved = transferRepository.save(transfer);

        try {
            accountService.debit(payerAccount.getId(), totalAmount,
                "TED para " + command.payeeName() + " (" + command.description() + ")");

            // TED would go through external settlement - simplified here as completed
            saved.setStatus(TransferStatus.COMPLETED);
            saved.setExecutedAt(OffsetDateTime.now());
            saved.setExternalReference("TED-" + System.currentTimeMillis());
            transferRepository.save(saved);

            eventPublisher.publishEvent(new TransferCompletedEvent(
                saved.getId(), saved.getEndToEndId(), saved.getTransferType(),
                payerAccount.getId(), null, command.amount(), 
                command.feeAmount() != null ? command.feeAmount() : Money.ZERO
            ));

            log.info("TED transfer completed: {} from {} to {} ({})", 
                endToEndId, payerAccount.getAccountNumber(), command.payeeName(), command.payeeBankName());

        } catch (Exception e) {
            saved.setStatus(TransferStatus.FAILED);
            saved.setFailedAt(OffsetDateTime.now());
            saved.setFailureReason(e.getMessage());
            transferRepository.save(saved);
            throw e;
        }

        eventPublisher.publishEvent(new TransferCreatedEvent(
            saved.getId(), saved.getEndToEndId(), saved.getTransferType(),
            payerAccount.getId(), null, command.amount(), idempotencyKey
        ));

        return saved;
    }

    public Transfer scheduleTransfer(ScheduleTransferCommand command, String idempotencyKey) {
        if (transferRepository.existsByIdempotencyKey(idempotencyKey)) {
            throw BusinessException.conflict("IDEMPOTENCY_KEY_EXISTS", 
                "Transfer with this idempotency key already exists");
        }

        Account payerAccount = accountRepository.findById(command.payerAccountId())
            .orElseThrow(() -> new NotFoundException("Payer account not found: " + command.payerAccountId()));

        validateActiveAccount(payerAccount);

        if (command.scheduledAt().isBefore(OffsetDateTime.now())) {
            throw BusinessException.badRequest("INVALID_SCHEDULE", "Scheduled time must be in the future");
        }

        Money totalAmount = command.amount().add(command.feeAmount() != null ? command.feeAmount() : Money.ZERO);
        Money availableBalance = payerAccount.getEffectiveAvailableBalance();
        if (availableBalance.isLessThan(totalAmount)) {
            throw new InsufficientFundsException(availableBalance, totalAmount);
        }

        String endToEndId = generateEndToEndId("SCH");

        Transfer transfer = Transfer.builder()
            .idempotencyKey(idempotencyKey)
            .endToEndId(endToEndId)
            .transferType(command.transferType())
            .status(TransferStatus.SCHEDULED)
            .amount(command.amount())
            .description(command.description())
            .payerAccountId(payerAccount.getId())
            .payerName(payerAccount.getHolderName())
            .payerCpf(payerAccount.getHolderCpf())
            .payerBranch(payerAccount.getBranch())
            .payerAccountNumber(payerAccount.getAccountNumber())
            .payerBankIspb("12345678")
            .payeeAccountId(command.payeeAccountId())
            .payeeName(command.payeeName())
            .payeeCpf(command.payeeCpf())
            .payeeBranch(command.payeeBranch())
            .payeeAccountNumber(command.payeeAccountNumber())
            .payeeBankIspb(command.payeeBankIspb())
            .payeeBankName(command.payeeBankName())
            .feeAmount(command.feeAmount())
            .scheduledAt(command.scheduledAt())
            .build();

        Transfer saved = transferRepository.save(transfer);

        // Block the amount immediately for scheduled transfers
        accountService.blockAmount(payerAccount.getId(), totalAmount, 
            "Transferência agendada para " + command.scheduledAt());

        eventPublisher.publishEvent(new TransferCreatedEvent(
            saved.getId(), saved.getEndToEndId(), saved.getTransferType(),
            payerAccount.getId(), command.payeeAccountId(), command.amount(), idempotencyKey
        ));

        log.info("Transfer scheduled: {} for {} from {}", endToEndId, command.scheduledAt(), payerAccount.getAccountNumber());
        return saved;
    }

    @Transactional(readOnly = true)
    public Transfer getTransfer(UUID transferId) {
        return transferRepository.findById(transferId)
            .orElseThrow(() -> new NotFoundException("Transfer not found: " + transferId));
    }

    @Transactional(readOnly = true)
    public Transfer getTransferByEndToEndId(String endToEndId) {
        return transferRepository.findByEndToEndId(endToEndId)
            .orElseThrow(() -> new NotFoundException("Transfer not found: " + endToEndId));
    }

    @Transactional(readOnly = true)
    public Transfer getTransferByIdempotencyKey(String idempotencyKey) {
        return transferRepository.findByIdempotencyKey(idempotencyKey)
            .orElseThrow(() -> new NotFoundException("Transfer not found for idempotency key: " + idempotencyKey));
    }

    @Transactional(readOnly = true)
    public List<Transfer> getTransfersByAccount(UUID accountId) {
        return transferRepository.findByPayerAccountId(accountId);
    }

    @Transactional(readOnly = true)
    public List<Transfer> getSentTransfers(UUID accountId) {
        return transferRepository.findByPayerAccountId(accountId);
    }

    @Transactional(readOnly = true)
    public List<Transfer> getReceivedTransfers(UUID accountId) {
        return transferRepository.findByPayeeAccountId(accountId);
    }

    @Transactional(readOnly = true)
    public Page<Transfer> findWithFilters(UUID accountId, TransferStatus status, TransferType transferType,
                                           OffsetDateTime startDate, OffsetDateTime endDate, Pageable pageable) {
        return transferRepository.findWithFilters(accountId, status, transferType, startDate, endDate, pageable);
    }

    public void cancelTransfer(UUID transferId, CancelTransferCommand command) {
        Transfer transfer = getTransfer(transferId);

        if (!transfer.canBeCancelled()) {
            throw BusinessException.conflict("CANNOT_CANCEL", "Transfer cannot be cancelled. Status: " + transfer.getStatus());
        }

        Account payerAccount = accountRepository.findById(transfer.getPayerAccountId())
            .orElseThrow(() -> new NotFoundException("Payer account not found"));

        Money totalAmount = transfer.getAmount().add(transfer.getFeeAmount() != null ? transfer.getFeeAmount() : Money.ZERO);

        if (transfer.getStatus() == TransferStatus.SCHEDULED) {
            // Release blocked amount
            accountService.releaseBlockedAmount(payerAccount.getId(), totalAmount, 
                "Cancelamento de transferência agendada: " + command.reason());
        } else if (transfer.getStatus() == TransferStatus.PROCESSING) {
            // For already processed, would need refund logic
            accountService.credit(payerAccount.getId(), totalAmount,
                "Estorno de transferência cancelada: " + command.reason());
        }

        transfer.setStatus(TransferStatus.CANCELLED);
        transfer.setCancelledAt(OffsetDateTime.now());
        transfer.setCancelledReason(command.reason());
        transferRepository.save(transfer);

        eventPublisher.publishEvent(new TransferCancelledEvent(
            transfer.getId(), transfer.getEndToEndId(), transfer.getTransferType(),
            payerAccount.getId(), command.reason()
        ));

        log.info("Transfer cancelled: {} - reason: {}", transfer.getEndToEndId(), command.reason());
    }

    public void executeScheduledTransfers() {
        List<Transfer> scheduledTransfers = transferRepository.findScheduledTransfersReadyToExecute(OffsetDateTime.now());
        
        for (Transfer transfer : scheduledTransfers) {
            try {
                executeScheduledTransfer(transfer);
            } catch (Exception e) {
                log.error("Failed to execute scheduled transfer: {}", transfer.getEndToEndId(), e);
                transfer.setStatus(TransferStatus.FAILED);
                transfer.setFailedAt(OffsetDateTime.now());
                transfer.setFailureReason(e.getMessage());
                transferRepository.save(transfer);
            }
        }
    }

    private void executeScheduledTransfer(Transfer transfer) {
        Account payerAccount = accountRepository.findById(transfer.getPayerAccountId())
            .orElseThrow(() -> new NotFoundException("Payer account not found"));

        validateActiveAccount(payerAccount);

        Money totalAmount = transfer.getAmount().add(transfer.getFeeAmount() != null ? transfer.getFeeAmount() : Money.ZERO);

        // Release blocked amount and debit
        accountService.releaseBlockedAmount(payerAccount.getId(), totalAmount, 
            "Execução de transferência agendada: " + transfer.getDescription());

        // For internal scheduled transfers
        if (transfer.getPayeeAccountId() != null) {
            Account payeeAccount = accountRepository.findById(transfer.getPayeeAccountId())
                .orElseThrow(() -> new NotFoundException("Payee account not found"));
            
            validateActiveAccount(payeeAccount);
            accountService.credit(payeeAccount.getId(), transfer.getAmount(),
                "Transferência agendada de " + payerAccount.getHolderName() + " (" + transfer.getDescription() + ")");
        }

        transfer.setStatus(TransferStatus.COMPLETED);
        transfer.setExecutedAt(OffsetDateTime.now());
        transferRepository.save(transfer);

        eventPublisher.publishEvent(new TransferCompletedEvent(
            transfer.getId(), transfer.getEndToEndId(), transfer.getTransferType(),
            payerAccount.getId(), transfer.getPayeeAccountId(), transfer.getAmount(),
            transfer.getFeeAmount() != null ? transfer.getFeeAmount() : Money.ZERO
        ));

        log.info("Scheduled transfer executed: {}", transfer.getEndToEndId());
    }

    private void validateActiveAccount(Account account) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw BusinessException.conflict("ACCOUNT_NOT_ACTIVE", 
                "Account is not active: " + account.getStatus());
        }
    }

    private String generateEndToEndId(String prefix) {
        return prefix + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public record CreateInternalTransferCommand(
        UUID payerAccountId,
        UUID payeeAccountId,
        Money amount,
        String description,
        Money feeAmount
    ) {}

    public record CreateTEDTransferCommand(
        UUID payerAccountId,
        String payeeName,
        String payeeCpf,
        String payeeBranch,
        String payeeAccountNumber,
        String payeeBankIspb,
        String payeeBankName,
        Money amount,
        String description,
        Money feeAmount
    ) {}

    public record ScheduleTransferCommand(
        UUID payerAccountId,
        UUID payeeAccountId,
        String payeeName,
        String payeeCpf,
        String payeeBranch,
        String payeeAccountNumber,
        String payeeBankIspb,
        String payeeBankName,
        TransferType transferType,
        Money amount,
        String description,
        Money feeAmount,
        OffsetDateTime scheduledAt
    ) {}

    public record CancelTransferCommand(String reason) {}
}