package com.ibank.credit.model;

public enum LoanStatus {
    SIMULATED("Simulado"),
    PENDING_APPROVAL("Pendente Aprovação"),
    APPROVED("Aprovado"),
    REJECTED("Rejeitado"),
    DISBURSED("Liberado"),
    ACTIVE("Ativo"),
    OVERDUE("Em Atraso"),
    RENEGOTIATED("Renegociado"),
    SETTLED("Quitado"),
    CANCELLED("Cancelado");

    private final String description;

    LoanStatus(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }

    public boolean isFinal() {
        return this == SETTLED || this == CANCELLED || this == REJECTED;
    }

    public boolean canDisburse() {
        return this == APPROVED;
    }

    public boolean canPay() {
        return this == ACTIVE || this == OVERDUE || this == RENEGOTIATED;
    }

    public boolean canRenegotiate() {
        return this == ACTIVE || this == OVERDUE;
    }

    public boolean canSettle() {
        return this == ACTIVE || this == OVERDUE || this == RENEGOTIATED;
    }
}