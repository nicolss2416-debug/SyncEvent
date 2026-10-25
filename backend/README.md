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
Si el evento no existe, responde `404 Not Found`. La cancelación y el aplazamiento se
implementarán como operaciones separadas porque requieren motivo y otros efectos de negocio.

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
