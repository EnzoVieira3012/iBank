package com.ibank.credit.model;

public enum LoanType {
    PERSONAL("Empréstimo Pessoal", false, 0, 84),
    CONSIGNMENT("Empréstimo Consignado", false, 0, 120),
    VEHICLE("Financiamento de Veículo", true, 12, 60),
    REAL_ESTATE("Financiamento Imobiliário", true, 60, 420),
    WORKING_CAPITAL("Capital de Giro", false, 0, 36),
    STUDENT("Empréstimo Estudantil", false, 0, 60);

    private final String description;
    private final boolean requiresCollateral;
    private final int minTermMonths;
    private final int maxTermMonths;

    LoanType(String description, boolean requiresCollateral, int minTermMonths, int maxTermMonths) {
        this.description = description;
        this.requiresCollateral = requiresCollateral;
        this.minTermMonths = minTermMonths;
        this.maxTermMonths = maxTermMonths;
    }

    public String getDescription() { return description; }
    public boolean isRequiresCollateral() { return requiresCollateral; }
    public int getMinTermMonths() { return minTermMonths; }
    public int getMaxTermMonths() { return maxTermMonths; }
}