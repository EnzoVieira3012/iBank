package com.ibank.card.service;

import com.ibank.card.model.Card;
import com.ibank.card.model.CardStatus;
import com.ibank.card.model.CardType;
import com.ibank.card.repository.CardRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @InjectMocks
    private CardService cardService;

    @Test
    void createCard_shouldGenerateCardNumberAndHash() {
        Card card = Card.builder()
                .accountId(UUID.randomUUID())
                .type(CardType.CREDIT)
                .holderName("John Doe")
                .expiryDate(LocalDate.now().plusYears(4))
                .creditLimit(new BigDecimal("5000.00"))
                .build();

        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));

        Card created = cardService.createCard(card);

        assertNotNull(created.getNumberHash());
        assertNotNull(created.getLastFour());
        assertNotNull(created.getMaskedNumber());
        assertNotNull(created.getCvvHash());
        assertEquals(CardStatus.PENDING_ACTIVATION, created.getStatus());
        assertEquals(new BigDecimal("5000.00"), created.getAvailableLimit());
    }

    @Test
    void activateCard_shouldChangeStatusToActive() {
        Card card = Card.builder()
                .id(UUID.randomUUID())
                .status(CardStatus.PENDING_ACTIVATION)
                .build();

        when(cardRepository.findById(card.getId())).thenReturn(Optional.of(card));
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));

        Card activated = cardService.activateCard(card.getId());

        assertEquals(CardStatus.ACTIVE, activated.getStatus());
    }

    @Test
    void blockCard_shouldChangeStatusToBlocked() {
        Card card = Card.builder()
                .id(UUID.randomUUID())
                .status(CardStatus.ACTIVE)
                .build();

        when(cardRepository.findById(card.getId())).thenReturn(Optional.of(card));
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));

        Card blocked = cardService.blockCard(card.getId(), "Suspicious activity");

        assertEquals(CardStatus.BLOCKED, blocked.getStatus());
    }
}