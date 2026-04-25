-- =============================================================================
-- transaction_orchestrator_schema.sql
-- Prueba Técnica — TumiPay
-- Modelo Relacional Normalizado (3FN)
-- =============================================================================

-- Crear la base de datos (ejecutar como superusuario)
-- CREATE DATABASE orchestrator_db;
-- \c orchestrator_db

-- Habilitar extensión para gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- -----------------------------------------------------------------------------
-- TABLA: customers
-- Información del cliente asociada a la transacción.
-- Separada de transactions para normalización (evitar dependencias transitivas).
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS customers (
    id                   UUID         NOT NULL DEFAULT gen_random_uuid(),
    document_type        VARCHAR(20)  NOT NULL,       -- CC, NIT, PASSPORT, etc.
    document_number      VARCHAR(50)  NOT NULL,
    country_calling_code VARCHAR(6)   NOT NULL,       -- +57, +1, etc.
    phone_number         VARCHAR(20)  NOT NULL,       -- sin código de país, sin formato
    email                VARCHAR(254) NOT NULL,
    first_name           VARCHAR(100) NOT NULL,
    middle_name          VARCHAR(100),
    last_name            VARCHAR(100) NOT NULL,
    second_last_name     VARCHAR(100),

    CONSTRAINT pk_customers PRIMARY KEY (id)
);

COMMENT ON TABLE  customers                      IS 'Información del cliente asociado a una transacción de pago';
COMMENT ON COLUMN customers.document_type        IS 'Tipo de documento legal: CC, NIT, PASSPORT, CE, etc.';
COMMENT ON COLUMN customers.document_number      IS 'Número del documento legal sin formato';
COMMENT ON COLUMN customers.country_calling_code IS 'Código de llamada internacional en formato +XX o +XXX';
COMMENT ON COLUMN customers.phone_number         IS 'Número de teléfono sin código de país y sin formato';
COMMENT ON COLUMN customers.email                IS 'Correo electrónico del cliente (RFC 5321 — máx 254 chars)';

-- -----------------------------------------------------------------------------
-- TABLA: transactions
-- Registro principal de transacciones de pago.
--
-- Decisiones de diseño:
--   - id: UUID generado por el microservicio (no secuencia DB) para evitar
--     colisiones en arquitecturas distribuidas.
--   - client_transaction_id: clave de idempotencia provista por el cliente.
--   - amount_cents: monto en centavos (entero) para evitar errores de punto
--     flotante en operaciones financieras.
--   - currency_code / country_code: CHAR fijo por estándar ISO.
--   - status: restringido por CHECK constraint para integridad referencial
--     sin FK hacia una tabla catálogo (simplicidad en v1).
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
    expiration_seconds    BIGINT       CHECK (expiration_seconds > 0),
    status                VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    processed_at          TIMESTAMP WITH TIME ZONE,
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    customer_id           UUID         NOT NULL,

    CONSTRAINT pk_transactions              PRIMARY KEY (id),
    CONSTRAINT uq_client_transaction_id     UNIQUE (client_transaction_id),
    CONSTRAINT fk_transactions_customer     FOREIGN KEY (customer_id)
                                                REFERENCES customers (id)
                                                ON DELETE RESTRICT,
    CONSTRAINT chk_transaction_status       CHECK (status IN (
        'PENDING', 'PROCESSING', 'APPROVED', 'REJECTED', 'EXPIRED', 'FAILED', 'REVERSED'
    ))
);

COMMENT ON TABLE  transactions                      IS 'Transacciones de pago orquestadas por el microservicio';
COMMENT ON COLUMN transactions.id                   IS 'Identificador único asignado por el microservicio (UUID v4)';
COMMENT ON COLUMN transactions.client_transaction_id IS 'ID de la transacción en el sistema cliente — clave de idempotencia';
COMMENT ON COLUMN transactions.amount_cents         IS 'Monto de la transacción en centavos (sin separador decimal)';
COMMENT ON COLUMN transactions.currency_code        IS 'Código de moneda ISO 4217 — 3 letras (ej: COP, USD)';
COMMENT ON COLUMN transactions.country_code         IS 'Código de país ISO 3166-1 Alpha-2 — 2 letras (ej: CO, US)';
COMMENT ON COLUMN transactions.payment_method_id    IS 'Identificador del método/proveedor de pago';
COMMENT ON COLUMN transactions.status               IS 'Estado de la transacción: PENDING → PROCESSING → APPROVED/REJECTED/FAILED';
COMMENT ON COLUMN transactions.processed_at         IS 'Fecha y hora en que se envió la transacción al proveedor';

-- -----------------------------------------------------------------------------
-- ÍNDICES
-- -----------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_transactions_status         ON transactions (status);
CREATE INDEX IF NOT EXISTS idx_transactions_payment_method ON transactions (payment_method_id);
CREATE INDEX IF NOT EXISTS idx_transactions_created_at     ON transactions (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_customers_email             ON customers (email);
CREATE INDEX IF NOT EXISTS idx_customers_document          ON customers (document_type, document_number);

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
-- DATOS DE EJEMPLO (opcional, para pruebas)
-- =============================================================================
/*
INSERT INTO customers (document_type, document_number, country_calling_code, phone_number, email,
                       first_name, last_name)
VALUES ('CC', '1234567890', '+57', '3001234567', 'juan.perez@example.com', 'Juan', 'Perez');

INSERT INTO transactions (id, client_transaction_id, amount_cents, currency_code, country_code,
                          payment_method_id, webhook_url, redirect_url, status, customer_id)
SELECT gen_random_uuid(), 'EXT-001', 150000, 'COP', 'CO',
       'MOCK_PSP', 'https://mi-app.co/webhook', 'https://mi-app.co/return',
       'APPROVED', id
FROM customers WHERE document_number = '1234567890';
*/
