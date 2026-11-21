-- iBank Transfer Tables Migration V4
-- Creates transfers table

CREATE TABLE transfers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    idempotency_key VARCHAR(100) NOT NULL UNIQUE,
    end_to_end_id VARCHAR(50) NOT NULL UNIQUE,
    transfer_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    amount NUMERIC(19,2) NOT NULL,
    description VARCHAR(140),
    -- Payer info
    payer_account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE RESTRICT,
    payer_name VARCHAR(255),
    payer_cpf VARCHAR(18),
    payer_branch VARCHAR(10),
    payer_account_number VARCHAR(20),
    payer_bank_ispb VARCHAR(8),
    -- Payee info
    payee_account_id UUID REFERENCES accounts(id) ON DELETE SET NULL,
    payee_name VARCHAR(255) NOT NULL,
    payee_cpf VARCHAR(18),
    payee_branch VARCHAR(10),
    payee_account_number VARCHAR(20),
    payee_bank_ispb VARCHAR(8),
    payee_bank_name VARCHAR(100),
    -- Scheduling
    scheduled_at TIMESTAMP WITH TIME ZONE,
    executed_at TIMESTAMP WITH TIME ZONE,
    failed_at TIMESTAMP WITH TIME ZONE,
    failure_reason VARCHAR(500),
    cancelled_at TIMESTAMP WITH TIME ZONE,
    cancelled_reason VARCHAR(500),
    -- Fee
    fee_amount NUMERIC(19,2),
    fee_charged_at TIMESTAMP WITH TIME ZONE,
    -- External reference
    external_reference VARCHAR(100),
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_transfers_payer_account ON transfers(payer_account_id);
CREATE INDEX idx_transfers_payee_account ON transfers(payee_account_id);
CREATE INDEX idx_transfers_status ON transfers(status);
CREATE INDEX idx_transfers_type ON transfers(transfer_type);
CREATE INDEX idx_transfers_scheduled_at ON transfers(scheduled_at);
CREATE INDEX idx_transfers_end_to_end ON transfers(end_to_end_id);
CREATE INDEX idx_transfers_idempotency ON transfers(idempotency_key);
CREATE INDEX idx_transfers_created_at ON transfers(created_at);

-- Transfer fee configuration table
CREATE TABLE transfer_fee_config (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    transfer_type VARCHAR(20) NOT NULL UNIQUE,
    fee_type VARCHAR(20) NOT NULL, -- FIXED, PERCENTAGE, TIERED
    fixed_fee NUMERIC(19,2),
    percentage_fee NUMERIC(5,4),
    min_fee NUMERIC(19,2),
    max_fee NUMERIC(19,2),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    effective_from DATE NOT NULL,
    effective_to DATE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

INSERT INTO transfer_fee_config (transfer_type, fee_type, fixed_fee, min_fee, max_fee, effective_from) VALUES
('INTERNAL', 'FIXED', 0.00, 0.00, 0.00, '2024-01-01'),
('TED', 'FIXED', 8.50, 0.00, 8.50, '2024-01-01'),
('DOC', 'FIXED', 5.50, 0.00, 5.50, '2024-01-01'),
('SCHEDULED', 'FIXED', 0.00, 0.00, 0.00, '2024-01-01');

-- Grant permissions
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO ibank;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO ibank;