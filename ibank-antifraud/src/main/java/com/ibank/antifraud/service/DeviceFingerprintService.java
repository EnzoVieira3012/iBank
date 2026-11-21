package com.ibank.antifraud.service;

import com.ibank.antifraud.controller.dto.DeviceFingerprintRequest;
import com.ibank.antifraud.event.DeviceRegisteredEvent;
import com.ibank.antifraud.exception.DeviceNotFoundException;
import com.ibank.antifraud.mapper.DeviceFingerprintMapper;
import com.ibank.antifraud.model.DeviceFingerprint;
import com.ibank.antifraud.model.DeviceTrustLevel;
import com.ibank.antifraud.repository.DeviceFingerprintRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceFingerprintService {

    private final DeviceFingerprintRepository deviceRepository;
    private final DeviceFingerprintMapper deviceMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public DeviceFingerprint registerDevice(DeviceFingerprintRequest request) {
        String deviceHash = generateDeviceHash(request);
        Optional<DeviceFingerprint> existing = deviceRepository.findByDeviceHash(deviceHash);

        DeviceFingerprint device;
        boolean isNewDevice = false;

        if (existing.isPresent()) {
            device = existing.get();
            device.setLastSeenAt(LocalDateTime.now());
            device.setSeenCount(device.getSeenCount() + 1);
            updateTrustLevel(device);
        } else {
            isNewDevice = true;
            device = DeviceFingerprint.builder()
                    .customerId(UUID.fromString(request.getCustomerId()))
                    .deviceHash(deviceHash)
                    .deviceId(request.getDeviceId())
                    .userAgent(request.getUserAgent())
                    .ipAddress(request.getIpAddress())
                    .country(request.getCountry())
                    .city(request.getCity())
                    .os(request.getOs())
                    .browser(request.getBrowser())
                    .screenResolution(request.getScreenResolution())
                    .timezone(request.getTimezone())
                    .language(request.getLanguage())
                    .canvasFingerprint(request.getCanvasFingerprint())
                    .webglFingerprint(request.getWebglFingerprint())
                    .fontsHash(request.getFontsHash())
                    .pluginsHash(request.getPluginsHash())
                    .batteryInfo(request.getBatteryInfo())
                    .trustLevel(DeviceTrustLevel.NEW)
                    .trustScore(50)
                    .firstSeenAt(LocalDateTime.now())
                    .lastSeenAt(LocalDateTime.now())
                    .seenCount(1)
                    .metadata(request.getMetadata())
                    .build();
        }

        DeviceFingerprint saved = deviceRepository.save(device);

        eventPublisher.publishEvent(new DeviceRegisteredEvent(
                this, saved.getId(), saved.getCustomerId(), saved.getDeviceHash(),
                saved.getTrustLevel(), isNewDevice
        ));

        log.info("Device {} for customer {}: {}", isNewDevice ? "registered" : "updated", saved.getCustomerId(), saved.getId());
        return saved;
    }

    private String generateDeviceHash(DeviceFingerprintRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append(request.getDeviceId() != null ? request.getDeviceId() : "");
        sb.append(request.getUserAgent() != null ? request.getUserAgent() : "");
        sb.append(request.getScreenResolution() != null ? request.getScreenResolution() : "");
        sb.append(request.getTimezone() != null ? request.getTimezone() : "");
        sb.append(request.getLanguage() != null ? request.getLanguage() : "");
        sb.append(request.getCanvasFingerprint() != null ? request.getCanvasFingerprint() : "");
        sb.append(request.getWebglFingerprint() != null ? request.getWebglFingerprint() : "");
        sb.append(request.getFontsHash() != null ? request.getFontsHash() : "");
        sb.append(request.getPluginsHash() != null ? request.getPluginsHash() : "");
        sb.append(request.getBatteryInfo() != null ? request.getBatteryInfo() : "");

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
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

    private void updateTrustLevel(DeviceFingerprint device) {
        if (device.getSeenCount() >= 10 && device.getTrustScore() >= 70) {
            device.setTrustLevel(DeviceTrustLevel.TRUSTED);
        } else if (device.getSeenCount() >= 5 && device.getTrustScore() >= 50) {
            device.setTrustLevel(DeviceTrustLevel.KNOWN);
        } else if (device.getTrustScore() < 30) {
            device.setTrustLevel(DeviceTrustLevel.SUSPICIOUS);
        } else {
            device.setTrustLevel(DeviceTrustLevel.KNOWN);
        }
    }

    @Transactional(readOnly = true)
    public DeviceFingerprint getById(UUID id) {
        return deviceRepository.findById(id)
                .orElseThrow(() -> new DeviceNotFoundException("Device not found: " + id));
    }

    @Transactional(readOnly = true)
    public Optional<DeviceFingerprint> findByDeviceHash(String deviceHash) {
        return deviceRepository.findByDeviceHash(deviceHash);
    }

    @Transactional(readOnly = true)
    public List<DeviceFingerprint> findByCustomerId(UUID customerId) {
        return deviceRepository.findByCustomerId(customerId);
    }

    @Transactional(readOnly = true)
    public List<DeviceFingerprint> findByCustomerIdAndTrustLevel(UUID customerId, DeviceTrustLevel trustLevel) {
        return deviceRepository.findByCustomerIdAndTrustLevel(customerId, trustLevel);
    }

    @Transactional(readOnly = true)
    public List<DeviceFingerprint> findByTrustLevel(DeviceTrustLevel trustLevel) {
        return deviceRepository.findByTrustLevel(trustLevel);
    }

    @Transactional(readOnly = true)
    public List<DeviceFingerprint> findByIpAddress(String ipAddress) {
        return deviceRepository.findByIpAddress(ipAddress);
    }

    @Transactional(readOnly = true)
    public boolean isKnownDevice(UUID customerId, String deviceHash) {
        return deviceRepository.existsByCustomerIdAndDeviceHash(customerId, deviceHash);
    }

    @Transactional(readOnly = true)
    public long countByCustomerId(UUID customerId) {
        return deviceRepository.countByCustomerId(customerId);
    }

    @Transactional
    public DeviceFingerprint updateTrustLevel(UUID deviceId, DeviceTrustLevel trustLevel) {
        DeviceFingerprint device = getById(deviceId);
        device.setTrustLevel(trustLevel);
        return deviceRepository.save(device);
    }

    @Transactional
    public void blockDevice(UUID deviceId) {
        DeviceFingerprint device = getById(deviceId);
        device.setTrustLevel(DeviceTrustLevel.BLOCKED);
        device.setTrustScore(0);
        deviceRepository.save(device);
    }
}