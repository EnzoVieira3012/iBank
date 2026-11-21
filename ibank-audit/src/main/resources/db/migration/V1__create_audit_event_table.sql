-- iBank Audit - Initial schema
-- Creates the audit_event table with indexes for common query patterns

CREATE TABLE IF NOT EXISTS audit_event (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100) NOT NULL,
    user_id VARCHAR(100),
    session_id VARCHAR(100),
    correlation_id VARCHAR(100),
    causation_id VARCHAR(100),
    payload JSONB,
    metadata JSONB,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    checksum VARCHAR(64) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_audit_event_entity ON audit_event (entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_event_timestamp ON audit_event (occurred_at);
CREATE INDEX IF NOT EXISTS idx_audit_event_user ON audit_event (user_id);
CREATE INDEX IF NOT EXISTS idx_audit_event_correlation ON audit_event (correlation_id);
CREATE INDEX IF NOT EXISTS idx_audit_event_checksum ON audit_event (checksum);
CREATE INDEX IF NOT EXISTS idx_audit_event_type_time ON audit_event (event_type, occurred_at);