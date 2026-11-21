package com.ibank.antifraud.exception;

import com.ibank.core.exception.BusinessException;

public class DeviceNotFoundException extends BusinessException {
    public DeviceNotFoundException(String message) {
        super("DEVICE_NOT_FOUND", message);
    }
}