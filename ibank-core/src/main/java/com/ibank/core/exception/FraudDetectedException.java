package com.ibank.core.exception;

public class FraudDetectedException extends BusinessException {

    private final String alertId;
    private final int riskScore;

    public FraudDetectedException(String alertId, int riskScore, String message) {
        super("FRAUD_DETECTED", message, org.springframework.http.HttpStatus.FORBIDDEN);
        this.alertId = alertId;
        this.riskScore = riskScore;
    }

    public String getAlertId() { return alertId; }
    public int getRiskScore() { return riskScore; }
}