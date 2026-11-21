package com.ibank.limit.service;

import com.ibank.limit.controller.dto.TransactionBlockRequest;
import com.ibank.limit.controller.dto.TransactionBlockResponse;
import com.ibank.limit.event.BlockCreatedEvent;
import com.ibank.limit.event.BlockRevokedEvent;
import com.ibank.limit.exception.BlockNotFoundException;
import com.ibank.limit.mapper.TransactionBlockMapper;
import com.ibank.limit.model.BlockScope;
import com.ibank.limit.model.BlockStatus;
import com.ibank.limit.model.BlockType;
import com.ibank.limit.model.TransactionBlock;
import com.ibank.limit.repository.TransactionBlockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlockService {

    private final TransactionBlockRepository blockRepository;
    private final TransactionBlockMapper blockMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public TransactionBlock createBlock(TransactionBlockRequest request) {
        TransactionBlock block = blockMapper.toEntity(request);
        block.setCustomerId(request.getCustomerId() != null ? UUID.fromString(request.getCustomerId()) : null);
        block.setAccountId(request.getAccountId() != null ? UUID.fromString(request.getAccountId()) : null);
        block.setCardId(request.getCardId() != null ? UUID.fromString(request.getCardId()) : null);
        block.setCreatedBy(UUID.fromString(request.getCreatedBy()));
        block.setStatus(BlockStatus.ACTIVE);

        validateBlock(block);

        TransactionBlock saved = blockRepository.save(block);

        eventPublisher.publishEvent(new BlockCreatedEvent(
                this, saved.getId(), saved.getCustomerId(), saved.getAccountId(), saved.getCardId(),
                saved.getBlockType(), saved.getScope(), saved.getStatus(), saved.getReason(),
                saved.getStartsAt(), saved.getExpiresAt(), saved.getCreatedBy()
        ));

        log.info("Transaction block created: {} for customer {}", saved.getId(), saved.getCustomerId());
        return saved;
    }

    private void validateBlock(TransactionBlock block) {
        if (block.getCustomerId() == null && block.getAccountId() == null && block.getCardId() == null) {
            throw new IllegalArgumentException("At least one of customerId, accountId, or cardId must be provided");
        }

        if (block.getScope() == BlockScope.CUSTOMER && block.getCustomerId() == null) {
            throw new IllegalArgumentException("Customer ID required for CUSTOMER scope");
        }
        if (block.getScope() == BlockScope.ACCOUNT && block.getAccountId() == null) {
            throw new IllegalArgumentException("Account ID required for ACCOUNT scope");
        }
        if (block.getScope() == BlockScope.CARD && block.getCardId() == null) {
            throw new IllegalArgumentException("Card ID required for CARD scope");
        }

        if (block.getExpiresAt() != null && block.getExpiresAt().isBefore(block.getStartsAt())) {
            throw new IllegalArgumentException("ExpiresAt must be after StartsAt");
        }
    }

    @Transactional(readOnly = true)
    public TransactionBlock getById(UUID id) {
        return blockRepository.findById(id)
                .orElseThrow(() -> new BlockNotFoundException("Block not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<TransactionBlock> findAll() {
        return blockRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<TransactionBlock> findByCustomerId(UUID customerId) {
        return blockRepository.findByCustomerId(customerId);
    }

    @Transactional(readOnly = true)
    public List<TransactionBlock> findByAccountId(UUID accountId) {
        return blockRepository.findByAccountId(accountId);
    }

    @Transactional(readOnly = true)
    public List<TransactionBlock> findByCardId(UUID cardId) {
        return blockRepository.findByCardId(cardId);
    }

    @Transactional(readOnly = true)
    public List<TransactionBlock> findByBlockType(BlockType blockType) {
        return blockRepository.findByBlockType(blockType);
    }

    @Transactional(readOnly = true)
    public List<TransactionBlock> findByStatus(BlockStatus status) {
        return blockRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<TransactionBlock> findActiveBlocksForCustomer(UUID customerId, UUID accountId, UUID cardId) {
        return blockRepository.findActiveBlocksForCustomer(customerId, LocalDateTime.now(), accountId, cardId);
    }

    @Transactional
    public TransactionBlock approveBlock(UUID blockId, UUID approvedBy) {
        TransactionBlock block = getById(blockId);
        if (block.getStatus() != BlockStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Block is not pending approval");
        }
        block.setStatus(BlockStatus.ACTIVE);
        block.setApprovedBy(approvedBy);
        block.setApprovedAt(LocalDateTime.now());
        return blockRepository.save(block);
    }

    @Transactional
    public TransactionBlock revokeBlock(UUID blockId, UUID revokedBy, String reason) {
        TransactionBlock block = getById(blockId);
        if (block.getStatus() == BlockStatus.REVOKED || block.getStatus() == BlockStatus.EXPIRED) {
            throw new IllegalStateException("Block already revoked or expired");
        }
        block.setStatus(BlockStatus.REVOKED);
        block.setRevokedBy(revokedBy);
        block.setRevokedAt(LocalDateTime.now());
        block.setRevokeReason(reason);

        TransactionBlock saved = blockRepository.save(block);

        eventPublisher.publishEvent(new BlockRevokedEvent(
                this, saved.getId(), saved.getCustomerId(), saved.getAccountId(), saved.getCardId(),
                saved.getBlockType(), reason, revokedBy
        ));

        log.info("Transaction block revoked: {} by {}", blockId, revokedBy);
        return saved;
    }

    @Transactional
    public void expireBlocks() {
        List<TransactionBlock> expired = blockRepository.findExpiredActiveBlocks(LocalDateTime.now());
        for (TransactionBlock block : expired) {
            block.setStatus(BlockStatus.EXPIRED);
            blockRepository.save(block);
        }
        if (!expired.isEmpty()) {
            log.info("Expired {} transaction blocks", expired.size());
        }
    }

    @Transactional
    public TransactionBlock updateBlock(UUID blockId, TransactionBlockRequest request) {
        TransactionBlock block = getById(blockId);
        if (block.getStatus() == BlockStatus.REVOKED || block.getStatus() == BlockStatus.EXPIRED) {
            throw new IllegalStateException("Cannot update revoked or expired block");
        }

        block.setReason(request.getReason());
        block.setExpiresAt(request.getExpiresAt());
        block.setConditions(request.getConditions());
        block.setAffectedChannels(request.getAffectedChannels());
        block.setAffectedTransactionTypes(request.getAffectedTransactionTypes());
        block.setMetadata(request.getMetadata());

        return blockRepository.save(block);
    }
}