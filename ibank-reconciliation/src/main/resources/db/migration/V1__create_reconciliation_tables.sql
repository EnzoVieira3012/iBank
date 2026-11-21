-- iBank Reconciliation - Initial schema
-- Creates tables for reconciliation jobs, breaks, and rules

CREATE TABLE IF NOT EXISTS reconciliation_job (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_type VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL,
    source_system VARCHAR(100),
    target_system VARCHAR(100),
    external_reference VARCHAR(100),
    parameters JSONB,
    total_records BIGINT NOT NULL DEFAULT 0,
    matched_records BIGINT NOT NULL DEFAULT 0,
    mismatched_records BIGINT NOT NULL DEFAULT 0,
    missing_in_source BIGINT NOT NULL DEFAULT 0,
    missing_in_target BIGINT NOT NULL DEFAULT 0,
    breaks_count BIGINT NOT NULL DEFAULT 0,
    resolved_breaks BIGINT NOT NULL DEFAULT 0,
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    error_message VARCHAR(2000),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_recon_job_status ON reconciliation_job (status);
CREATE INDEX IF NOT EXISTS idx_recon_job_type ON reconciliation_job (job_type);
CREATE INDEX IF NOT EXISTS idx_recon_job_started ON reconciliation_job (started_at);
CREATE INDEX IF NOT EXISTS idx_recon_job_external ON reconciliation_job (external_reference);

CREATE TABLE IF NOT EXISTS reconciliation_break (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL REFERENCES reconciliation_job(id) ON DELETE CASCADE,
    break_type VARCHAR(50) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100) NOT NULL,
    source_reference VARCHAR(100),
    target_reference VARCHAR(100),
    source_amount NUMERIC(19,4),
    target_amount NUMERIC(19,4),
    difference_amount NUMERIC(19,4),
    currency VARCHAR(3),
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    assignee_id VARCHAR(100),
    source_data JSONB,
    target_data JSONB,
    resolution_data JSONB,
    resolution_notes VARCHAR(2000),
    resolved_by VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMPTZ,
    due_date TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_recon_break_job ON reconciliation_break (job_id);
CREATE INDEX IF NOT EXISTS idx_recon_break_status ON reconciliation_break (status);
CREATE INDEX IF NOT EXISTS idx_recon_break_type ON reconciliation_break (break_type);
CREATE INDEX IF NOT EXISTS idx_recon_break_entity ON reconciliation_break (entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_recon_break_created ON reconciliation_break (created_at);
CREATE INDEX IF NOT EXISTS idx_recon_break_assignee ON reconciliation_break (assignee_id);
CREATE INDEX IF NOT EXISTS idx_recon_break_due ON reconciliation_break (due_date) WHERE status IN ('OPEN', 'IN_PROGRESS');

CREATE TABLE IF NOT EXISTS reconciliation_rule (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    job_type VARCHAR(50) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    priority INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    match_keys JSONB NOT NULL,
    compare_fields JSONB NOT NULL,
    tolerance_config JSONB,
    break_type_mapping JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_recon_rule_job_type ON reconciliation_rule (job_type);
CREATE INDEX IF NOT EXISTS idx_recon_rule_active ON reconciliation_rule (active);
CREATE INDEX IF NOT EXISTS idx_recon_rule_priority ON reconciliation_rule (priority);

-- Default rules for DAILY_SETTLEMENT
INSERT INTO reconciliation_rule (name, description, job_type, entity_type, priority, active, match_keys, compare_fields, tolerance_config, break_type_mapping)
VALUES
    ('transfer_amount_match', 'Match transfers by reference and compare amounts', 'DAILY_SETTLEMENT', 'Transfer', 100, true,
     '{"reference": "reference"}',
     '{"amount": "amount", "currency": "currency", "status": "status", "fee": "fee"}',
     '{"absolute": "0.01", "percentage": "0.01"}',
     '{"amount": "AMOUNT_MISMATCH", "currency": "CURRENCY_MISMATCH", "status": "STATUS_MISMATCH", "fee": "FEE_MISMATCH"}'),
    ('pix_transfer_match', 'Match PIX transfers by end-to-end ID', 'DAILY_SETTLEMENT', 'PixTransfer', 90, true,
     '{"endToEndId": "endToEndId"}',
     '{"amount": "amount", "currency": "currency", "status": "status"}',
     '{"absolute": "0.01"}',
     '{"amount": "AMOUNT_MISMATCH", "currency": "CURRENCY_MISMATCH", "status": "STATUS_MISMATCH"}'),
    ('card_transaction_match', 'Match card transactions by authorization code', 'DAILY_SETTLEMENT', 'CardTransaction', 80, true,
     '{"authorizationCode": "authorizationCode"}',
     '{"amount": "amount", "currency": "currency", "merchant": "merchant", "fee": "fee"}',
     '{"absolute": "0.01", "percentage": "0.1"}',
     '{"amount": "AMOUNT_MISMATCH", "currency": "CURRENCY_MISMATCH", "fee": "FEE_MISMATCH"}'),
    ('account_balance_match', 'Match account balances', 'DAILY_SETTLEMENT', 'Account', 70, true,
     '{"accountId": "accountId"}',
     '{"balance": "balance", "availableBalance": "availableBalance", "currency": "currency"}',
     '{"absolute": "0.01"}',
     '{"balance": "AMOUNT_MISMATCH", "currency": "CURRENCY_MISMATCH"}')
ON CONFLICT (name) DO NOTHING;