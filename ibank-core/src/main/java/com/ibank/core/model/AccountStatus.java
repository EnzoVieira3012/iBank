package com.ibank.core.model;

public enum AccountStatus {
    ACTIVE("Ativa"),
    BLOCKED("Bloqueada"),
    CLOSED("Encerrada"),
    PENDING("Pendente Abertura"),
    RESTRICTED("Restrita");

    private final String description;

    AccountStatus(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}