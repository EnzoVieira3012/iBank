package com.ibank.pix.model;

public enum PixTransactionStatus {
    PENDING("Pendente"),
    PROCESSING("Processando"),
    COMPLETED("Concluída"),
    FAILED("Falhou"),
    REFUNDED("Devolvida"),
    PARTIALLY_REFUNDED("Parcialmente Devolvida"),
    CANCELLED("Cancelada"),
    EXPIRED("Expirada");

    private final String description;

    PixTransactionStatus(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }

    public boolean isFinal() {
        return this == COMPLETED || this == FAILED || this == REFUNDED || 
               this == PARTIALLY_REFUNDED || this == CANCELLED || this == EXPIRED;
    }

    public boolean canRefund() {
        return this == COMPLETED || this == PARTIALLY_REFUNDED;
    }
}