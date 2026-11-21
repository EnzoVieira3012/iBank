package com.ibank.card.service;

import com.ibank.card.event.CardCreatedEvent;
import com.ibank.card.exception.CardAlreadyExistsException;
import com.ibank.card.exception.CardNotFoundException;
import com.ibank.card.exception.InvalidCardStatusException;
import com.ibank.card.mapper.CardMapper;
import com.ibank.card.model.Card;
import com.ibank.card.model.CardStatus;
import com.ibank.card.model.CardType;
import com.ibank.card.repository.CardRepository;
import com.ibank.core.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardService {

    private final CardRepository cardRepository;
    private final CardMapper cardMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Card createCard(Card card) {
        validateCardCreation(card);

        String number = generateCardNumber(card.getType());
        String numberHash = hash(number);
        String lastFour = number.substring(number.length() - 4);
        String maskedNumber = CardMapper.INSTANCE.maskNumber(number);

        String cvv = generateCVV();
        String cvvHash = hash(cvv);

        String pinHash = null;
        if (card.getPinHash() != null) {
            pinHash = hash(card.getPinHash());
        }

        card.setNumberHash(numberHash);
        card.setLastFour(lastFour);
        card.setMaskedNumber(maskedNumber);
        card.setCvvHash(cvvHash);
        card.setPinHash(pinHash);
        card.setStatus(CardStatus.PENDING_ACTIVATION);
        card.setAvailableLimit(card.getCreditLimit() != null ? card.getCreditLimit() : BigDecimal.ZERO);
        card.setDailyUsed(BigDecimal.ZERO);
        card.setMonthlyUsed(BigDecimal.ZERO);
        card.setLastDailyReset(LocalDate.now());
        card.setLastMonthlyReset(LocalDate.now());
        card.setPinTries(0);
        card.setContactlessEnabled(card.getContactlessEnabled() != null ? card.getContactlessEnabled() : true);
        card.setOnlineEnabled(card.getOnlineEnabled() != null ? card.getOnlineEnabled() : true);
        card.setInternationalEnabled(card.getInternationalEnabled() != null ? card.getInternationalEnabled() : false);

        Card saved = cardRepository.save(card);

        eventPublisher.publishEvent(new CardCreatedEvent(this, saved.getId(), saved.getAccountId(), saved.getType().name()));

        log.info("Card created: {} for account {}", saved.getId(), saved.getAccountId());
        return saved;
    }

    private void validateCardCreation(Card card) {
        if (card.getAccountId() == null) {
            throw new InvalidCardStatusException("Account ID is required");
        }
        if (card.getType() == null) {
            throw new InvalidCardStatusException("Card type is required");
        }
        if (card.getHolderName() == null || card.getHolderName().trim().isEmpty()) {
            throw new InvalidCardStatusException("Holder name is required");
        }
        if (card.getExpiryDate() == null) {
            throw new InvalidCardStatusException("Expiry date is required");
        }
        if (card.getExpiryDate().isBefore(LocalDate.now())) {
            throw new InvalidCardStatusException("Expiry date cannot be in the past");
        }
    }

    private String generateCardNumber(CardType type) {
        String prefix = switch (type) {
            case CREDIT -> "4";
            case DEBIT -> "5";
            case MULTIPLE -> "6";
            case PREPAID -> "3";
        };
        StringBuilder sb = new StringBuilder(prefix);
        for (int i = 0; i < 15; i++) {
            sb.append((int) (Math.random() * 10));
        }
        return sb.toString();
    }

    private String generateCVV() {
        int cvv = 100 + (int) (Math.random() * 900);
        return String.valueOf(cvv);
    }

    private String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hash algorithm not available", e);
        }
    }

    @Transactional(readOnly = true)
    public Optional<Card> findById(UUID id) {
        return cardRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Card getById(UUID id) {
        return cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException("Card not found: " + id));
    }

    @Transactional(readOnly = true)
    public Optional<Card> findByNumberHash(String numberHash) {
        return cardRepository.findByNumberHash(numberHash);
    }

    @Transactional(readOnly = true)
    public List<Card> findByAccountId(UUID accountId) {
        return cardRepository.findByAccountId(accountId);
    }

    @Transactional(readOnly = true)
    public List<Card> findByAccountIdAndStatus(UUID accountId, CardStatus status) {
        return cardRepository.findByAccountIdAndStatus(accountId, status);
    }

    @Transactional
    public Card activateCard(UUID cardId) {
        Card card = getById(cardId);
        if (card.getStatus() != CardStatus.PENDING_ACTIVATION) {
            throw new InvalidCardStatusException("Card cannot be activated from status: " + card.getStatus());
        }
        card.setStatus(CardStatus.ACTIVE);
        return cardRepository.save(card);
    }

    @Transactional
    public Card blockCard(UUID cardId, String reason) {
        Card card = getById(cardId);
        if (card.getStatus() == CardStatus.CANCELLED || card.getStatus() == CardStatus.EXPIRED) {
            throw new InvalidCardStatusException("Cannot block card with status: " + card.getStatus());
        }
        card.setStatus(CardStatus.BLOCKED);
        return cardRepository.save(card);
    }

    @Transactional
    public Card unblockCard(UUID cardId) {
        Card card = getById(cardId);
        if (card.getStatus() != CardStatus.BLOCKED) {
            throw new InvalidCardStatusException("Only blocked cards can be unblocked");
        }
        card.setStatus(CardStatus.ACTIVE);
        return cardRepository.save(card);
    }

    @Transactional
    public Card cancelCard(UUID cardId, String reason) {
        Card card = getById(cardId);
        if (card.getStatus() == CardStatus.CANCELLED) {
            throw new InvalidCardStatusException("Card already cancelled");
        }
        card.setStatus(CardStatus.CANCELLED);
        card.setClosedAt(LocalDateTime.now());
        card.setClosedReason(reason);
        return cardRepository.save(card);
    }

    @Transactional
    public Card reportLost(UUID cardId) {
        Card card = getById(cardId);
        card.setStatus(CardStatus.LOST);
        return cardRepository.save(card);
    }

    @Transactional
    public Card reportStolen(UUID cardId) {
        Card card = getById(cardId);
        card.setStatus(CardStatus.STOLEN);
        return cardRepository.save(card);
    }

    @Transactional
    public Card updateLimits(UUID cardId, BigDecimal creditLimit, BigDecimal dailyLimit, BigDecimal monthlyLimit) {
        Card card = getById(cardId);
        if (creditLimit != null) {
            card.setCreditLimit(creditLimit);
            card.setAvailableLimit(creditLimit.subtract(card.getCreditLimit().subtract(card.getAvailableLimit())));
        }
        if (dailyLimit != null) {
            card.setDailyLimit(dailyLimit);
        }
        if (monthlyLimit != null) {
            card.setMonthlyLimit(monthlyLimit);
        }
        return cardRepository.save(card);
    }

    @Transactional
    public Card updateFeatures(UUID cardId, Boolean contactless, Boolean online, Boolean international) {
        Card card = getById(cardId);
        if (contactless != null) card.setContactlessEnabled(contactless);
        if (online != null) card.setOnlineEnabled(online);
        if (international != null) card.setInternationalEnabled(international);
        return cardRepository.save(card);
    }

    @Transactional
    public void resetDailyLimits() {
        List<Card> cards = cardRepository.findByStatus(CardStatus.ACTIVE);
        LocalDate today = LocalDate.now();
        for (Card card : cards) {
            if (!today.equals(card.getLastDailyReset())) {
                card.setDailyUsed(BigDecimal.ZERO);
                card.setLastDailyReset(today);
                cardRepository.save(card);
            }
        }
    }

    @Transactional
    public void resetMonthlyLimits() {
        List<Card> cards = cardRepository.findByStatus(CardStatus.ACTIVE);
        LocalDate today = LocalDate.now();
        for (Card card : cards) {
            if (!today.equals(card.getLastMonthlyReset()) || today.getDayOfMonth() == 1) {
                card.setMonthlyUsed(BigDecimal.ZERO);
                card.setLastMonthlyReset(today);
                cardRepository.save(card);
            }
        }
    }

    @Transactional
    public void checkExpiry() {
        List<Card> expiringCards = cardRepository.findByExpiryDateBefore(LocalDate.now());
        for (Card card : expiringCards) {
            if (card.getStatus() == CardStatus.ACTIVE) {
                card.setStatus(CardStatus.EXPIRED);
                cardRepository.save(card);
            }
        }
    }
}