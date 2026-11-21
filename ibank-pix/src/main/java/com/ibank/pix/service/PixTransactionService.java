package com.ibank.pix.service;

import com.ibank.account.model.Account;
import com.ibank.account.repository.AccountRepository;
import com.ibank.account.service.AccountService;
import com.ibank.core.exception.BusinessException;
import com.ibank.core.exception.InsufficientFundsException;
import com.ibank.core.exception.NotFoundException;
import com.ibank.core.model.AccountStatus;
import com.ibank.core.util.Money;
import com.ibank.pix.client.MockDictClient;
import com.ibank.pix.model.PixKey;
import com.ibank.pix.model.PixTransaction;
import com.ibank.pix.model.PixTransactionStatus;
import com.ibank.pix.model.QRCode;
import com.ibank.pix.repository.PixKeyRepository;
import com.ibank.pix.repository.PixTransactionRepository;
import com.ibank.pix.repository.QRCodeRepository;
import com.ibank.pix.event.PixPaymentCompletedEvent;
import com.ibank.pix.event.PixRefundEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PixTransactionService {

    private final PixTransactionRepository transactionRepository;
    private final PixKeyRepository pixKeyRepository;
    private final QRCodeRepository qrCodeRepository;
    private final AccountRepository accountRepository;
    private final AccountService accountService;
    private final MockDictClient mockDictClient;
    private final ApplicationEventPublisher eventPublisher;

    public PixTransaction sendPix(SendPixCommand command, String idempotencyKey) {
        Account payerAccount = accountRepository.findById(command.payerAccountId())
            .orElseThrow(() -> new NotFoundException("Payer account not found: " + command.payerAccountId()));

        validateActiveAccount(payerAccount);
        validatePixEnabled(payerAccount);

        PixKey payeeKey = pixKeyRepository.findByKeyValue(command.payeeKeyValue())
            .orElseThrow(() -> new NotFoundException("PIX key not found: " + command.payeeKeyValue()));

        if (!payeeKey.canBeUsed()) {
            throw BusinessException.conflict("KEY_NOT_USABLE", "PIX key is not active: " + payeeKey.getStatus());
        }

        Account payeeAccount = accountRepository.findById(payeeKey.getAccountId())
            .orElseThrow(() -> new NotFoundException("Payee account not found for key"));

        validateActiveAccount(payeeAccount);

        if (payerAccount.getId().equals(payeeAccount.getId())) {
            throw BusinessException.badRequest("SAME_ACCOUNT", "Cannot send PIX to own account");
        }

        validatePixLimit(payerAccount, command.amount());

        Money availableBalance = payerAccount.getEffectiveAvailableBalance();
        if (availableBalance.isLessThan(command.amount())) {
            throw new InsufficientFundsException(availableBalance, command.amount());
        }

        String endToEndId = generateEndToEndId();

        PixTransaction transaction = PixTransaction.builder()
            .endToEndId(endToEndId)
            .status(PixTransactionStatus.PROCESSING)
            .amount(command.amount())
            .description(command.description())
            .payerAccountId(payerAccount.getId())
            .payerName(payerAccount.getHolderName())
            .payerCpf(payerAccount.getHolderCpf())
            .payerKeyId(command.payerKeyId())
            .payerKeyValue(command.payerKeyValue())
            .payerKeyType(command.payerKeyType())
            .payerBranch(payerAccount.getBranch())
            .payerAccountNumber(payerAccount.getAccountNumber())
            .payeeAccountId(payeeAccount.getId())
            .payeeName(payeeAccount.getHolderName())
            .payeeCpf(payeeAccount.getHolderCpf())
            .payeeKeyId(payeeKey.getId())
            .payeeKeyValue(payeeKey.getKeyValue())
            .payeeKeyType(payeeKey.getKeyType().name())
            .payeeBranch(payeeAccount.getBranch())
            .payeeAccountNumber(payeeAccount.getAccountNumber())
            .build();

        PixTransaction saved = transactionRepository.save(transaction);

        try {
            // Debit payer
            accountService.debit(payerAccount.getId(), command.amount(), 
                "PIX para " + payeeAccount.getHolderName() + " (" + command.description() + ")");

            // Credit payee
            accountService.credit(payeeAccount.getId(), command.amount(),
                "PIX de " + payerAccount.getHolderName() + " (" + command.description() + ")");

            saved.setStatus(PixTransactionStatus.COMPLETED);
            saved.setProcessedAt(OffsetDateTime.now());
            transactionRepository.save(saved);

            eventPublisher.publishEvent(new PixPaymentCompletedEvent(
                saved.getId(), saved.getEndToEndId(),
                payerAccount.getId(), payeeAccount.getId(),
                command.amount(), command.payerKeyId(), payeeKey.getId()
            ));

            log.info("PIX completed: {} from {} to {} amount {}", 
                endToEndId, payerAccount.getAccountNumber(), payeeAccount.getAccountNumber(), command.amount());

        } catch (Exception e) {
            saved.setStatus(PixTransactionStatus.FAILED);
            saved.setFailedAt(OffsetDateTime.now());
            saved.setFailureReason(e.getMessage());
            transactionRepository.save(saved);

            log.error("PIX failed: {}", endToEndId, e);
            throw e;
        }

        return saved;
    }

    public PixTransaction payQRCode(PayQRCodeCommand command, String idempotencyKey) {
        QRCode qrCode = qrCodeRepository.findByEmv(command.emv())
            .orElseThrow(() -> new NotFoundException("Invalid QR Code"));

        if (!qrCode.isUsable()) {
            throw BusinessException.conflict("QR_CODE_NOT_USABLE", 
                qrCode.isExpired() ? "QR Code expired" : "QR Code usage limit reached");
        }

        Account payerAccount = accountRepository.findById(command.payerAccountId())
            .orElseThrow(() -> new NotFoundException("Payer account not found"));

        validateActiveAccount(payerAccount);
        validatePixEnabled(payerAccount);

        Account payeeAccount = accountRepository.findById(qrCode.getAccountId())
            .orElseThrow(() -> new NotFoundException("Payee account not found"));

        validateActiveAccount(payeeAccount);

        if (payerAccount.getId().equals(payeeAccount.getId())) {
            throw BusinessException.badRequest("SAME_ACCOUNT", "Cannot pay own QR Code");
        }

        Money amount = qrCode.getAmount() != null ? qrCode.getAmount() : command.amount();
        if (amount == null || amount.isZero()) {
            throw BusinessException.badRequest("AMOUNT_REQUIRED", "Amount is required for this QR Code");
        }

        validatePixLimit(payerAccount, amount);

        Money availableBalance = payerAccount.getEffectiveAvailableBalance();
        if (availableBalance.isLessThan(amount)) {
            throw new InsufficientFundsException(availableBalance, amount);
        }

        String endToEndId = generateEndToEndId();

        PixTransaction transaction = PixTransaction.builder()
            .endToEndId(endToEndId)
            .status(PixTransactionStatus.PROCESSING)
            .amount(amount)
            .description(command.description())
            .payerAccountId(payerAccount.getId())
            .payerName(payerAccount.getHolderName())
            .payerCpf(payerAccount.getHolderCpf())
            .payerKeyId(command.payerKeyId())
            .payerKeyValue(command.payerKeyValue())
            .payerKeyType(command.payerKeyType())
            .payerBranch(payerAccount.getBranch())
            .payerAccountNumber(payerAccount.getAccountNumber())
            .payeeAccountId(payeeAccount.getId())
            .payeeName(payeeAccount.getHolderName())
            .payeeCpf(payeeAccount.getHolderCpf())
            .payeeKeyId(qrCode.getPixKeyId())
            .payeeKeyValue(qrCode.getPixKeyId() != null ? 
                pixKeyRepository.findById(qrCode.getPixKeyId()).map(PixKey::getKeyValue).orElse("") : "")
            .payeeKeyType(qrCode.getPixKeyId() != null ? 
                pixKeyRepository.findById(qrCode.getPixKeyId()).map(PixKey::getKeyType).map(Enum::name).orElse("") : "")
            .payeeBranch(payeeAccount.getBranch())
            .payeeAccountNumber(payeeAccount.getAccountNumber())
            .build();

        PixTransaction saved = transactionRepository.save(transaction);

        try {
            accountService.debit(payerAccount.getId(), amount,
                "PIX QR Code para " + payeeAccount.getHolderName() + " (" + command.description() + ")");
            accountService.credit(payeeAccount.getId(), amount,
                "PIX QR Code de " + payerAccount.getHolderName() + " (" + command.description() + ")");

            saved.setStatus(PixTransactionStatus.COMPLETED);
            saved.setProcessedAt(OffsetDateTime.now());
            transactionRepository.save(saved);

            qrCode.markUsed();
            qrCode.setTransactionId(saved.getId());
            qrCodeRepository.save(qrCode);

            eventPublisher.publishEvent(new PixPaymentCompletedEvent(
                saved.getId(), saved.getEndToEndId(),
                payerAccount.getId(), payeeAccount.getId(),
                amount, command.payerKeyId(), qrCode.getPixKeyId()
            ));

            log.info("PIX QR Code payment completed: {} from {} to {}", 
                endToEndId, payerAccount.getAccountNumber(), payeeAccount.getAccountNumber());

        } catch (Exception e) {
            saved.setStatus(PixTransactionStatus.FAILED);
            saved.setFailedAt(OffsetDateTime.now());
            saved.setFailureReason(e.getMessage());
            transactionRepository.save(saved);
            throw e;
        }

        return saved;
    }

    public PixTransaction refundPix(RefundPixCommand command, String idempotencyKey) {
        PixTransaction original = transactionRepository.findByEndToEndId(command.originalEndToEndId())
            .orElseThrow(() -> new NotFoundException("Original transaction not found: " + command.originalEndToEndId()));

        if (!original.canRefund()) {
            throw BusinessException.conflict("CANNOT_REFUND", "Transaction cannot be refunded: " + original.getStatus());
        }

        Money refundAmount = command.amount() != null ? command.amount() : original.getRemainingRefundableAmount();
        if (refundAmount.isZero() || refundAmount.isGreaterThan(original.getRemainingRefundableAmount())) {
            throw BusinessException.badRequest("INVALID_REFUND_AMOUNT", 
                "Refund amount exceeds remaining refundable: " + original.getRemainingRefundableAmount());
        }

        Account payerAccount = accountRepository.findById(original.getPayerAccountId())
            .orElseThrow(() -> new NotFoundException("Payer account not found"));
        Account payeeAccount = accountRepository.findById(original.getPayeeAccountId())
            .orElseThrow(() -> new NotFoundException("Payee account not found"));

        validateActiveAccount(payerAccount);
        validateActiveAccount(payeeAccount);

        String refundEndToEndId = generateEndToEndId();

        PixTransaction refund = PixTransaction.builder()
            .endToEndId(refundEndToEndId)
            .status(PixTransactionStatus.PROCESSING)
            .amount(refundAmount)
            .description("Devolução: " + command.reason())
            .payerAccountId(payeeAccount.getId()) // Reversed: payee becomes payer
            .payerName(payeeAccount.getHolderName())
            .payerCpf(payeeAccount.getHolderCpf())
            .payeeAccountId(payerAccount.getId()) // Reversed: payer becomes payee
            .payeeName(payerAccount.getHolderName())
            .payeeCpf(payerAccount.getHolderCpf())
            .refundId(original.getId())
            .refundEndToEndId(original.getEndToEndId())
            .build();

        PixTransaction saved = transactionRepository.save(refund);

        try {
            accountService.debit(payeeAccount.getId(), refundAmount,
                "Devolução PIX para " + payerAccount.getHolderName() + " (" + command.reason() + ")");
            accountService.credit(payerAccount.getId(), refundAmount,
                "Devolução PIX de " + payeeAccount.getHolderName() + " (" + command.reason() + ")");

            saved.setStatus(PixTransactionStatus.COMPLETED);
            saved.setProcessedAt(OffsetDateTime.now());
            saved.setRefundedAmount(refundAmount);
            saved.setRefundReason(command.reason());
            saved.setRefundedAt(OffsetDateTime.now());
            transactionRepository.save(saved);

            // Update original transaction
            original.setRefundedAmount(original.getRefundedAmount() != null ? 
                original.getRefundedAmount().add(refundAmount) : refundAmount);
            if (original.getRefundedAmount().equals(original.getAmount())) {
                original.setStatus(PixTransactionStatus.REFUNDED);
            } else {
                original.setStatus(PixTransactionStatus.PARTIALLY_REFUNDED);
            }
            transactionRepository.save(original);

            eventPublisher.publishEvent(new PixRefundEvent(
                saved.getId(), saved.getEndToEndId(),
                original.getId(), original.getEndToEndId(),
                payeeAccount.getId(), payerAccount.getId(),
                refundAmount, command.reason()
            ));

            log.info("PIX refund completed: {} for original {} amount {}", 
                refundEndToEndId, original.getEndToEndId(), refundAmount);

        } catch (Exception e) {
            saved.setStatus(PixTransactionStatus.FAILED);
            saved.setFailedAt(OffsetDateTime.now());
            saved.setFailureReason(e.getMessage());
            transactionRepository.save(saved);
            throw e;
        }

        return saved;
    }

    @Transactional(readOnly = true)
    public PixTransaction getTransaction(UUID transactionId) {
        return transactionRepository.findById(transactionId)
            .orElseThrow(() -> new NotFoundException("Transaction not found: " + transactionId));
    }

    @Transactional(readOnly = true)
    public PixTransaction getTransactionByEndToEndId(String endToEndId) {
        return transactionRepository.findByEndToEndId(endToEndId)
            .orElseThrow(() -> new NotFoundException("Transaction not found: " + endToEndId));
    }

    @Transactional(readOnly = true)
    public List<PixTransaction> getTransactionsByAccount(UUID accountId) {
        return transactionRepository.findByPayerAccountId(accountId);
    }

    @Transactional(readOnly = true)
    public List<PixTransaction> getSentTransactions(UUID accountId) {
        return transactionRepository.findByPayerAccountId(accountId);
    }

    @Transactional(readOnly = true)
    public List<PixTransaction> getReceivedTransactions(UUID accountId) {
        return transactionRepository.findByPayeeAccountId(accountId);
    }

    private void validateActiveAccount(Account account) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw BusinessException.conflict("ACCOUNT_NOT_ACTIVE", 
                "Account is not active: " + account.getStatus());
        }
    }

    private void validatePixEnabled(Account account) {
        if (!account.getPixEnabled()) {
            throw BusinessException.conflict("PIX_DISABLED", "PIX is disabled for this account");
        }
    }

    private void validatePixLimit(Account account, Money amount) {
        if (account.getPixLimit() != null && amount.isGreaterThan(account.getPixLimit())) {
            throw BusinessException.conflict("PIX_LIMIT_EXCEEDED", 
                "PIX amount exceeds limit: " + account.getPixLimit());
        }
    }

    private String generateEndToEndId() {
        return "E" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public record SendPixCommand(
        UUID payerAccountId,
        String payerKeyValue,
        UUID payerKeyId,
        String payerKeyType,
        String payeeKeyValue,
        Money amount,
        String description
    ) {}

    public record PayQRCodeCommand(
        UUID payerAccountId,
        String payerKeyValue,
        UUID payerKeyId,
        String payerKeyType,
        String emv,
        Money amount,
        String description
    ) {}

    public record RefundPixCommand(
        String originalEndToEndId,
        Money amount,
        String reason
    ) {}
}