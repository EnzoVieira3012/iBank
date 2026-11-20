-- iBank Core Tables Migration V1
-- Creates base tables for audit, users, and reference data

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Users table (simplified for auth)
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    cpf_cnpj VARCHAR(18) UNIQUE,
    phone VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    roles VARCHAR(500) NOT NULL DEFAULT 'CLIENT',
    failed_login_attempts INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMP WITH TIME ZONE,
    password_changed_at TIMESTAMP WITH TIME ZONE,
    last_login_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_cpf_cnpj ON users(cpf_cnpj);
CREATE INDEX idx_users_status ON users(status);

-- Audit log table (partitioned by month)
CREATE TABLE audit_log (
    id BIGSERIAL,
    correlation_id UUID NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100),
    action VARCHAR(50) NOT NULL,
    user_id UUID,
    username VARCHAR(100),
    ip_address INET,
    user_agent TEXT,
    request_method VARCHAR(10),
    request_path VARCHAR(500),
    request_body JSONB,
    response_body JSONB,
    response_status INT,
    execution_time_ms BIGINT,
    error_message TEXT,
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
) PARTITION BY RANGE (created_at);

-- Create indexes on audit_log
CREATE INDEX idx_audit_log_correlation_id ON audit_log(correlation_id);
CREATE INDEX idx_audit_log_entity ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_log_user ON audit_log(user_id);
CREATE INDEX idx_audit_log_created_at ON audit_log(created_at);
CREATE INDEX idx_audit_log_action ON audit_log(action);

-- Create monthly partitions for audit_log (current year + next year)
DO $$
DECLARE
    start_date DATE := DATE_TRUNC('month', CURRENT_DATE);
    end_date DATE := start_date + INTERVAL '13 months';
    partition_start DATE;
    partition_end DATE;
    partition_name TEXT;
BEGIN
    WHILE start_date < end_date LOOP
        partition_start := start_date;
        partition_end := start_date + INTERVAL '1 month';
        partition_name := 'audit_log_' || TO_CHAR(partition_start, 'YYYY_MM');
        
        EXECUTE format(
            'CREATE TABLE IF NOT EXISTS %I PARTITION OF audit_log FOR VALUES FROM (%L) TO (%L)',
            partition_name, partition_start, partition_end
        );
        
        start_date := partition_end;
    END LOOP;
END $$;

