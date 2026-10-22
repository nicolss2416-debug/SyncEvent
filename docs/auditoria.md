# Auditoría técnica y correcciones

**Fecha:** 22 de octubre de 2026
**Alcance:** estructura del repositorio, backend Spring Boot, modelo de dominio, configuración de base de datos y tests.
**Resultado:** el backend compila, pasa la suite de tests y arranca conectándose a PostgreSQL 18 con Flyway operativo.

---

## 1. Resumen ejecutivo

| Métrica | Antes | Después |
| --- | --- | --- |
| Compilación | Fallaba (`release version 21 not supported`) | Exitosa |
| Tests | No ejecutables | 7 tests aprobados |
| Conexión a base de datos | Sin configurar | Verificada contra PostgreSQL 18.6 |
| Modelo de dominio | No compilaba (`EventStatus` inexistente) | Compila y valida datos |
| Documentación | Solo un README de una línea | README + 3 documentos técnicos + guía de BD |
| Organización | `Backend`/`Frontend` en mayúscula, `src` suelto | Estructura estándar en minúscula |

---

## 2. Hallazgos

| ID | Severidad | Archivo / zona | Problema | Estado |
| --- | --- | --- | --- | --- |
| A-01 | Crítica | `src/main/java/.../Event.java` | Referenciaba `EventStatus`, que no existía: el proyecto no compilaba | Corregido |
| A-02 | Crítica | `application.properties` | No había configuración de datasource; JPA y Flyway no podían iniciar | Corregido |
| A-03 | Crítica | `src/` en la raíz | Código fuera del módulo Maven, por lo que no se compilaba | Corregido |
| A-04 | Alta | `Event.java` | `attendeeIds` quedaba en `null`; cualquier lectura provocaba `NullPointerException` | Corregido |
| A-05 | Alta | `Event.java` | La lista de asistentes se exponía y mutaba directamente | Corregido |
| A-06 | Alta | `SynceventApplicationTests` | `@SpringBootTest` intentaba usar PostgreSQL y fallaba sin base | Corregido con perfil `test` + H2 |
| A-07 | Media | `Event.java` | Sin validación: nombre vacío, fecha nula o aforo ≤ 0 eran aceptados | Corregido |
| A-08 | Media | `pom.xml` | Metadatos vacíos (`<name/>`, `<url/>`, licencias/desarrolladores sin datos) | Corregido |
| A-09 | Media | Estructura del repo | `Backend` y `Frontend` no coincidían con `backend`/`frontend` del remoto | Corregido |
| A-10 | Media | `Event.java` | `updatedAt` se actualizaba con lógica duplicada en cada setter | Corregido con `touch()` |
| A-11 | Baja | `Event.java` | Sin `equals`/`hashCode`, comportamiento incorrecto en colecciones | Corregido |
| A-12 | Baja | `pom.xml` | Sin base de datos para tests | Corregido (H2 en scope `test`) |
| A-13 | Baja | Documentación | Documentación insuficiente | Corregido |
| A-14 | Informativo | Entorno | Solo había JRE 25, sin `javac` | Documentado |
| A-15 | Informativo | `application.properties` | Sin `open-in-view=false` ni zona horaria UTC | Corregido |

---

## 3. Correcciones línea por línea

### 3.1 `backend/src/main/java/com/syncevent/domain/model/Event.java`

Archivo original en `src/main/java/com/syncevent/syncevent/domain/model/Event.java`.
Se movió al módulo Maven y se renombró el paquete.

| Línea final | Antes | Después | Motivo |
| --- | --- | --- | --- |
| 1 | `package com.syncevent.syncevent.domain.model;` | `package com.syncevent.domain.model;` | El segmento `syncevent` estaba duplicado y no coincidía con la clase principal `com.syncevent.SynceventApplication`. |
| 4 | Sin `ArrayList` | `import java.util.ArrayList;` | Necesario para inicializar y copiar la lista de asistentes. |
| 6 | Sin `Objects` | `import java.util.Objects;` | Validaciones nulas y `hashCode` seguros. |
| 9-12 | Javadoc de una línea | Javadoc descriptivo | Documentación del contrato de la clase. |
| 26-31 | `public Event() { }` | Inicializa `attendeeIds = new ArrayList<>()` | Evita `NullPointerException` al consultar asistentes. |
| 33-42 | Sin Javadoc | Contrato documentado (`@param`, `@throws`) | Deja explícitas las precondiciones. |
| 44 | `this.id = UUID...` directo | `this();` como primera sentencia | Reutiliza la inicialización del constructor vacío. |
| 46 | `this.name = name;` | `this.name = requireText(name, "name");` | Rechaza nombres nulos, vacíos o en blanco. |
| 48 | `this.dateTime = dateTime;` | `Objects.requireNonNull(dateTime, ...)` | La fecha es obligatoria. |
| 50 | `this.maxCapacity = maxCapacity;` | `this.maxCapacity = requireCapacity(maxCapacity);` | El aforo debe ser mayor que cero. |
| 52-54 | `updatedAt = LocalDateTime.now()` repetido | Se calcula `now` una sola vez | Consistencia entre `createdAt` y `updatedAt`. |
| 69-71 | `this.name = name; updatedAt = now()` | Valida y delega en `touch()` | Validación + lógica de actualización única. |
| 87-89 | `this.dateTime = dateTime; updatedAt = now()` | Valida y delega en `touch()` | Consistencia. |
| 105-107 | `this.maxCapacity = maxCapacity; ...` | Valida y delega en `touch()` | Consistencia. |
| 110-116 | `return attendeeIds;` | `return List.copyOf(attendeeIds);` | Copia inmutable: impide mutaciones externas. |
| 118-126 | `this.attendeeIds = attendeeIds;` | Copia defensiva, `null` → lista vacía | Evita aliasing y nulos. |
| 153-155 | Lógica repetida en cada setter | Método privado `touch()` | Elimina duplicación (DRY). |
| 157-162 | Sin validación | `requireText(...)` | Regla de nombre obligatorio centralizada. |
| 164-169 | Sin validación | `requireCapacity(...)` | Regla de aforo positivo centralizada. |
| 171-185 | Sin `equals`/`hashCode` | Igualdad por `id` | Identidad correcta en `Set`/`Map`. |
| 187-196 | `toString` existente | Sin cambios funcionales | Se conserva el formato de depuración. |

