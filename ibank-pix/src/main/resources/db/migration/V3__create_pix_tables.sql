-- iBank PIX Tables Migration V3
-- Creates PIX keys, transactions, and QR codes tables

-- PIX Keys table
CREATE TABLE pix_keys (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    key_value VARCHAR(100) NOT NULL UNIQUE,
    key_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    holder_name VARCHAR(255) NOT NULL,
    holder_cpf VARCHAR(18) NOT NULL,
    holder_account_type VARCHAR(20) NOT NULL,
    holder_branch VARCHAR(10),
    holder_account_number VARCHAR(20),
    dict_registration_id VARCHAR(100),
    dict_registered_at TIMESTAMP WITH TIME ZONE,
    claimed_at TIMESTAMP WITH TIME ZONE,
    claimed_by_cpf VARCHAR(18),
    blocked_at TIMESTAMP WITH TIME ZONE,
    blocked_reason VARCHAR(500),
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_pix_keys_account_id ON pix_keys(account_id);
CREATE INDEX idx_pix_keys_holder_cpf ON pix_keys(holder_cpf);
CREATE INDEX idx_pix_keys_status ON pix_keys(status);
CREATE INDEX idx_pix_keys_type ON pix_keys(key_type);
CREATE INDEX idx_pix_keys_dict_reg ON pix_keys(dict_registration_id);

-- PIX Transactions table
CREATE TABLE pix_transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    end_to_end_id VARCHAR(50) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    amount NUMERIC(19,2) NOT NULL,
    description VARCHAR(140),
    -- Payer info
    payer_account_id UUID REFERENCES accounts(id) ON DELETE SET NULL,
    payer_name VARCHAR(255),
    payer_cpf VARCHAR(18),
    payer_key_id UUID REFERENCES pix_keys(id) ON DELETE SET NULL,
    payer_key_value VARCHAR(100),
    payer_key_type VARCHAR(20),
    payer_branch VARCHAR(10),
    payer_account_number VARCHAR(20),
    -- Payee info
    payee_account_id UUID REFERENCES accounts(id) ON DELETE SET NULL,
    payee_name VARCHAR(255),
    payee_cpf VARCHAR(18),
    payee_key_id UUID REFERENCES pix_keys(id) ON DELETE SET NULL,
    payee_key_value VARCHAR(100),
    payee_key_type VARCHAR(20),
    payee_branch VARCHAR(10),
    payee_account_number VARCHAR(20),
    -- Refund
    refund_id UUID REFERENCES pix_transactions(id) ON DELETE SET NULL,
    refund_end_to_end_id VARCHAR(50),
    refunded_amount NUMERIC(19,2),
    refund_reason VARCHAR(500),
    refunded_at TIMESTAMP WITH TIME ZONE,
    -- Processing
    processed_at TIMESTAMP WITH TIME ZONE,
    failed_at TIMESTAMP WITH TIME ZONE,
    failure_reason VARCHAR(500),
    dict_response JSONB,
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_pix_tx_payer_account ON pix_transactions(payer_account_id);
CREATE INDEX idx_pix_tx_payee_account ON pix_transactions(payee_account_id);
CREATE INDEX idx_pix_tx_payer_key ON pix_transactions(payer_key_id);
CREATE INDEX idx_pix_tx_payee_key ON pix_transactions(payee_key_id);
CREATE INDEX idx_pix_tx_status ON pix_transactions(status);
CREATE INDEX idx_pix_tx_end_to_end ON pix_transactions(end_to_end_id);
CREATE INDEX idx_pix_tx_created_at ON pix_transactions(created_at);
CREATE INDEX idx_pix_tx_refund_id ON pix_transactions(refund_id);

-- PIX QR Codes table
CREATE TABLE pix_qr_codes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    emv VARCHAR(500) NOT NULL UNIQUE,
    payload VARCHAR(2000) NOT NULL,
    qr_code_type VARCHAR(20) NOT NULL,
    amount NUMERIC(19,2),
    description VARCHAR(140),
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    pix_key_id UUID REFERENCES pix_keys(id) ON DELETE SET NULL,
    transaction_id UUID REFERENCES pix_transactions(id) ON DELETE SET NULL,
    expires_at TIMESTAMP WITH TIME ZONE,
    used_at TIMESTAMP WITH TIME ZONE,
    usage_count INT NOT NULL DEFAULT 0,
    max_usage INT NOT NULL DEFAULT 1,
    image_base64 TEXT,
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_pix_qr_account ON pix_qr_codes(account_id);
CREATE INDEX idx_pix_qr_key ON pix_qr_codes(pix_key_id);
CREATE INDEX idx_pix_qr_tx ON pix_qr_codes(transaction_id);
CREATE INDEX idx_pix_qr_emv ON pix_qr_codes(emv);
CREATE INDEX idx_pix_qr_expires ON pix_qr_codes(expires_at);
CREATE INDEX idx_pix_qr_type ON pix_qr_codes(qr_code_type);

-- Grant permissions
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO ibank;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO ibank;