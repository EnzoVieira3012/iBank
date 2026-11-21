package com.ibank.core.model;

public enum AccountType {
    CURRENT("Conta Corrente", true, false, null),
    SAVINGS("Conta Poupança", false, true, java.math.BigDecimal.valueOf(0.005)),
    SALARY("Conta Salário", false, false, null),
    PAYMENT("Conta Pagamento", true, false, null);

    private final String description;
    private final boolean allowsOverdraft;
    private final boolean hasYield;
    private final java.math.BigDecimal yieldRate;

    AccountType(String description, boolean allowsOverdraft, boolean hasYield, java.math.BigDecimal yieldRate) {
        this.description = description;
        this.allowsOverdraft = allowsOverdraft;
        this.hasYield = hasYield;
        this.yieldRate = yieldRate;
    }

    public String getDescription() { return description; }
    public boolean isAllowsOverdraft() { return allowsOverdraft; }
    public boolean isHasYield() { return hasYield; }
    public java.math.BigDecimal getYieldRate() { return yieldRate; }
}