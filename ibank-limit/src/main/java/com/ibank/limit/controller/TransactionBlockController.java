package com.ibank.limit.controller;

import com.ibank.limit.controller.dto.TransactionBlockRequest;
import com.ibank.limit.controller.dto.TransactionBlockResponse;
import com.ibank.limit.mapper.TransactionBlockMapper;
import com.ibank.limit.model.BlockScope;
import com.ibank.limit.model.BlockStatus;
import com.ibank.limit.model.BlockType;
import com.ibank.limit.model.TransactionBlock;
import com.ibank.limit.service.BlockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/blocks")
@RequiredArgsConstructor
@Tag(name = "Transaction Blocks", description = "Transaction block management: velocity blocks, fraud blocks, manual blocks, compliance blocks")
public class TransactionBlockController {

    private final BlockService blockService;
    private final TransactionBlockMapper blockMapper;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    @Operation(summary = "Create a transaction block")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Block created"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "409", description = "Conflicting block exists")
    })
    public ResponseEntity<TransactionBlockResponse> create(@Valid @RequestBody TransactionBlockRequest request) {
        TransactionBlock block = blockService.createBlock(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(blockMapper.toResponse(block));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "Get block by ID")
    public ResponseEntity<TransactionBlockResponse> getById(@PathVariable UUID id) {
        TransactionBlock block = blockService.getById(id);
        return ResponseEntity.ok(blockMapper.toResponse(block));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "List all blocks")
    public ResponseEntity<List<TransactionBlockResponse>> listAll() {
        List<TransactionBlock> blocks = blockService.findAll();
        return ResponseEntity.ok(blocks.stream().map(blockMapper::toResponse).toList());
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "List blocks for a customer")
    public ResponseEntity<List<TransactionBlockResponse>> listByCustomer(@PathVariable UUID customerId) {
        List<TransactionBlock> blocks = blockService.findByCustomerId(customerId);
        return ResponseEntity.ok(blocks.stream().map(blockMapper::toResponse).toList());
    }

    @GetMapping("/account/{accountId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "List blocks for an account")
    public ResponseEntity<List<TransactionBlockResponse>> listByAccount(@PathVariable UUID accountId) {
        List<TransactionBlock> blocks = blockService.findByAccountId(accountId);
        return ResponseEntity.ok(blocks.stream().map(blockMapper::toResponse).toList());
    }

    @GetMapping("/card/{cardId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "List blocks for a card")
    public ResponseEntity<List<TransactionBlockResponse>> listByCard(@PathVariable UUID cardId) {
        List<TransactionBlock> blocks = blockService.findByCardId(cardId);
        return ResponseEntity.ok(blocks.stream().map(blockMapper::toResponse).toList());
    }

    @GetMapping("/type/{blockType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "List blocks by type")
    public ResponseEntity<List<TransactionBlockResponse>> listByType(@PathVariable BlockType blockType) {
        List<TransactionBlock> blocks = blockService.findByBlockType(blockType);
        return ResponseEntity.ok(blocks.stream().map(blockMapper::toResponse).toList());
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "List blocks by status")
    public ResponseEntity<List<TransactionBlockResponse>> listByStatus(@PathVariable BlockStatus status) {
        List<TransactionBlock> blocks = blockService.findByStatus(status);
        return ResponseEntity.ok(blocks.stream().map(blockMapper::toResponse).toList());
    }

    @GetMapping("/customer/{customerId}/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "List active blocks for a customer")
    public ResponseEntity<List<TransactionBlockResponse>> listActiveForCustomer(
            @PathVariable UUID customerId,
            @RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) UUID cardId) {
        List<TransactionBlock> blocks = blockService.findActiveBlocksForCustomer(customerId, accountId, cardId);
        return ResponseEntity.ok(blocks.stream().map(blockMapper::toResponse).toList());
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Approve a pending block")
    public ResponseEntity<TransactionBlockResponse> approve(
            @PathVariable UUID id,
            @RequestParam UUID approvedBy) {
        TransactionBlock block = blockService.approveBlock(id, approvedBy);
        return ResponseEntity.ok(blockMapper.toResponse(block));
    }

    @PostMapping("/{id}/revoke")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Revoke a block")
    public ResponseEntity<TransactionBlockResponse> revoke(
            @PathVariable UUID id,
            @RequestParam UUID revokedBy,
            @RequestParam String reason) {
        TransactionBlock block = blockService.revokeBlock(id, revokedBy, reason);
        return ResponseEntity.ok(blockMapper.toResponse(block));
    }

    @PostMapping("/expire")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    @Operation(summary = "Expire blocks that have passed their expiry date")
    public ResponseEntity<Void> expireBlocks() {
        blockService.expireBlocks();
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Update a block")
    public ResponseEntity<TransactionBlockResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody TransactionBlockRequest request) {
        TransactionBlock block = blockService.updateBlock(id, request);
        return ResponseEntity.ok(blockMapper.toResponse(block));
    }
}