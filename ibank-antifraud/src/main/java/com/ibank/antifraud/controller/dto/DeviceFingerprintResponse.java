package com.ibank.antifraud.controller.dto;

import com.ibank.antifraud.model.DeviceTrustLevel;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class DeviceFingerprintResponse {

    private UUID id;
    private UUID customerId;
    private String deviceHash;
    private String deviceId
    private String userAgent;
    private String ipAddress;
    private String country;
    private String city;
    private String os;
    private String browser;
    private String screenResolution;
    private String timezone;
    private String language;
    private String canvasFingerprint;
    private String webglFingerprint;
    private String fontsHash;
    private String pluginsHash;
    private String batteryInfo;
    private DeviceTrustLevel trustLevel;
    private Integer trustScore;
    private LocalDateTime firstSeenAt;
    private LocalDateTime lastSeenAt;
    private Integer seenCount;
    private String metadata;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}