package com.ibank.core.exception;

public class NotFoundException extends BusinessException {

    public NotFoundException(String resource, String identifier) {
        super("NOT_FOUND", resource + " not found: " + identifier, org.springframework.http.HttpStatus.NOT_FOUND);
    }

    public NotFoundException(String message) {
        super("NOT_FOUND", message, org.springframework.http.HttpStatus.NOT_FOUND);
    }
}