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
┌─────────────────────────────────────────────────────────────────┐
│                        INFRASTRUCTURE                           │
│                                                                 │
│  ┌──────────────────┐              ┌──────────────────────────┐ │
│  │  Inbound Adapter │              │    Outbound Adapters     │ │
│  │  (REST / HTTP)   │              │                          │ │
│  │                  │              │  ┌────────────────────┐  │ │
│  │  TransactionCon- │              │  │ TransactionPers-   │  │ │
│  │  troller         │              │  │ istenceAdapter     │  │ │
│  └────────┬─────────┘              │  │ (JPA/PostgreSQL)   │  │ │
│           │                        │  └────────────────────┘  │ │
│           │ calls                  │                          │ │
│           ▼                        │  ┌────────────────────┐  │ │
│  ┌─────────────────────────────┐   │  │ MockPaymentPro-    │  │ │
│  │        APPLICATION          │   │  │ viderAdapter       │  │ │
│  │                             │   │  │ (PSP Integration)  │  │ │
│  │  TransactionService         │───┼─▶└────────────────────┘  │ │
│  │  (implements Use Case port) │   │                          │ │
│  └──────────────┬──────────────┘   └──────────────────────────┘ │
│                 │                                               │
│       ┌─────────▼─────────┐                                     │
│       │      DOMAIN       │                                     │
│       │                   │                                     │
│       │  Transaction      │                                     │
│       │  Customer         │                                     │
│       │  TransactionStatus│                                     │
│       │  (Ports defined   │                                     │
│       │   here)           │                                     │
│       └───────────────────┘                                     │
└─────────────────────────────────────────────────────────────────┘
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
| **Repository** | `TransactionRepository` (port) + `TransactionPersistenceAdapter` | Abstraer el acceso a datos |
| **Mapper** (MapStruct) | `TransactionRestMapper`, `TransactionPersistenceMapper` | Convertir entre capas sin acoplamiento |
| **Facade** | `TransactionController` | Simplificar la interfaz HTTP al caso de uso |
| **Template Method** | `GlobalExceptionHandler` | Manejar familias de excepciones de forma uniforme |

---

## Estructura del Proyecto

```
src/main/java/com/tumipay/orchestrator/
├── domain/
│   ├── model/
│   │   ├── Transaction.java          ← Aggregate Root
│   │   ├── Customer.java             ← Value Object
│   │   └── TransactionStatus.java    ← Enum de estados
│   ├── port/
│   │   ├── in/
│   │   │   ├── TransactionUseCase.java        ← Puerto primario
│   │   │   └── CreateTransactionCommand.java
│   │   └── out/
│   │       ├── TransactionRepository.java     ← Puerto secundario
│   │       └── PaymentProviderPort.java       ← Puerto secundario
│   └── exception/
│       ├── TransactionNotFoundException.java
│       ├── DuplicateTransactionException.java
│       └── PaymentProviderNotFoundException.java
├── application/
│   └── service/
│       └── TransactionService.java   ← Implementa TransactionUseCase
└── infrastructure/
    ├── adapter/
    │   ├── in/rest/
    │   │   ├── controller/TransactionController.java
    │   │   ├── dto/request/
    │   │   │   ├── CreateTransactionRequest.java
    │   │   │   └── CustomerRequest.java
    │   │   ├── dto/response/
    │   │   │   ├── ApiResponse.java
    │   │   │   └── TransactionResponse.java
    │   │   └── mapper/TransactionRestMapper.java
    │   └── out/
    │       ├── persistence/
    │       │   ├── TransactionPersistenceAdapter.java
    │       │   ├── entity/TransactionEntity.java
    │       │   ├── entity/CustomerEntity.java
    │       │   ├── repository/JpaTransactionRepository.java
    │       │   └── mapper/TransactionPersistenceMapper.java
    │       └── provider/
    │           └── MockPaymentProviderAdapter.java
    └── exception/
        └── GlobalExceptionHandler.java
```

---

## Contrato API

### Base URL
```
http://localhost:8080/api/v1
```

### POST /transactions — Crear Transacción

