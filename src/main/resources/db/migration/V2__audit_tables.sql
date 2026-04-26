-- =============================================================================
-- V2__audit_tables.sql
-- Transaction Orchestrator — Audit Tables
-- Tracks all changes to customers and transactions for compliance
-- =============================================================================

-- -----------------------------------------------------------------------------
-- CUSTOMER_AUDIT
-- Tracks all changes to customer records (INSERT, UPDATE, DELETE)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS customer_audit (
    id                   UUID         NOT NULL DEFAULT gen_random_uuid(),
    customer_id          UUID         NOT NULL,
    action               VARCHAR(10)  NOT NULL,  -- INSERT, UPDATE, DELETE
    document_type        VARCHAR(20),
    document_number      VARCHAR(50),
    country_calling_code VARCHAR(6),
    phone_number         VARCHAR(20),
    email                VARCHAR(254),
    first_name           VARCHAR(100),
    middle_name          VARCHAR(100),
    last_name            VARCHAR(100),
    second_last_name     VARCHAR(100),
    changed_by           VARCHAR(100),            -- User or system that made the change
    changed_at           TIMESTAMP    NOT NULL DEFAULT now(),
    transaction_id       UUID,                    -- Optional reference to related transaction

    CONSTRAINT pk_customer_audit PRIMARY KEY (id),
    CONSTRAINT fk_customer_audit_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT chk_customer_audit_action CHECK (action IN ('INSERT', 'UPDATE', 'DELETE'))
);

-- -----------------------------------------------------------------------------
-- TRANSACTION_AUDIT
-- Tracks all changes to transaction records (INSERT, UPDATE, DELETE)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS transaction_audit (
    id                    UUID         NOT NULL DEFAULT gen_random_uuid(),
    transaction_id        UUID         NOT NULL,
    action                VARCHAR(10)  NOT NULL,  -- INSERT, UPDATE, DELETE
    client_transaction_id VARCHAR(100),
    amount_cents          BIGINT,
    currency_code         VARCHAR(3),
    country_code          VARCHAR(2),
    payment_method_id     VARCHAR(50),
    webhook_url           VARCHAR(500),
    redirect_url          VARCHAR(500),
    description           VARCHAR(255),
    expiration_seconds    BIGINT,
    status                VARCHAR(20),
    old_status            VARCHAR(20),             -- Previous status for UPDATE actions
    processed_at          TIMESTAMP,
    customer_id           UUID,
    changed_by            VARCHAR(100),            -- User or system that made the change
    changed_at            TIMESTAMP    NOT NULL DEFAULT now(),

    CONSTRAINT pk_transaction_audit PRIMARY KEY (id),
    CONSTRAINT fk_transaction_audit_transaction FOREIGN KEY (transaction_id) REFERENCES transactions (id),
    CONSTRAINT chk_transaction_audit_action CHECK (action IN ('INSERT', 'UPDATE', 'DELETE'))
);

-- -----------------------------------------------------------------------------
-- INDEXES for efficient querying
-- -----------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_customer_audit_customer_id ON customer_audit (customer_id);
CREATE INDEX IF NOT EXISTS idx_customer_audit_changed_at   ON customer_audit (changed_at DESC);
CREATE INDEX IF NOT EXISTS idx_customer_audit_action       ON customer_audit (action);

CREATE INDEX IF NOT EXISTS idx_transaction_audit_transaction_id ON transaction_audit (transaction_id);
CREATE INDEX IF NOT EXISTS idx_transaction_audit_changed_at     ON transaction_audit (changed_at DESC);
CREATE INDEX IF NOT EXISTS idx_transaction_audit_action         ON transaction_audit (action);
CREATE INDEX IF NOT EXISTS idx_transaction_audit_status         ON transaction_audit (status);
