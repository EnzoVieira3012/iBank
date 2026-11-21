package com.ibank.card.service;

import com.ibank.card.event.CardTransactionAuthorizedEvent;
import com.ibank.card.exception.CardNotFoundException;
import com.ibank.card.exception.InsufficientLimitException;
import com.ibank.card.exception.InvalidCardStatusException;
import com.ibank.card.exception.TransactionNotFoundException;
import com.ibank.card.mapper.CardTransactionMapper;
import com.ibank.card.model.Card;
import com.ibank.card.model.CardStatus;
import com.ibank.card.model.CardTransaction;
import com.ibank.card.model.TransactionStatus;
import com.ibank.card.model.TransactionType;
import com.ibank.card.repository.CardRepository;
import com.ibank.card.repository.CardTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardTransactionService {

    private final CardTransactionRepository transactionRepository;
    private final CardRepository cardRepository;
    private final CardTransactionMapper transactionMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public CardTransaction authorize(CardTransaction transaction) {
        Card card = getValidCard(transaction.getCardId());
        validateTransaction(card, transaction);

        transaction.setCardId(card.getId());
        transaction.setStatus(TransactionStatus.AUTHORIZED);
        transaction.setProcessedAt(LocalDateTime.now());
        transaction.setAuthorizationCode(generateAuthCode());
        transaction.setNsu(generateNsu());

        if (transaction.getType() == TransactionType.PURCHASE || transaction.getType() == TransactionType.WITHDRAWAL) {
            updateCardLimits(card, transaction.getAmount());
        }

        CardTransaction saved = transactionRepository.save(transaction);

        eventPublisher.publishEvent(new CardTransactionAuthorizedEvent(
                this, saved.getId(), card.getId(), card.getAccountId(),
                transaction.getType(), transaction.getAmount(), transaction.getAuthorizationCode()
        ));

        log.info("Transaction authorized: {} for card {}", saved.getId(), card.getId());
        return saved;
    }

    private Card getValidCard(UUID cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException("Card not found: " + cardId));

        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new InvalidCardStatusException("Card is not active: " + card.getStatus());
        }

        if (card.getExpiryDate().isBefore(java.time.LocalDate.now())) {
            throw new InvalidCardStatusException("Card expired");
        }

        return card;
    }

    private void validateTransaction(Card card, CardTransaction transaction) {
        if (transaction.getIdempotencyKey() != null &&
                transactionRepository.existsByIdempotencyKey(transaction.getIdempotencyKey())) {
            throw new InvalidCardStatusException("Duplicate idempotency key");
        }

        if (transaction.getType() == TransactionType.PURCHASE || transaction.getType() == TransactionType.WITHDRAWAL) {
            BigDecimal available = card.getAvailableLimit();
            if (card.getDailyLimit() != null) {
                available = available.min(card.getDailyLimit().subtract(card.getDailyUsed()));
            }
            if (card.getMonthlyLimit() != null) {
                available = available.min(card.getMonthlyLimit().subtract(card.getMonthlyUsed()));
            }

            if (transaction.getAmount().compareTo(available) > 0) {
                throw new InsufficientLimitException("Insufficient limit. Available: " + available);
            }
        }

        if (!card.getOnlineEnabled() && isOnlineTransaction(transaction)) {
            throw new InvalidCardStatusException("Online transactions disabled");
        }

        if (!card.getContactlessEnabled() && isContactlessTransaction(transaction)) {
            throw new InvalidCardStatusException("Contactless transactions disabled");
        }

        if (!card.getInternationalEnabled() && isInternationalTransaction(transaction)) {
            throw new InvalidCardStatusException("International transactions disabled");
        }
    }

    private boolean isOnlineTransaction(CardTransaction transaction) {
        return transaction.getMerchantCategoryCode() != null &&
                (transaction.getMerchantCategoryCode().startsWith("5") || transaction.getMerchantCategoryCode().startsWith("6"));
    }

    private boolean isContactlessTransaction(CardTransaction transaction) {
        return transaction.getMetadata() != null && transaction.getMetadata().contains("contactless");
    }

    private boolean isInternationalTransaction(CardTransaction transaction) {
        return transaction.getMerchantCountry() != null &&
                !"BR".equals(transaction.getMerchantCountry());
    }

    private void updateCardLimits(Card card, BigDecimal amount) {
        card.setAvailableLimit(card.getAvailableLimit().subtract(amount));
        card.setDailyUsed(card.getDailyUsed().add(amount));
        card.setMonthlyUsed(card.getMonthlyUsed().add(amount));
        cardRepository.save(card);
    }

    @Transactional
    public CardTransaction capture(UUID transactionId) {
        CardTransaction transaction = getById(transactionId);
        if (transaction.getStatus() != TransactionStatus.AUTHORIZED) {
            throw new InvalidCardStatusException("Only authorized transactions can be captured");
        }
        transaction.setStatus(TransactionStatus.CAPTURED);
        return transactionRepository.save(transaction);
    }

    @Transactional
    public CardTransaction settle(UUID transactionId) {
        CardTransaction transaction = getById(transactionId);
        if (transaction.getStatus() != TransactionStatus.CAPTURED) {
            throw new InvalidCardStatusException("Only captured transactions can be settled");
        }
        transaction.setStatus(TransactionStatus.SETTLED);
        transaction.setSettledAt(LocalDateTime.now());
        return transactionRepository.save(transaction);
    }

    @Transactional
    public CardTransaction reverse(UUID transactionId, String reason) {
        CardTransaction transaction = getById(transactionId);
        if (transaction.getStatus() == TransactionStatus.REVERSED || transaction.getStatus() == TransactionStatus.REFUNDED) {
            throw new InvalidCardStatusException("Transaction already reversed/refunded");
        }
        if (transaction.getStatus() != TransactionStatus.AUTHORIZED && transaction.getStatus() != TransactionStatus.CAPTURED) {
            throw new InvalidCardStatusException("Cannot reverse transaction in status: " + transaction.getStatus());
        }

        Card card = cardRepository.findById(transaction.getCardId()).orElse(null);
        if (card != null && (transaction.getType() == TransactionType.PURCHASE || transaction.getType() == TransactionType.WITHDRAWAL)) {
            card.setAvailableLimit(card.getAvailableLimit().add(transaction.getAmount()));
            card.setDailyUsed(card.getDailyUsed().subtract(transaction.getAmount()));
            card.setMonthlyUsed(card.getMonthlyUsed().subtract(transaction.getAmount()));
            cardRepository.save(card);
        }

        transaction.setStatus(TransactionStatus.REVERSED);
        transaction.setReversalReason(reason);
        return transactionRepository.save(transaction);
    }

    @Transactional
    public CardTransaction refund(UUID transactionId, String reason) {
        CardTransaction original = getById(transactionId);
        if (original.getStatus() != TransactionStatus.SETTLED) {
            throw new InvalidCardStatusException("Only settled transactions can be refunded");
        }

        CardTransaction refund = new CardTransaction();
        refund.setCardId(original.getCardId());
        refund.setType(TransactionType.REFUND);
        refund.setStatus(TransactionStatus.AUTHORIZED);
        refund.setAmount(original.getAmount());
        refund.setCurrency(original.getCurrency());
        refund.setMerchantName(original.getMerchantName());
        refund.setDescription("Refund: " + reason);
        refund.setOriginalTransactionId(original.getId());
        refund.setAuthorizationCode(generateAuthCode());
        refund.setNsu(generateNsu());
        refund.setProcessedAt(LocalDateTime.now());

        Card card = cardRepository.findById(original.getCardId()).orElse(null);
        if (card != null) {
            card.setAvailableLimit(card.getAvailableLimit().add(original.getAmount()));
            cardRepository.save(card);
        }

        CardTransaction saved = transactionRepository.save(refund);
        original.setStatus(TransactionStatus.REFUNDED);
        transactionRepository.save(original);

        return saved;
    }

    @Transactional
    public CardTransaction decline(UUID transactionId, String reason) {
        CardTransaction transaction = getById(transactionId);
        transaction.setStatus(TransactionStatus.DECLINED);
        transaction.setReversalReason(reason);
        return transactionRepository.save(transaction);
    }

    @Transactional(readOnly = true)
    public CardTransaction getById(UUID id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found: " + id));
    }

    @Transactional(readOnly = true)
    public Optional<CardTransaction> findByIdempotencyKey(String key) {
        return transactionRepository.findByIdempotencyKey(key);
    }

    @Transactional(readOnly = true)
    public List<CardTransaction> findByCardId(UUID cardId) {
        return transactionRepository.findByCardId(cardId);
    }

    @Transactional(readOnly = true)
    public List<CardTransaction> findByCardIdAndStatus(UUID cardId, TransactionStatus status) {
        return transactionRepository.findByCardIdAndStatus(cardId, status);
    }

    private String generateAuthCode() {
        return String.format("%06d", (int) (Math.random() * 1000000));
    }

    private String generateNsu() {
        return String.format("%012d", System.currentTimeMillis() % 1000000000000L);
    }
}