-- =============================================================================
-- V1__init_schema.sql
-- Transaction Orchestrator — Initial Schema
-- Normalized to 3NF
-- =============================================================================

-- -----------------------------------------------------------------------------
-- CUSTOMERS
-- Stores customer information associated with a transaction.
-- Separated from transactions to allow future reuse / normalization.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS customers (
    id                   UUID         NOT NULL DEFAULT gen_random_uuid(),
    document_type        VARCHAR(20)  NOT NULL,
    document_number      VARCHAR(50)  NOT NULL,
    country_calling_code VARCHAR(6)   NOT NULL,
    phone_number         VARCHAR(20)  NOT NULL,
    email                VARCHAR(254) NOT NULL,
    first_name           VARCHAR(100) NOT NULL,
    middle_name          VARCHAR(100),
    last_name            VARCHAR(100) NOT NULL,
    second_last_name     VARCHAR(100),

    CONSTRAINT pk_customers PRIMARY KEY (id)
);

-- -----------------------------------------------------------------------------
-- TRANSACTIONS
-- Core transaction table. Stores one record per payment attempt.
-- client_transaction_id must be unique (idempotency key from the client system).
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS transactions (
    id                    UUID         NOT NULL,
    client_transaction_id VARCHAR(100) NOT NULL,
    amount_cents          BIGINT       NOT NULL CHECK (amount_cents > 0),
    currency_code         VARCHAR(3)      NOT NULL,          -- ISO 4217
    country_code          VARCHAR(2)      NOT NULL,          -- ISO 3166-1 Alpha-2
    payment_method_id     VARCHAR(50)  NOT NULL,
    webhook_url           VARCHAR(500) NOT NULL,
    redirect_url          VARCHAR(500) NOT NULL,
    description           VARCHAR(255),
    expiration_seconds    BIGINT,
    status                VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    processed_at          TIMESTAMP,
    created_at            TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at            TIMESTAMP    NOT NULL DEFAULT now(),
    customer_id           UUID         NOT NULL,

    CONSTRAINT pk_transactions              PRIMARY KEY (id),
    CONSTRAINT uq_client_transaction_id     UNIQUE (client_transaction_id),
    CONSTRAINT fk_transactions_customer     FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT chk_status CHECK (status IN ('PENDING','PROCESSING','APPROVED','REJECTED','EXPIRED','FAILED','REVERSED'))
);

-- -----------------------------------------------------------------------------
-- INDEXES
-- -----------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_transactions_status        ON transactions (status);
CREATE INDEX IF NOT EXISTS idx_transactions_payment_method ON transactions (payment_method_id);
CREATE INDEX IF NOT EXISTS idx_transactions_created_at    ON transactions (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_customers_email            ON customers (email);
CREATE INDEX IF NOT EXISTS idx_customers_document        ON customers (document_type, document_number);
