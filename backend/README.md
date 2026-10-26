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
| `JWT_SECRET` | Clave HMAC codificada en Base64, mínimo 32 bytes; obligatoria |
| `JWT_ACCESS_TOKEN_TTL` | Vigencia máxima del token de acceso, por defecto `PT30M` |
| `JWT_IDLE_TIMEOUT` | Tiempo máximo de inactividad, por defecto `PT30M` |
| `BOOTSTRAP_ADMIN_EMAIL` | Correo para aprovisionar el primer Administrador (opcional) |
| `BOOTSTRAP_ADMIN_PASSWORD` | Contraseña para aprovisionar el primer Administrador (opcional) |
| `SERVER_PORT` | Puerto HTTP, por defecto `8080` |

No se guarda una contraseña por defecto en la configuración del backend. En
PowerShell, define las variables en la sesión antes de ejecutar:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/syncevent"
$env:DB_USER = "<usuario>"
$env:DB_PASSWORD = "<contraseña>"
$env:JWT_SECRET = "<clave aleatoria Base64 de al menos 32 bytes>"
$env:BOOTSTRAP_ADMIN_EMAIL = "admin@example.com"
$env:BOOTSTRAP_ADMIN_PASSWORD = "<contraseña segura de al menos 8 caracteres>"
```

En PowerShell puedes generar una clave aleatoria para la sesión actual:

```powershell
$randomBytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($randomBytes)
$env:JWT_SECRET = [Convert]::ToBase64String($randomBytes)
$rng.Dispose()
```

No subas contraseñas ni claves reales al repositorio ni las incluyas en archivos versionados.
Al iniciar la aplicación, Flyway ejecuta las migraciones y Hibernate valida el esquema.

## Registro e inicio de sesión

`POST /api/auth/register` recibe `email` y `password` (entre 8 y 128 caracteres) y
crea una cuenta activa con el rol `INVITADO`. La contraseña se almacena con PBKDF2; el
correo se normaliza a minúsculas y es único. Un correo existente produce `409 Conflict`.

`POST /api/auth/login` devuelve un token JWT `Bearer` con el rol de la cuenta. Los datos
incorrectos y las cuentas inactivas comparten el mismo error `401`, y los intentos fallidos
se registran sin almacenar contraseñas. Los endpoints protegidos reciben:

```text
Authorization: Bearer <accessToken>
```

Ejemplo para iniciar sesión desde PowerShell:

```powershell
$login = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/auth/login" `
  -ContentType "application/json" -Body '{"email":"persona@example.com","password":"mi-clave-segura"}'
