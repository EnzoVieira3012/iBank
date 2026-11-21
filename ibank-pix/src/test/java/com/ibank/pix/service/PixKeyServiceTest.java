package com.ibank.pix.service;

import com.ibank.account.model.Account;
import com.ibank.account.model.AccountType;
import com.ibank.account.repository.AccountRepository;
import com.ibank.core.exception.BusinessException;
import com.ibank.core.exception.NotFoundException;
import com.ibank.core.model.AccountStatus;
import com.ibank.pix.client.MockDictClient;
import com.ibank.pix.model.PixKey;
import com.ibank.pix.service.QRCodeService;
import com.ibank.pix.model.PixKeyStatus;
import com.ibank.pix.model.PixKeyType;
import com.ibank.pix.repository.PixKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PixKeyServiceTest {

    @Mock
    private PixKeyRepository pixKeyRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private MockDictClient mockDictClient;

    @Mock
    private QRCodeService qrCodeService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PixKeyService pixKeyService;

    private Account account;
    private PixKeyService.CreatePixKeyCommand validCommand;

    @BeforeEach
    void setUp() {
        account = Account.builder()
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
            .pixEnabled(true)
            .pixLimit(com.ibank.core.util.Money.of(5000))
            .build();

        validCommand = new PixKeyService.CreatePixKeyCommand(
            account.getId(),
            PixKeyType.CPF,
            "123.456.789-00"
        );
    }

    @Test
    void createKey_ShouldCreateKey_WhenValidCpf() {
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        when(pixKeyRepository.existsByKeyValue(validCommand.keyValue())).thenReturn(false);
        when(pixKeyRepository.countByAccountIdAndStatus(account.getId(), PixKeyStatus.ACTIVE)).thenReturn(0L);
        when(pixKeyRepository.save(any(PixKey.class))).thenAnswer(inv -> inv.getArgument(0));
        
        var dictResult = new MockDictClient.DictRegistrationResult(true, "DICT-ABC123", null);
        when(mockDictClient.registerKey(any(PixKey.class))).thenReturn(dictResult);

        PixKey result = pixKeyService.createKey(validCommand, "idem-key");

        assertThat(result).isNotNull();
        assertThat(result.getKeyValue()).isEqualTo("123.456.789-00");
        assertThat(result.getKeyType()).isEqualTo(PixKeyType.CPF);
        assertThat(result.getStatus()).isEqualTo(PixKeyStatus.ACTIVE);
        assertThat(result.getDictRegistrationId()).isEqualTo("DICT-ABC123");
        assertThat(result.getAccountId()).isEqualTo(account.getId());

        verify(pixKeyRepository).save(any(PixKey.class));
        verify(mockDictClient).registerKey(any(PixKey.class));
        verify(qrCodeService).generateStaticQRCode(any(), any(), any(), any(), any(), any(), any());
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void createKey_ShouldThrowNotFound_WhenAccountNotExists() {
        when(accountRepository.findById(account.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pixKeyService.createKey(validCommand, "idem-key"))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void createKey_ShouldThrowConflict_WhenKeyAlreadyExists() {
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        when(pixKeyRepository.existsByKeyValue(validCommand.keyValue())).thenReturn(true);

        assertThatThrownBy(() -> pixKeyService.createKey(validCommand, "idem-key"))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", "KEY_ALREADY_EXISTS");
    }

    @Test
    void createKey_ShouldThrowConflict_WhenMaxKeysReached() {
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        when(pixKeyRepository.existsByKeyValue(validCommand.keyValue())).thenReturn(false);
        when(pixKeyRepository.countByAccountIdAndStatus(account.getId(), PixKeyStatus.ACTIVE)).thenReturn(5L);

        assertThatThrownBy(() -> pixKeyService.createKey(validCommand, "idem-key"))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", "MAX_KEYS_REACHED");
    }

    @Test
    void createKey_ShouldThrowError_WhenInvalidFormat() {
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        
        var invalidCommand = new PixKeyService.CreatePixKeyCommand(
            account.getId(), PixKeyType.CPF, "123");

        assertThatThrownBy(() -> pixKeyService.createKey(invalidCommand, "idem-key"))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", "INVALID_KEY_FORMAT");
    }

    @Test
    void createKey_ShouldBlockKey_WhenDictRegistrationFails() {
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        when(pixKeyRepository.existsByKeyValue(validCommand.keyValue())).thenReturn(false);
        when(pixKeyRepository.countByAccountIdAndStatus(account.getId(), PixKeyStatus.ACTIVE)).thenReturn(0L);
        when(pixKeyRepository.save(any(PixKey.class))).thenAnswer(inv -> inv.getArgument(0));
        
        var dictResult = new MockDictClient.DictRegistrationResult(false, null, "DICT unavailable");
        when(mockDictClient.registerKey(any(PixKey.class))).thenReturn(dictResult);

        assertThatThrownBy(() -> pixKeyService.createKey(validCommand, "idem-key"))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", "DICT_REGISTRATION_FAILED");
    }

    @Test
    void createRandomKey_ShouldGenerateRandomKey() {
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        when(pixKeyRepository.existsByKeyValue(anyString())).thenReturn(false);
        when(pixKeyRepository.countByAccountIdAndStatus(account.getId(), PixKeyStatus.ACTIVE)).thenReturn(0L);
        when(pixKeyRepository.save(any(PixKey.class))).thenAnswer(inv -> inv.getArgument(0));
        
        var dictResult = new MockDictClient.DictRegistrationResult(true, "DICT-ABC123", null);
        when(mockDictClient.registerKey(any(PixKey.class))).thenReturn(dictResult);

        PixKey result = pixKeyService.createRandomKey(account.getId(), "idem-key");

        assertThat(result).isNotNull();
        assertThat(result.getKeyType()).isEqualTo(PixKeyType.RANDOM);
        assertThat(result.getKeyValue()).matches("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}");
    }

    @Test
    void blockKey_ShouldBlockActiveKey() {
        PixKey key = PixKey.builder()
            .id(UUID.randomUUID())
            .keyValue("123.456.789-00")
            .keyType(PixKeyType.CPF)
            .status(PixKeyStatus.ACTIVE)
            .accountId(account.getId())
            .build();

        when(pixKeyRepository.findById(key.getId())).thenReturn(Optional.of(key));
        when(pixKeyRepository.save(any(PixKey.class))).thenAnswer(inv -> inv.getArgument(0));

        pixKeyService.blockKey(key.getId(), "Suspeita de fraude");

        assertThat(key.getStatus()).isEqualTo(PixKeyStatus.BLOCKED);
        assertThat(key.getBlockedReason()).isEqualTo("Suspeita de fraude");
        assertThat(key.getBlockedAt()).isNotNull();
    }

    @Test
    void blockKey_ShouldThrowConflict_WhenAlreadyBlocked() {
        PixKey key = PixKey.builder()
            .id(UUID.randomUUID())
            .status(PixKeyStatus.BLOCKED)
            .build();

        when(pixKeyRepository.findById(key.getId())).thenReturn(Optional.of(key));

        assertThatThrownBy(() -> pixKeyService.blockKey(key.getId(), "Reason"))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", "KEY_ALREADY_BLOCKED");
    }

    @Test
    void unblockKey_ShouldUnblockBlockedKey() {
        PixKey key = PixKey.builder()
            .id(UUID.randomUUID())
            .status(PixKeyStatus.BLOCKED)
            .blockedReason("Suspeita de fraude")
            .build();

        when(pixKeyRepository.findById(key.getId())).thenReturn(Optional.of(key));
        when(pixKeyRepository.save(any(PixKey.class))).thenAnswer(inv -> inv.getArgument(0));

        pixKeyService.unblockKey(key.getId());

        assertThat(key.getStatus()).isEqualTo(PixKeyStatus.ACTIVE);
        assertThat(key.getBlockedReason()).isNull();
        assertThat(key.getBlockedAt()).isNull();
    }

    @Test
    void deleteKey_ShouldDeleteBlockedKey() {
        PixKey key = PixKey.builder()
            .id(UUID.randomUUID())
            .keyValue("123.456.789-00")
            .status(PixKeyStatus.BLOCKED)
            .build();

        when(pixKeyRepository.findById(key.getId())).thenReturn(Optional.of(key));

        pixKeyService.deleteKey(key.getId());

        verify(mockDictClient).removeKey("123.456.789-00");
        verify(pixKeyRepository).delete(key);
    }

    @Test
    void deleteKey_ShouldThrowConflict_WhenActive() {
        PixKey key = PixKey.builder()
            .id(UUID.randomUUID())
            .status(PixKeyStatus.ACTIVE)
            .build();

        when(pixKeyRepository.findById(key.getId())).thenReturn(Optional.of(key));

        assertThatThrownBy(() -> pixKeyService.deleteKey(key.getId()))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", "KEY_ACTIVE");
    }

    @Test
    void lookupKeyInDict_ShouldReturnResult() {
        var lookupResult = new MockDictClient.DictLookupResult(
            true,
            new MockDictClient.DictKeyInfo(
                "123.456.789-00", PixKeyType.CPF, "account-id", "CURRENT", 
                "0001", "12345678-9", "João Silva", "123.456.789-00", "12345678"
            ),
            null
        );
        when(mockDictClient.lookupKey("123.456.789-00")).thenReturn(lookupResult);

        var result = pixKeyService.lookupKeyInDict("123.456.789-00");

        assertThat(result.found()).isTrue();
        assertThat(result.keyInfo().keyValue()).isEqualTo("123.456.789-00");
    }
}