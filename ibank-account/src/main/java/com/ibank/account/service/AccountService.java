package com.ibank.account.service;

import com.ibank.account.model.Account;
import com.ibank.account.model.AccountType;
import com.ibank.account.repository.AccountRepository;
import com.ibank.account.event.AccountOpenedEvent;
import com.ibank.account.event.AccountClosedEvent;
import com.ibank.account.event.AccountBlockedEvent;
import com.ibank.account.event.AccountUnblockedEvent;
import com.ibank.account.event.BalanceChangedEvent;
import com.ibank.core.exception.BusinessException;
import com.ibank.core.exception.InsufficientFundsException;
import com.ibank.core.exception.NotFoundException;
import com.ibank.core.model.AccountStatus;
import com.ibank.core.util.Money;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AccountService {

    private final AccountRepository accountRepository;
    private final ApplicationEventPublisher eventPublisher;

    public Account openAccount(OpenAccountCommand command, String idempotencyKey) {
        if (accountRepository.existsByHolderCpfAndAccountType(command.getHolderCpf(), command.getAccountType())) {
            throw BusinessException.conflict("ACCOUNT_ALREADY_EXISTS",
                "CPF " + command.getHolderCpf() + " already has a " + command.getAccountType() + " account");
        }

        Account account = Account.builder()
            .accountNumber(generateAccountNumber())
            .branch(command.getBranch() != null ? command.getBranch() : "0001")
            .accountType(command.getAccountType())
            .status(AccountStatus.ACTIVE)
            .holderName(command.getHolderName())
            .holderCpf(command.getHolderCpf())
            .holderEmail(command.getHolderEmail())
            .holderPhone(command.getHolderPhone())
            .holderBirthDate(command.getHolderBirthDate())
            .overdraftLimit(command.getAccountType().isAllowsOverdraft() ? Money.of(1000) : Money.ZERO)
            .dailyWithdrawalLimit(Money.of(5000))
            .dailyTransferLimit(Money.of(20000))
            .monthlyTransferLimit(Money.of(100000))
            .pixLimit(Money.of(5000))
            .pixEnabled(true)
            .debitCardEnabled(false)
            .creditCardEnabled(false)
            .interestRate(command.getAccountType().getYieldRate())
            .maintenanceFee(Money.of(command.getAccountType() == AccountType.CURRENT ? 15.00 : 0.00))
            .build();

        Account saved = accountRepository.save(account);

        eventPublisher.publishEvent(new AccountOpenedEvent(
            saved.getId(),
            saved.getAccountNumber(),
            saved.getHolderCpf(),
            saved.getAccountType(),
            idempotencyKey
        ));

        log.info("Account opened: {} for CPF: {}", saved.getAccountNumber(), saved.getHolderCpf());
        return saved;
    }

    @Transactional(readOnly = true)
    public Account getAccount(UUID id) {
        return accountRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Account not found: " + id));
    }

    @Transactional(readOnly = true)
    public Account getAccountByNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new NotFoundException("Account not found: " + accountNumber));
    }

    @Transactional(readOnly = true)
    public Page<Account> listAccounts(String cpf, AccountStatus status, AccountType accountType, String branch, Pageable pageable) {
        return accountRepository.findWithFilters(cpf, status, accountType, branch, pageable);
    }

    @Transactional(readOnly = true)
    public List<Account> getAccountsByCpf(String cpf) {
        return accountRepository.findByHolderCpf(cpf);
    }

    public Account updateAccount(UUID id, UpdateAccountCommand command) {
        Account account = getAccount(id);

        if (command.getHolderName() != null) {
            account.setHolderName(command.getHolderName());
        }
        if (command.getHolderEmail() != null) {
            account.setHolderEmail(command.getHolderEmail());
        }
        if (command.getHolderPhone() != null) {
            account.setHolderPhone(command.getHolderPhone());
        }
        if (command.getPixEnabled() != null) {
            account.setPixEnabled(command.getPixEnabled());
        }
        if (command.getDebitCardEnabled() != null) {
            account.setDebitCardEnabled(command.getDebitCardEnabled());
        }
        if (command.getCreditCardEnabled() != null) {
            account.setCreditCardEnabled(command.getCreditCardEnabled());
        }
        if (command.getDailyWithdrawalLimit() != null) {
            account.setDailyWithdrawalLimit(Money.of(command.getDailyWithdrawalLimit()));
        }
        if (command.getDailyTransferLimit() != null) {
            account.setDailyTransferLimit(Money.of(command.getDailyTransferLimit()));
        }
        if (command.getMonthlyTransferLimit() != null) {
            account.setMonthlyTransferLimit(Money.of(command.getMonthlyTransferLimit()));
        }
        if (command.getPixLimit() != null) {
            account.setPixLimit(Money.of(command.getPixLimit()));
        }
        if (command.getOverdraftLimit() != null) {
            if (!account.getAccountType().isAllowsOverdraft()) {
                throw BusinessException.badRequest("OVERDRAFT_NOT_ALLOWED",
                    "Account type " + account.getAccountType() + " does not allow overdraft");
            }
            account.setOverdraftLimit(Money.of(command.getOverdraftLimit()));
        }

        Account saved = accountRepository.save(account);
        log.info("Account updated: {}", saved.getAccountNumber());
        return saved;
    }

    public void blockAccount(UUID id, BlockAccountCommand command) {
        Account account = getAccount(id);

        if (account.getStatus() == AccountStatus.BLOCKED) {
            throw BusinessException.conflict("ACCOUNT_ALREADY_BLOCKED", "Account is already blocked");
        }
        if (account.getStatus() == AccountStatus.CLOSED) {
            throw BusinessException.conflict("ACCOUNT_CLOSED", "Cannot block a closed account");
        }

        account.setStatus(AccountStatus.BLOCKED);
        account.setBlockedAt(OffsetDateTime.now());
        account.setBlockedReason(command.getReason());

        accountRepository.save(account);

        eventPublisher.publishEvent(new AccountBlockedEvent(
            account.getId(),
            account.getAccountNumber(),
            account.getHolderCpf(),
            command.getReason()
        ));

        log.info("Account blocked: {} - reason: {}", account.getAccountNumber(), command.getReason());
    }

    public void unblockAccount(UUID id) {
        Account account = getAccount(id);

        if (account.getStatus() != AccountStatus.BLOCKED) {
            throw BusinessException.conflict("ACCOUNT_NOT_BLOCKED", "Account is not blocked");
        }

        account.setStatus(AccountStatus.ACTIVE);
        account.setBlockedAt(null);
        account.setBlockedReason(null);

        accountRepository.save(account);

        eventPublisher.publishEvent(new AccountUnblockedEvent(
            account.getId(),
            account.getAccountNumber(),
            account.getHolderCpf()
        ));

        log.info("Account unblocked: {}", account.getAccountNumber());
    }

    public void closeAccount(UUID id, CloseAccountCommand command) {
        Account account = getAccount(id);

        if (account.getStatus() == AccountStatus.CLOSED) {
            throw BusinessException.conflict("ACCOUNT_ALREADY_CLOSED", "Account is already closed");
        }
        if (!account.getBalance().isZero()) {
            throw BusinessException.unprocessable("BALANCE_NOT_ZERO",
                "Cannot close account with non-zero balance. Current: " + account.getBalance());
        }
        if (account.getOverdraftUsed().isPositive()) {
            throw BusinessException.unprocessable("OVERDRAFT_PENDING",
                "Cannot close account with pending overdraft: " + account.getOverdraftUsed());
        }

        account.setStatus(AccountStatus.CLOSED);
        account.setClosedAt(OffsetDateTime.now());
        account.setClosedReason(command.getReason());

        accountRepository.save(account);

        eventPublisher.publishEvent(new AccountClosedEvent(
            account.getId(),
            account.getAccountNumber(),
            account.getHolderCpf(),
            command.getReason()
        ));

        log.info("Account closed: {} - reason: {}", account.getAccountNumber(), command.getReason());
    }

    public void credit(UUID id, Money amount, String description) {
        Account account = getAccount(id);
        validateActiveAccount(account);

        Money oldBalance = account.getBalance();
        account.credit(amount);
        account.setLastTransactionAt(OffsetDateTime.now());

        accountRepository.save(account);

        eventPublisher.publishEvent(new BalanceChangedEvent(
            account.getId(),
            account.getAccountNumber(),
            oldBalance,
            account.getBalance(),
            "CREDIT",
            description
        ));
    }

    public void debit(UUID id, Money amount, String description) {
        Account account = getAccount(id);
        validateActiveAccount(account);

        if (!account.canWithdraw(amount)) {
            throw new InsufficientFundsException(account.getEffectiveAvailableBalance(), amount);
        }

        Money oldBalance = account.getBalance();
        account.debit(amount);
        account.setLastTransactionAt(OffsetDateTime.now());

        if (account.getOverdraftUsed().isPositive()) {
            account.useOverdraft(Money.ZERO);
        }

        accountRepository.save(account);

        eventPublisher.publishEvent(new BalanceChangedEvent(
            account.getId(),
            account.getAccountNumber(),
            oldBalance,
            account.getBalance(),
            "DEBIT",
            description
        ));
    }

    public void blockAmount(UUID id, Money amount, String description) {
        Account account = getAccount(id);
        validateActiveAccount(account);

        if (!account.getAvailableBalance().isGreaterThanOrEqual(amount)) {
            throw new InsufficientFundsException(account.getAvailableBalance(), amount);
        }

        Money oldAvailable = account.getAvailableBalance();
        account.blockAmount(amount);
        accountRepository.save(account);

        eventPublisher.publishEvent(new BalanceChangedEvent(
            account.getId(),
            account.getAccountNumber(),
            account.getBalance(),
            account.getBalance(),
            "BLOCK",
            description
        ));
    }

    public void releaseBlockedAmount(UUID id, Money amount, String description) {
        Account account = getAccount(id);
        validateActiveAccount(account);

        if (!account.getBlockedBalance().isGreaterThanOrEqual(amount)) {
            throw BusinessException.badRequest("INSUFFICIENT_BLOCKED_BALANCE",
                "Blocked balance insufficient. Blocked: " + account.getBlockedBalance() + ", Requested: " + amount);
        }

        account.releaseBlockedAmount(amount);
        accountRepository.save(account);

        eventPublisher.publishEvent(new BalanceChangedEvent(
            account.getId(),
            account.getAccountNumber(),
            account.getBalance(),
            account.getBalance(),
            "RELEASE_BLOCK",
            description
        ));
    }

    @Transactional(readOnly = true)
    public AccountBalanceDTO getBalance(UUID id) {
        Account account = getAccount(id);
        return new AccountBalanceDTO(
            account.getId(),
            account.getAccountNumber(),
            account.getBalance(),
            account.getAvailableBalance(),
            account.getBlockedBalance(),
            account.getOverdraftLimit(),
            account.getOverdraftUsed(),
            account.getEffectiveAvailableBalance()
        );
    }

    @Transactional(readOnly = true)
    public AccountSummaryDTO getSummary(UUID id) {
        Account account = getAccount(id);
        return new AccountSummaryDTO(
            account.getId(),
            account.getAccountNumber(),
            account.getBranch(),
            account.getAccountType(),
            account.getStatus(),
            account.getHolderName(),
            account.getHolderCpf(),
            account.getBalance(),
            account.getAvailableBalance(),
            account.getOverdraftLimit(),
            account.getOverdraftUsed(),
            account.getCreatedAt(),
            account.getLastTransactionAt()
        );
    }

    private void validateActiveAccount(Account account) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw BusinessException.conflict("ACCOUNT_NOT_ACTIVE",
                "Account is not active. Current status: " + account.getStatus());
        }
    }

    private String generateAccountNumber() {
        String number;
        do {
            int base = 10000000 + (int) (Math.random() * 90000000);
            int dv = calculateDV(String.valueOf(base));
            number = base + "-" + dv;
        } while (accountRepository.findByAccountNumber(number).isPresent());
        return number;
    }

    private int calculateDV(String number) {
        int sum = 0;
        int weight = 2;
        for (int i = number.length() - 1; i >= 0; i--) {
            sum += Character.getNumericValue(number.charAt(i)) * weight;
            weight = weight == 9 ? 2 : weight + 1;
        }
        int dv = 11 - (sum % 11);
        return dv >= 10 ? 0 : dv;
    }

    public record OpenAccountCommand(
        String holderName,
        String holderCpf,
        String holderEmail,
        String holderPhone,
        java.time.LocalDate holderBirthDate,
        AccountType accountType,
        String branch
    ) {}

    public record UpdateAccountCommand(
        String holderName,
        String holderEmail,
        String holderPhone,
        Boolean pixEnabled,
        Boolean debitCardEnabled,
        Boolean creditCardEnabled,
        BigDecimal dailyWithdrawalLimit,
        BigDecimal dailyTransferLimit,
        BigDecimal monthlyTransferLimit,
        BigDecimal pixLimit,
        BigDecimal overdraftLimit
    ) {}

    public record BlockAccountCommand(String reason) {}

    public record CloseAccountCommand(String reason) {}

    public record AccountBalanceDTO(
        UUID id,
        String accountNumber,
        Money balance,
        Money availableBalance,
        Money blockedBalance,
        Money overdraftLimit,
        Money overdraftUsed,
        Money effectiveAvailableBalance
    ) {}

    public record AccountSummaryDTO(
        UUID id,
        String accountNumber,
        String branch,
        AccountType accountType,
        AccountStatus status,
        String holderName,
        String holderCpf,
        Money balance,
        Money availableBalance,
        Money overdraftLimit,
        Money overdraftUsed,
        OffsetDateTime createdAt,
        OffsetDateTime lastTransactionAt
    ) {}
}