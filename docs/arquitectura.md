# Arquitectura

## Visión general

SyncEvent es un monorepositorio con un backend Java/Spring Boot y un cliente web React.
El backend organiza sus módulos por dominio funcional y separa presentación, aplicación,
dominio y adaptadores de infraestructura. Los modelos de dominio y sus puertos no dependen
de las entidades JPA.

```text
┌──────────────────────────────────────────────┐
│ Presentación (GraphQL / REST)                │
│ Controladores y resolvers                    │
├──────────────────────────────────────────────┤
│ Aplicación                                   │
│ Casos de uso y servicios de orquestación      │
├──────────────────────────────────────────────┤
│ Dominio y puertos                            │
│ Reglas, modelos y contratos de persistencia  │
├──────────────────────────────────────────────┤
│ Infraestructura                              │
│ JPA, PostgreSQL, Flyway, seguridad            │
└──────────────────────────────────────────────┘
```

Los módulos implementados incluyen usuarios/autenticación, eventos, participantes,
inscripciones, asistencia, multas y certificados. La cobertura de la interfaz React no es
uniforme: los flujos conectados al backend se describen en `frontend/README.md` y en la guía
de desarrollo; las pantallas que aún mantienen estado local no deben considerarse fuente de
datos persistidos.

## Paquetes del backend

```text
com.syncevent
├── SynceventApplication
├── domain.model                # Modelos y reglas comunes de eventos
├── events/                     # API REST/GraphQL, aplicación, puertos y JPA
├── users/                      # Cuentas, roles, JWT, sesiones y actividad
├── participants/               # Fichas y correcciones auditadas
├── registrations/              # Inscripciones, cancelaciones y QR opaco
├── attendance/                 # Check-in, estadísticas en vivo, ausencias y multas
├── certificates/               # Emisión, PDFs, descarga y verificación
└── infrastructure/             # Configuración transversal
```

### Decisiones de diseño

1. **Dominio desacoplado de JPA.** Las entidades persistentes traducen a modelos de dominio.
2. **Puertos para persistencia y colaboración.** Los servicios de aplicación dependen de
   contratos; los adaptadores Spring Data/JPA implementan esos contratos.
3. **UUID como identidad persistente.** Las entidades y los contratos API usan identificadores UUID.
4. **Flyway es propietario del esquema.** Hibernate valida el esquema; las migraciones existentes
   no se editan y los cambios se agregan en una nueva versión.
5. **Configuración externa.** La conexión y JWT se configuran mediante variables de entorno; no
   deben añadirse secretos al repositorio.
6. **Pruebas con H2.** La suite automatizada usa H2; por ello, pasarla no acredita por sí solo
   que todas las migraciones hayan sido ejecutadas contra PostgreSQL.

## Flujo de una petición (objetivo)

```text
React → REST o GraphQL → Controlador/Resolver → Servicio de aplicación
                     → Modelo de dominio y puerto
                     → Adaptador JPA → PostgreSQL
```

## Estado actual de componentes

| Componente | Estado |
| --- | --- |
| Autenticación, cuentas, roles y sesiones JWT | Implementado en backend; login, registro, cierre y administración de cuentas conectados |
| Eventos: persistencia, validaciones, transiciones, cancelación y aplazamiento | Implementado en backend; CRUD y ciclo de vida conectados desde el frontend |
| Participantes e historial | Backend implementado; perfil actual y CRUD básico conectados; historial e importación masiva pendientes en frontend |
| Inscripciones, autinscripción y cancelación | Backend implementado; flujos de operador/invitado y lectura de inscripciones conectados |
| Asistencia QR/manual y métricas en vivo | Backend y check-in QR/manual conectados; el frontend consulta métricas iniciales, pero aún no consume la suscripción en vivo |
| Ausencias, multas y política | Backend implementado; listado, resolución y política de asistencia conectados para Administración |
| Certificados PDF, propiedad, revocación y verificación | Backend implementado; listado, descarga propia, revocación administrativa y verificación pública conectados; emisión masiva pendiente |
| GraphQL | Eventos, dashboard y asistencia en vivo; no reemplaza los endpoints REST de otros módulos |
| Migraciones de base de datos | V1–V14; H2 es el entorno de pruebas automatizadas |

## Deuda técnica conocida

- Las operaciones GraphQL son deliberadamente parciales; seguridad de transporte y autorización
   por resolver deben mantenerse alineadas cuando se agreguen operaciones.
- La bitácora aún no tiene una consulta backend para poblar la pantalla; no se deben presentar
   registros locales como auditoría real.
- La importación CSV carece de endpoint de persistencia y permanece pendiente.
- El backend emite un token QR opaco para inscripción, pero no entrega una imagen QR de acceso.
- La generación de certificados se solicita manualmente; la emisión automática al finalizar un
   evento y el sistema de notificaciones siguen pendientes.
- Las pruebas usan H2; se requiere validar las migraciones nuevas en un PostgreSQL real antes
   de desplegarlas.
- La matriz de estados oficial está en `documentacion/Requerimientos.txt`; el flujo de evento
   es BORRADOR → PUBLICADO → EN_CURSO → FINALIZADO, con cancelación y aplazamiento por separado.

## Matriz de estados y transiciones implementadas

| Proceso | Estado actual | Siguiente estado permitido | Condición | Actor autorizado / acción |
| --- | --- | --- | --- | --- |
| Evento | BORRADOR | PUBLICADO o CANCELADO | Publicar directamente; cancelar exige motivo | Administrador u Operador; publicar o cancelar |
| Evento | PUBLICADO | EN_CURSO, APLAZADO o CANCELADO | Aplazar exige motivo y nuevo horario; cancelar exige motivo | Administrador u Operador |
| Evento | APLAZADO | PUBLICADO | Confirmación de la nueva fecha solicitada | Administrador u Operador |
| Evento | EN_CURSO | FINALIZADO o CANCELADO | Finalizar sigue el orden; cancelar exige motivo | Administrador u Operador |
| Evento | FINALIZADO o CANCELADO | Ninguno | Estados terminales | Sin transición posterior |
| Inscripción | ACTIVA | CANCELADA | Motivo; sin asistencia registrada; evento no finalizado ni cancelado | Administrador u Operador cualquier inscripción; Invitado solo la propia |
| Asistencia | Sin registro | Registrada | Inscripción activa y evento EN_CURSO; máximo un check-in | Administrador, Operador o Monitor; QR o manual |
| Certificado | No emitido | VIGENTE | Check-in registrado y evento FINALIZADO | Administrador u Operador; emisión manual por inscripción |
| Certificado | VIGENTE | REVOCADO | Motivo obligatorio | Administrador |
| Multa | PENDIENTE | RESUELTA | Resolución administrativa | Administrador |

La regla funcional especifica emisión automática de certificados dentro de 20 minutos; la
implementación actual permite emisión manual. Las notificaciones y el QR como imagen para
inscripciones también siguen pendientes.

Estos puntos están priorizados en la sección final de la [auditoría](auditoria.md).