### 3.2 `backend/src/main/java/com/syncevent/domain/model/EventStatus.java` (nuevo)

| Línea | Contenido | Motivo |
| --- | --- | --- |
| 1 | `package com.syncevent.domain.model;` | Coherencia con `Event`. |
| 8-9 | `DRAFT` | Estado inicial usado por el constructor. |
| 12-13 | `PUBLISHED` | Evento visible para asistentes. |
| 15-16 | `CANCELLED` | Evento cancelado. |
| 18-19 | `FINISHED` | Evento finalizado. |

Sin este archivo el proyecto no compilaba (hallazgo **A-01**).

### 3.3 `backend/pom.xml`

| Línea final | Antes | Después | Motivo |
| --- | --- | --- | --- |
| 14 | `<name/>` | `<name>SyncEvent Backend</name>` | Metadato vacío. |
| 15 | `<description/>` | `<description>API de gestión...</description>` | Metadato vacío. |
| 16-18 | Bloques `<url/>`, `<licenses>`, `<developers>`, `<scm>` vacíos | Eliminados | XML inválido a nivel semántico y ruido. |
| 89-93 | Sin H2 | Dependencia `com.h2database:h2` con `scope=test` | Permite ejecutar tests sin PostgreSQL. |

### 3.4 `backend/src/main/resources/application.properties`

Antes solo existía la línea 1 (`spring.application.name=syncevent`).

| Línea final | Contenido | Motivo |
| --- | --- | --- |
| 2 | `server.port=${SERVER_PORT:8080}` | Puerto configurable. |
| 7 | `spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/syncevent}` | URL JDBC con valor de desarrollo y override por entorno. |
| 8 | `spring.datasource.username=${DB_USER:syncevent}` | Usuario configurable. |
| 9 | `spring.datasource.password=${DB_PASSWORD:syncevent}` | Contraseña configurable (solo desarrollo). |
| 10 | `spring.datasource.driver-class-name=org.postgresql.Driver` | Driver explícito. |
| 14 | `spring.jpa.hibernate.ddl-auto=validate` | Hibernate no altera el esquema; lo gobierna Flyway. |
| 15 | `spring.jpa.open-in-view=false` | Evita mantener sesiones abiertas durante la vista/respuesta. |
| 16 | `spring.jpa.properties.hibernate.jdbc.time_zone=UTC` | Fechas coherentes entre JVM y base. |
| 19 | `spring.flyway.enabled=true` | Migraciones automáticas al arrancar. |
| 20 | `spring.flyway.locations=classpath:db/migration` | Ubicación explícita de los scripts. |

### 3.5 `backend/src/test/resources/application-test.properties` (nuevo)

| Línea | Contenido | Motivo |
| --- | --- | --- |
| 2 | `jdbc:h2:mem:syncevent;MODE=PostgreSQL;DB_CLOSE_DELAY=-1` | Base en memoria con sintaxis compatible. |
| 3-5 | `sa`, contraseña vacía, driver H2 | Conexión de test. |
| 7 | `ddl-auto=none` | No se requiere esquema. |
| 10 | `flyway.enabled=false` | Flyway no aplica migraciones de PostgreSQL en H2. |

### 3.6 `backend/src/test/java/com/syncevent/SynceventApplicationTests.java`

| Línea final | Antes | Después | Motivo |
| --- | --- | --- | --- |
| 5 | Sin import | `import ...ActiveProfiles;` | Activar el perfil de test. |
| 8 | Sin anotación | `@ActiveProfiles("test")` | Evita que el test dependa de PostgreSQL. |

### 3.7 `backend/src/test/java/com/syncevent/domain/model/EventTest.java` (nuevo)

