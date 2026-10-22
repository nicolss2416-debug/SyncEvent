# Base de datos

## Creación local

```bash
sudo -u postgres psql -f database/init.sql
```

El script crea de forma idempotente:

- Rol `syncevent` con contraseña `syncevent` (solo para desarrollo).
- Base de datos `syncevent` cuyo propietario es ese rol.

## Configuración de la aplicación

`backend/src/main/resources/application.properties` toma estos valores por defecto:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/syncevent
spring.datasource.username=syncevent
spring.datasource.password=syncevent
```

Se pueden sobrescribir con `DB_URL`, `DB_USER` y `DB_PASSWORD`.

## Migraciones

Flyway se ejecuta automáticamente al arrancar la aplicación y busca los scripts en:

```text
backend/src/main/resources/db/migration/
```

La nomenclatura es `V<número>__<descripción>.sql`, por ejemplo:

```text
V1__crear_tabla_events.sql
```

Cada migración aplicada queda registrada en la tabla `flyway_schema_history`.

## Verificación

```bash
psql "postgresql://syncevent:syncevent@localhost:5432/syncevent" \
  -c "select version, description, success from flyway_schema_history;"
```

## Nota de seguridad

Las credenciales incluidas son únicamente para desarrollo local. En otros entornos se deben
definir por variables de entorno o un gestor de secretos.
