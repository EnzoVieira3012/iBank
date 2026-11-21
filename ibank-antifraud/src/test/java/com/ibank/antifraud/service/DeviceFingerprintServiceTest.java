package com.ibank.antifraud.service;

import com.ibank.antifraud.controller.dto.DeviceFingerprintRequest;
import com.ibank.antifraud.exception.DeviceNotFoundException;
import com.ibank.antifraud.model.DeviceFingerprint;
import com.ibank.antifraud.model.DeviceTrustLevel;
import com.ibank.antifraud.repository.DeviceFingerprintRepository;
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
class DeviceFingerprintServiceTest {

    @Mock
    private DeviceFingerprintRepository deviceRepository;

    @InjectMocks
    private DeviceFingerprintService deviceService;

    @Test
    void registerDevice_newDevice_shouldCreateWithNewTrustLevel() {
        DeviceFingerprintRequest request = DeviceFingerprintRequest.builder()
                .customerId(UUID.randomUUID().toString())
                .deviceId("device-123")
                .userAgent("Mozilla/5.0")
                .ipAddress("192.168.1.1")
                .country("BR")
                .city("Sao Paulo")
                .os("Windows 10")
                .browser("Chrome")
                .screenResolution("1920x1080")
                .timezone("America/Sao_Paulo")
                .language("pt-BR")
                .build();

        when(deviceRepository.findByDeviceHash(anyString())).thenReturn(Optional.empty());
        when(deviceRepository.save(any(DeviceFingerprint.class))).thenAnswer(inv -> {
            DeviceFingerprint d = inv.getArgument(0);
            d.setId(UUID.randomUUID());
            return d;
        });

        DeviceFingerprint result = deviceService.registerDevice(request);

        assertNotNull(result);
        assertEquals(DeviceTrustLevel.NEW, result.getTrustLevel());
        assertEquals(1, result.getSeenCount());
        assertEquals(50, result.getTrustScore());
    }

    @Test
    void registerDevice_existingDevice_shouldUpdateAndIncrementCount() {
        UUID customerId = UUID.randomUUID();
        DeviceFingerprint existing = DeviceFingerprint.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
                .deviceHash("hash123")
                .trustLevel(DeviceTrustLevel.KNOWN)
                .trustScore(60)
                .seenCount(3)
                .firstSeenAt(LocalDateTime.now().minusDays(10))
                .lastSeenAt(LocalDateTime.now().minusDays(1))
                .build();

        DeviceFingerprintRequest request = DeviceFingerprintRequest.builder()
                .customerId(customerId.toString())
                .deviceId("device-123")
                .userAgent("Mozilla/5.0")
                .ipAddress("192.168.1.1")
                .country("BR")
                .city("Sao Paulo")
                .build();

        when(deviceRepository.findByDeviceHash(anyString())).thenReturn(Optional.of(existing));
        when(deviceRepository.save(any(DeviceFingerprint.class))).thenAnswer(inv -> inv.getArgument(0));

        DeviceFingerprint result = deviceService.registerDevice(request);

        assertEquals(4, result.getSeenCount());
        assertNotNull(result.getLastSeenAt());
    }

    @Test
    void registerDevice_seenCountReachesThreshold_shouldUpgradeTrustLevel() {
        DeviceFingerprint existing = DeviceFingerprint.builder()
                .id(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .deviceHash("hash123")
                .trustLevel(DeviceTrustLevel.KNOWN)
                .trustScore(75)
                .seenCount(9)
                .firstSeenAt(LocalDateTime.now().minusDays(30))
                .lastSeenAt(LocalDateTime.now().minusDays(1))
                .build();

        DeviceFingerprintRequest request = DeviceFingerprintRequest.builder()
                .customerId(existing.getCustomerId().toString())
                .deviceId("device-123")
                .userAgent("Mozilla/5.0")
                .build();

        when(deviceRepository.findByDeviceHash(anyString())).thenReturn(Optional.of(existing));
        when(deviceRepository.save(any(DeviceFingerprint.class))).thenAnswer(inv -> inv.getArgument(0));

        DeviceFingerprint result = deviceService.registerDevice(request);

        assertEquals(10, result.getSeenCount());
        assertEquals(DeviceTrustLevel.TRUSTED, result.getTrustLevel());
    }

    @Test
    void isKnownDevice_shouldReturnTrue() {
        UUID customerId = UUID.randomUUID();
        String deviceHash = "hash123";

        when(deviceRepository.existsByCustomerIdAndDeviceHash(customerId, deviceHash)).thenReturn(true);

        boolean known = deviceService.isKnownDevice(customerId, deviceHash);

        assertTrue(known);
    }

    @Test
    void getById_notFound_shouldThrowException() {
        UUID deviceId = UUID.randomUUID();
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.empty());

        assertThrows(DeviceNotFoundException.class, () -> deviceService.getById(deviceId));
    }

    @Test
    void blockDevice_shouldSetBlockedTrustLevel() {
        UUID deviceId = UUID.randomUUID();
        DeviceFingerprint device = DeviceFingerprint.builder()
                .id(deviceId)
                .trustLevel(DeviceTrustLevel.SUSPICIOUS)
                .trustScore(20)
                .build();

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));
        when(deviceRepository.save(any(DeviceFingerprint.class))).thenAnswer(inv -> inv.getArgument(0));

        deviceService.blockDevice(deviceId);

        verify(deviceRepository).save(argThat(d ->
                d.getTrustLevel() == DeviceTrustLevel.BLOCKED && d.getTrustScore() == 0));
    }
}