# SyncEvent

Plataforma web para la **gestión de eventos, certificación y organización**.

El proyecto está organizado como monorepositorio: el backend (API) ya cuenta con una base
funcional en Spring Boot; el frontend y la capa de persistencia de eventos se encuentran en
construcción.

---

## Stack tecnológico

| Capa | Tecnología |
| --- | --- |
| Lenguaje | Java 21 |
| Framework | Spring Boot 4.1.1 (Web MVC, Data JPA, GraphQL, Security, Validation) |
| Base de datos | PostgreSQL 18 |
| Migraciones | Flyway |
| Build | Maven Wrapper (`mvnw`) |
| Tests | JUnit 5 + Spring Boot Test + H2 (perfil `test`) |
| Frontend | Por definir (propuesta: React + Vite + TypeScript) |

---

## Estructura del repositorio

```text
SyncEvent/
├── backend/                 # API REST/GraphQL en Spring Boot
│   ├── src/main/java/com/syncevent/
│   │   ├── SynceventApplication.java
│   │   └── domain/model/    # Modelo de dominio (Event, EventStatus)
│   ├── src/main/resources/
│   │   ├── application.properties
│   │   └── db/migration/    # Migraciones Flyway
│   ├── src/test/            # Tests unitarios y de contexto
│   ├── mvnw / mvnw.cmd
│   └── pom.xml
├── database/                # Scripts de creación de base de datos
│   ├── init.sql
│   └── README.md
├── docs/                    # Documentación técnica y auditoría
│   ├── arquitectura.md
│   ├── desarrollo.md
│   └── auditoria.md
├── frontend/                # Reservado para la aplicación cliente
├── .env.example             # Variables de entorno de referencia
└── README.md
```

---

## Requisitos

- **JDK 21** (debe incluir `javac`; un JRE no es suficiente).
  Verificar con `javac -version`.
- **PostgreSQL 18** en ejecución (local o contenedor).
- **Docker** (opcional, para levantar PostgreSQL rápidamente).
- No es necesario instalar Maven: se usa el wrapper incluido.

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

La aplicación usa PostgreSQL por defecto y permite sobrescribir la conexión sin tocar el código:

| Variable | Valor por defecto | Descripción |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/syncevent` | URL JDBC |
| `DB_USER` | `syncevent` | Usuario de la base |
| `DB_PASSWORD` | `syncevent` | Contraseña (solo desarrollo) |
| `SERVER_PORT` | `8080` | Puerto HTTP |

Copiar `.env.example` y exportar las variables antes de arrancar:

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
Spring Security genera una contraseña de desarrollo que aparece en el log al iniciar
(usuario: `user`).

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
- [x] Conexión a PostgreSQL verificada + Flyway operativo.
- [x] Modelo de dominio inicial `Event` / `EventStatus`.
- [x] Tests unitarios y de contexto.
- [ ] Migraciones del esquema de eventos.
- [ ] Entidades JPA, repositorios y casos de uso.
- [ ] API GraphQL/REST de eventos.
- [ ] Autenticación y autorización por roles.
- [ ] Frontend.

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
