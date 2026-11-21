package com.ibank.account.service;

import com.ibank.account.model.Account;
import com.ibank.account.model.AccountType;
import com.ibank.account.repository.AccountRepository;
import com.ibank.core.exception.BusinessException;
import com.ibank.core.exception.InsufficientFundsException;
import com.ibank.core.exception.NotFoundException;
import com.ibank.core.model.AccountStatus;
import com.ibank.core.util.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AccountService accountService;

    private AccountService.OpenAccountCommand validCommand;
    private Account savedAccount;

    @BeforeEach
    void setUp() {
        validCommand = new AccountService.OpenAccountCommand(
            "João Silva",
            "123.456.789-00",
            "joao@email.com",
            "+5511999999999",
            LocalDate.of(1990, 1, 1),
            AccountType.CURRENT,
            "0001"
        );

        savedAccount = Account.builder()
            .id(UUID.randomUUID())
            .accountNumber("12345678-9")
            .branch("0001")
            .accountType(AccountType.CURRENT)
            .status(AccountStatus.ACTIVE)
            .holderName("João Silva")
            .holderCpf("123.456.789-00")
            .holderEmail("joao@email.com")
            .holderPhone("+5511999999999")
            .holderBirthDate(LocalDate.of(1990, 1, 1))
            .balance(Money.ZERO)
            .availableBalance(Money.ZERO)
            .build();
    }

    @Test
    void openAccount_ShouldCreateAccount_WhenValidCommand() {
        when(accountRepository.existsByHolderCpfAndAccountType(anyString(), any()))
            .thenReturn(false);
        when(accountRepository.save(any(Account.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        AccountService.AccountSummaryDTO result = accountService.openAccount(validCommand, "idem-key");

        assertThat(result).isNotNull();
        assertThat(result.id()).isNotNull();
        assertThat(result.accountNumber()).matches("\\d{8}-\\d");
        assertThat(result.holderCpf()).isEqualTo("123.456.789-00");
        assertThat(result.accountType()).isEqualTo(AccountType.CURRENT);
        assertThat(result.status()).isEqualTo(AccountStatus.ACTIVE);

        verify(accountRepository).save(any(Account.class));
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void openAccount_ShouldThrowConflict_WhenAccountAlreadyExists() {
        when(accountRepository.existsByHolderCpfAndAccountType(anyString(), any()))
            .thenReturn(true);

        assertThatThrownBy(() -> accountService.openAccount(validCommand, "idem-key"))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", "ACCOUNT_ALREADY_EXISTS");

        verify(accountRepository, never()).save(any());
    }

    @Test
    void getAccount_ShouldReturnAccount_WhenExists() {
        UUID id = UUID.randomUUID();
        when(accountRepository.findById(id)).thenReturn(Optional.of(savedAccount));

        Account result = accountService.getAccount(id);

        assertThat(result).isEqualTo(savedAccount);
    }

    @Test
    void getAccount_ShouldThrowNotFound_WhenNotExists() {
        UUID id = UUID.randomUUID();
        when(accountRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccount(id))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void blockAccount_ShouldBlockAccount_WhenActive() {
        UUID id = savedAccount.getId();
        when(accountRepository.findById(id)).thenReturn(Optional.of(savedAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        var command = new AccountService.BlockAccountRequest("Suspeita de fraude");
        accountService.blockAccount(id, command);

        assertThat(savedAccount.getStatus()).isEqualTo(AccountStatus.BLOCKED);
        assertThat(savedAccount.getBlockedReason()).isEqualTo("Suspeita de fraude");
        assertThat(savedAccount.getBlockedAt()).isNotNull();

        verify(accountRepository).save(savedAccount);
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void blockAccount_ShouldThrowConflict_WhenAlreadyBlocked() {
        savedAccount.setStatus(AccountStatus.BLOCKED);
        UUID id = savedAccount.getId();
        when(accountRepository.findById(id)).thenReturn(Optional.of(savedAccount));

        var command = new AccountService.BlockAccountRequest("Suspeita de fraude");

        assertThatThrownBy(() -> accountService.blockAccount(id, command))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", "ACCOUNT_ALREADY_BLOCKED");
    }

    @Test
    void blockAccount_ShouldThrowConflict_WhenClosed() {
        savedAccount.setStatus(AccountStatus.CLOSED);
        UUID id = savedAccount.getId();
        when(accountRepository.findById(id)).thenReturn(Optional.of(savedAccount));

        var command = new AccountService.BlockAccountRequest("Suspeita de fraude");

        assertThatThrownBy(() -> accountService.blockAccount(id, command))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", "ACCOUNT_CLOSED");
    }

    @Test
    void unblockAccount_ShouldUnblock_WhenBlocked() {
        savedAccount.setStatus(AccountStatus.BLOCKED);
        savedAccount.setBlockedReason("Suspeita de fraude");
        UUID id = savedAccount.getId();
        when(accountRepository.findById(id)).thenReturn(Optional.of(savedAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        accountService.unblockAccount(id);

        assertThat(savedAccount.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(savedAccount.getBlockedReason()).isNull();
        assertThat(savedAccount.getBlockedAt()).isNull();

        verify(accountRepository).save(savedAccount);
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void unblockAccount_ShouldThrowConflict_WhenNotBlocked() {
        UUID id = savedAccount.getId();
        when(accountRepository.findById(id)).thenReturn(Optional.of(savedAccount));

        assertThatThrownBy(() -> accountService.unblockAccount(id))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", "ACCOUNT_NOT_BLOCKED");
    }

    @Test
    void closeAccount_ShouldClose_WhenBalanceZero() {
        UUID id = savedAccount.getId();
        when(accountRepository.findById(id)).thenReturn(Optional.of(savedAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        var command = new AccountService.CloseAccountRequest("Solicitação do cliente");
        accountService.closeAccount(id, command);

        assertThat(savedAccount.getStatus()).isEqualTo(AccountStatus.CLOSED);
        assertThat(savedAccount.getClosedReason()).isEqualTo("Solicitação do cliente");
        assertThat(savedAccount.getClosedAt()).isNotNull();

        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void closeAccount_ShouldThrowUnprocessable_WhenBalanceNotZero() {
        savedAccount.setBalance(Money.of(100));
        UUID id = savedAccount.getId();
        when(accountRepository.findById(id)).thenReturn(Optional.of(savedAccount));

        var command = new AccountService.CloseAccountRequest("Solicitação do cliente");

        assertThatThrownBy(() -> accountService.closeAccount(id, command))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", "BALANCE_NOT_ZERO");
    }

    @Test
    void credit_ShouldIncreaseBalance_WhenActive() {
        UUID id = savedAccount.getId();
        when(accountRepository.findById(id)).thenReturn(Optional.of(savedAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        accountService.credit(id, Money.of(500), "Depósito inicial");

        assertThat(savedAccount.getBalance()).isEqualTo(Money.of(500));
        assertThat(savedAccount.getAvailableBalance()).isEqualTo(Money.of(500));
        assertThat(savedAccount.getLastTransactionAt()).isNotNull();

        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void debit_ShouldDecreaseBalance_WhenSufficientFunds() {
        savedAccount.setBalance(Money.of(500));
        savedAccount.setAvailableBalance(Money.of(500));
        UUID id = savedAccount.getId();
        when(accountRepository.findById(id)).thenReturn(Optional.of(savedAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        accountService.debit(id, Money.of(200), "Saque");

        assertThat(savedAccount.getBalance()).isEqualTo(Money.of(300));
        assertThat(savedAccount.getAvailableBalance()).isEqualTo(Money.of(300));

        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void debit_ShouldThrowInsufficientFunds_WhenInsufficientBalance() {
        savedAccount.setBalance(Money.of(100));
        savedAccount.setAvailableBalance(Money.of(100));
        UUID id = savedAccount.getId();
        when(accountRepository.findById(id)).thenReturn(Optional.of(savedAccount));

        assertThatThrownBy(() -> accountService.debit(id, Money.of(200), "Saque"))
            .isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void debit_ShouldUseOverdraft_WhenCurrentAccountWithOverdraft() {
        savedAccount.setBalance(Money.of(100));
        savedAccount.setAvailableBalance(Money.of(100));
        savedAccount.setOverdraftLimit(Money.of(1000));
        UUID id = savedAccount.getId();
        when(accountRepository.findById(id)).thenReturn(Optional.of(savedAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        accountService.debit(id, Money.of(500), "Saque com cheque especial");

        assertThat(savedAccount.getBalance()).isEqualTo(Money.of(-400));
        assertThat(savedAccount.getOverdraftUsed()).isEqualTo(Money.of(400));
    }

    @Test
    void blockAmount_ShouldBlockAmount_WhenSufficientAvailable() {
        savedAccount.setBalance(Money.of(500));
        savedAccount.setAvailableBalance(Money.of(500));
        UUID id = savedAccount.getId();
        when(accountRepository.findById(id)).thenReturn(Optional.of(savedAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        accountService.blockAmount(id, Money.of(200), "PIX agendado");

        assertThat(savedAccount.getAvailableBalance()).isEqualTo(Money.of(300));
        assertThat(savedAccount.getBlockedBalance()).isEqualTo(Money.of(200));
        assertThat(savedAccount.getBalance()).isEqualTo(Money.of(500));
    }

    @Test
    void releaseBlockedAmount_ShouldRelease_WhenSufficientBlocked() {
        savedAccount.setBalance(Money.of(500));
        savedAccount.setAvailableBalance(Money.of(300));
        savedAccount.setBlockedBalance(Money.of(200));
        UUID id = savedAccount.getId();
        when(accountRepository.findById(id)).thenReturn(Optional.of(savedAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        accountService.releaseBlockedAmount(id, Money.of(200), "PIX cancelado");

        assertThat(savedAccount.getAvailableBalance()).isEqualTo(Money.of(500));
        assertThat(savedAccount.getBlockedBalance()).isEqualTo(Money.ZERO);
    }

    @Test
    void updateAccount_ShouldUpdateFields_WhenValid() {
        UUID id = savedAccount.getId();
        when(accountRepository.findById(id)).thenReturn(Optional.of(savedAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        var command = new AccountService.UpdateAccountCommand(
            "João Santos",
            "joao.santos@email.com",
            "+5511888888888",
            false,
            true,
            false,
            BigDecimal.valueOf(3000),
            BigDecimal.valueOf(15000),
            BigDecimal.valueOf(80000),
            BigDecimal.valueOf(3000),
            BigDecimal.valueOf(2000)
        );

        AccountService.AccountSummaryDTO result = accountService.updateAccount(id, command);

        assertThat(savedAccount.getHolderName()).isEqualTo("João Santos");
        assertThat(savedAccount.getHolderEmail()).isEqualTo("joao.santos@email.com");
        assertThat(savedAccount.getHolderPhone()).isEqualTo("+5511888888888");
        assertThat(savedAccount.getPixEnabled()).isFalse();
        assertThat(savedAccount.getDebitCardEnabled()).isTrue();
        assertThat(savedAccount.getDailyWithdrawalLimit()).isEqualTo(Money.of(3000));
    }

    @Test
    void updateAccount_ShouldThrowError_WhenOverdraftOnSavings() {
        savedAccount.setAccountType(AccountType.SAVINGS);
        UUID id = savedAccount.getId();
        when(accountRepository.findById(id)).thenReturn(Optional.of(savedAccount));

        var command = new AccountService.UpdateAccountCommand(
            null, null, null, null, null, null,
            null, null, null, null, BigDecimal.valueOf(1000)
        );

        assertThatThrownBy(() -> accountService.updateAccount(id, command))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", "OVERDRAFT_NOT_ALLOWED");
    }

    @Test
    void getBalance_ShouldReturnCompleteBalanceInfo() {
        savedAccount.setBalance(Money.of(1000));
        savedAccount.setAvailableBalance(Money.of(800));
        savedAccount.setBlockedBalance(Money.of(200));
        savedAccount.setOverdraftLimit(Money.of(500));
        savedAccount.setOverdraftUsed(Money.of(100));
        UUID id = savedAccount.getId();
        when(accountRepository.findById(id)).thenReturn(Optional.of(savedAccount));

        AccountService.AccountBalanceDTO result = accountService.getBalance(id);

        assertThat(result.balance()).isEqualTo(Money.of(1000));
        assertThat(result.availableBalance()).isEqualTo(Money.of(800));
        assertThat(result.blockedBalance()).isEqualTo(Money.of(200));
        assertThat(result.overdraftLimit()).isEqualTo(Money.of(500));
        assertThat(result.overdraftUsed()).isEqualTo(Money.of(100));
        assertThat(result.effectiveAvailableBalance()).isEqualTo(Money.of(1200)); // 800 + (500-100)
    }
}