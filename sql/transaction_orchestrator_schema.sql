-- =============================================================================
-- transaction_orchestrator_schema.sql
-- Prueba Técnica — TumiPay
-- =============================================================================

-- Crear la base de datos
CREATE DATABASE orchestrator_db;

-- Habilitar extensión para gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- -----------------------------------------------------------------------------
-- TABLA: customers
-- Entidad cliente con identidad propia. Puede tener múltiples transacciones.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS customers (
    id                   UUID         NOT NULL DEFAULT gen_random_uuid(),
    document_type        VARCHAR(20)  NOT NULL,       -- CC, NIT, PASSPORT, etc.
    document_number      VARCHAR(50)  NOT NULL,
    country_calling_code VARCHAR(6)   NOT NULL,       -- +57, +1, etc.
    phone_number         VARCHAR(20)  NOT NULL,       -- sin código de país
    email                VARCHAR(254) NOT NULL,
    first_name           VARCHAR(100) NOT NULL,
    middle_name          VARCHAR(100),
    last_name            VARCHAR(100) NOT NULL,
    second_last_name     VARCHAR(100),

    CONSTRAINT pk_customers PRIMARY KEY (id),
    CONSTRAINT uq_customers_document UNIQUE (document_type, document_number)
);

COMMENT ON TABLE  customers                      IS 'Clientes del sistema de pagos - entidad con identidad propia';
COMMENT ON COLUMN customers.document_type        IS 'Tipo de documento legal: CC, NIT, PASSPORT, CE, etc.';
COMMENT ON COLUMN customers.document_number      IS 'Número del documento legal sin formato';
COMMENT ON COLUMN customers.email                IS 'Correo electrónico del cliente (RFC 5321 — máx 254 chars)';

-- -----------------------------------------------------------------------------
-- TABLA: transactions
-- Registro principal de transacciones de pago.
--
-- Decisiones de diseño:
--   - id: UUID generado por el microservicio (no secuencia DB) para evitar
--     colisiones en arquitecturas distribuidas.
--   - client_transaction_id: clave de idempotencia provista por el cliente.
--   - customer_id: FK a customers (un cliente puede tener muchas transacciones).
--   - version: optimistic locking para concurrencia.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS transactions (
    id                    UUID         NOT NULL,
    client_transaction_id VARCHAR(100) NOT NULL,
    amount_cents          BIGINT       NOT NULL CHECK (amount_cents > 0),
    currency_code         VARCHAR(3)   NOT NULL,          -- ISO 4217
    country_code          VARCHAR(2)   NOT NULL,          -- ISO 3166-1 Alpha-2
    payment_method_id     VARCHAR(50)  NOT NULL,
    webhook_url           VARCHAR(500) NOT NULL,
    redirect_url          VARCHAR(500) NOT NULL,
    description           VARCHAR(255),
    expiration_seconds    BIGINT       CHECK (expiration_seconds > 0),
    status                VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    processed_at          TIMESTAMP WITH TIME ZONE,
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    customer_id           UUID         NOT NULL,
    version               INTEGER      NOT NULL DEFAULT 0,

    CONSTRAINT pk_transactions              PRIMARY KEY (id),
    CONSTRAINT uq_client_transaction_id     UNIQUE (client_transaction_id),
    CONSTRAINT fk_transactions_customer     FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT chk_transaction_status       CHECK (status IN (
        'PENDING', 'PROCESSING', 'APPROVED', 'REJECTED', 'EXPIRED', 'FAILED', 'REVERSED'
    ))
);

COMMENT ON TABLE  transactions                      IS 'Transacciones de pago orquestadas por el microservicio';
COMMENT ON COLUMN transactions.id                   IS 'Identificador único asignado por el microservicio (UUID v4)';
COMMENT ON COLUMN transactions.client_transaction_id  IS 'ID de la transacción en el sistema cliente — clave de idempotencia';
COMMENT ON COLUMN transactions.customer_id            IS 'FK al cliente (un cliente puede tener múltiples transacciones)';

-- -----------------------------------------------------------------------------
-- ÍNDICES
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

-- =============================================================================
-- TABLAS DE AUDITORIA
-- Rastrean todos los cambios a clientes y transacciones para compliance
-- =============================================================================

-- -----------------------------------------------------------------------------
-- CUSTOMER_AUDIT
-- Rastrea todos los cambios a registros de cliente (INSERT, UPDATE, DELETE)
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
    changed_by           VARCHAR(100),            -- Usuario o sistema que hizo el cambio
    changed_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    transaction_id       UUID,                    -- Referencia opcional a transacción relacionada

    CONSTRAINT pk_customer_audit PRIMARY KEY (id),
    CONSTRAINT fk_customer_audit_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT chk_customer_audit_action CHECK (action IN ('INSERT', 'UPDATE', 'DELETE'))
);

-- -----------------------------------------------------------------------------
-- TRANSACTION_AUDIT
-- Rastrea todos los cambios a registros de transacción (INSERT, UPDATE, DELETE)
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
    old_status            VARCHAR(20),             -- Status anterior para acciones UPDATE
    processed_at          TIMESTAMP WITH TIME ZONE,
    customer_id           UUID,
    changed_by            VARCHAR(100),            -- Usuario o sistema que hizo el cambio
    changed_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT pk_transaction_audit PRIMARY KEY (id),
    CONSTRAINT fk_transaction_audit_transaction FOREIGN KEY (transaction_id) REFERENCES transactions (id),
    CONSTRAINT chk_transaction_audit_action CHECK (action IN ('INSERT', 'UPDATE', 'DELETE'))
);

-- -----------------------------------------------------------------------------
-- INDICES para consultas eficientes en tablas de auditoría
-- -----------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_customer_audit_customer_id ON customer_audit (customer_id);
CREATE INDEX IF NOT EXISTS idx_customer_audit_changed_at   ON customer_audit (changed_at DESC);
CREATE INDEX IF NOT EXISTS idx_customer_audit_action       ON customer_audit (action);

CREATE INDEX IF NOT EXISTS idx_transaction_audit_transaction_id ON transaction_audit (transaction_id);
CREATE INDEX IF NOT EXISTS idx_transaction_audit_changed_at     ON transaction_audit (changed_at DESC);
CREATE INDEX IF NOT EXISTS idx_transaction_audit_action         ON transaction_audit (action);
CREATE INDEX IF NOT EXISTS idx_transaction_audit_status         ON transaction_audit (status);

-- =============================================================================
-- DATOS DE EJEMPLO (opcional, para pruebas)
-- Customer es una tabla separada - se inserta primero, luego se referencia
-- =============================================================================
/*
-- Insertar cliente primero
INSERT INTO customers (document_type, document_number, country_calling_code,
                       phone_number, email, first_name, last_name)
VALUES ('CC', '1234567890', '+57', '3001234567', 'juan.perez@example.com', 'Juan', 'Perez')
RETURNING id;

-- Insertar transacción referenciando al cliente
INSERT INTO transactions (id, client_transaction_id, amount_cents, currency_code, country_code,
                          payment_method_id, webhook_url, redirect_url, status, customer_id)
SELECT gen_random_uuid(), 'EXT-001', 150000, 'COP', 'CO',
       'MOCK_PSP', 'https://mi-app.co/webhook', 'https://mi-app.co/return',
       'APPROVED', id
FROM customers WHERE document_number = '1234567890';
*/