**Request:**
```json
POST /api/v1/transactions
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

### GET /transactions/{transaction_id} — Consultar Transacción

```
GET /api/v1/transactions/550e8400-e29b-41d4-a716-446655440000
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

---

## Modelo de Base de Datos

```
┌─────────────────────────────────────┐
│           customers                 │
├─────────────────────────────────────┤
│ PK id                UUID           │
│    document_type     VARCHAR(20)    │
│    document_number   VARCHAR(50)    │
│    country_calling_code VARCHAR(6)  │
│    phone_number      VARCHAR(20)    │
│    email             VARCHAR(254)   │
│    first_name        VARCHAR(100)   │
│    middle_name       VARCHAR(100)   │
│    last_name         VARCHAR(100)   │
│    second_last_name  VARCHAR(100)   │
└──────────────────┬──────────────────┘
                   │ 1
                   │
                   │ 1
┌──────────────────▼──────────────────┐
│           transactions              │
├─────────────────────────────────────┤
│ PK id                UUID           │
│ UQ client_transaction_id VARCHAR    │
│    amount_cents      BIGINT         │
│    currency_code     CHAR(3)        │
│    country_code      CHAR(2)        │
│    payment_method_id VARCHAR(50)    │
│    webhook_url       VARCHAR(500)   │
│    redirect_url      VARCHAR(500)   │
│    description       VARCHAR(255)   │
│    expiration_seconds BIGINT        │
│    status            VARCHAR(20)    │
│    processed_at      TIMESTAMP TZ   │
│    created_at        TIMESTAMP TZ   │
│    updated_at        TIMESTAMP TZ   │
│ FK customer_id       UUID           │
└─────────────────────────────────────┘
```

Los scripts SQL están en `sql/transaction_orchestrator_schema.sql`.

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
| Integración | Spring Boot Test + Testcontainers (PostgreSQL real) | Flujos completos |
| Contrato | Spring MVC Test (`@WebMvcTest`) | Todos los endpoints |

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

---

## Suposiciones

1. **Un cliente por transacción:** Se asume que cada transacción tiene exactamente un objeto cliente. No hay reutilización de clientes entre transacciones en esta versión.
2. **Idempotencia por `client_transaction_id`:** Se asume que este campo es la clave de idempotencia provista por el cliente. Si ya existe, se rechaza con código `002`.
3. **Autenticación externa:** No se implementa autenticación/autorización en esta prueba. En producción se agregaría OAuth2/JWT mediante Spring Security.
4. **Notificación webhook asíncrona:** La notificación al webhook se asume asíncrona (fuera del scope de la prueba). En producción se implementaría con un message broker (Kafka/RabbitMQ).
5. **Un solo microservicio:** El orquestador y los adaptadores de proveedor están en el mismo servicio. En producción los adaptadores de PSP serían microservicios independientes.
6. **PostgreSQL como BD:** Se eligió PostgreSQL por su soporte nativo de UUID, JSON y su robustez en aplicaciones financieras.

---

## Riesgos Identificados

| Riesgo | Impacto | Probabilidad | Mitigación |
|--------|---------|-------------|------------|
| Timeout del PSP sin respuesta | Alto | Media | Implementar circuit breaker (Resilience4j) y reintentos con backoff exponencial |
| Duplicación de transacción por retry del cliente | Alto | Alta | Idempotencia implementada por `client_transaction_id` único en BD |
| Carga alta en el orquestador | Medio | Media | Escalar horizontalmente (stateless), caché con Redis para consultas frecuentes |
| Fallo de BD entre persistir y enviar al PSP | Alto | Baja | Estado `PENDING` permite reconciliación; job programado para reintentar transacciones atascadas |
| Cambio de esquema sin migración | Alto | Baja | Flyway + validación DDL en arranque (`ddl-auto: validate`) |
| Exposición de datos sensibles en logs | Alto | Media | Nunca loguear datos de cliente; usar masking en herramientas de observabilidad |
| Falta de autenticación | Crítico | — | Agregar Spring Security + OAuth2 antes de ir a producción |
