package com.ibank.pix.client;

import com.ibank.pix.model.PixKey;
import com.ibank.pix.model.PixKeyType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class MockDictClient {

    private final Map<String, DictEntry> dict = new ConcurrentHashMap<>();

    public record DictEntry(
        String keyValue,
        PixKeyType keyType,
        String accountId,
        String accountType,
        String branch,
        String accountNumber,
        String holderName,
        String holderCpf,
        String ispb,
        OffsetDateTime registeredAt,
        String registrationId
    ) {}

    public DictRegistrationResult registerKey(PixKey pixKey) {
        String registrationId = "DICT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        DictEntry entry = new DictEntry(
            pixKey.getKeyValue(),
            pixKey.getKeyType(),
            pixKey.getAccountId().toString(),
            pixKey.getHolderAccountType(),
            pixKey.getHolderBranch(),
            pixKey.getHolderAccountNumber(),
            pixKey.getHolderName(),
            pixKey.getHolderCpf(),
            "12345678", // ISPB do iBank
            OffsetDateTime.now(),
            registrationId
        );

        dict.put(pixKey.getKeyValue(), entry);
        
        log.info("Mock DICT: Registered key {} with registrationId {}", pixKey.getKeyValue(), registrationId);
        
        return new DictRegistrationResult(true, registrationId, null);
    }

    public DictLookupResult lookupKey(String keyValue) {
        DictEntry entry = dict.get(keyValue);
        
        if (entry == null) {
            log.info("Mock DICT: Key {} not found", keyValue);
            return new DictLookupResult(false, null, "Chave não encontrada no DICT");
        }

        log.info("Mock DICT: Found key {} for account {}", keyValue, entry.accountId());
        
        return new DictLookupResult(true, new DictKeyInfo(
            entry.keyValue(),
            entry.keyType(),
            entry.accountId(),
            entry.accountType(),
            entry.branch(),
            entry.accountNumber(),
            entry.holderName(),
            entry.holderCpf(),
            entry.ispb()
        ), null);
    }

    public DictKeyInfo getKeyInfo(String keyValue) {
        DictEntry entry = dict.get(keyValue);
        if (entry == null) return null;
        
        return new DictKeyInfo(
            entry.keyValue(),
            entry.keyType(),
            entry.accountId(),
            entry.accountType(),
            entry.branch(),
            entry.accountNumber(),
            entry.holderName(),
            entry.holderCpf(),
            entry.ispb()
        );
    }

    public boolean claimKey(String keyValue, String newAccountId, String newHolderCpf) {
        DictEntry entry = dict.get(keyValue);
        if (entry == null) return false;
        
        dict.put(keyValue, new DictEntry(
            entry.keyValue(),
            entry.keyType(),
            newAccountId,
            entry.accountType(),
            entry.branch(),
            entry.accountNumber(),
            entry.holderName(),
            newHolderCpf,
            entry.ispb(),
            entry.registeredAt(),
            entry.registrationId()
        ));
        
        log.info("Mock DICT: Key {} claimed by account {}", keyValue, newAccountId);
        return true;
    }

    public void removeKey(String keyValue) {
        dict.remove(keyValue);
        log.info("Mock DICT: Key {} removed", keyValue);
    }

    public record DictRegistrationResult(
        boolean success,
        String registrationId,
        String error
    ) {}

    public record DictLookupResult(
        boolean found,
        DictKeyInfo keyInfo,
        String error
    ) {}

    public record DictKeyInfo(
        String keyValue,
        PixKeyType keyType,
        String accountId,
        String accountType,
        String branch,
        String accountNumber,
        String holderName,
        String holderCpf,
        String ispb
    ) {}
}