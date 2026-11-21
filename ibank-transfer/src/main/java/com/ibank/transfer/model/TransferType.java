package com.ibank.transfer.model;

public enum TransferType {
    INTERNAL("Transferência Interna", false, false),
    TED("TED", true, true),
    DOC("DOC", true, false),
    SCHEDULED("Transferência Agendada", false, false);

    private final String description;
    private final boolean requiresCutoffValidation;
    private final boolean isInterbank;

    TransferType(String description, boolean requiresCutoffValidation, boolean isInterbank) {
        this.description = description;
        this.requiresCutoffValidation = requiresCutoffValidation;
        this.isInterbank = isInterbank;
    }

    public String getDescription() { return description; }
    public boolean isRequiresCutoffValidation() { return requiresCutoffValidation; }
    public boolean isInterbank() { return isInterbank; }
}