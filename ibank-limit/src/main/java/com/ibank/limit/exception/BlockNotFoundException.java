package com.ibank.limit.exception;

import com.ibank.core.exception.BusinessException;

public class BlockNotFoundException extends BusinessException {
    public BlockNotFoundException(String message) {
        super("BLOCK_NOT_FOUND", message);
    }
}