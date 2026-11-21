package com.ibank.transfer.model;

public enum TransferStatus {
    PENDING("Pendente"),
    SCHEDULED("Agendada"),
    PROCESSING("Processando"),
    COMPLETED("Concluída"),
    FAILED("Falhou"),
    CANCELLED("Cancelada"),
    REJECTED("Rejeitada"),
    EXPIRED("Expirada");

    private final String description;

    TransferStatus(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }

    public boolean isFinal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED || 
               this == REJECTED || this == EXPIRED;
    }

    public boolean canCancel() {
        return this == PENDING || this == SCHEDULED || this == PROCESSING;
    }
}