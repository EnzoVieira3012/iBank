package com.ibank.pix.model;

public enum PixKeyStatus {
    ACTIVE("Ativa"),
    INACTIVE("Inativa"),
    PENDING("Pendente Validação"),
    BLOCKED("Bloqueada"),
    CLAIMED("Reivindicada por outro usuário");

    private final String description;

    PixKeyStatus(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}