-- iBank Fee Module Migration
-- Version: 7
-- Description: Creates tables for fee configs, fee charges, interest rates, interest accruals

-- Fee Configs table
CREATE TABLE IF NOT EXISTS fee_configs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    fee_type VARCHAR(30) NOT NULL CHECK (fee_type IN (
        'ACCOUNT_MAINTENANCE', 'ACCOUNT_OPENING', 'ACCOUNT_CLOSURE',
        'CARD_ANNUAL', 'CARD_REISSUE', 'CARD_LATE_PAYMENT', 'CARD_OVERLIMIT',
        'CARD_CASH_ADVANCE', 'CARD_INTERNATIONAL',
        'TRANSFER_TED', 'TRANSFER_DOC', 'TRANSFER_PIX', 'TRANSFER_INTERNAL', 'TRANSFER_SCHEDULED',
        'CREDIT_ORIGINATION', 'CREDIT_LATE_PAYMENT', 'CREDIT_PREPAYMENT', 'CREDIT_RENEGOTIATION',
        'STATEMENT_PAPER', 'STATEMENT_DIGITAL', 'OVERDRAFT_USAGE', 'CUSTOM'
    )),
    product_code VARCHAR(50),
    channel VARCHAR(20),
    customer_segment VARCHAR(30),
    period VARCHAR(20) NOT NULL CHECK (period IN (
        'ONE_TIME', 'DAILY', 'WEEKLY', 'MONTHLY', 'QUARTERLY', 'SEMI_ANNUAL', 'ANNUAL', 'PER_TRANSACTION', 'PERCENTAGE'
    )),
    calculation_type VARCHAR(20) NOT NULL CHECK (calculation_type IN (
        'FIXED', 'PERCENTAGE', 'TIERED', 'MIN_MAX', 'FORMULA'
    )),
    fixed_amount NUMERIC(19,4),
    percentage_rate NUMERIC(10,6),
    min_amount NUMERIC(19,4),
    max_amount NUMERIC(19,4),
    tier_config JSONB,
    formula VARCHAR(500),
    tax_included BOOLEAN DEFAULT FALSE,
    tax_rate NUMERIC(10,6),
    valid_from DATE NOT NULL,
    valid_to DATE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    description VARCHAR(500),
    priority INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_fee_config_type ON fee_configs(fee_type);
CREATE INDEX IF NOT EXISTS idx_fee_config_product ON fee_configs(product_code);
CREATE INDEX IF NOT EXISTS idx_fee_config_channel ON fee_configs(channel);
CREATE INDEX IF NOT EXISTS idx_fee_config_active ON fee_configs(active);
CREATE INDEX IF NOT EXISTS idx_fee_config_dates ON fee_configs(valid_from, valid_to);

