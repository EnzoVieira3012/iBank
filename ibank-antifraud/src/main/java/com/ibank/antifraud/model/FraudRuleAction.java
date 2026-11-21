package com.ibank.antifraud.model;

public enum FraudRuleAction {
    ALLOW,
    ALERT,
    BLOCK,
    CHALLENGE,
    REVIEW,
    REQUIRE_2FA,
    REDUCE_LIMIT,
    NOTIFY_CUSTOMER
}