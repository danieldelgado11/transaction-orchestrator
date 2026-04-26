# ADR 004: Optimistic Locking para Transacciones

## Estado

**Accepted**

## Contexto

En un sistema de pagos concurrente, múltiples operaciones pueden intentar modificar la misma transacción simultáneamente:

- **Race condition en actualización de estado**: Thread A lee transacción PENDING, Thread B la actualiza a FAILED, Thread A intenta actualizar a APPROVED basándose en datos obsoletos
- **Reconciliación concurrente**: Jobs de reconciliación pueden modificar transacciones mientras se procesan webhooks
- **Reintentos del cliente**: Múltiples requests idénticos pueden procesarse en paralelo antes de que el primero complete

Sin protección de concurrencia, podríamos tener:
- Lost updates (última escritura gana, perdiendo estados intermedios)
- Estados inconsistentes (transacción marcada como APPROVED y FAILED simultáneamente)
- Dificultad para debuggear problemas de integridad

## Decisión

Implementamos **Optimistic Locking** usando la anotación `@Version` de JPA.

### Cómo funciona

```
Thread A: READ  tx(id=123, status=PENDING, version=5)
Thread B: READ  tx(id=123, status=PENDING, version=5)

Thread B: UPDATE tx SET status=APPROVED, version=6 WHERE id=123 AND version=5
          → ÉXITO (1 fila afectada)

Thread A: UPDATE tx SET status=FAILED, version=6 WHERE id=123 AND version=5
          → FALLO (0 filas afectadas - versión ya es 6)
          → Lanza OptimisticLockException
          → Traducido a ConcurrentModificationException (código 006)
```

### Implementación

```java
@Entity
public class TransactionEntity {
    // ... otros campos ...
    
    @Version
    @Column(name = "version", nullable = false)
    private Integer version;
}
```

```sql
ALTER TABLE transactions ADD COLUMN version INTEGER NOT NULL DEFAULT 0;
```

### API Response

Cuando ocurre un conflicto de concurrencia:

```json
{
  "response_code": "006",
  "response_message": "La transacción 550e8400-e29b-41d4-a716-446655440000 fue modificada por otro proceso. Por favor, reintente la operación.",
  "data": null
}
```

HTTP Status: **409 CONFLICT**

## Consecuencias

### Positivas

- **No bloquea**: A diferencia del pessimistic locking, no mantiene locks en la base de datos
- **Escalable**: Funciona bien bajo alta concurrencia sin degradar performance
- **Fail-fast**: Detecta conflictos inmediatamente, permitiendo al cliente reintentar
- **Sin deadlocks**: No hay riesgo de deadlocks entre transacciones

### Negativas

- **Reintentos necesarios**: El cliente debe estar preparado para recibir 409 y reintentar
- **No garantiza orden**: Si Thread A y Thread B intentan actualizar, el que llega primero gana (puede no ser el deseado)
- **Overhead de versión**: Requiere columna adicional y gestión en todas las updates

## Alternativas Consideradas

| Alternativa | Por qué no se eligió |
|-------------|---------------------|
| Pessimistic Locking (SELECT FOR UPDATE) | Bloquea la fila, reduce throughput, riesgo de deadlocks |
| Compare-and-Swap (CAS) manual | Más código, fácil olvidar la condición de versión |
| Event Sourcing | Overkill para este caso de uso, complejidad innecesaria |
| Serializable isolation level | Impacta performance de todas las queries, no solo transacciones |

## Rollback Strategy

Si optimistic locking causa demasiados conflictos:

1. **Estrategia de reintentos automáticos**: Implementar `@Retryable` de Spring en el service
2. **Cola de procesamiento serial**: Mover actualizaciones de la misma transacción a una cola single-threaded
3. **Merge de estados**: En lugar de fallar, aplicar reglas de negocio para resolver conflictos (ej: APPROVED gana sobre PENDING)

## Métricas

Monitorear:

```
orchestrator_concurrent_modification_exceptions_total  # Contador de conflictos
orchestrator_transaction_update_conflicts_rate       # Tasa de conflictos por minuto
```

Umbral de alerta: > 5% de updates resultan en conflicto indica problema de diseño.

## Referencias
n
- [JPA Optimistic Locking - Baeldung](https://www.baeldung.com/jpa-optimistic-locking)
- [Optimistic vs Pessimistic Locking](https://stackoverflow.com/questions/129329/optimistic-vs-pessimistic-locking)
- [Handling OptimisticLockException in Spring](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/dao/OptimisticLockingFailureException.html)

---
*Decision tomada: Abril 2025*
*Última actualización: Abril 2025*
