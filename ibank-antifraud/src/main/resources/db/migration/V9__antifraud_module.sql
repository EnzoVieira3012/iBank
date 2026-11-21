-- iBank Antifraud Module Migration
-- Version: 9
-- Description: Creates tables for fraud rules, risk scores, device fingerprints, fraud alerts, fraud cases

-- Fraud Rules table
CREATE TABLE IF NOT EXISTS fraud_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    rule_type VARCHAR(30) NOT NULL CHECK (rule_type IN (
        'VELOCITY', 'AMOUNT_THRESHOLD', 'GEOGRAPHY', 'DEVICE_FINGERPRINT',
        'TIME_PATTERN', 'MERCHANT_CATEGORY', 'BENEFICIARY_PATTERN',
        'CHANNEL_ANOMALY', 'BEHAVIORAL_DEVIATION', 'VELOCITY_PIX',
        'VELOCITY_TRANSFER', 'VELOCITY_CARD', 'NEW_DEVICE',
        'NEW_BENEFICIARY', 'HIGH_RISK_COUNTRY', 'HIGH_RISK_MCC',
        'ROUND_AMOUNT', 'STRUCTURING', 'CUSTOM'
    )),
    action VARCHAR(20) NOT NULL CHECK (action IN (
        'ALLOW', 'ALERT', 'BLOCK', 'CHALLENGE', 'REVIEW', 'REQUIRE_2FA',
        'REDUCE_LIMIT', 'NOTIFY_CUSTOMER'
    )),
    priority INTEGER NOT NULL DEFAULT 100,
    conditions JSONB NOT NULL,
    score_impact INTEGER NOT NULL DEFAULT 0,
    valid_from DATE NOT NULL,
    valid_to DATE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    channel VARCHAR(20),
    product_code VARCHAR(50),
    customer_segment VARCHAR(30),
    created_by UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_fraud_rules_type ON fraud_rules(rule_type);
CREATE INDEX IF NOT EXISTS idx_fraud_rules_active ON fraud_rules(active);
CREATE INDEX IF NOT EXISTS idx_fraud_rules_priority ON fraud_rules(priority);
CREATE INDEX IF NOT EXISTS idx_fraud_rules_dates ON fraud_rules(valid_from, valid_to);

