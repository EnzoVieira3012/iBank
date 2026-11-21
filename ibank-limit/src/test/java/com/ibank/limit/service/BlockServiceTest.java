package com.ibank.limit.service;

import com.ibank.limit.controller.dto.TransactionBlockRequest;
import com.ibank.limit.exception.BlockNotFoundException;
import com.ibank.limit.model.BlockScope;
import com.ibank.limit.model.BlockStatus;
import com.ibank.limit.model.BlockType;
import com.ibank.limit.model.TransactionBlock;
import com.ibank.limit.repository.TransactionBlockRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BlockServiceTest {

    @Mock
    private TransactionBlockRepository blockRepository;

    @InjectMocks
    private BlockService blockService;

    @Test
    void createBlock_customerScope_shouldCreate() {
        TransactionBlockRequest request = TransactionBlockRequest.builder()
                .customerId(UUID.randomUUID().toString())
                .blockType(BlockType.MANUAL)
                .scope(BlockScope.CUSTOMER)
                .reason("Manual review required")
                .startsAt(LocalDateTime.now())
                .createdBy(UUID.randomUUID().toString())
                .build();

        TransactionBlock saved = TransactionBlock.builder()
                .id(UUID.randomUUID())
                .customerId(UUID.fromString(request.getCustomerId()))
                .blockType(BlockType.MANUAL)
                .scope(BlockScope.CUSTOMER)
                .status(BlockStatus.ACTIVE)
                .reason(request.getReason())
                .startsAt(request.getStartsAt())
                .createdBy(UUID.fromString(request.getCreatedBy()))
                .build();

        when(blockRepository.save(any(TransactionBlock.class))).thenReturn(saved);

        TransactionBlock result = blockService.createBlock(request);

        assertNotNull(result);
        assertEquals(BlockStatus.ACTIVE, result.getStatus());
        assertEquals(request.getReason(), result.getReason());
    }

    @Test
    void createBlock_accountScope_shouldCreate() {
        TransactionBlockRequest request = TransactionBlockRequest.builder()
                .accountId(UUID.randomUUID().toString())
                .blockType(BlockType.FRAUD_RULE)
                .scope(BlockScope.ACCOUNT)
                .reason("Suspicious activity detected")
                .startsAt(LocalDateTime.now())
                .createdBy(UUID.randomUUID().toString())
                .build();

        TransactionBlock saved = TransactionBlock.builder()
                .id(UUID.randomUUID())
                .accountId(UUID.fromString(request.getAccountId()))
                .blockType(BlockType.FRAUD_RULE)
                .scope(BlockScope.ACCOUNT)
                .status(BlockStatus.ACTIVE)
                .reason(request.getReason())
                .startsAt(request.getStartsAt())
                .createdBy(UUID.fromString(request.getCreatedBy()))
                .build();

        when(blockRepository.save(any(TransactionBlock.class))).thenReturn(saved);

        TransactionBlock result = blockService.createBlock(request);

        assertNotNull(result);
        assertEquals(BlockStatus.ACTIVE, result.getStatus());
        assertEquals(request.getReason(), result.getReason());
    }

    @Test
    void createBlock_missingScopeId_shouldThrowException() {
        TransactionBlockRequest request = TransactionBlockRequest.builder()
                .blockType(BlockType.MANUAL)
                .scope(BlockScope.CUSTOMER)
                .reason("Test")
                .startsAt(LocalDateTime.now())
                .createdBy(UUID.randomUUID().toString())
                .build();

        assertThrows(IllegalArgumentException.class, () -> blockService.createBlock(request));
    }

    @Test
    void revokeBlock_shouldSetRevoked() {
        UUID blockId = UUID.randomUUID();
        TransactionBlock block = TransactionBlock.builder()
                .id(blockId)
                .customerId(UUID.randomUUID())
                .blockType(BlockType.MANUAL)
                .status(BlockStatus.ACTIVE)
                .reason("Test block")
                .startsAt(LocalDateTime.now().minusDays(1))
                .build();

        when(blockRepository.findById(blockId)).thenReturn(Optional.of(block));
        when(blockRepository.save(any(TransactionBlock.class))).thenAnswer(inv -> inv.getArgument(0));

        TransactionBlock result = blockService.revokeBlock(blockId, UUID.randomUUID(), "No longer needed");

        assertEquals(BlockStatus.REVOKED, result.getStatus());
        assertNotNull(result.getRevokedAt());
        assertEquals("No longer needed", result.getRevokeReason());
    }

    @Test
    void revokeBlock_alreadyRevoked_shouldThrowException() {
        UUID blockId = UUID.randomUUID();
        TransactionBlock block = TransactionBlock.builder()
                .id(blockId)
                .status(BlockStatus.REVOKED)
                .build();

        when(blockRepository.findById(blockId)).thenReturn(Optional.of(block));

        assertThrows(IllegalStateException.class, () ->
                blockService.revokeBlock(blockId, UUID.randomUUID(), "Test"));
    }

    @Test
    void getById_notFound_shouldThrowException() {
        UUID blockId = UUID.randomUUID();
        when(blockRepository.findById(blockId)).thenReturn(Optional.empty());

        assertThrows(BlockNotFoundException.class, () -> blockService.getById(blockId));
    }

    @Test
    void expireBlocks_shouldUpdateStatus() {
        TransactionBlock block = TransactionBlock.builder()
                .id(UUID.randomUUID())
                .status(BlockStatus.ACTIVE)
                .expiresAt(LocalDateTime.now().minusHours(1))
                .build();

        when(blockRepository.findExpiredActiveBlocks(any(LocalDateTime.class)))
                .thenReturn(List.of(block));
        when(blockRepository.save(any(TransactionBlock.class))).thenAnswer(inv -> inv.getArgument(0));

        blockService.expireBlocks();

        verify(blockRepository).save(argThat(b -> b.getStatus() == BlockStatus.EXPIRED));
    }
}