-- Fee Charges table
CREATE TABLE IF NOT EXISTS fee_charges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID REFERENCES accounts(id),
    card_id UUID REFERENCES cards(id),
    loan_id UUID REFERENCES loans(id),
    transfer_id UUID REFERENCES transfers(id),
    fee_config_id UUID REFERENCES fee_configs(id),
    fee_type VARCHAR(30) NOT NULL CHECK (fee_type IN (
        'ACCOUNT_MAINTENANCE', 'ACCOUNT_OPENING', 'ACCOUNT_CLOSURE',
        'CARD_ANNUAL', 'CARD_REISSUE', 'CARD_LATE_PAYMENT', 'CARD_OVERLIMIT',
        'CARD_CASH_ADVANCE', 'CARD_INTERNATIONAL',
        'TRANSFER_TED', 'TRANSFER_DOC', 'TRANSFER_PIX', 'TRANSFER_INTERNAL', 'TRANSFER_SCHEDULED',
        'CREDIT_ORIGINATION', 'CREDIT_LATE_PAYMENT', 'CREDIT_PREPAYMENT', 'CREDIT_RENEGOTIATION',
        'STATEMENT_PAPER', 'STATEMENT_DIGITAL', 'OVERDRAFT_USAGE', 'CUSTOM'
    )),
    description VARCHAR(500),
    charge_date DATE NOT NULL,
    due_date DATE,
    amount NUMERIC(19,4) NOT NULL,
    tax_amount NUMERIC(19,4),
    total_amount NUMERIC(19,4),
    status VARCHAR(20) NOT NULL CHECK (status IN (
        'PENDING', 'CHARGED', 'PAID', 'PARTIALLY_PAID', 'OVERDUE', 'WAIVED', 'CANCELLED', 'REFUNDED'
    )),
    paid_at TIMESTAMP,
    payment_transaction_id UUID REFERENCES card_transactions(id),
    idempotency_key VARCHAR(36) UNIQUE,
    calculation_details JSONB,
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_fee_charge_account ON fee_charges(account_id);
CREATE INDEX IF NOT EXISTS idx_fee_charge_card ON fee_charges(card_id);
CREATE INDEX IF NOT EXISTS idx_fee_charge_loan ON fee_charges(loan_id);
CREATE INDEX IF NOT EXISTS idx_fee_charge_transfer ON fee_charges(transfer_id);
CREATE INDEX IF NOT EXISTS idx_fee_charge_config ON fee_charges(fee_config_id);
CREATE INDEX IF NOT EXISTS idx_fee_charge_status ON fee_charges(status);
CREATE INDEX IF NOT EXISTS idx_fee_charge_date ON fee_charges(charge_date);
CREATE INDEX IF NOT EXISTS idx_fee_charge_due_date ON fee_charges(due_date);
CREATE INDEX IF NOT EXISTS idx_fee_charge_idempotency ON fee_charges(idempotency_key);

-- Interest Rates table
CREATE TABLE IF NOT EXISTS interest_rates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    index_type VARCHAR(20) NOT NULL CHECK (index_type IN ('SELIC', 'CDI', 'TJLP', 'TR', 'IPCA', 'IGP_M', 'FIXED', 'CUSTOM')),
    product_code VARCHAR(50),
    rate_date DATE NOT NULL,
    rate_value NUMERIC(15,8) NOT NULL,
    spread NUMERIC(10,6),
    effective_rate NUMERIC(15,8),
    source VARCHAR(50),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    UNIQUE(index_type, rate_date, product_code)
);

CREATE INDEX IF NOT EXISTS idx_interest_rate_index ON interest_rates(index_type);
CREATE INDEX IF NOT EXISTS idx_interest_rate_product ON interest_rates(product_code);
CREATE INDEX IF NOT EXISTS idx_interest_rate_date ON interest_rates(rate_date);
CREATE INDEX IF NOT EXISTS idx_interest_rate_active ON interest_rates(active);

-- Interest Accruals table
CREATE TABLE IF NOT EXISTS interest_accruals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID REFERENCES accounts(id),
    card_id UUID REFERENCES cards(id),
    loan_id UUID REFERENCES loans(id),
    accrual_date DATE NOT NULL,
    principal_balance NUMERIC(19,4) NOT NULL,
    rate_applied NUMERIC(15,8) NOT NULL,
    days INTEGER NOT NULL,
    interest_amount NUMERIC(19,4) NOT NULL,
    tax_amount NUMERIC(19,4),
    total_amount NUMERIC(19,4),
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACCRUED', 'CAPITALIZED', 'PAID', 'WAIVED', 'CANCELLED')),
    capitalized_at TIMESTAMP,
    paid_at TIMESTAMP,
    calculation_details JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_interest_accrual_account ON interest_accruals(account_id);
CREATE INDEX IF NOT EXISTS idx_interest_accrual_card ON interest_accruals(card_id);
CREATE INDEX IF NOT EXISTS idx_interest_accrual_loan ON interest_accruals(loan_id);
CREATE INDEX IF NOT EXISTS idx_interest_accrual_date ON interest_accruals(accrual_date);
CREATE INDEX IF NOT EXISTS idx_interest_accrual_status ON interest_accruals(status);