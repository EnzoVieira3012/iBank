-- iBank Card Module Migration
-- Version: 6
-- Description: Creates tables for cards, transactions, invoices, and installments

-- Cards table
CREATE TABLE IF NOT EXISTS cards (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL REFERENCES accounts(id),
    number_hash VARCHAR(64) NOT NULL UNIQUE,
    last_four VARCHAR(4) NOT NULL,
    masked_number VARCHAR(19) NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('CREDIT', 'DEBIT', 'MULTIPLE', 'PREPAID')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'BLOCKED', 'CANCELLED', 'EXPIRED', 'PENDING_ACTIVATION', 'LOST', 'STOLEN')),
    holder_name VARCHAR(100) NOT NULL,
    expiry_date DATE NOT NULL,
    cvv_hash VARCHAR(64) NOT NULL,
    credit_limit NUMERIC(19,2),
    available_limit NUMERIC(19,2),
    daily_limit NUMERIC(19,2),
    monthly_limit NUMERIC(19,2),
    daily_used NUMERIC(19,2) DEFAULT 0,
    monthly_used NUMERIC(19,2) DEFAULT 0,
    last_daily_reset DATE,
    last_monthly_reset DATE,
    pin_hash VARCHAR(64),
    pin_tries INTEGER DEFAULT 0,
    contactless_enabled BOOLEAN DEFAULT TRUE,
    online_enabled BOOLEAN DEFAULT TRUE,
    international_enabled BOOLEAN DEFAULT FALSE,
    token_id VARCHAR(100),
    closed_at TIMESTAMP,
    closed_reason VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_cards_account_id ON cards(account_id);
CREATE INDEX IF NOT EXISTS idx_cards_number_hash ON cards(number_hash);
CREATE INDEX IF NOT EXISTS idx_cards_status ON cards(status);
CREATE INDEX IF NOT EXISTS idx_cards_type ON cards(type);

-- Card Transactions table
CREATE TABLE IF NOT EXISTS card_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    card_id UUID NOT NULL REFERENCES cards(id),
    type VARCHAR(20) NOT NULL CHECK (type IN ('PURCHASE', 'WITHDRAWAL', 'PAYMENT', 'REFUND', 'FEE', 'INTEREST', 'INSTALLMENT_PAYMENT')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('AUTHORIZED', 'CAPTURED', 'SETTLED', 'REVERSED', 'REFUNDED', 'DECLINED', 'PENDING', 'CANCELLED')),
    amount NUMERIC(19,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'BRL',
    merchant_name VARCHAR(200),
    merchant_category_code VARCHAR(4),
    merchant_city VARCHAR(100),
    merchant_country VARCHAR(2),
    authorization_code VARCHAR(20),
    nsu VARCHAR(20),
    tid VARCHAR(20),
    installments INTEGER,
    current_installment INTEGER,
    description VARCHAR(500),
    original_transaction_id UUID REFERENCES card_transactions(id),
    reversal_reason VARCHAR(255),
    processed_at TIMESTAMP,
    settled_at TIMESTAMP,
    idempotency_key VARCHAR(36) UNIQUE,
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_card_trans_card_id ON card_transactions(card_id);
CREATE INDEX IF NOT EXISTS idx_card_trans_status ON card_transactions(status);
CREATE INDEX IF NOT EXISTS idx_card_trans_type ON card_transactions(type);
CREATE INDEX IF NOT EXISTS idx_card_trans_created_at ON card_transactions(created_at);
CREATE INDEX IF NOT EXISTS idx_card_trans_merchant ON card_transactions(merchant_name);
CREATE INDEX IF NOT EXISTS idx_card_trans_auth_code ON card_transactions(authorization_code);
CREATE INDEX IF NOT EXISTS idx_card_trans_idempotency ON card_transactions(idempotency_key);

-- Card Invoices table
CREATE TABLE IF NOT EXISTS card_invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    card_id UUID NOT NULL REFERENCES cards(id),
    reference_month DATE NOT NULL,
    opening_date DATE NOT NULL,
    closing_date DATE NOT NULL,
    due_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('OPEN', 'CLOSED', 'PAID', 'PARTIALLY_PAID', 'OVERDUE', 'CANCELLED')),
    total_amount NUMERIC(19,2) NOT NULL,
    paid_amount NUMERIC(19,2) DEFAULT 0,
    minimum_payment NUMERIC(19,2),
    interest_amount NUMERIC(19,2) DEFAULT 0,
    fee_amount NUMERIC(19,2) DEFAULT 0,
    previous_balance NUMERIC(19,2) DEFAULT 0,
    paid_at TIMESTAMP,
    payment_method VARCHAR(50),
    payment_transaction_id UUID REFERENCES card_transactions(id),
    details JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    UNIQUE(card_id, reference_month)
);

CREATE INDEX IF NOT EXISTS idx_invoices_card_id ON card_invoices(card_id);
CREATE INDEX IF NOT EXISTS idx_invoices_status ON card_invoices(status);
CREATE INDEX IF NOT EXISTS idx_invoices_due_date ON card_invoices(due_date);
CREATE INDEX IF NOT EXISTS idx_invoices_reference_month ON card_invoices(reference_month);

-- Invoice Installments table
CREATE TABLE IF NOT EXISTS invoice_installments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id UUID NOT NULL REFERENCES card_invoices(id),
    installment_number INTEGER NOT NULL,
    total_installments INTEGER NOT NULL,
    due_date DATE NOT NULL,
    amount NUMERIC(19,2) NOT NULL,
    paid_amount NUMERIC(19,2) DEFAULT 0,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'PAID', 'OVERDUE', 'CANCELLED')),
    paid_at TIMESTAMP,
    payment_transaction_id UUID REFERENCES card_transactions(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_installments_invoice_id ON invoice_installments(invoice_id);
CREATE INDEX IF NOT EXISTS idx_installments_status ON invoice_installments(status);
CREATE INDEX IF NOT EXISTS idx_installments_due_date ON invoice_installments(due_date);