# ADR 001: Arquitectura Hexagonal (Puertos y Adaptadores)

## Estado

**Accepted**

## Contexto

El Transaction Orchestrator es un microservicio crítico que coordina transacciones de pago entre múltiples proveedores (PSPs). Necesitamos una arquitectura que:

- Permita cambiar proveedores de pago sin modificar la lógica de negocio
- Facilite el testing unitario e integración
- Desacople la infraestructura (HTTP, JPA, Kafka) del dominio
- Soporte múltiples canales de entrada (REST, gRPC, messaging en el futuro)

## Decisión

Adoptamos la **Arquitectura Hexagonal** (Puertos y Adaptadores) como patrón arquitectónico principal.

### Estructura de Carpetas

```
src/main/java/com/tumipay/orchestrator/
├── domain/              # Núcleo de negocio - sin dependencias externas
│   ├── model/           # Entidades, Value Objects, Eventos
│   ├── port/in/         # Puertos de entrada (API del dominio)
│   └── port/out/        # Puertos de salida (interfaces requeridas)
├── application/         # Casos de uso - orquesta el flujo
│   └── service/
└── infrastructure/        # Adaptadores concretos
    ├── adapter/in/      # Adaptadores de entrada (REST)
    └── adapter/out/     # Adaptadores de salida (JPA, Kafka)
```

### Reglas de Dependencia

1. **Dominio**: No tiene dependencias externas. Solo usa Java SE.
2. **Aplicación**: Depende solo del dominio. Implementa puertos de entrada, usa puertos de salida.
3. **Infraestructura**: Depende de aplicación y dominio. Adapta frameworks externos.

## Consecuencias

### Positivas

- **Independencia de frameworks**: Podemos cambiar de Spring a Quarkus sin tocar dominio
- **Testabilidad**: El dominio se prueba sin Spring, sin base de datos, sin HTTP
- **Flexibilidad**: Nuevos PSPs se agregan implementando `PaymentProviderPort`
- **Mantenibilidad**: Cambios en infraestructura no afectan la lógica de negocio

### Negativas

- **Curva de aprendizaje**: Requiere entender la inversión de dependencias
- **Más código**: Mappers entre entidades JPA y modelos de dominio
- **Indirección**: Navegar el código requiere saltar entre capas

## Alternativas Consideradas

| Alternativa | Por qué no se eligió |
|-------------|---------------------|
| MVC tradicional | Mezcla de concerns, difícil de probar |
| Clean Architecture | Muy similar, Hexagonal más pragmático para este scope |
| DDD modularity | Aplica a nivel de bounded context, no microservicio |

## Referencias

- [Hexagonal Architecture by Alistair Cockburn](https://alistair.cockburn.us/hexagonal-architecture/)
- [Growing Object-Oriented Software, Freeman & Pryce](http://www.growing-object-oriented-software.com/)

---
*Decision tomada: Abril 2025*
*Última actualización: Abril 2025*
