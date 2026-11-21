package com.ibank.limit.model;

public enum BlockType {
    VELOCITY,
    SUSPICIOUS_PATTERN,
    FRAUD_RULE,
    MANUAL,
    COMPLIANCE,
    OVERDUE_PAYMENT,
    KYC_EXPIRED,
    DOCUMENT_PENDING,
    CUSTOM
}