-- Reference data: Transaction types
CREATE TABLE transaction_type (
    code VARCHAR(20) PRIMARY KEY,
    description VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    is_credit BOOLEAN NOT NULL,
    requires_approval BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO transaction_type (code, description, category, is_credit, requires_approval) VALUES
('PIX_SEND', 'PIX Enviado', 'PIX', FALSE, FALSE),
('PIX_RECEIVE', 'PIX Recebido', 'PIX', TRUE, FALSE),
('PIX_REFUND', 'Devolução PIX', 'PIX', TRUE, FALSE),
('TED_SEND', 'TED Enviada', 'TRANSFER', FALSE, TRUE),
('TED_RECEIVE', 'TED Recebida', 'TRANSFER', TRUE, FALSE),
('INTERNAL_SEND', 'Transferência Interna Enviada', 'TRANSFER', FALSE, FALSE),
('INTERNAL_RECEIVE', 'Transferência Interna Recebida', 'TRANSFER', TRUE, FALSE),
('CARD_PURCHASE', 'Compra no Cartão', 'CARD', FALSE, FALSE),
('CARD_PAYMENT', 'Pagamento de Fatura', 'CARD', TRUE, FALSE),
('LOAN_DISBURSEMENT', 'Liberação de Empréstimo', 'CREDIT', TRUE, FALSE),
('LOAN_INSTALLMENT', 'Pagamento de Parcela', 'CREDIT', FALSE, FALSE),
('FEE_CHARGE', 'Cobrança de Tarifa', 'FEE', FALSE, FALSE),
('INTEREST_CREDIT', 'Crédito de Juros', 'INTEREST', TRUE, FALSE),
('INTEREST_DEBIT', 'Débito de Juros', 'INTEREST', FALSE, FALSE);

-- Reference data: Account types
CREATE TABLE account_type (
    code VARCHAR(20) PRIMARY KEY,
    description VARCHAR(100) NOT NULL,
    allows_overdraft BOOLEAN NOT NULL DEFAULT FALSE,
    has_yield BOOLEAN NOT NULL DEFAULT FALSE,
    yield_rate NUMERIC(5,4),
    active BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO account_type (code, description, allows_overdraft, has_yield, yield_rate) VALUES
('CURRENT', 'Conta Corrente', TRUE, FALSE, NULL),
('SAVINGS', 'Conta Poupança', FALSE, TRUE, 0.0050),
('SALARY', 'Conta Salário', FALSE, FALSE, NULL),
('PAYMENT', 'Conta Pagamento', TRUE, FALSE, NULL);

-- Reference data: Pix key types
CREATE TABLE pix_key_type (
    code VARCHAR(20) PRIMARY KEY,
    description VARCHAR(100) NOT NULL,
    pattern VARCHAR(200),
    example VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO pix_key_type (code, description, pattern, example) VALUES
('CPF', 'CPF', '^\d{3}\.\d{3}\.\d{3}-\d{2}$', '123.456.789-00'),
('CNPJ', 'CNPJ', '^\d{2}\.\d{3}\.\d{3}/\d{4}-\d{2}$', '12.345.678/0001-90'),
('EMAIL', 'E-mail', '^[^@]+@[^@]+\.[^@]+$', 'usuario@email.com'),
('PHONE', 'Telefone', '^\+55\d{2}\d{9}$', '+5511999999999'),
('RANDOM', 'Chave Aleatória', '^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$', '123e4567-e89b-12d3-a456-426614174000');

-- Reference data: Card types
CREATE TABLE card_type (
    code VARCHAR(20) PRIMARY KEY,
    description VARCHAR(100) NOT NULL,
    is_credit BOOLEAN NOT NULL,
    has_installments BOOLEAN NOT NULL DEFAULT FALSE,
    max_installments INT,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO card_type (code, description, is_credit, has_installments, max_installments) VALUES
('CREDIT', 'Cartão de Crédito', TRUE, TRUE, 12),
('DEBIT', 'Cartão de Débito', FALSE, FALSE, NULL),
('MULTIPLE', 'Cartão Múltiplo (Crédito/Débito)', TRUE, TRUE, 12),
('PREPAID', 'Cartão Pré-pago', FALSE, FALSE, NULL);

-- Reference data: Loan status
CREATE TABLE loan_status (
    code VARCHAR(20) PRIMARY KEY,
    description VARCHAR(100) NOT NULL,
    is_final BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO loan_status (code, description, is_final) VALUES
('SIMULATED', 'Simulado', FALSE),
('PENDING_APPROVAL', 'Pendente Aprovação', FALSE),
('APPROVED', 'Aprovado', FALSE),
('REJECTED', 'Rejeitado', TRUE),
('DISBURSED', 'Liberado', FALSE),
('ACTIVE', 'Ativo', FALSE),
('OVERDUE', 'Em Atraso', FALSE),
('RENEGOTIATED', 'Renegociado', FALSE),
('SETTLED', 'Quitado', TRUE),
('CANCELLED', 'Cancelado', TRUE);

-- Reference data: Fee categories
CREATE TABLE fee_category (
    code VARCHAR(20) PRIMARY KEY,
    description VARCHAR(100) NOT NULL,
    is_recurring BOOLEAN NOT NULL DEFAULT FALSE,
    frequency VARCHAR(20),
    active BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO fee_category (code, description, is_recurring, frequency) VALUES
('ACCOUNT_MAINTENANCE', 'Manutenção de Conta', TRUE, 'MONTHLY'),
('CARD_ANNUITY', 'Anuidade Cartão', TRUE, 'YEARLY'),
('TRANSFER_TED', 'TED', FALSE, NULL),
('TRANSFER_DOC', 'DOC', FALSE, NULL),
('PIX_WITHDRAWAL', 'Saque PIX', FALSE, NULL),
('OVERDRAFT', 'Cheque Especial', FALSE, NULL),
('LOAN_ORIGINATION', 'Taxa de Abertura Crédito', FALSE, NULL),
('LATE_PAYMENT', 'Multa por Atraso', FALSE, NULL),
('STATEMENT', 'Emissão de Extrato', FALSE, NULL);

-- Grant permissions
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO ibank;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO ibank;