package com.ibank.antifraud.model;

public enum CaseStatus {
    OPEN,
    INVESTIGATING,
    PENDING_INFO,
    ESCALATED,
    RESOLVED_FRAUD,
    RESOLVED_LEGITIMATE,
    CLOSED,
    SAR_FILED
}