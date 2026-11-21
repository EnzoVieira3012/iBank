package com.ibank.antifraud.controller;

import com.ibank.antifraud.controller.dto.DeviceFingerprintRequest;
import com.ibank.antifraud.controller.dto.DeviceFingerprintResponse;
import com.ibank.antifraud.mapper.DeviceFingerprintMapper;
import com.ibank.antifraud.model.DeviceFingerprint;
import com.ibank.antifraud.model.DeviceTrustLevel;
import com.ibank.antifraud.service.DeviceFingerprintService;
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
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/antifraud/devices")
@RequiredArgsConstructor
@Tag(name = "Device Fingerprinting", description = "Device registration, trust scoring, and device-based fraud prevention")
public class DeviceFingerprintController {

    private final DeviceFingerprintService deviceService;
    private final DeviceFingerprintMapper deviceMapper;

    @PostMapping("/register")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "Register or update a device fingerprint", description = "Creates or updates device fingerprint for a customer")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Device registered"),
        @ApiResponse(responseCode = "200", description = "Device updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<DeviceFingerprintResponse> registerDevice(@Valid @RequestBody DeviceFingerprintRequest request) {
        DeviceFingerprint device = deviceService.registerDevice(request);
        boolean isNew = device.getCreatedAt().equals(device.getUpdatedAt());
        return ResponseEntity.status(isNew ? HttpStatus.CREATED : HttpStatus.OK).body(deviceMapper.toResponse(device));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "Get device fingerprint by ID")
    public ResponseEntity<DeviceFingerprintResponse> getById(@PathVariable UUID id) {
        DeviceFingerprint device = deviceService.getById(id);
        return ResponseEntity.ok(deviceMapper.toResponse(device));
    }

    @GetMapping("/hash/{deviceHash}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "Get device fingerprint by hash")
    public ResponseEntity<DeviceFingerprintResponse> getByHash(@PathVariable String deviceHash) {
        Optional<DeviceFingerprint> device = deviceService.findByDeviceHash(deviceHash);
        return device.map(d -> ResponseEntity.ok(deviceMapper.toResponse(d)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "List devices for a customer")
    public ResponseEntity<List<DeviceFingerprintResponse>> listByCustomer(@PathVariable UUID customerId) {
        List<DeviceFingerprint> devices = deviceService.findByCustomerId(customerId);
        return ResponseEntity.ok(devices.stream().map(deviceMapper::toResponse).toList());
    }

    @GetMapping("/customer/{customerId}/trust/{trustLevel}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "List devices for a customer by trust level")
    public ResponseEntity<List<DeviceFingerprintResponse>> listByCustomerAndTrust(
            @PathVariable UUID customerId,
            @PathVariable DeviceTrustLevel trustLevel) {
        List<DeviceFingerprint> devices = deviceService.findByCustomerIdAndTrustLevel(customerId, trustLevel);
        return ResponseEntity.ok(devices.stream().map(deviceMapper::toResponse).toList());
    }

    @GetMapping("/trust/{trustLevel}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "List devices by trust level")
    public ResponseEntity<List<DeviceFingerprintResponse>> listByTrust(@PathVariable DeviceTrustLevel trustLevel) {
        List<DeviceFingerprint> devices = deviceService.findByTrustLevel(trustLevel);
        return ResponseEntity.ok(devices.stream().map(deviceMapper::toResponse).toList());
    }

    @GetMapping("/ip/{ipAddress}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    @Operation(summary = "List devices by IP address")
    public ResponseEntity<List<DeviceFingerprintResponse>> listByIp(@PathVariable String ipAddress) {
        List<DeviceFingerprint> devices = deviceService.findByIpAddress(ipAddress);
        return ResponseEntity.ok(devices.stream().map(deviceMapper::toResponse).toList());
    }

    @GetMapping("/customer/{customerId}/known/{deviceHash}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "Check if device is known for customer")
    public ResponseEntity<Boolean> isKnownDevice(
            @PathVariable UUID customerId,
            @PathVariable String deviceHash) {
        boolean known = deviceService.isKnownDevice(customerId, deviceHash);
        return ResponseEntity.ok(known);
    }

    @GetMapping("/customer/{customerId}/count")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'CLIENT')")
    @Operation(summary = "Count devices for a customer")
    public ResponseEntity<Long> countByCustomer(@PathVariable UUID customerId) {
        long count = deviceService.countByCustomerId(customerId);
        return ResponseEntity.ok(count);
    }

    @PatchMapping("/{id}/trust")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Update device trust level")
    public ResponseEntity<DeviceFingerprintResponse> updateTrustLevel(
            @PathVariable UUID id,
            @RequestParam DeviceTrustLevel trustLevel) {
        DeviceFingerprint device = deviceService.updateTrustLevel(id, trustLevel);
        return ResponseEntity.ok(deviceMapper.toResponse(device));
    }

    @PostMapping("/{id}/block")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Block a device")
    public ResponseEntity<Void> blockDevice(@PathVariable UUID id) {
        deviceService.blockDevice(id);
        return ResponseEntity.ok().build();
    }
}