$headers = @{ Authorization = "Bearer $($login.accessToken)" }
Invoke-RestMethod -Uri "http://localhost:8080/api/events" -Headers $headers
```

El token y la sesión expiran después de 30 minutos sin actividad. Mientras la sesión siga
activa, el cliente puede llamar `POST /api/auth/refresh` antes de que expire el token para
obtener uno nuevo. `POST /api/auth/logout` revoca la sesión actual y responde `204`.
Cada inicio de sesión crea una sesión independiente y los eventos de registro, acceso
exitoso o fallido y cierre se guardan en `auth_activity`. El cliente debe descartar el
token al cerrar sesión o al salir; el cierre de una pestaña por sí solo no puede detectarse
desde este backend.

El Administrador inicial se puede crear al iniciar la aplicación configurando las dos
variables `BOOTSTRAP_ADMIN_EMAIL` y `BOOTSTRAP_ADMIN_PASSWORD`. La contraseña debe tener
entre 8 y 128 caracteres. Si no se configura ninguna, no se crea una cuenta de bootstrap;
si se configura solo una, o el correo ya pertenece a una cuenta que no sea Administrador
activo, la aplicación falla al iniciar. El proceso no eleva privilegios de una cuenta
existente ni reemplaza su contraseña. Una vez creado, se pueden retirar esas variables.

## Administración de usuarios

Todos los siguientes endpoints requieren `ROLE_ADMINISTRADOR`:

- `POST /api/admin/users`: crear cuenta con correo, contraseña y uno o varios roles.
- `GET /api/admin/users?page=0&size=10` y `GET /api/admin/users/{userId}`: consultar cuentas.
- `PUT /api/admin/users/{userId}`: actualizar el correo.
- `POST /api/admin/users/{userId}/roles`: agregar un rol sin quitar los existentes.
- `PATCH /api/admin/users/{userId}/active`: activar o desactivar la cuenta.

Las respuestas no exponen hashes de contraseña. Los cambios administrativos se registran
en `auth_activity`; los tokens de cuentas desactivadas son rechazados inmediatamente. No
se permite desactivar al último Administrador activo. Los roles se incluyen en el JWT, por
lo que las asignaciones nuevas se reflejan cuando el usuario inicia una sesión nueva; no
hay operación de quitar roles, en concordancia con la regla explícita de conservar los
roles ya asignados. La migración `V6__create_user_authentication.sql` crea cuentas, roles,
sesiones y actividad. El vínculo entre una cuenta de Invitado y un participante todavía no
está implementado.

## Módulo de eventos: creación y consulta

`POST /api/events` crea un evento en estado `BORRADOR`. Requiere autenticación y
una autoridad `ROLE_ADMINISTRADOR` o `ROLE_OPERADOR`. El registro/inicio de sesión con JWT
ya está implementado, pero la asignación de roles de administración y la provisión inicial
de una cuenta Admin siguen pendientes. Los tests simulan esos roles.

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
El aforo puede reducirse, pero no por debajo del número de inscripciones activas.
También se rechazan cambios de fecha o modalidad porque todavía no se puede ejecutar el
flujo requerido de confirmación y notificación a inscritos. Los conflictos de asignación
de monitores tampoco se verifican; dependen de un módulo aún no implementado.

La cancelación implementada todavía no invalida tokens QR ni notifica a inscritos; el módulo
de notificaciones está pendiente. Tampoco representa la auditoría general de todas las
escrituras: este historial guarda únicamente cancelaciones.

`POST /api/events/{eventId}/postponements` aplaza un evento `PUBLICADO` con motivo,
nueva fecha y hora. La API rechaza los conflictos de lugar y horario con `409 Conflict`.
El evento queda `APLAZADO` hasta que Admin u Operador confirme la nueva fecha mediante
`POST /api/events/{eventId}/postponements/confirmation`; entonces vuelve a `PUBLICADO`.
Cada solicitud y confirmación se registra como una fila append-only en
`event_postponement_history`, creada por la migración `V3`.

Las notificaciones a inscritos todavía no se envían, pues el módulo de notificaciones está
pendiente. La API conserva el nuevo horario al solicitar el aplazamiento, mientras el estado
permanece `APLAZADO` hasta la confirmación.

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
`participants`, creada por Flyway en `V4__create_participants.sql`.

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

## Inscripción a eventos (RF-013, alcance parcial)

`POST /api/events/{eventId}/registrations` permite a Administrador u Operador inscribir a
un participante existente enviando `{"participantId":"<UUID>"}`. Solo se aceptan eventos
`PUBLICADO`; una inscripción duplicada, el aforo completo, un evento en otro estado o un
cruce horario devuelve `409 Conflict`. El cruce identifica el evento que lo ocasiona.
La inscripción queda asociada al evento, al participante y al usuario que la creó. La tabla
`event_registrations` y sus restricciones se crean con `V7__create_event_registrations.sql`.

Un Administrador puede vincular una ficha existente a una cuenta Invitado mediante
`POST /api/admin/users/{userId}/participant`, enviando
`{"participantId":"<UUID>"}`. La relación es uno a uno: una cuenta Invitado puede tener
una sola ficha y una ficha no puede quedar asociada a más de una cuenta. La operación queda
en el registro de actividad y la relación se crea con `V8__link_guest_accounts_to_participants.sql`.

Una vez vinculada la cuenta, el Invitado se inscribe a sí mismo mediante
`POST /api/events/{eventId}/registrations/self`, sin enviar un identificador de participante.
La API obtiene la ficha asociada a la cuenta autenticada; si no existe vinculación, responde
`409 Conflict` y no crea la inscripción. El endpoint anterior `POST /api/events/{eventId}/registrations`
conserva su uso por Administrador y Operador para inscribir a cualquier participante.

La respuesta `201 Created` incluye un `qrToken` opaco y único, de 32 bytes aleatorios
codificados en Base64 URL-safe. Por ahora se entrega el token, no una imagen QR ni una
operación de lectura/validación del código.

Este incremento todavía no implementa la consulta de multas pendientes; esta regla de RF-013
se difirió para una etapa posterior. Por tanto, RF-013 está implementado parcialmente.
Tampoco se ha probado la migración contra un PostgreSQL real en esta etapa.

## Asistencia y check-in (RF-019 a RF-021, alcance parcial)

Administrador, Operador o Monitor puede registrar asistencia únicamente cuando el evento
está `EN_CURSO` y la inscripción está activa. Para QR, se envía el token recibido al
inscribirse a `POST /api/events/{eventId}/attendance/qr` como
`{"qrToken":"<token>"}`. Un token inválido o uno perteneciente a otro evento se rechaza;
un segundo check-in devuelve `409 Conflict` e incluye `checkedInAt`, la hora del primer
registro.

Para el flujo manual, `GET /api/events/{eventId}/attendance/registrations?q=<documento-o-nombre>`
busca entre las inscripciones activas del evento y devuelve hasta 50 coincidencias, con
nombre y documento para distinguir homónimos. La confirmación se hace en
`POST /api/events/{eventId}/attendance/manual` enviando
`{"participantId":"<UUID>"}`. La respuesta indica método (`QR` o `MANUAL`), hora y usuario
que registró la asistencia; el check-in manual también queda en el registro de actividad.
La migración `V9__create_event_attendance.sql` impone un check-in como máximo por inscripción.

No se implementan todavía la imagen QR descargable, cancelación de inscripciones,
notificaciones, asistencia en tiempo real ni reporte de inasistencias/multas. El bloqueo
por multas continúa pendiente de RF-032.

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
