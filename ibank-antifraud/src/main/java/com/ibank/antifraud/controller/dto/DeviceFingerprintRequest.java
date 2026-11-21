package com.ibank.antifraud.controller.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.UUID;

@Data
public class DeviceFingerprintRequest {

    @NotNull(message = "Customer ID is required")
    private String customerId;

    @Size(max = 100, message = "Device ID must not exceed 100 characters")
    private String deviceId;

    @Size(max = 500, message = "User agent must not exceed 500 characters")
    private String userAgent;

    @Size(max = 45, message = "IP address must not exceed 45 characters")
    private String ipAddress;

    @Size(min = 2, max = 2, message = "Country must be 2 characters")
    private String country;

    @Size(max = 100, message = "City must not exceed 100 characters")
    private String city;

    @Size(max = 50, message = "OS must not exceed 50 characters")
    private String os;

    @Size(max = 50, message = "Browser must not exceed 50 characters")
    private String browser;

    @Size(max = 20, message = "Screen resolution must not exceed 20 characters")
    private String screenResolution;

    @Size(max = 50, message = "Timezone must not exceed 50 characters")
    private String timezone;

    @Size(min = 2, max = 10, message = "Language must be 2-10 characters")
    private String language;

    @Size(max = 100, message = "Canvas fingerprint must not exceed 100 characters")
    private String canvasFingerprint;

    @Size(max = 100, message = "WebGL fingerprint must not exceed 100 characters")
    private String webglFingerprint;

    @Size(max = 64, message = "Fonts hash must not exceed 64 characters")
    private String fontsHash;

    @Size(max = 64, message = "Plugins hash must not exceed 64 characters")
    private String pluginsHash;

    @Size(max = 50, message = "Battery info must not exceed 50 characters")
    private String batteryInfo;

    private String metadata;
}