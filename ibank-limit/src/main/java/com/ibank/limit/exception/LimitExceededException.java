package com.ibank.limit.exception;

import com.ibank.core.exception.BusinessException;
import com.ibank.limit.model.LimitType;
import lombok.Getter;

@Getter
public class LimitExceededException extends BusinessException {
    private final LimitType limitType;
    private final String limitScope;
    private final String currentUsage;
    private final String limitValue;

    public LimitExceededException(String message, LimitType limitType, String limitScope,
                                  String currentUsage, String limitValue) {
        super("LIMIT_EXCEEDED", message);
        this.limitType = limitType;
        this.limitScope = limitScope;
        this.currentUsage = currentUsage;
        this.limitValue = limitValue;
    }
}