package com.ibank.pix.model;

public enum PixKeyType {
    CPF("CPF", "^\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}$", "123.456.789-00"),
    CNPJ("CNPJ", "^\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}$", "12.345.678/0001-90"),
    EMAIL("E-mail", "^[^@]+@[^@]+\\.[^@]+$", "usuario@email.com"),
    PHONE("Telefone", "^\\+55\\d{2}\\d{9}$", "+5511999999999"),
    RANDOM("Chave Aleatória", "^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$", "123e4567-e89b-12d3-a456-426614174000");

    private final String description;
    private final String pattern;
    private final String example;

    PixKeyType(String description, String pattern, String example) {
        this.description = description;
        this.pattern = pattern;
        this.example = example;
    }

    public String getDescription() { return description; }
    public String getPattern() { return pattern; }
    public String getExample() { return example; }

    public boolean validate(String key) {
        return key != null && key.matches(pattern);
    }
}