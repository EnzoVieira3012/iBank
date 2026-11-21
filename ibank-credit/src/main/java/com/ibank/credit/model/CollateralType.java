package com.ibank.credit.model;

public enum CollateralType {
    VEHICLE("Veículo"),
    REAL_ESTATE("Imóvel"),
    INVESTMENT("Investimentos"),
    RECEIVABLES("Recebíveis"),
    GUARANTOR("Avalista"),
    INSURANCE("Seguro Prestamista"),
    NONE("Sem Garantia");

    private final String description;

    CollateralType(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}