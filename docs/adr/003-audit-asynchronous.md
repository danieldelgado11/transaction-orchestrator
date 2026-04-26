# ADR 003: Auditoría Asíncrona vía Kafka

## Estado

**Accepted**

## Contexto

Requerimientos regulatorios y de compliance exigen trazabilidad completa:
- Quién creó/modificó cada transacción
- Cambios de estado con timestamp
- Datos del cliente asociados (snapshot)

Restricciones:
- No debe impactar el tiempo de respuesta de la API (< 200ms p95)
- No debe bloquear transacciones si auditoría falla
- Debe soportar reintentos ante fallos temporales

## Decisión

Implementamos auditoría **asíncrona** usando Kafka como message broker.

### Arquitectura

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  Transaction │────▶│ AuditService │────▶│    Kafka     │
│   Service    │     │ (post-commit)│     │   Topics     │
└──────────────┘     └──────────────┘     └──────────────┘
      │                                          │
      │                                          ▼
      │                                   ┌──────────────┐
      │                                   │ AuditConsumer│
      │                                   │ (Async)      │
      │                                   └──────────────┘
      │                                          │
      ▼                                          ▼
┌──────────────┐                        ┌──────────────┐
│  PostgreSQL  │                        │  PostgreSQL  │
│transactions│                        │ audit_tables │
└──────────────┘                        └──────────────┘
```

### Flujo

1. **Transacción completa**: Se persiste en BD principal
2. **Post-commit**: AuditService registra eventos en TransactionSynchronization
3. **Publish**: Eventos enviados a Kafka (fire-and-forget)
4. **Consume**: Consumer independiente persiste en tablas de auditoría
5. **Desacoplado**: Fallo en auditoría no afecta la transacción principal

### Decisiones de Diseño

| Aspecto | Decisión | Racional |
|---------|----------|----------|
| Sync vs Async | Async | No bloquear response time |
| Outbox vs Direct | Direct + Post-commit | Simpler, transacción ya guardada |
| Event Structure | Snapshots completos | Compliance requiere datos históricos |
| Separate DB Schema | Sí | Aislar load, retenición diferente |

## Consecuencias

### Positivas

- **Performance**: API response time no incluye escritura de auditoría
- **Resiliencia**: Kafka retenta automáticamente ante fallos
- **Escalabilidad**: Consumer puede escalar independientemente
- **Desacoplamiento**: Cambios en auditoría no afectan flujo principal

### Negativas

- **Eventual consistency**: Auditoría puede llegar con delay de segundos
- **Complejidad**: Más componentes (Kafka, Consumer, tópicos)
- **Debugging**: Difícil correlacionar problemas entre flujos

## Alternativas Consideradas

| Alternativa | Por qué no se eligió |
|-------------|---------------------|
| Auditoría síncrona en misma TX | Impacta performance, bloquea API |
| Outbox Pattern | Overkill, transacción ya persistida antes de publish |
| Cambio Data Capture (Debezium) | Complejidad operativa alta |
| Event Sourcing completo | No se necesita historial completo de estado |

## Manejo de Fallos

### Escenarios

| Escenario | Comportamiento |
|-----------|----------------|
| Kafka disponible, DB no | Evento en Kafka, reintenta consumer |
| Kafka no disponible | Log warning, no falla transacción |
| Consumer caído | Eventos acumulan, procesa al levantar |

### Garantías

- **At-least-once delivery**: Evento puede duplicarse (idempotente)
- **No message loss**: Kafka retención + acks=all
- **Best-effort**: No bloquea si auditoría falla

## Referencias

- [The Outbox Pattern - Chris Richardson](https://microservices.io/patterns/data/transactional-outbox.html)
- [Event-Driven Architecture - AWS](https://aws.amazon.com/event-driven-architecture/)
- [Change Data Capture - Debezium](https://debezium.io/)

---
*Decision tomada: Abril 2025*
*Última actualización: Abril 2025*
