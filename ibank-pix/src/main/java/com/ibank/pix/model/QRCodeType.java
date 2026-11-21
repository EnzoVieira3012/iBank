package com.ibank.pix.model;

public enum QRCodeType {
    STATIC("Estatico", "Não expira, valor fixo ou aberto"),
    DYNAMIC("Dinamico", "Expira, valor definido, uso único");

    private final String description;
    private final String details;

    QRCodeType(String description, String details) {
        this.description = description;
        this.details = details;
    }

    public String getDescription() { return description; }
    public String getDetails() { return details; }
}