| Línea | Prueba | Qué cubre |
| --- | --- | --- |
| 20-26 | `creaEventoEnEstadoBorrador` | Estado inicial y lista vacía. |
| 28-37 | `rechazaNombreVacio` | Validación de nombre. |
| 39-47 | `rechazaAforoInvalido` | Validación de aforo. |
| 49-58 | `rechazaFechaNula` | Fecha obligatoria. |
| 60-66 | `noPermiteModificarLaLista...` | Inmutabilidad de la copia devuelta. |
| 68-77 | `almacenaUnaCopiaDefensiva...` | Copia defensiva en el setter. |

### 3.8 Estructura y documentación

| Cambio | Detalle |
| --- | --- |
| `src/` → `backend/src/` | El código estaba fuera del proyecto Maven; ahora se compila con `./mvnw`. |
| `Backend/` → `backend/` | Alineado con la estructura del repositorio remoto (`backend/.gitkeep`). |
| `Frontend/` → `frontend/` | Misma razón. |
| `.gitignore` raíz | Añadidas reglas Java/Maven (`target/`, `*.class`) y de entorno (`.env`). |
| `database/init.sql` | Script idempotente para crear rol y base. |
| `.env.example` | Variables de conexión documentadas. |
| `frontend/README.md` | Estado y propuesta tecnológica. |
| `README.md`, `docs/*` | Documentación completa en español. |

---

## 4. Verificación de compilación y tests

El equipo solo tenía un JRE (sin `javac`), lo que producía:

```text
[ERROR] Fatal error compiling: error: release version 21 not supported
```

Para la verificación se usó un JDK 21 temporal:

```bash
JAVA_HOME=/tmp/opencode/jdk21 PATH=/tmp/opencode/jdk21/bin:$PATH ./mvnw -B test
```

Resultado:

```text
Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

> **Acción recomendada para el equipo:** instalar el JDK 21 (`sudo dnf install java-21-openjdk-devel`)
> para que `./mvnw` funcione sin configurar `JAVA_HOME` manualmente.

---

## 5. Verificación de la conexión a la base de datos

### 5.1 Estado inicial

`application.properties` solo declaraba el nombre de la aplicación. Con `spring-boot-starter-data-jpa`
y el driver de PostgreSQL, Spring Boot no tenía URL, usuario ni contraseña, por lo que la
conexión era imposible (hallazgo **A-02**).

### 5.2 Prueba realizada

Se levantó PostgreSQL 18.6 en un contenedor temporal con las mismas credenciales que la
configuración por defecto:

```bash
docker run -d --name syncevent-db-test \
  -e POSTGRES_DB=syncevent \
  -e POSTGRES_USER=syncevent \
  -e POSTGRES_PASSWORD=syncevent \
  -p 5433:5432 postgres:18-alpine

DB_URL='jdbc:postgresql://localhost:5433/syncevent' \
DB_USER=syncevent DB_PASSWORD=syncevent SERVER_PORT=8081 \
java -jar target/syncevent-0.0.1-SNAPSHOT.jar
```

Evidencia en el log:

```text
HikariPool-1 - Added connection org.postgresql.jdbc.PgConnection@...
FlywayExecutor : Database: jdbc:postgresql://localhost:5433/syncevent (PostgreSQL 18.6)
Tomcat started on port 8081 (http)
Started SynceventApplication in 4.538 seconds
```

Comprobación en la base:

```text
 Schema |         Name          | Type  |  Owner
--------+-----------------------+-------+----------
 public | flyway_schema_history | table | syncevent
```

Conclusión: **la configuración de conexión es correcta**; el pool de conexiones se crea,
Flyway se ejecuta y la aplicación arranca.

### 5.3 Limitación del entorno local

La instancia PostgreSQL del equipo (`localhost:5432`) no tiene el rol `senafactory` y no se
disponía de la contraseña del superusuario `postgres`, por lo que no fue posible crear allí
la base durante la auditoría. Para reproducirla:

```bash
sudo -u postgres psql -f database/init.sql
psql "postgresql://syncevent:syncevent@localhost:5432/syncevent" -c '\conninfo'
```

Si el comando anterior responde `You are connected to database "syncevent"`, el entorno local
queda listo y `./mvnw spring-boot:run` conectará sin variables adicionales.

---

## 6. Recomendaciones pendientes (priorizadas)

1. **Alta — Persistencia:** crear la entidad JPA de eventos, su repositorio y la migración
   `V1__crear_tabla_events.sql`.
2. **Alta — Contrato GraphQL:** definir el esquema (`graphql/schema.graphqls`) y los resolvers.
3. **Alta — Seguridad:** sustituir la configuración por defecto por autenticación explícita
   (JWT o sesión) y roles.
4. **Media — Pruebas de integración:** añadir Testcontainers para validar PostgreSQL real en CI.
5. **Media — CI/CD:** pipeline que ejecute `./mvnw verify` en cada push.
6. **Media — Flyway:** primera migración versionada y política de rollback.
7. **Baja — Calidad:** añadir Checkstyle/Spotless y cobertura con JaCoCo.
8. **Baja — Actuator:** exponer `/actuator/health` para monitoreo.
