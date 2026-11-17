# SyncEvent

Plataforma web para la **gestión de eventos, certificación y organización**.

El proyecto está organizado como monorepositorio. El backend implementa los principales
flujos de eventos, usuarios, participantes, inscripciones, asistencia y certificados. El
frontend React integra autenticación, eventos, administración de cuentas, inscripciones de
Invitado y varios flujos de asistencia/certificados; importación masiva, bitácora consultable,
notificaciones y emisión automática de certificados siguen pendientes.

---

## Stack tecnológico

| Capa | Tecnología |
| --- | --- |
| Lenguaje / runtime | Java 21 LTS (`java.version=21` en pom.xml) |
| Framework | Spring Boot 3.4.5 (Web MVC, Data JPA, GraphQL, Security, Validation) |
| Base de datos | PostgreSQL 18 |
| Migraciones | Flyway (23 migraciones versionadas V1..V23) |
| Build | Maven Wrapper (`mvnw`) |
| Tests | JUnit 5 + Spring Boot Test + ArchUnit + H2 (perfil `test`) |
| Frontend | React 18 + Vite 5 + TypeScript |

---

## Estructura del repositorio

```text
SyncEvent/
├── backend/                 # API REST/GraphQL en Spring Boot
│   ├── src/main/java/com/syncevent/
│   │   ├── SynceventApplication.java
│   │   ├── events/          # Bounded context de Eventos
│   │   ├── participants/    # Bounded context de Participantes
│   │   ├── registrations/   # Bounded context de Inscripciones
│   │   ├── attendance/      # Bounded context de Asistencia y Penalizaciones
│   │   ├── certificates/    # Bounded context de Certificados PDF y verificación
│   │   ├── users/           # Bounded context de Cuentas, Roles y Autenticación
│   │   ├── audit/           # Módulo de Auditoría y Trazabilidad
│   │   └── shared/          # Clases transversales, seguridad y excepciones
│   ├── src/main/resources/
│   │   ├── application.properties
│   │   └── db/migration/    # 23 migraciones Flyway (V1..V23)
│   ├── src/test/            # Tests unitarios, integración y arquitectura (ArchUnit)
│   ├── mvnw / mvnw.cmd
│   └── pom.xml
├── database/                # Scripts de creación de base de datos
│   ├── init.sql
│   └── README.md
├── docs/                    # Documentación técnica y auditoría
│   ├── arquitectura.md
│   ├── desarrollo.md
│   └── auditoria.md
├── frontend/                # Aplicación cliente React + TypeScript + Vite
├── .env.example             # Variables de entorno de referencia
└── README.md
```

---

## Requisitos

- **JDK 21 LTS** (debe incluir `javac`). Verificar con `javac -version`.
- **PostgreSQL 18** en ejecución (local o contenedor).
- **Docker** (opcional, para levantar PostgreSQL rápidamente).
- No es necesario instalar Maven: se usa el wrapper incluido (`mvnw` / `mvnw.cmd`).

---

## Configuración de la base de datos

1. Crear el rol y la base de datos locales:

   ```bash
   sudo -u postgres psql -f database/init.sql
   ```

2. Verificar la conexión:

   ```bash
   psql "postgresql://syncevent:syncevent@localhost:5432/syncevent" -c '\conninfo'
   ```

3. Alternativa con Docker:

   ```bash
   docker run -d --name syncevent-db \
     -e POSTGRES_DB=syncevent \
     -e POSTGRES_USER=syncevent \
     -e POSTGRES_PASSWORD=syncevent \
     -p 5432:5432 postgres:18-alpine
   ```

### Variables de entorno

La aplicación usa PostgreSQL por defecto con valores estándar de desarrollo y permite sobrescribir la conexión mediante variables de entorno:

| Variable | Valor por defecto | Descripción |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/syncevent` | URL JDBC |
| `DB_USER` | `syncevent` | Usuario de la base de datos |
| `DB_PASSWORD` | `syncevent` | Contraseña (solo desarrollo local) |
| `JWT_SECRET` | Obligatoria en `prod` | Clave Base64 de al menos 256 bits (32 bytes) |
| `EXPOSE_DEV_OTP` | `false` | Solo `true` en entorno de desarrollo para pruebas |
| `JWT_ACCESS_TOKEN_TTL` | `PT30M` | Vigencia máxima del token de acceso |
| `JWT_IDLE_TIMEOUT` | `PT30M` | Tiempo máximo de inactividad de la sesión |
| `BOOTSTRAP_ADMIN_EMAIL` | Opcional | Correo inicial del Administrador |
| `BOOTSTRAP_ADMIN_PASSWORD` | Opcional | Contraseña inicial del Administrador |
| `SERVER_PORT` | `8080` | Puerto HTTP |

> **Gestión del esquema:** Flyway ejecuta automáticamente las 23 migraciones (`V1` a `V23`) en orden estricto al arrancar el backend. Hibernate opera con `spring.jpa.hibernate.ddl-auto=validate`, asegurando que el esquema validado en base de datos coincida exactamente con las entidades JPA sin alterar tablas en tiempo de ejecución.

Copiar `.env.example`, generar `JWT_SECRET` con una fuente criptográfica segura y exportar las variables antes de arrancar:

```bash
set -a
source .env
set +a
```

---

## Ejecución

```bash
cd backend
./mvnw spring-boot:run
```

La API queda disponible en `http://localhost:8080`.
No se crea un usuario de desarrollo ni se genera una contraseña al iniciar. Usa
`POST /api/auth/register` para crear una cuenta Invitado y `POST /api/auth/login` para
obtener un token. La asignación de roles administrativos y la provisión inicial de Admin
se aprovisiona inicialmente con `BOOTSTRAP_ADMIN_EMAIL` y `BOOTSTRAP_ADMIN_PASSWORD`; luego
el Admin gestiona cuentas y roles desde `/api/admin/users`. Consulta
[backend/README.md](./backend/README.md) para los endpoints y la configuración.

> La conexión se valida al arrancar: HikariCP abre el pool y Flyway aplica las migraciones.
> Si la base no existe o las credenciales son incorrectas, la aplicación no inicia.

---

## Tests

```bash
cd backend
./mvnw test
```

Los tests usan el perfil `test` con H2 en memoria, por lo que **no requieren PostgreSQL**
(ver `src/test/resources/application-test.properties`).

---

## Estado del proyecto

- [x] Estructura base del monorepositorio.
- [x] Backend Spring Boot compilando y arrancando.
- [x] Conexión a PostgreSQL verificada + Flyway operativo (23 migraciones).
- [x] Modelo de dominio inicial `Event` / `EventStatus`.
- [x] Tests unitarios, de contexto y arquitectura hexagonal.
- [x] Registro e inicio de sesión con JWT y roles.
- [x] Protección contra ataques de fuerza bruta y limitación de tasa.
- [x] Administración de usuarios y asignación de roles.
- [ ] Optimización de peticiones frontend (N+1).
- [ ] Auditoría de mutaciones GraphQL.
- [ ] Importación masiva CSV atómica en backend.

---

## Documentación

- [Arquitectura](docs/arquitectura.md)
- [Guía de desarrollo](docs/desarrollo.md)
- [Auditoría técnica y correcciones](docs/auditoria.md)
- [Base de datos](database/README.md)

---

## Convención de commits

Se utiliza [Conventional Commits](https://www.conventionalcommits.org/es/):

```text
feat: nueva funcionalidad
fix: corrección de errores
refactor: cambio interno sin alterar comportamiento
docs: documentación
test: pruebas
chore: tareas de mantenimiento
```

Ejemplo: `fix(domain): validar aforo máximo en Event`.
