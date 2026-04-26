# Transaction Orchestrator Microservice

**Prueba Técnica — TumiPay | IT-TEST-001**

Microservicio de orquestación de transacciones de pago construido con **Spring Boot 3** y **Arquitectura Hexagonal (Ports & Adapters)**.

---

## Tabla de Contenidos

1. [Arquitectura](#arquitectura)
2. [Patrones de Diseño Aplicados](#patrones-de-diseño-aplicados)
3. [Estructura del Proyecto](#estructura-del-proyecto)
4. [Contrato API](#contrato-api)
5. [Modelo de Base de Datos](#modelo-de-base-de-datos)
6. [Integración Continua](#integración-continua)
7. [Calidad de Código](#calidad-de-código)
8. [Cómo Ejecutar](#cómo-ejecutar)
9. [Decisiones Arquitectónicas](#decisiones-arquitectónicas)
10. [Suposiciones](#suposiciones)
11. [Riesgos Identificados](#riesgos-identificados)

---

## Arquitectura

El microservicio sigue el modelo de **Arquitectura Hexagonal** (también conocida como Ports & Adapters o Clean Architecture):

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            INFRASTRUCTURE                                   │
│                                                                             │
│  ┌──────────────────┐                    ┌──────────────────────────────┐   │
│  │  Inbound Adapter │                    │      Outbound Adapters       │   │
│  │  (REST / HTTP)   │                    │                              │   │
│  │                  │                    │  ┌────────────────────────┐  │   │
│  │  TransactionCon- │                    │  │ TransactionPersistence │  │   │
│  │  troller         │                    │  │ Adapter (JPA)          │  │   │
│  └────────┬─────────┘                    │  └────────────────────────┘  │   │
│           │                              │                              │   │
│           │ calls                        │  ┌────────────────────────┐  │   │
│           ▼                              │  │ CustomerRepository     │  │   │
│  ┌─────────────────────────────┐       │  │ (JPA)                  │  │   │
│  │        APPLICATION          │       │  └────────────────────────┘  │   │
│  │                             │       │                              │   │
│  │  ┌───────────────────────┐  │       │  ┌────────────────────────┐  │   │
│  │  │ TransactionService    │  │       │  │ MockPaymentProvider    │  │   │
│  │  │ (TransactionUseCase)  │──┼───────┼─▶│ Adapter (PSP)          │  │   │
│  │  └───────────────────────┘  │       │  └────────────────────────┘  │   │
│  │                             │       │                              │   │
│  │  ┌───────────────────────┐  │       │  ┌────────────────────────┐  │   │
│  │  │ AuditService          │  │       │  │ Resilience4jCircuit    │  │   │
│  │  │ (AuditUseCase)        │──┼───────┼─▶│ BreakerAdapter           │  │   │
│  │  └───────────────────────┘  │       │  └────────────────────────┘  │   │
│  │                             │       │                              │   │
│  └──────────────┬──────────────┘       │  ┌────────────────────────┐  │   │
│                 │                      │  │ KafkaAuditPublisher    │  │   │
│       ┌─────────▼─────────┐            │  │ Adapter (Kafka)        │  │   │
│       │      DOMAIN       │            │  └────────────────────────┘  │   │
│       │                   │            │                              │   │
│       │  ┌─────────────┐  │            │  ┌────────────────────────┐  │   │
│       │  │ Transaction │  │            │  │ AuditPersistence       │  │   │
│       │  │ Customer    │  │            │  │ Adapter (JPA)          │  │   │
│       │  │ Transaction │  │            │  └────────────────────────┘  │   │
│       │  │ Status      │  │            └──────────────────────────────┘   │
│       │  │ AuditEvent  │  │                                                 │
│       │  │ (Ports)     │  │                                                 │
│       │  └─────────────┘  │                                                 │
│       └───────────────────┘                                                 │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Capas

| Capa | Responsabilidad |
|------|----------------|
| **Domain** | Entidades, Value Objects, lógica de negocio, interfaces de puertos (in/out) |
| **Application** | Casos de uso, orquestación del flujo, sin dependencias de frameworks |
| **Infrastructure** | Adaptadores HTTP, JPA, proveedores de pago, manejo de excepciones |

---

## Patrones de Diseño Aplicados

| Patrón | Dónde | Por qué |
|--------|-------|---------|
| **Ports & Adapters** | Toda la arquitectura | Desacoplar el dominio de la infraestructura |
| **Strategy + Registry** | `TransactionService` + proveedores | Agregar nuevos PSPs sin modificar código existente |
| **Factory Method** | `Transaction.create()` | Encapsular la construcción del agregado con lógica de inicialización |
| **Command** | `CreateTransactionCommand` | Transportar datos de la petición HTTP hacia el caso de uso |
| **Repository** | `TransactionRepository`, `CustomerRepository` (ports) | Abstraer el acceso a datos |
| **Mapper** (MapStruct) | `TransactionRestMapper`, `TransactionPersistenceMapper` | Convertir entre capas sin acoplamiento |
| **Facade** | `TransactionController` | Simplificar la interfaz HTTP al caso de uso |
| **Template Method** | `GlobalExceptionHandler` | Manejar familias de excepciones de forma uniforme |
| **Circuit Breaker** | `CircuitBreakerPort` + `Resilience4jCircuitBreakerAdapter` | Tolerancia a fallos con proveedores de pago |
| **Event-Driven** | `AuditService` + `KafkaAuditPublisherAdapter` | Auditoría asíncrona desacoplada |
| **Transactional Outbox** | `AuditService` (afterCommit) | Garantizar consistencia eventual de auditoría |
| **Anti-Corruption Layer** | Mappers entre capas | Aislar el modelo de dominio de modelos externos |

---

## Estructura del Proyecto

```
src/main/java/com/tumipay/orchestrator/
├── domain/
│   ├── model/
│   │   ├── Transaction.java              ← Aggregate Root
│   │   ├── Customer.java                 ← Entity
│   │   ├── TransactionStatus.java        ← Enum de estados
│   │   ├── AuditAction.java              ← Enum de acciones de auditoría
│   │   ├── TransactionAuditEvent.java    ← Evento de dominio
│   │   └── CustomerAuditEvent.java       ← Evento de dominio
│   ├── port/
│   │   ├── in/
│   │   │   ├── TransactionUseCase.java   ← Puerto primario
│   │   │   ├── AuditUseCase.java         ← Puerto primario (auditoría)
│   │   │   └── CreateTransactionCommand.java
│   │   └── out/
│   │       ├── TransactionRepository.java      ← Puerto secundario
│   │       ├── CustomerRepository.java           ← Puerto secundario
│   │       ├── PaymentProviderPort.java          ← Puerto secundario
│   │       ├── CircuitBreakerPort.java         ← Puerto secundario (resiliencia)
│   │       ├── AuditPublisherPort.java         ← Puerto secundario (auditoría)
│   │       └── AuditRepositoryPort.java        ← Puerto secundario (auditoría)
│   └── exception/
│       ├── TransactionNotFoundException.java
│       ├── DuplicateTransactionException.java
│       └── PaymentProviderNotFoundException.java
├── application/
│   └── service/
│       ├── TransactionService.java       ← Implementa TransactionUseCase
│       └── AuditService.java             ← Implementa AuditUseCase
└── infrastructure/
    ├── adapter/
    │   ├── in/rest/
    │   │   ├── controller/TransactionController.java
    │   │   ├── dto/request/
    │   │   ├── dto/response/
    │   │   └── mapper/TransactionRestMapper.java
    │   └── out/
    │       ├── persistence/
    │       │   ├── TransactionPersistenceAdapter.java
    │       │   ├── CustomerPersistenceAdapter.java
    │       │   ├── AuditPersistenceAdapter.java    ← Persistencia de auditoría
    │       │   ├── entity/TransactionEntity.java
    │       │   ├── entity/CustomerEntity.java
    │       │   ├── entity/TransactionAuditEntity.java
    │       │   ├── entity/CustomerAuditEntity.java
    │       │   ├── repository/JpaTransactionRepository.java
    │       │   ├── repository/JpaCustomerRepository.java
    │       │   ├── repository/TransactionAuditJpaRepository.java
    │       │   ├── repository/CustomerAuditJpaRepository.java
    │       │   └── mapper/TransactionPersistenceMapper.java
    │       ├── provider/
    │       │   └── MockPaymentProviderAdapter.java
    │       ├── resilience/
    │       │   └── Resilience4jCircuitBreakerAdapter.java  ← Circuit Breaker
    │       └── messaging/kafka/
    │           ├── KafkaAuditPublisherAdapter.java         ← Publicación de auditoría
    │           └── KafkaAuditConsumer.java
    └── exception/
        └── GlobalExceptionHandler.java
```

---

## Contrato API

### Base URL
```
http://localhost:8080/v1/transactions
```

### POST /transactions — Crear Transacción

**Request:**
```json
POST /v1/transactions
Content-Type: application/json

{
  "client_transaction_id": "EXT-REF-001",
  "amount_cents": 150000,
  "currency_code": "COP",
  "country_code": "CO",
  "payment_method_id": "MOCK_PSP",
  "webhook_url": "https://mi-app.co/webhook",
  "redirect_url": "https://mi-app.co/return",
  "description": "Compra en línea",
  "expiration_seconds": 1800,
  "customer": {
    "document_type": "CC",
    "document_number": "1234567890",
    "country_calling_code": "+57",
    "phone_number": "3001234567",
    "email": "cliente@example.com",
    "first_name": "Juan",
    "middle_name": "Carlos",
    "last_name": "Perez",
    "second_last_name": "Lopez"
  }
}
```

**Response 201 Created:**
```json
{
  "response_code": "000",
  "response_message": "Successful operation",
  "data": {
    "transaction_id": "550e8400-e29b-41d4-a716-446655440000",
    "processed_at": "2026-04-24T10:30:00",
    "client_transaction_id": "EXT-REF-001",
    "payment_method_id": "MOCK_PSP",
    "currency_code": "COP",
    "country_code": "CO",
    "description": "Compra en línea",
    "status": "APPROVED"
  }
}
```

### GET /v1/transactions/{transaction_id} — Consultar Transacción

```
GET /v1/transactions/550e8400-e29b-41d4-a716-446655440000
```

**Response 200 OK:** (misma estructura `data` de arriba)

### Diccionario de Códigos de Error

| Código | HTTP | Descripción |
|--------|------|-------------|
| `000` | 201/200 | Operación exitosa |
| `001` | 422 | Error de validación (campo inválido u obligatorio faltante) |
| `002` | 409 | `client_transaction_id` duplicado |
| `003` | 404 | Transacción no encontrada |
| `004` | 400 | Proveedor de pago no disponible para el método indicado |
| `005` | 500 | Error interno del servidor |
| `006` | 409 | Conflicto de concurrencia (transacción modificada por otro proceso) |

---

## Modelo de Base de Datos

```
┌─────────────────────────────────────┐      ┌──────────────────────────────────────┐
│           customers                 │      │         customer_audit               │
├─────────────────────────────────────┤      ├──────────────────────────────────────┤
│ PK id                UUID           │◄─────│ FK customer_id       UUID            │
│ UQ document_type     VARCHAR(20)    │      │    action            VARCHAR(10)     │
│ UQ document_number   VARCHAR(50)    │      │    document_type     VARCHAR(20)     │
│    country_calling_code VARCHAR(6)  │      │    document_number   VARCHAR(50)     │
│    phone_number      VARCHAR(20)    │      │    email             VARCHAR(254)    │
│ UQ email             VARCHAR(254)   │      │    changed_by        VARCHAR(100)    │
│    first_name        VARCHAR(100)   │      │    changed_at        TIMESTAMP       │
│    middle_name       VARCHAR(100)   │      │    transaction_id    UUID            │
│    last_name         VARCHAR(100)   │      └──────────────────────────────────────┘
│    second_last_name  VARCHAR(100)   │
└──────────────────┬──────────────────┘
                   │ 1
                   │
                   │ *
┌──────────────────▼──────────────────┐      ┌──────────────────────────────────────┐
│           transactions              │      │       transaction_audit              │
├─────────────────────────────────────┤      ├──────────────────────────────────────┤
│ PK id                UUID           │◄─────│ FK transaction_id    UUID            │
│ UQ client_transaction_id VARCHAR    │      │    action            VARCHAR(10)     │
│    amount_cents      BIGINT         │      │    client_trans_id   VARCHAR(100)  │
│    currency_code     CHAR(3)        │      │    amount_cents      BIGINT          │
│    country_code      CHAR(2)        │      │    currency_code     VARCHAR(3)      │
│    payment_method_id VARCHAR(50)    │      │    status            VARCHAR(20)     │
│    webhook_url       VARCHAR(500)   │      │    old_status        VARCHAR(20)     │
│    redirect_url      VARCHAR(500)   │      │    customer_id       UUID            │
│    description       VARCHAR(255)   │      │    changed_by        VARCHAR(100)    │
│    expiration_seconds BIGINT        │      │    changed_at        TIMESTAMP       │
│    status            VARCHAR(20)   │      └──────────────────────────────────────┘
│    processed_at      TIMESTAMP TZ   │
│    created_at        TIMESTAMP TZ   │
│    updated_at        TIMESTAMP TZ   │
│ FK customer_id       UUID           │
│    version           INTEGER        │
└─────────────────────────────────────┘
```

**Diseño DDD:** Customer es una **Entity** (tiene identidad propia). Un cliente puede tener múltiples transacciones (relación 1:N). Se busca/crea por documento (tipo+número) o email para mantener idempotencia y evitar duplicados.

**Tablas de Auditoría:** Las tablas `customer_audit` y `transaction_audit` registran todos los cambios (INSERT, UPDATE, DELETE) para cumplimiento normativo y trazabilidad completa.

*Beneficios:* historial consolidado del cliente, KYC, límites de riesgo, análisis de comportamiento de compra, auditoría completa para compliance.

Los scripts SQL están en:
- `sql/transaction_orchestrator_schema.sql` — Schema completo
- `src/main/resources/db/migration/V1__init_schema.sql` — Migración inicial (Flyway)
- `src/main/resources/db/migration/V2__audit_tables.sql` — Migración de tablas de auditoría

---

## Integración Continua

Herramienta: **GitHub Actions** (`.github/workflows/ci-cd.yml`)

Pipeline de 5 etapas:

```
Push/PR → [1] Build & Test → [2] Code Quality → [3] Docker Build
                                                       │
                                              ┌────────┴────────┐
                                              ▼                 ▼
                                        [4] Deploy         [5] Deploy
                                          Staging         Production
                                        (develop)           (main)
```

1. **Build & Test** — Maven + JaCoCo (cobertura mínima 80%) con PostgreSQL en servicio
2. **Code Quality** — SonarQube (análisis estático, code smells, seguridad)
3. **Docker Build** — Imagen multi-stage, publicada en GitHub Container Registry
4. **Deploy Staging** — `kubectl set image` en ambiente de staging (rama `develop`)
5. **Deploy Production** — Despliegue con aprobación manual (rama `main`, environment protegido)

---

## Calidad de Código

### Estrategia de Testing

| Tipo | Herramienta | Cobertura objetivo |
|------|-------------|-------------------|
| Unitarios | JUnit 5 + Mockito | 80%+ líneas de negocio |
| Integración | Spring Boot Test + Testcontainers (PostgreSQL + Kafka) | Flujos completos con BD y mensajería reales |
| Contrato | Spring MVC Test (`@WebMvcTest`) | Todos los endpoints |
| Componentes | `@SpringBootTest` | AuditService, CircuitBreaker, Mappers |

### Tests Implementados

- `TransactionServiceTest` — Unitarios del servicio de transacciones (mock de repositorios, proveedores, circuit breaker)
- `AuditServiceTest` — Unitarios de publicación de auditoría con verificación de TransactionSynchronization
- `TransactionPersistenceMapperTest` — Verificación de conversión entre entidades JPA y objetos de dominio
- `Resilience4jCircuitBreakerAdapterTest` — Tests de integración del Circuit Breaker
- `TransactionControllerIntegrationTest` — Tests de integración de endpoints REST

### Herramientas de Calidad

- **JaCoCo** — Reporte de cobertura, umbral mínimo configurable en `pom.xml`
- **SonarQube** — Análisis estático: bugs, vulnerabilidades, code smells, duplicaciones
- **Checkstyle / SpotBugs** — Estilo de código y análisis de bugs estáticos (configurables)
- **Dependabot / OWASP Dependency Check** — Vulnerabilidades en dependencias
- **Lombok + MapStruct** — Reducir código boilerplate, disminuyendo superficie de error

### Buenas Prácticas Aplicadas

- Inmutabilidad en entidades de dominio (`@Builder` + sin setters)
- Validaciones en capa HTTP (`@Valid`, `@NotBlank`, `@Pattern`, etc.)
- Manejo centralizado de excepciones (`@RestControllerAdvice`)
- Logging estructurado con niveles apropiados (`@Slf4j`)
- Transaccionalidad declarativa (`@Transactional`)
- Publicación de eventos después del commit (`TransactionSynchronization`)
- Null-safety con `Objects.requireNonNull` en adaptadores

---

## Cómo Ejecutar

### Prerrequisitos
- Java 17+
- Maven 3.9+
- Docker & Docker Compose

### Opción 1: Docker Compose (recomendado)
```bash
git clone <repo>
cd transaction-orchestrator
docker-compose up -d
```

### Opción 2: Local con Maven
```bash
# Iniciar solo la BD
docker-compose up -d db

# Ejecutar la aplicación
mvn spring-boot:run \
  -Dspring-boot.run.arguments="--DB_HOST=localhost --DB_USER=orchestrator --DB_PASS=orchestrator"
```

### Documentación API
```
http://localhost:8080/api/swagger-ui.html
```

### Ejecutar Tests
```bash
mvn test                    # Unitarios
mvn verify                  # Unitarios + integración + cobertura
```

---

## Decisiones Arquitectónicas

### 1. Arquitectura Hexagonal
**Decisión:** Separar estrictamente dominio, aplicación e infraestructura mediante puertos e interfaces.

**Razón:** Permite cambiar el framework HTTP, la base de datos o los proveedores de pago sin tocar la lógica de negocio. En un contexto de pagos esto es crítico: los PSPs cambian con frecuencia.

### 2. Strategy + Registry para proveedores de pago
**Decisión:** Cada proveedor de pago implementa `PaymentProviderPort` y se registra automáticamente.

**Razón:** Agregar un nuevo PSP es crear una clase `@Component` sin modificar ningún código existente (principio Open/Closed).

### 3. Persistir antes de enviar al proveedor
**Decisión:** La transacción se guarda en BD **antes** de enviarse al PSP.

**Razón:** Garantiza trazabilidad y auditabilidad. Si el PSP responde con error o timeout, la transacción queda registrada en estado `PENDING` y puede reintentarse.

### 4. Monto en centavos (BIGINT)
**Decisión:** Almacenar montos como enteros en centavos.

**Razón:** Evita errores de punto flotante en cálculos financieros. Práctica estándar en la industria de pagos.

### 5. UUID generado por el servicio
**Decisión:** Los IDs de transacción son UUIDs generados por el microservicio, no por la base de datos.

**Razón:** Permite generar el ID antes de persistir, facilitando idempotencia y arquitecturas event-driven.

### 6. Flyway para migraciones
**Decisión:** Flyway gestiona el schema de BD.

**Razón:** Versionado y reproducibilidad del esquema en todos los ambientes (dev, staging, prod).

### 7. Circuit Breaker con Resilience4j
**Decisión:** Implementar Circuit Breaker como puerto secundario (`CircuitBreakerPort`) con adaptador Resilience4j.

**Razón:** Protege contra cascadas de fallos cuando un PSP está caído. El adaptador implementa fallback automático que marca transacciones como `FAILED` para reintento posterior, sin afectar el dominio ni la aplicación.

### 8. Auditoría asíncrona con Event-Driven
**Decisión:** La auditoría se publica a Kafka y persiste en BD mediante eventos, usando `AuditUseCase` como puerto de entrada.

**Razón:** Desacopla la auditoría del flujo transaccional principal. Usa Transactional Outbox pattern (publicación después del commit) para garantizar consistencia eventual sin afectar latencia de la API.

### 9. Customer como Entity separada
**Decisión:** Customer tiene su propia tabla y repositorio (`CustomerRepository`), no es un Value Object embebido.

**Razón:** Permite historial consolidado del cliente, KYC, límites de riesgo y análisis de comportamiento. La lógica `findOrCreate` garantiza idempotencia por documento o email.

### 10. Optimistic Locking con @Version
**Decisión:** Campo `version` en `transactions` para control de concurrencia optimista.

**Razón:** Evita condiciones de carrera cuando múltiples procesos intentan actualizar la misma transacción simultáneamente.

---

## Suposiciones

1. **Customer como Entity:** Un cliente tiene identidad propia (UUID) y puede tener múltiples transacciones. Se busca/crea por documento (tipo+número) o email para mantener idempotencia. Esto permite historial consolidado del cliente, KYC, y análisis de comportamiento.
2. **Idempotencia por `client_transaction_id`:** Se asume que este campo es la clave de idempotencia provista por el cliente. Si ya existe, se rechaza con código `002`.
3. **Autenticación externa:** No se implementa autenticación/autorización en esta prueba. En producción se agregaría OAuth2/JWT mediante Spring Security.
4. **Notificación webhook asíncrona:** La notificación al webhook se asume asíncrona (fuera del scope de la prueba). En producción se implementaría con un message broker (Kafka/RabbitMQ).
5. **Un solo microservicio:** El orquestador y los adaptadores de proveedor están en el mismo servicio. En producción los adaptadores de PSP serían microservicios independientes.
6. **PostgreSQL como BD:** Se eligió PostgreSQL por su soporte nativo de UUID, JSON y su robustez en aplicaciones financieras.

---

## Riesgos Identificados

| Riesgo | Impacto | Probabilidad | Mitigación |
|--------|---------|-------------|------------|
| Timeout del PSP sin respuesta | Alto | Media | ✅ Circuit Breaker implementado con Resilience4j; reintentos con backoff exponencial |
| Duplicación de transacción por retry del cliente | Alto | Alta | ✅ Idempotencia implementada por `client_transaction_id` único en BD |
| Duplicación de cliente (mismo documento/email) | Medio | Media | ✅ Unique constraints en BD + lógica `findOrCreate` en CustomerRepository |
| Carga alta en el orquestador | Medio | Media | Escalar horizontalmente (stateless), caché con Redis para consultas frecuentes |
| Fallo de BD entre persistir y enviar al PSP | Alto | Baja | ✅ Estado `PENDING` permite reconciliación; job programado para reintentar transacciones atascadas |
| Cambio de esquema sin migración | Alto | Baja | ✅ Flyway + validación DDL en arranque (`ddl-auto: validate`) |
| Condición de carrera en actualización | Alto | Media | ✅ Optimistic locking con @Version en entidades JPA |
| Pérdida de eventos de auditoría | Medio | Media | ✅ Kafka con ACKs=all; persistencia en BD como backup; monitoreo de lag |
| Exposición de datos sensibles en logs | Alto | Media | ✅ Nunca loguear datos de cliente; usar masking en herramientas de observabilidad |
| Falta de autenticación | Crítico | — | Agregar Spring Security + OAuth2 antes de ir a producción |
