# Arquitectura

## Visión general

SyncEvent es un monorepositorio compuesto por una API en Spring Boot y, a futuro, un
cliente web. La API sigue una **arquitectura por capas** con el objetivo de mantener el
dominio aislado de los detalles de infraestructura.

```text
┌──────────────────────────────────────────────┐
│ Presentación (GraphQL / REST)                │
│ Controladores y resolvers                    │
├──────────────────────────────────────────────┤
│ Aplicación                                   │
│ Casos de uso y servicios de orquestación      │
├──────────────────────────────────────────────┤
│ Dominio                                      │
│ Event, EventStatus y reglas de negocio       │
├──────────────────────────────────────────────┤
│ Infraestructura                              │
│ JPA, PostgreSQL, Flyway, seguridad            │
└──────────────────────────────────────────────┘
```

En el estado actual solo están implementadas la capa de dominio y la infraestructura base
(conexión a base de datos y migraciones). Las capas de aplicación y presentación se
incorporarán en las siguientes iteraciones.

## Paquetes del backend

```text
com.syncevent
├── SynceventApplication        # Punto de entrada
└── domain.model                # Modelo de dominio
    ├── Event                   # Entidad de dominio
    └── EventStatus             # Estados del ciclo de vida
```

### Decisiones de diseño

1. **Dominio sin dependencias de framework.** `Event` es un POJO puro: no usa anotaciones de
   JPA ni de Spring. Esto permite probar las reglas de negocio sin levantar el contexto y
   deja abierta la posibilidad de separar el modelo de persistencia.
2. **Identificadores UUID como cadena.** El `id` se genera con `UUID.randomUUID()` para evitar
   colisiones sin depender de la base de datos.
3. **Lista de asistentes encapsulada.** Se usan copias defensivas para que ninguna capa externa
   pueda alterar el estado interno del evento.
4. **Esquema gestionado por Flyway.** Hibernate se configura con `ddl-auto=validate`; la
   estructura de la base solo cambia mediante migraciones versionadas.
5. **Configuración por variables de entorno.** No hay credenciales fijas en el código: los
   valores por defecto son de desarrollo y se sobrescriben con `DB_URL`, `DB_USER` y
   `DB_PASSWORD`.
6. **Tests aislados de PostgreSQL.** El perfil `test` usa H2 en memoria, de modo que la suite
   puede ejecutarse sin infraestructura externa.

## Flujo de una petición (objetivo)

```text
Cliente → API GraphQL → Resolver → Caso de uso → Dominio
                                      ↓
                              Repositorio JPA → PostgreSQL
```

## Estado y próximos pasos

| Componente | Estado |
| --- | --- |
| Dominio `Event` | Implementado |
| Enumeración `EventStatus` | Implementado |
| Conexión PostgreSQL + Flyway | Verificado |
| Tests de dominio y contexto | Implementado |
| Entidades JPA / repositorios | Pendiente |
| Casos de uso | Pendiente |
| API GraphQL (esquema y resolvers) | Pendiente |
| Seguridad por roles | Pendiente |
| Frontend | Pendiente |

## Deuda técnica conocida

- `Event` aún no se persiste: no existe entidad JPA ni repositorio.
- No hay migraciones de esquema (`db/migration` está vacío).
- La seguridad usa la configuración por defecto de Spring Security (usuario generado).
- No se ha definido el contrato GraphQL.
- No hay pipeline de integración continua.

Estos puntos están priorizados en la sección final de la [auditoría](auditoria.md).
