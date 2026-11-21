package com.ibank.credit.model;

public enum InstallmentStatus {
    PENDING("Pendente"),
    PAID("Paga"),
    PARTIALLY_PAID("Parcialmente Paga"),
    OVERDUE("Em Atraso"),
    RENEGOTIATED("Renegociada"),
    WAIVED("Perdoada");

    private final String description;

    InstallmentStatus(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }

    public boolean isOpen() {
        return this == PENDING || this == PARTIALLY_PAID || this == OVERDUE;
    }

    public boolean isClosed() {
        return this == PAID || this == WAIVED;
    }
}