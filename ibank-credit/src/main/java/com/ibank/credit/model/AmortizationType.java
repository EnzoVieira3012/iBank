package com.ibank.credit.model;

public enum AmortizationType {
    PRICE("Tabela Price", "Parcelas fixas, juros decrescentes, amortização crescente"),
    SAC("Tabela SAC", "Amortização fixa, juros decrescentes, parcelas decrescentes"),
    SACADOR("Tabela Sacador", "Amortização crescente, juros decrescentes, parcelas crescentes");

    private final String description;
    private final String details;

    AmortizationType(String description, String details) {
        this.description = description;
        this.details = details;
    }

    public String getDescription() { return description; }
    public String getDetails() { return details; }
}