-- Risk Scores table
CREATE TABLE IF NOT EXISTS risk_scores (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL REFERENCES customers(id),
    account_id UUID REFERENCES accounts(id),
    card_id UUID REFERENCES cards(id),
    transaction_id UUID,
    score INTEGER NOT NULL CHECK (score >= 0 AND score <= 1000),
    risk_level VARCHAR(20) NOT NULL CHECK (risk_level IN (
        'VERY_LOW', 'LOW', 'MEDIUM', 'HIGH', 'VERY_HIGH', 'CRITICAL'
    )),
    factor_scores JSONB,
    triggered_rules JSONB,
    context JSONB,
    calculated_at TIMESTAMP NOT NULL,
    model_version VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_risk_scores_customer ON risk_scores(customer_id);
CREATE INDEX IF NOT EXISTS idx_risk_scores_account ON risk_scores(account_id);
CREATE INDEX IF NOT EXISTS idx_risk_scores_card ON risk_scores(card_id);
CREATE INDEX IF NOT EXISTS idx_risk_scores_transaction ON risk_scores(transaction_id);
CREATE INDEX IF NOT EXISTS idx_risk_scores_level ON risk_scores(risk_level);
CREATE INDEX IF NOT EXISTS idx_risk_scores_calculated ON risk_scores(calculated_at);

-- Device Fingerprints table
CREATE TABLE IF NOT EXISTS device_fingerprints (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL REFERENCES customers(id),
    device_hash VARCHAR(64) NOT NULL UNIQUE,
    device_id VARCHAR(100),
    user_agent VARCHAR(500),
    ip_address VARCHAR(45),
    country VARCHAR(2),
    city VARCHAR(100),
    os VARCHAR(50),
    browser VARCHAR(50),
    screen_resolution VARCHAR(20),
    timezone VARCHAR(50),
    language VARCHAR(10),
    canvas_fingerprint VARCHAR(100),
    webgl_fingerprint VARCHAR(100),
    fonts_hash VARCHAR(64),
    plugins_hash VARCHAR(64),
    battery_info VARCHAR(50),
    trust_level VARCHAR(20) NOT NULL CHECK (trust_level IN (
        'TRUSTED', 'KNOWN', 'NEW', 'SUSPICIOUS', 'BLOCKED'
    )),
    trust_score INTEGER,
    first_seen_at TIMESTAMP NOT NULL,
    last_seen_at TIMESTAMP NOT NULL,
    seen_count INTEGER NOT NULL DEFAULT 1,
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_device_customer ON device_fingerprints(customer_id);
CREATE INDEX IF NOT EXISTS idx_device_hash ON device_fingerprints(device_hash);
CREATE INDEX IF NOT EXISTS idx_device_trust ON device_fingerprints(trust_level);
CREATE INDEX IF NOT EXISTS idx_device_last_seen ON device_fingerprints(last_seen_at);

-- Fraud Alerts table
CREATE TABLE IF NOT EXISTS fraud_alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL REFERENCES customers(id),
    account_id UUID REFERENCES accounts(id),
    card_id UUID REFERENCES cards(id),
    transaction_id UUID,
    rule_id UUID REFERENCES fraud_rules(id),
    status VARCHAR(20) NOT NULL CHECK (status IN (
        'OPEN', 'IN_REVIEW', 'ESCALATED', 'FALSE_POSITIVE',
        'CONFIRMED_FRAUD', 'CLOSED', 'AUTO_CLOSED'
    )),
    severity VARCHAR(20) NOT NULL CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    title VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    risk_score INTEGER,
    risk_level VARCHAR(20) CHECK (risk_level IN (
        'VERY_LOW', 'LOW', 'MEDIUM', 'HIGH', 'VERY_HIGH', 'CRITICAL'
    )),
    triggered_rules JSONB,
    transaction_context JSONB,
    assigned_to UUID,
    assigned_at TIMESTAMP,
    reviewed_at TIMESTAMP,
    reviewed_by UUID,
    resolution VARCHAR(500),
    false_positive_reason VARCHAR(500),
    case_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_alerts_customer ON fraud_alerts(customer_id);
CREATE INDEX IF NOT EXISTS idx_alerts_account ON fraud_alerts(account_id);
CREATE INDEX IF NOT EXISTS idx_alerts_card ON fraud_alerts(card_id);
CREATE INDEX IF NOT EXISTS idx_alerts_transaction ON fraud_alerts(transaction_id);
CREATE INDEX IF NOT EXISTS idx_alerts_status ON fraud_alerts(status);
CREATE INDEX IF NOT EXISTS idx_alerts_severity ON fraud_alerts(severity);
CREATE INDEX IF NOT EXISTS idx_alerts_created ON fraud_alerts(created_at);
CREATE INDEX IF NOT EXISTS idx_alerts_assigned ON fraud_alerts(assigned_to);

-- Fraud Cases table
CREATE TABLE IF NOT EXISTS fraud_cases (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    case_number VARCHAR(30) NOT NULL UNIQUE,
    customer_id UUID NOT NULL REFERENCES customers(id),
    account_id UUID REFERENCES accounts(id),
    card_id UUID REFERENCES cards(id),
    status VARCHAR(20) NOT NULL CHECK (status IN (
        'OPEN', 'INVESTIGATING', 'PENDING_INFO', 'ESCALATED',
        'RESOLVED_FRAUD', 'RESOLVED_LEGITIMATE', 'CLOSED', 'SAR_FILED'
    )),
    title VARCHAR(200) NOT NULL,
    description VARCHAR(2000),
    total_amount NUMERIC(19,2),
    currency VARCHAR(3),
    transaction_count INTEGER,
    first_transaction_date DATE,
    last_transaction_date DATE,
    assigned_to UUID,
    assigned_at TIMESTAMP,
    investigator_notes TEXT,
    resolution VARCHAR(1000),
    resolved_at TIMESTAMP,
    resolved_by UUID,
    sar_filed BOOLEAN NOT NULL DEFAULT FALSE,
    sar_filed_at TIMESTAMP,
    sar_reference VARCHAR(50),
    escalated_at TIMESTAMP,
    escalated_to UUID,
    alert_ids JSONB,
    evidence JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_cases_customer ON fraud_cases(customer_id);
CREATE INDEX IF NOT EXISTS idx_cases_status ON fraud_cases(status);
CREATE INDEX IF NOT EXISTS idx_cases_assigned ON fraud_cases(assigned_to);
CREATE INDEX IF NOT EXISTS idx_cases_created ON fraud_cases(created_at);
CREATE INDEX IF NOT EXISTS idx_cases_sar ON fraud_cases(sar_filed);