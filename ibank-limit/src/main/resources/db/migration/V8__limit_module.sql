-- iBank Limit Module Migration
-- Version: 8
-- Description: Creates tables for customer limits, transaction blocks, limit usages

-- Customer Limits table
CREATE TABLE IF NOT EXISTS customer_limits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL REFERENCES customers(id),
    account_id UUID REFERENCES accounts(id),
    card_id UUID REFERENCES cards(id),
    limit_type VARCHAR(40) NOT NULL CHECK (limit_type IN (
        'DAILY_TRANSACTION_COUNT', 'DAILY_TRANSACTION_AMOUNT',
        'MONTHLY_TRANSACTION_COUNT', 'MONTHLY_TRANSACTION_AMOUNT',
        'PER_TRANSACTION_AMOUNT',
        'DAILY_PIX_COUNT', 'DAILY_PIX_AMOUNT',
        'MONTHLY_PIX_COUNT', 'MONTHLY_PIX_AMOUNT',
        'DAILY_TRANSFER_COUNT', 'DAILY_TRANSFER_AMOUNT',
        'DAILY_TED_COUNT', 'DAILY_TED_AMOUNT',
        'DAILY_CARD_PURCHASE_COUNT', 'DAILY_CARD_PURCHASE_AMOUNT',
        'DAILY_CARD_WITHDRAWAL_COUNT', 'DAILY_CARD_WITHDRAWAL_AMOUNT',
        'DAILY_CASH_ADVANCE_AMOUNT',
        'OVERDRAFT_LIMIT', 'CREDIT_CARD_LIMIT'
    )),
    scope VARCHAR(20) NOT NULL CHECK (scope IN ('ACCOUNT', 'CUSTOMER', 'CARD', 'PRODUCT', 'CHANNEL', 'GLOBAL')),
    limit_value NUMERIC(19,2) NOT NULL,
    current_usage NUMERIC(19,2),
    period_start TIMESTAMP,
    period_end TIMESTAMP,
    valid_from DATE NOT NULL,
    valid_to DATE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    warning_threshold NUMERIC(5,2),
    block_on_exceed BOOLEAN DEFAULT FALSE,
    conditions JSONB,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_limits_customer ON customer_limits(customer_id);
CREATE INDEX IF NOT EXISTS idx_limits_account ON customer_limits(account_id);
CREATE INDEX IF NOT EXISTS idx_limits_card ON customer_limits(card_id);
CREATE INDEX IF NOT EXISTS idx_limits_type ON customer_limits(limit_type);
CREATE INDEX IF NOT EXISTS idx_limits_scope ON customer_limits(scope);
CREATE INDEX IF NOT EXISTS idx_limits_active ON customer_limits(active);
CREATE INDEX IF NOT EXISTS idx_limits_dates ON customer_limits(valid_from, valid_to);

-- Transaction Blocks table
CREATE TABLE IF NOT EXISTS transaction_blocks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID REFERENCES customers(id),
    account_id UUID REFERENCES accounts(id),
    card_id UUID REFERENCES cards(id),
    block_type VARCHAR(30) NOT NULL CHECK (block_type IN (
        'VELOCITY', 'SUSPICIOUS_PATTERN', 'FRAUD_RULE', 'MANUAL',
        'COMPLIANCE', 'OVERDUE_PAYMENT', 'KYC_EXPIRED', 'DOCUMENT_PENDING', 'CUSTOM'
    )),
    scope VARCHAR(30) NOT NULL CHECK (scope IN (
        'ACCOUNT', 'CUSTOMER', 'CARD', 'TRANSACTION_TYPE', 'CHANNEL',
        'IP_ADDRESS', 'DEVICE_FINGERPRINT', 'GLOBAL'
    )),
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'EXPIRED', 'REVOKED', 'PENDING_APPROVAL')),
    reason VARCHAR(500) NOT NULL,
    starts_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP,
    created_by UUID NOT NULL,
    approved_by UUID,
    approved_at TIMESTAMP,
    revoked_by UUID,
    revoked_at TIMESTAMP,
    revoke_reason VARCHAR(500),
    conditions JSONB,
    affected_channels VARCHAR(200),
    affected_transaction_types VARCHAR(200),
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_blocks_customer ON transaction_blocks(customer_id);
CREATE INDEX IF NOT EXISTS idx_blocks_account ON transaction_blocks(account_id);
CREATE INDEX IF NOT EXISTS idx_blocks_card ON transaction_blocks(card_id);
CREATE INDEX IF NOT EXISTS idx_blocks_type ON transaction_blocks(block_type);
CREATE INDEX IF NOT EXISTS idx_blocks_status ON transaction_blocks(status);
CREATE INDEX IF NOT EXISTS idx_blocks_scope ON transaction_blocks(scope);
CREATE INDEX IF NOT EXISTS idx_blocks_dates ON transaction_blocks(starts_at, expires_at);
CREATE INDEX IF NOT EXISTS idx_blocks_created_by ON transaction_blocks(created_by);

-- Limit Usages table
CREATE TABLE IF NOT EXISTS limit_usages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL REFERENCES customers(id),
    account_id UUID REFERENCES accounts(id),
    card_id UUID REFERENCES cards(id),
    limit_type VARCHAR(40) NOT NULL CHECK (limit_type IN (
        'DAILY_TRANSACTION_COUNT', 'DAILY_TRANSACTION_AMOUNT',
        'MONTHLY_TRANSACTION_COUNT', 'MONTHLY_TRANSACTION_AMOUNT',
        'PER_TRANSACTION_AMOUNT',
        'DAILY_PIX_COUNT', 'DAILY_PIX_AMOUNT',
        'MONTHLY_PIX_COUNT', 'MONTHLY_PIX_AMOUNT',
        'DAILY_TRANSFER_COUNT', 'DAILY_TRANSFER_AMOUNT',
        'DAILY_TED_COUNT', 'DAILY_TED_AMOUNT',
        'DAILY_CARD_PURCHASE_COUNT', 'DAILY_CARD_PURCHASE_AMOUNT',
        'DAILY_CARD_WITHDRAWAL_COUNT', 'DAILY_CARD_WITHDRAWAL_AMOUNT',
        'DAILY_CASH_ADVANCE_AMOUNT',
        'OVERDRAFT_LIMIT', 'CREDIT_CARD_LIMIT'
    )),
    period_start TIMESTAMP NOT NULL,
    period_end TIMESTAMP NOT NULL,
    count BIGINT NOT NULL DEFAULT 0,
    amount NUMERIC(19,2),
    last_transaction_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_usage_customer ON limit_usages(customer_id);
CREATE INDEX IF NOT EXISTS idx_usage_account ON limit_usages(account_id);
CREATE INDEX IF NOT EXISTS idx_usage_card ON limit_usages(card_id);
CREATE INDEX IF NOT EXISTS idx_usage_type ON limit_usages(limit_type);
CREATE INDEX IF NOT EXISTS idx_usage_period ON limit_usages(period_start, period_end);
CREATE INDEX IF NOT EXISTS idx_usage_updated ON limit_usages(updated_at);

-- Unique constraint to prevent duplicate usage records per period
CREATE UNIQUE INDEX IF NOT EXISTS uk_usage_customer_type_period
    ON limit_usages(customer_id, limit_type, period_start, period_end)
    WHERE account_id IS NULL AND card_id IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_usage_account_type_period
    ON limit_usages(account_id, limit_type, period_start, period_end)
    WHERE account_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_usage_card_type_period
    ON limit_usages(card_id, limit_type, period_start, period_end)
    WHERE card_id IS NOT NULL;