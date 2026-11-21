package com.ibank.antifraud.model;

public enum AlertStatus {
    OPEN,
    IN_REVIEW,
    ESCALATED,
    FALSE_POSITIVE,
    CONFIRMED_FRAUD,
    CLOSED,
    AUTO_CLOSED
}