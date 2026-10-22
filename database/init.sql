-- ============================================================
-- SyncEvent - creación de rol y base de datos para desarrollo
-- Uso: sudo -u postgres psql -f database/init.sql
-- ============================================================

-- Rol de aplicación (idempotente)
DO
$$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'syncevent') THEN
        CREATE ROLE syncevent WITH LOGIN PASSWORD 'syncevent';
    END IF;
END
$$;

-- Base de datos (idempotente mediante \gexec de psql)
SELECT 'CREATE DATABASE syncevent OWNER syncevent'
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'syncevent')\gexec

GRANT ALL PRIVILEGES ON DATABASE syncevent TO syncevent;
