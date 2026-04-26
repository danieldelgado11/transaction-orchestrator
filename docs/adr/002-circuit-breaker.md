# ADR 002: Circuit Breaker para Proveedores de Pago

## Estado

**Accepted**

## Contexto

Los proveedores de pago (PSPs) externos son puntos de fallo potenciales:
- Latencia variable (200ms - 30s)
- Timeouts ocasionales
- Errores 500/503 durante mantenimiento
- Caídas completas del servicio

Sin protección, una falla en un PSP puede:
- Saturar conexiones del pool de HTTP
- Timeout transacciones válidas
- Propagar errores a clientes
- Afectar otros PSPs (efecto cascada)

## Decisión

Implementamos **Circuit Breaker** usando Resilience4j para todas las llamadas a PSPs.

### Configuración

```yaml
resilience4j:
  circuitbreaker:
    instances:
      payment-provider:
        slidingWindowSize: 10           # Evalúa últimas 10 llamadas
        minimumNumberOfCalls: 5         # Actúa después de 5 llamadas
        failureRateThreshold: 50        # Abre si 50% fallan
        waitDurationInOpenState: 30s    # Reintenta después de 30s
        slowCallDurationThreshold: 5s   # Considera lenta si > 5s
        slowCallRateThreshold: 80       # Abre si 80% son lentas
```

### Estados

```
CLOSED  → [fallos] → OPEN (rechaza llamadas) → [30s] → HALF_OPEN
 (normal)    ↑_________________________________________↓
              [éxito] → CLOSED, [fallo] → OPEN
```

### Fallback

Cuando el circuito está abierto:
- Marca transacción como **FAILED**
- No se pierde la transacción (persistida previamente)
- Permite reintento manual o automático
- No bloquea el thread del cliente

## Consecuencias

### Positivas

- **Fail-fast**: Rechaza rápido cuando PSP no responde
- **Graceful degradation**: Sistema continúa operando con otros PSPs
- **Auto-recovery**: Detecta automáticamente cuando PSP vuelve
- **Observabilidad**: Métricas de estado del circuito en Actuator

### Negativas

- **Complejidad**: Más componentes en el flujo
- **Configuración**: Requiere tuning por PSP (no todos tienen misma SLA)
- **Falsos positivos**: Puede abrirse por picos de latencia normales

## Alternativas Consideradas

| Alternativa | Por qué no se eligió |
|-------------|---------------------|
| Retry simple | Sin aislación de fallos, amplifica problemas |
| Bulkhead (Resilience4j) | Aún más complejo, Circuit Breaker es suficiente |
| Implementación propia | Resilience4j probado, con métricas integradas |
| Sin protección | Inaceptable para producción |

## Métricas

El Circuit Breaker expone métricas via Micrometer:

```
resilience4j_circuitbreaker_state{name="payment-provider"}  # 0=CLOSED, 1=OPEN, 2=HALF_OPEN
resilience4j_circuitbreaker_calls_total{name="payment-provider",kind="successful"}
resilience4j_circuitbreaker_calls_total{name="payment-provider",kind="failed"}
resilience4j_circuitbreaker_calls_total{name="payment-provider",kind="ignored"}
```

## Referencias

- [Resilience4j Documentation](https://resilience4j.readme.io/)
- [Release It! by Michael Nygard](https://pragprog.com/titles/mnee2/release-it-second-edition/)
- [Circuit Breaker Pattern - Microsoft](https://docs.microsoft.com/en-us/azure/architecture/patterns/circuit-breaker)

---
*Decision tomada: Abril 2025*
*Última actualización: Abril 2025*
