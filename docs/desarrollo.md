# Guía de desarrollo

## 1. Preparar el entorno

### JDK

Se requiere **JDK 21 con `javac`** (un JRE no sirve para compilar):

```bash
javac -version   # debe responder 21.x
```

En Fedora/RHEL:

```bash
sudo dnf install java-21-openjdk-devel
```

### Base de datos

Opción A, PostgreSQL local:

```bash
sudo -u postgres psql -f database/init.sql
```

Opción B, contenedor:

```bash
docker run -d --name syncevent-db \
  -e POSTGRES_DB=syncevent \
  -e POSTGRES_USER=syncevent \
  -e POSTGRES_PASSWORD=syncevent \
  -p 5432:5432 postgres:18-alpine
```

### Variables de entorno

```bash
cp .env.example .env
set -a && source .env && set +a
```

| Variable | Defecto |
| --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/syncevent` |
| `DB_USER` | `syncevent` |
| `DB_PASSWORD` | `syncevent` |
| `SERVER_PORT` | `8080` |

## 2. Ejecutar la aplicación

```bash
cd backend
./mvnw spring-boot:run
```

Al arrancar se comprueba la conexión y Flyway aplica las migraciones pendientes.
Spring Security imprime una contraseña de desarrollo en el log (usuario `user`).

## 3. Ejecutar los tests

```bash
cd backend
./mvnw test
```

- Perfil `test`: H2 en memoria, Flyway deshabilitado.
- Tests de dominio: `EventTest`.
- Test de contexto: `SynceventApplicationTests`.

Para empaquetar:

```bash
./mvnw clean package
java -jar target/syncevent-0.0.1-SNAPSHOT.jar
```

## 4. Base de datos y migraciones

- Ubicación: `backend/src/main/resources/db/migration/`.
- Formato: `V<n>__<descripcion>.sql`.
- Nunca modificar una migración ya aplicada; crear una nueva.
- `spring.jpa.hibernate.ddl-auto=validate` impide cambios automáticos de esquema.

Verificar el estado:

```bash
psql "postgresql://syncevent:syncevent@localhost:5432/syncevent" \
  -c "select installed_rank, version, description, success from flyway_schema_history;"
```

## 5. Flujo de trabajo con Git

Ramas existentes: `main` (estable) y `develop` (integración).

```bash
git switch develop
git pull origin develop
git switch -c feat/nombre-corto
# desarrollar y probar
git add .
git commit -m "feat(modulo): descripción"
git push -u origin feat/nombre-corto
```

Convención de mensajes (Conventional Commits):

| Prefijo | Uso |
| --- | --- |
| `feat` | Nueva funcionalidad |
| `fix` | Corrección de errores |
| `refactor` | Reestructuración sin cambio funcional |
| `test` | Pruebas |
| `docs` | Documentación |
| `chore` | Mantenimiento, dependencias, configuración |

## 6. Solución de problemas

| Síntoma | Causa probable | Solución |
| --- | --- | --- |
| `release version 21 not supported` | Solo hay JRE instalado | Instalar JDK 21 (`java-21-openjdk-devel`) |
| `Connection refused` a PostgreSQL | Servicio detenido | `sudo systemctl start postgresql` |
| `password authentication failed` | Credenciales incorrectas | Revisar `DB_USER` / `DB_PASSWORD` o ejecutar `database/init.sql` |
| `database "syncevent" does not exist` | Base no creada | Ejecutar `database/init.sql` |
| Flyway falla con `validate` | Entidades y esquema desalineados | Crear la migración correspondiente |
| `relation "..." does not exist` | Migración no aplicada | Revisar `db/migration` y reiniciar |
