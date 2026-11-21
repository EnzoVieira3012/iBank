package com.ibank.antifraud.model;

import com.ibank.core.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "device_fingerprints", indexes = {
    @Index(name = "idx_device_customer", columnList = "customer_id"),
    @Index(name = "idx_device_hash", columnList = "device_hash"),
    @Index(name = "idx_device_trust", columnList = "trust_level"),
    @Index(name = "idx_device_last_seen", columnList = "last_seen_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceFingerprint extends BaseEntity {

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "device_hash", nullable = false, length = 64)
    private String deviceHash;

    @Column(name = "device_id", length = 100)
    private String deviceId;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "country", length = 2)
    private String country;

    @Column(name = "city", length = 100)
    private String city

    @Column(name = "os", length = 50)
    private String os;

    @Column(name = "browser", length = 50)
    private String browser;

    @Column(name = "screen_resolution", length = 20)
    private String screenResolution;

    @Column(name = "timezone", length = 50)
    private String timezone;

    @Column(name = "language", length = 10)
    private String language;

    @Column(name = "canvas_fingerprint", length = 100)
    private String canvasFingerprint;

    @Column(name = "webgl_fingerprint", length = 100)
    private String webglFingerprint;

    @Column(name = "fonts_hash", length = 64)
    private String fontsHash;

    @Column(name = "plugins_hash", length = 64)
    private String pluginsHash;

    @Column(name = "battery_info", length = 50)
    private String batteryInfo;

    @Enumerated(EnumType.STRING)
    @Column(name = "trust_level", nullable = false, length = 20)
    private DeviceTrustLevel trustLevel;

    @Column(name = "trust_score")
    private Integer trustScore;

    @Column(name = "first_seen_at", nullable = false)
    private LocalDateTime firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private LocalDateTime lastSeenAt;

    @Column(name = "seen_count", nullable = false)
    private Integer seenCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @Version
    private Long version;
}