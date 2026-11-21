-- iBank Account Tables Migration V2
-- Creates accounts and related tables

-- Accounts table
CREATE TABLE accounts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    account_number VARCHAR(20) NOT NULL UNIQUE,
    branch VARCHAR(10) NOT NULL DEFAULT '0001',
    account_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    holder_name VARCHAR(255) NOT NULL,
    holder_cpf VARCHAR(18) NOT NULL,
    holder_email VARCHAR(255),
    holder_phone VARCHAR(20),
    holder_birth_date DATE,
    balance NUMERIC(19,2) NOT NULL DEFAULT 0.00,
    available_balance NUMERIC(19,2) NOT NULL DEFAULT 0.00,
    blocked_balance NUMERIC(19,2) NOT NULL DEFAULT 0.00,
    overdraft_limit NUMERIC(19,2) NOT NULL DEFAULT 0.00,
    overdraft_used NUMERIC(19,2) NOT NULL DEFAULT 0.00,
    daily_withdrawal_limit NUMERIC(19,2),
    daily_transfer_limit NUMERIC(19,2),
    monthly_transfer_limit NUMERIC(19,2),
    pix_limit NUMERIC(19,2),
    is_pix_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    is_debit_card_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    is_credit_card_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    interest_rate NUMERIC(5,4),
    maintenance_fee NUMERIC(19,2),
    last_transaction_at TIMESTAMP WITH TIME ZONE,
    closed_at TIMESTAMP WITH TIME ZONE,
    closed_reason VARCHAR(500),
    blocked_at TIMESTAMP WITH TIME ZONE,
    blocked_reason VARCHAR(500),
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_accounts_holder_cpf ON accounts(holder_cpf);
CREATE INDEX idx_accounts_status ON accounts(status);
CREATE INDEX idx_accounts_branch ON accounts(branch);
CREATE INDEX idx_accounts_account_type ON accounts(account_type);
CREATE INDEX idx_accounts_created_at ON accounts(created_at);

-- Account limits usage tracking
CREATE TABLE account_limit_usage (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    limit_type VARCHAR(50) NOT NULL,
    period_start TIMESTAMP WITH TIME ZONE NOT NULL,
    period_end TIMESTAMP WITH TIME ZONE NOT NULL,
    used_amount NUMERIC(19,2) NOT NULL DEFAULT 0.00,
    limit_amount NUMERIC(19,2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (account_id, limit_type, period_start)
);

CREATE INDEX idx_account_limit_usage_account ON account_limit_usage(account_id);
CREATE INDEX idx_account_limit_usage_period ON account_limit_usage(period_start, period_end);

-- Account transaction summary (materialized view for performance)
CREATE TABLE account_transaction_summary (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    total_credits NUMERIC(19,2) NOT NULL DEFAULT 0.00,
    total_debits NUMERIC(19,2) NOT NULL DEFAULT 0.00,
    credit_count INT NOT NULL DEFAULT 0,
    debit_count INT NOT NULL DEFAULT 0,
    pix_sent_count INT NOT NULL DEFAULT 0,
    pix_received_count INT NOT NULL DEFAULT 0,
    transfer_sent_count INT NOT NULL DEFAULT 0,
    transfer_received_count INT NOT NULL DEFAULT 0,
    fee_charged NUMERIC(19,2) NOT NULL DEFAULT 0.00,
    interest_earned NUMERIC(19,2) NOT NULL DEFAULT 0.00,
    interest_charged NUMERIC(19,2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (account_id, date)
);

CREATE INDEX idx_account_tx_summary_account ON account_transaction_summary(account_id);
CREATE INDEX idx_account_tx_summary_date ON account_transaction_summary(date);

-- Account balance history (for statements)
CREATE TABLE account_balance_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    opening_balance NUMERIC(19,2) NOT NULL,
    closing_balance NUMERIC(19,2) NOT NULL,
    min_balance NUMERIC(19,2) NOT NULL,
    max_balance NUMERIC(19,2) NOT NULL,
    avg_balance NUMERIC(19,2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (account_id, date)
);

CREATE INDEX idx_account_balance_history_account ON account_balance_history(account_id);
CREATE INDEX idx_account_balance_history_date ON account_balance_history(date);

-- Grant permissions
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO ibank;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO ibank;