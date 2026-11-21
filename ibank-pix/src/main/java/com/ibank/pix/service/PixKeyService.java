package com.ibank.pix.service;

import com.ibank.account.model.Account;
import com.ibank.account.repository.AccountRepository;
import com.ibank.core.exception.BusinessException;
import com.ibank.core.exception.NotFoundException;
import com.ibank.pix.client.MockDictClient;
import com.ibank.pix.model.PixKey;
import com.ibank.pix.model.PixKeyStatus;
import com.ibank.pix.model.PixKeyType;
import com.ibank.pix.repository.PixKeyRepository;
import com.ibank.pix.event.PixKeyRegisteredEvent;
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
public class PixKeyService {

    private final PixKeyRepository pixKeyRepository;
    private final AccountRepository accountRepository;
    private final MockDictClient mockDictClient;
    private final QRCodeService qrCodeService;
    private final ApplicationEventPublisher eventPublisher;

    public PixKey createKey(CreatePixKeyCommand command, String idempotencyKey) {
        Account account = accountRepository.findById(command.accountId())
            .orElseThrow(() -> new NotFoundException("Account not found: " + command.accountId()));

        if (!command.keyType().validate(command.keyValue())) {
            throw BusinessException.badRequest("INVALID_KEY_FORMAT",
                "Invalid format for key type " + command.keyType() + ". Example: " + command.keyType().getExample());
        }

        if (pixKeyRepository.existsByKeyValue(command.keyValue())) {
            throw BusinessException.conflict("KEY_ALREADY_EXISTS", "PIX key already registered: " + command.keyValue());
        }

        long activeKeysCount = pixKeyRepository.countByAccountIdAndStatus(account.getId(), PixKeyStatus.ACTIVE);
        if (activeKeysCount >= 5) {
            throw BusinessException.conflict("MAX_KEYS_REACHED", "Maximum of 5 active PIX keys per account");
        }

        PixKey pixKey = PixKey.builder()
            .keyValue(command.keyValue())
            .keyType(command.keyType())
            .status(PixKeyStatus.PENDING)
            .accountId(account.getId())
            .holderName(account.getHolderName())
            .holderCpf(account.getHolderCpf())
            .holderAccountType(account.getAccountType().name())
            .holderBranch(account.getBranch())
            .holderAccountNumber(account.getAccountNumber())
            .build();

        PixKey saved = pixKeyRepository.save(pixKey);

        // Register in mock DICT
        var dictResult = mockDictClient.registerKey(saved);
        if (dictResult.success()) {
            saved.setStatus(PixKeyStatus.ACTIVE);
            saved.setDictRegistrationId(dictResult.registrationId());
            saved.setDictRegisteredAt(OffsetDateTime.now());
            pixKeyRepository.save(saved);

            // Generate static QR code for the key
            qrCodeService.generateStaticQRCode(
                account.getId(), saved.getId(), account.getHolderName(),
                account.getHolderCpf(), account.getAccountType().name(),
                account.getBranch(), account.getAccountNumber()
            );
        } else {
            saved.setStatus(PixKeyStatus.BLOCKED);
            saved.setBlockedReason("DICT registration failed: " + dictResult.error());
            pixKeyRepository.save(saved);
            throw BusinessException.unprocessable("DICT_REGISTRATION_FAILED", dictResult.error());
        }

        eventPublisher.publishEvent(new PixKeyRegisteredEvent(
            saved.getId(), saved.getKeyValue(), saved.getKeyType(),
            account.getId(), account.getHolderCpf(), idempotencyKey
        ));

        log.info("PIX key created: {} for account {}", saved.getKeyValue(), account.getAccountNumber());
        return saved;
    }

    public PixKey createRandomKey(UUID accountId, String idempotencyKey) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new NotFoundException("Account not found: " + accountId));

        String randomKey = UUID.randomUUID().toString();
        return createKey(new CreatePixKeyCommand(accountId, PixKeyType.RANDOM, randomKey), idempotencyKey);
    }

    @Transactional(readOnly = true)
    public PixKey getKey(UUID keyId) {
        return pixKeyRepository.findById(keyId)
            .orElseThrow(() -> new NotFoundException("PIX key not found: " + keyId));
    }

    @Transactional(readOnly = true)
    public PixKey getKeyByValue(String keyValue) {
        return pixKeyRepository.findByKeyValue(keyValue)
            .orElseThrow(() -> new NotFoundException("PIX key not found: " + keyValue));
    }

    @Transactional(readOnly = true)
    public List<PixKey> getKeysByAccount(UUID accountId) {
        return pixKeyRepository.findByAccountId(accountId);
    }

    @Transactional(readOnly = true)
    public List<PixKey> getKeysByAccount(UUID accountId, PixKeyStatus status) {
        return pixKeyRepository.findByAccountIdAndStatus(accountId, status);
    }

    public void blockKey(UUID keyId, String reason) {
        PixKey key = getKey(keyId);
        
        if (key.getStatus() == PixKeyStatus.BLOCKED) {
            throw BusinessException.conflict("KEY_ALREADY_BLOCKED", "Key is already blocked");
        }
        if (key.getStatus() == PixKeyStatus.CLAIMED) {
            throw BusinessException.conflict("KEY_CLAIMED", "Key has been claimed by another user");
        }

        key.setStatus(PixKeyStatus.BLOCKED);
        key.setBlockedAt(OffsetDateTime.now());
        key.setBlockedReason(reason);
        pixKeyRepository.save(key);

        log.info("PIX key blocked: {} - reason: {}", key.getKeyValue(), reason);
    }

    public void unblockKey(UUID keyId) {
        PixKey key = getKey(keyId);
        
        if (key.getStatus() != PixKeyStatus.BLOCKED) {
            throw BusinessException.conflict("KEY_NOT_BLOCKED", "Key is not blocked");
        }

        key.setStatus(PixKeyStatus.ACTIVE);
        key.setBlockedAt(null);
        key.setBlockedReason(null);
        pixKeyRepository.save(key);

        log.info("PIX key unblocked: {}", key.getKeyValue());
    }

    public void deleteKey(UUID keyId) {
        PixKey key = getKey(keyId);
        
        if (key.getStatus() == PixKeyStatus.ACTIVE) {
            throw BusinessException.conflict("KEY_ACTIVE", "Cannot delete active key. Block it first.");
        }

        mockDictClient.removeKey(key.getKeyValue());
        pixKeyRepository.delete(key);

        log.info("PIX key deleted: {}", key.getKeyValue());
    }

    public MockDictClient.DictLookupResult lookupKeyInDict(String keyValue) {
        return mockDictClient.lookupKey(keyValue);
    }

    public record CreatePixKeyCommand(UUID accountId, PixKeyType keyType, String keyValue) {}
}