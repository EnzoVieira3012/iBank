package com.ibank.transfer.service;

import com.ibank.account.model.Account;
import com.ibank.account.model.AccountStatus;
import com.ibank.account.model.AccountType;
import com.ibank.account.repository.AccountRepository;
import com.ibank.core.exception.BusinessException;
import com.ibank.core.exception.InsufficientFundsException;
import com.ibank.core.exception.NotFoundException;
import com.ibank.core.util.Money;
import com.ibank.transfer.model.Transfer;
import com.ibank.transfer.model.TransferStatus;
import com.ibank.transfer.model.TransferType;
import com.ibank.transfer.repository.TransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountService accountService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private TransferService transferService;

    private Account payerAccount;
    private Account payeeAccount;
    private UUID payerAccountId;
    private UUID payeeAccountId;

    @BeforeEach
    void setUp() {
        payerAccountId = UUID.randomUUID();
        payeeAccountId = UUID.randomUUID();

        payerAccount = Account.builder()
            .id(payerAccountId)
            .accountNumber("12345678-9")
            .branch("0001")
            .accountType(AccountType.CURRENT)
            .status(AccountStatus.ACTIVE)
            .holderName("João Silva")
            .holderCpf("123.456.789-00")
            .balance(Money.of(1000))
            .availableBalance(Money.of(1000))
            .build();

        payeeAccount = Account.builder()
            .id(payeeAccountId)
            .accountNumber("87654321-0")
            .branch("0001")
            .accountType(AccountType.CURRENT)
            .status(AccountStatus.ACTIVE)
            .holderName("Maria Santos")
            .holderCpf("987.654.321-00")
            .balance(Money.of(500))
            .availableBalance(Money.of(500))
            .build();
    }

    @Test
    void createInternalTransfer_ShouldSucceed_WhenValid() {
        String idempotencyKey = UUID.randomUUID().toString();
        when(transferRepository.existsByIdempotencyKey(idempotencyKey)).thenReturn(false);
        when(accountRepository.findById(payerAccountId)).thenReturn(Optional.of(payerAccount));
        when(accountRepository.findById(payeeAccountId)).thenReturn(Optional.of(payeeAccount));
        when(transferRepository.save(any(Transfer.class))).thenAnswer(inv -> inv.getArgument(0));
        when(accountService.debit(any(), any(), any())).thenReturn(Money.ZERO);
        when(accountService.credit(any(), any(), any())).thenReturn(Money.ZERO);

        var command = new TransferService.CreateInternalTransferCommand(
            payerAccountId, payeeAccountId, Money.of(100), "Teste", Money.of(0)
        );

        Transfer result = transferService.createInternalTransfer(command, idempotencyKey);

        assertThat(result).isNotNull();
        assertThat(result.getTransferType()).isEqualTo(TransferType.INTERNAL);
        assertThat(result.getStatus()).isEqualTo(TransferStatus.COMPLETED);
        assertThat(result.getAmount()).isEqualTo(Money.of(100));
        assertThat(result.getPayerAccountId()).isEqualTo(payerAccountId);
        assertThat(result.getPayeeAccountId()).isEqualTo(payeeAccountId);
        assertThat(result.getIdempotencyKey()).isEqualTo(idempotencyKey);
        assertThat(result.getEndToEndId()).startsWith("INT");

        verify(transferRepository).save(any(Transfer.class));
        verify(accountService).debit(eq(payerAccountId), eq(Money.of(100)), anyString());
        verify(accountService).credit(eq(payeeAccountId), eq(Money.of(100)), anyString());
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void createInternalTransfer_ShouldThrowConflict_WhenIdempotencyKeyExists() {
        String idempotencyKey = UUID.randomUUID().toString();
        when(transferRepository.existsByIdempotencyKey(idempotencyKey)).thenReturn(true);

        var command = new TransferService.CreateInternalTransferCommand(
            payerAccountId, payeeAccountId, Money.of(100), "Teste", Money.ZERO
        );

        assertThatThrownBy(() -> transferService.createInternalTransfer(command, idempotencyKey))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", "IDEMPOTENCY_KEY_EXISTS");
    }

    @Test
    void createInternalTransfer_ShouldThrowNotFound_WhenPayerAccountNotExists() {
        String idempotencyKey = UUID.randomUUID().toString();
        when(transferRepository.existsByIdempotencyKey(idempotencyKey)).thenReturn(false);
        when(accountRepository.findById(payerAccountId)).thenReturn(Optional.empty());

        var command = new TransferService.CreateInternalTransferCommand(
            payerAccountId, payeeAccountId, Money.of(100), "Teste", Money.ZERO
        );

        assertThatThrownBy(() -> transferService.createInternalTransfer(command, idempotencyKey))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void createInternalTransfer_ShouldThrowInsufficientFunds_WhenBalanceLow() {
        String idempotencyKey = UUID.randomUUID().toString();
        payerAccount.setAvailableBalance(Money.of(50));
        when(transferRepository.existsByIdempotencyKey(idempotencyKey)).thenReturn(false);
        when(accountRepository.findById(payerAccountId)).thenReturn(Optional.of(payerAccount));
        when(accountRepository.findById(payeeAccountId)).thenReturn(Optional.of(payeeAccount));

        var command = new TransferService.CreateInternalTransferCommand(
            payerAccountId, payeeAccountId, Money.of(100), "Teste", Money.ZERO
        );

        assertThatThrownBy(() -> transferService.createInternalTransfer(command, idempotencyKey))
            .isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void createInternalTransfer_ShouldThrowConflict_WhenSameAccount() {
        String idempotencyKey = UUID.randomUUID().toString();
        when(transferRepository.existsByIdempotencyKey(idempotencyKey)).thenReturn(false);
        when(accountRepository.findById(payerAccountId)).thenReturn(Optional.of(payerAccount));
        when(accountRepository.findById(payerAccountId)).thenReturn(Optional.of(payerAccount));

        var command = new TransferService.CreateInternalTransferCommand(
            payerAccountId, payerAccountId, Money.of(100), "Teste", Money.ZERO
        );

        assertThatThrownBy(() -> transferService.createInternalTransfer(command, idempotencyKey))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", "SAME_ACCOUNT");
    }

    @Test
    void createTEDTransfer_ShouldSucceed_WhenBeforeCutoff() {
        String idempotencyKey = UUID.randomUUID().toString();
        when(transferRepository.existsByIdempotencyKey(idempotencyKey)).thenReturn(false);
        when(accountRepository.findById(payerAccountId)).thenReturn(Optional.of(payerAccount));
        when(transferRepository.save(any(Transfer.class))).thenAnswer(inv -> inv.getArgument(0));
        when(accountService.debit(any(), any(), any())).thenReturn(Money.ZERO);

        var command = new TransferService.CreateTEDTransferCommand(
            payerAccountId, "Maria Santos", "987.654.321-00", "0001", "87654321-0",
            "87654321", "Banco Teste", Money.of(200), "TED Teste", Money.of(8.50)
        );

        Transfer result = transferService.createTEDTransfer(command, idempotencyKey);

        assertThat(result).isNotNull();
        assertThat(result.getTransferType()).isEqualTo(TransferType.TED);
        assertThat(result.getStatus()).isEqualTo(TransferStatus.COMPLETED);
        assertThat(result.getAmount()).isEqualTo(Money.of(200));
        assertThat(result.getFeeAmount()).isEqualTo(Money.of(8.50));
        assertThat(result.getEndToEndId()).startsWith("TED");
    }

    @Test
    void scheduleTransfer_ShouldSucceed_WhenFutureDate() {
        String idempotencyKey = UUID.randomUUID().toString();
        OffsetDateTime futureTime = OffsetDateTime.now().plusDays(1);
        
        when(transferRepository.existsByIdempotencyKey(idempotencyKey)).thenReturn(false);
        when(accountRepository.findById(payerAccountId)).thenReturn(Optional.of(payerAccount));
        when(transferRepository.save(any(Transfer.class))).thenAnswer(inv -> inv.getArgument(0));
        when(accountService.blockAmount(any(), any(), any())).thenReturn(Money.ZERO);

        var command = new TransferService.ScheduleTransferCommand(
            payerAccountId, payeeAccountId, "Maria Santos", "987.654.321-00",
            "0001", "87654321-0", "87654321", "Banco Teste",
            TransferType.INTERNAL, Money.of(150), "Agendado", Money.ZERO, futureTime
        );

        Transfer result = transferService.scheduleTransfer(command, idempotencyKey);

        assertThat(result).isNotNull();
        assertThat(result.getTransferType()).isEqualTo(TransferType.INTERNAL);
        assertThat(result.getStatus()).isEqualTo(TransferStatus.SCHEDULED);
        assertThat(result.getScheduledAt()).isEqualTo(futureTime);
        assertThat(result.getEndToEndId()).startsWith("SCH");

        verify(accountService).blockAmount(eq(payerAccountId), eq(Money.of(150)), anyString());
    }

    @Test
    void scheduleTransfer_ShouldThrowBadRequest_WhenPastDate() {
        String idempotencyKey = UUID.randomUUID().toString();
        OffsetDateTime pastTime = OffsetDateTime.now().minusDays(1);
        
        when(transferRepository.existsByIdempotencyKey(idempotencyKey)).thenReturn(false);
        when(accountRepository.findById(payerAccountId)).thenReturn(Optional.of(payerAccount));

        var command = new TransferService.ScheduleTransferCommand(
            payerAccountId, payeeAccountId, "Maria Santos", "987.654.321-00",
            "0001", "87654321-0", "87654321", "Banco Teste",
            TransferType.INTERNAL, Money.of(150), "Agendado", Money.ZERO, pastTime
        );

        assertThatThrownBy(() -> transferService.scheduleTransfer(command, idempotencyKey))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", "INVALID_SCHEDULE");
    }

    @Test
    void cancelTransfer_ShouldCancelScheduledTransfer() {
        Transfer scheduledTransfer = Transfer.builder()
            .id(UUID.randomUUID())
            .endToEndId("SCH123456")
            .transferType(TransferType.INTERNAL)
            .status(TransferStatus.SCHEDULED)
            .amount(Money.of(150))
            .payerAccountId(payerAccountId)
            .feeAmount(Money.ZERO)
            .build();

        when(transferRepository.findById(scheduledTransfer.getId())).thenReturn(Optional.of(scheduledTransfer));
        when(accountRepository.findById(payerAccountId)).thenReturn(Optional.of(payerAccount));
        when(accountService.releaseBlockedAmount(any(), any(), any())).thenReturn(Money.ZERO);
        when(transferRepository.save(any(Transfer.class))).thenAnswer(inv -> inv.getArgument(0));

        var command = new TransferService.CancelTransferCommand("Mudança de planos");
        transferService.cancelTransfer(scheduledTransfer.getId(), command);

        assertThat(scheduledTransfer.getStatus()).isEqualTo(TransferStatus.CANCELLED);
        assertThat(scheduledTransfer.getCancelledReason()).isEqualTo("Mudança de planos");
        assertThat(scheduledTransfer.getCancelledAt()).isNotNull();

        verify(accountService).releaseBlockedAmount(eq(payerAccountId), eq(Money.of(150)), anyString());
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void cancelTransfer_ShouldThrowConflict_WhenCompleted() {
        Transfer completedTransfer = Transfer.builder()
            .id(UUID.randomUUID())
            .status(TransferStatus.COMPLETED)
            .build();

        when(transferRepository.findById(completedTransfer.getId())).thenReturn(Optional.of(completedTransfer));

        var command = new TransferService.CancelTransferCommand("Tentativa");

        assertThatThrownBy(() -> transferService.cancelTransfer(completedTransfer.getId(), command))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", "CANNOT_CANCEL");
    }
}