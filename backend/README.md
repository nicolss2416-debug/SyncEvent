# Backend SyncEvent

## Requisitos técnicos

- JDK 21
- PostgreSQL accesible para ejecutar la aplicación
- Maven Wrapper incluido (`mvnw.cmd`)

El backend usa Spring Boot 4.1.1, Spring MVC, Spring Data JPA, Spring Security,
Jakarta Validation y Flyway. El esquema de producción se administra mediante
migraciones versionadas en `src/main/resources/db/migration`.

## Configurar PostgreSQL

La conexión usa estas variables:

| Variable | Uso |
| --- | --- |
| `DB_URL` | URL JDBC; por defecto `jdbc:postgresql://localhost:5432/syncevent` |
| `DB_USER` | Usuario de PostgreSQL, obligatorio |
| `DB_PASSWORD` | Contraseña de PostgreSQL, obligatoria |
| `SERVER_PORT` | Puerto HTTP, por defecto `8080` |

No se guarda una contraseña por defecto en la configuración del backend. En
PowerShell, define las variables en la sesión antes de ejecutar:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/syncevent"
$env:DB_USER = "<usuario>"
$env:DB_PASSWORD = "<contraseña>"
```

No subas contraseñas reales al repositorio ni las incluyas en archivos versionados.
Al iniciar la aplicación, Flyway ejecuta las migraciones y Hibernate valida el esquema.

## Módulo de eventos: creación y consulta

`POST /api/events` crea un evento en estado `BORRADOR`. Requiere autenticación y
una autoridad `ROLE_ADMINISTRADOR` o `ROLE_OPERADOR`. El módulo de autenticación y
la emisión/verificación de JWT y la asignación de esos roles aún no están implementados.
Por ahora, los tests simulan esos roles; no hay cuentas de aplicación para usar manualmente
el endpoint.

Ejemplo de cuerpo JSON:

```json
{
  "name": "Capacitación Java",
  "description": "Introducción a Java",
  "type": "CAPACITACION",
  "modality": "PRESENCIAL",
  "startAt": "2027-03-01T09:00:00",
  "endAt": "2027-03-01T12:00:00",
  "location": "Sede central",
  "capacity": 30,
  "durationHours": 3.00
}
```

Para `VIRTUAL` se requiere `meetingUrl`; para `HIBRIDA` se requieren `location` y
`meetingUrl`. Si el horario se cruza con otro evento no finalizado ni cancelado en el
mismo lugar, la API responde `409 Conflict`. Los datos inválidos producen `400 Bad Request`;
la creación correcta produce `201 Created`.

`GET /api/events` devuelve una lista paginada. Requiere uno de los roles
`ADMINISTRADOR`, `OPERADOR`, `MONITOR` o `INVITADO`. Admin y Operador pueden ver todos
los estados; Monitor solo ve `PUBLICADO` y `EN_CURSO`; Invitado solo ve `PUBLICADO`.
Los filtros opcionales son `type`, `modality`, `status`, `from` y `to`; las fechas tienen
formato `YYYY-MM-DD` y filtran por la fecha de inicio del evento. La paginación inicia en
`page=0`, usa `size=10` por defecto y admite hasta 100 elementos por página. `sort` acepta
`DATE` o `NAME`, y `direction` acepta `ASC` o `DESC`.

Ejemplo:

```text
GET /api/events?type=CAPACITACION&from=2027-03-01&to=2027-03-31&page=0&size=10&sort=DATE&direction=ASC
```

El filtro por estado nunca amplía lo visible para el rol: por ejemplo, un Invitado que
solicite `status=BORRADOR` no recibe borradores. Un rango invertido, una paginación inválida
o un filtro no reconocido produce `400 Bad Request`.

El endpoint `PATCH /api/events/{eventId}/status`, restringido a Administrador y Operador,
avanza el estado en el orden `BORRADOR` → `PUBLICADO` → `EN_CURSO` → `FINALIZADO`.
Solo se permite la transición al estado siguiente; un salto, una transición desde un estado
final o el uso de este endpoint para `CANCELADO`/`APLAZADO` responde `409 Conflict`.
Si el evento no existe, responde `404 Not Found`. La cancelación se realiza mediante
los endpoints específicos descritos abajo para cancelación y aplazamiento.

`POST /api/events/{eventId}/cancellation` permite al Administrador u Operador cancelar
eventos en estado `BORRADOR`, `PUBLICADO` o `EN_CURSO`, con un motivo obligatorio. La
operación conserva el evento y guarda el estado anterior, el motivo y la fecha en
`event_cancellation_history`; eventos finalizados o en estados no cancelables responden
`409 Conflict`. La nueva migración `V2` crea esa tabla.

`PUT /api/events/{eventId}` permite al Administrador u Operador reemplazar los datos
editables del evento, conservando su estado actual. No se pueden editar eventos
`FINALIZADO` o `CANCELADO`. Si los nuevos datos generan un cruce de horario en el mismo
lugar, responde `409 Conflict`; un evento inexistente responde `404 Not Found`.
Como aún no existe el módulo de inscripciones, por ahora se rechazan reducciones de aforo.
También se rechazan cambios de fecha o modalidad porque todavía no se puede ejecutar el
flujo requerido de confirmación y notificación a inscritos. Los conflictos de asignación
de monitores tampoco se verifican; dependen de un módulo aún no implementado.

La cancelación implementada todavía no invalida códigos QR ni notifica a inscritos, porque
los módulos de inscripciones y notificaciones están pendientes. Tampoco representa la
auditoría general de todas las escrituras: este historial guarda únicamente cancelaciones.

`POST /api/events/{eventId}/postponements` aplaza un evento `PUBLICADO` con motivo,
nueva fecha y hora. La API rechaza los conflictos de lugar y horario con `409 Conflict`.
El evento queda `APLAZADO` hasta que Admin u Operador confirme la nueva fecha mediante
`POST /api/events/{eventId}/postponements/confirmation`; entonces vuelve a `PUBLICADO`.
Cada solicitud y confirmación se registra como una fila append-only en
`event_postponement_history`, creada por la migración `V3`.

Las notificaciones a inscritos todavía no se envían, pues el módulo de notificaciones y
las inscripciones no están implementados. La API conserva el nuevo horario al solicitar
el aplazamiento, mientras el estado permanece `APLAZADO` hasta la confirmación.

## Registro de participantes

`POST /api/participants` permite a Administrador u Operador registrar una persona con
tipo y número de documento, nombres, apellidos, correo y teléfono. Los campos obligatorios
y el correo se validan; nombres y apellidos se normalizan a espacios simples y capitalización
por palabra. El tipo y número de documento se recortan y normalizan a mayúsculas. La base
impide duplicados por combinación de tipo y número; la respuesta `409 Conflict` incluye
`existingParticipantId` para abrir el registro existente. Una respuesta correcta es
`201 Created` e incluye `createdBy` y `createdAt`.

El catálogo no define los tipos de documento admitidos ni formatos concretos por tipo, por
lo que esta versión no impone reglas de formato adicionales. El registro queda en la tabla
`participants`, creada por Flyway en `V4__create_participants.sql`. La lectura y edición de
participantes corresponden al siguiente incremento.

`GET /api/participants?q={texto}&page=0&size=10` permite a Administrador, Operador
y Monitor buscar parcialmente por documento, nombre o correo; el resultado está paginado
y ordenado por apellidos y nombres. Una búsqueda válida sin coincidencias devuelve una
página vacía (`empty: true`). `GET /api/participants/{participantId}` abre la ficha para
esos mismos roles.

`PUT /api/participants/{participantId}` permite a Administrador u Operador corregir los
datos con las mismas validaciones. El documento se mantiene único; un duplicado devuelve
`409 Conflict`, y una ficha inexistente devuelve `404 Not Found`. Cada corrección conserva
`createdBy`/`createdAt`, establece `updatedBy`/`updatedAt` y añade una fila de auditoría en
`participant_change_history` mediante la migración `V5`. Monitor solo tiene lectura.
Los certificados aún no están implementados; cuando se agreguen deberán conservar una
instantánea de los datos al momento de emisión para respetar RN-19.

## Ejecutar pruebas

Las pruebas usan H2 en memoria y no requieren un servidor PostgreSQL:

```powershell
.\mvnw.cmd test
```

## Iniciar la aplicación

Con PostgreSQL disponible y las variables de conexión definidas:

```powershell
.\mvnw.cmd spring-boot:run
```

La API queda disponible en `http://localhost:8080`.
