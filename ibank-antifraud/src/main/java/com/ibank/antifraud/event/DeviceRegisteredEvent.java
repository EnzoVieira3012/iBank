package com.ibank.antifraud.event;

import com.ibank.antifraud.model.DeviceTrustLevel;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class DeviceRegisteredEvent extends ApplicationEvent {
    private final UUID deviceId;
    private final UUID customerId;
    private final String deviceHash;
    private final DeviceTrustLevel trustLevel;
    private final boolean isNewDevice;

    public DeviceRegisteredEvent(Object source, UUID deviceId, UUID customerId, String deviceHash,
                                 DeviceTrustLevel trustLevel, boolean isNewDevice) {
        super(source);
        this.deviceId = deviceId;
        this.customerId = customerId;
        this.deviceHash = deviceHash;
        this.trustLevel = trustLevel;
        this.isNewDevice = isNewDevice;
    }
}