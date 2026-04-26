-- =============================================================================
-- V1__init_schema.sql
-- Transaction Orchestrator — Initial Schema


--
-- Decision de diseño: Customer es una Entity con identidad propia.
-- Un cliente puede tener múltiples transacciones. Se normaliza en tabla separada
-- para evitar duplicación de datos y permitir historial consolidado del cliente.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- CUSTOMERS
-- Tabla de clientes con identidad propia. Puede tener múltiples transacciones.
-- Idempotencia: documento (tipo+número) es único.
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

    CONSTRAINT pk_customers PRIMARY KEY (id),
    CONSTRAINT uq_customers_document UNIQUE (document_type, document_number)
);

-- -----------------------------------------------------------------------------
-- TRANSACTIONS
-- Core transaction table. Stores one record per payment attempt.
-- Referencia a customers mediante FK (un cliente puede tener muchas transacciones).
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS transactions (
    id                    UUID         NOT NULL,
    client_transaction_id VARCHAR(100) NOT NULL,
    amount_cents          BIGINT       NOT NULL CHECK (amount_cents > 0),
    currency_code         VARCHAR(3)   NOT NULL,
    country_code          VARCHAR(2)   NOT NULL,
    payment_method_id     VARCHAR(50)  NOT NULL,
    webhook_url           VARCHAR(500) NOT NULL,
    redirect_url          VARCHAR(500) NOT NULL,
    description           VARCHAR(255),
    expiration_seconds    BIGINT,
    status                VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    processed_at          TIMESTAMP WITH TIME ZONE ,
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    customer_id           UUID         NOT NULL,
    version               INTEGER      NOT NULL DEFAULT 0,

    CONSTRAINT pk_transactions              PRIMARY KEY (id),
    CONSTRAINT uq_client_transaction_id     UNIQUE (client_transaction_id),
    CONSTRAINT fk_transactions_customer     FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT chk_status CHECK (status IN ('PENDING','PROCESSING','APPROVED','REJECTED','EXPIRED','FAILED','REVERSED'))
);

-- -----------------------------------------------------------------------------
-- INDEXES
-- -----------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_transactions_status              ON transactions (status);
CREATE INDEX IF NOT EXISTS idx_transactions_payment_method      ON transactions (payment_method_id);
CREATE INDEX IF NOT EXISTS idx_transactions_created_at          ON transactions (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_customers_document               ON customers (document_type, document_number);

-- -----------------------------------------------------------------------------
-- TRIGGER: actualizar updated_at automáticamente
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_transactions_updated_at
    BEFORE UPDATE ON transactions
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
