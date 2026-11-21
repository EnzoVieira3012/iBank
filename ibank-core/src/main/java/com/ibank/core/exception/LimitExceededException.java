package com.ibank.core.exception;

import java.math.BigDecimal;
import java.time.Instant;

public class LimitExceededException extends BusinessException {

    private final String limitType;
    private final BigDecimal currentUsage;
    private final BigDecimal limit;
    private final Instant resetAt;

    public LimitExceededException(String limitType, BigDecimal currentUsage, BigDecimal limit, Instant resetAt, int retryAfterSeconds) {
        super("LIMIT_EXCEEDED",
            limitType + " limit exceeded. Current: " + currentUsage + ", Limit: " + limit + ", Resets at: " + resetAt,
            org.springframework.http.HttpStatus.TOO_MANY_REQUESTS);
        this.limitType = limitType;
        this.currentUsage = currentUsage;
        this.limit = limit;
        this.resetAt = resetAt;
    }

    public String getLimitType() { return limitType; }
    public BigDecimal getCurrentUsage() { return currentUsage; }
    public BigDecimal getLimit() { return limit; }
    public Instant getResetAt() { return resetAt; }
    public int getRetryAfterSeconds() { return (int) java.time.Duration.between(Instant.now(), resetAt).getSeconds(); }
}