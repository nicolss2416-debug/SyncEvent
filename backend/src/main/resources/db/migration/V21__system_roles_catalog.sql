-- =============================================================================
-- V21: Catálogo de roles del sistema y validación de integridad referencial.
-- =============================================================================

CREATE TABLE IF NOT EXISTS roles (
    name VARCHAR(30) PRIMARY KEY,
    display_name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO roles (name, display_name, description) VALUES
    ('ADMINISTRADOR', 'Administrador', 'Gestión total del sistema, configuración, cuentas y auditoría.'),
    ('OPERADOR', 'Operador', 'Creación y administración de eventos, inscripciones y reportes.'),
    ('MONITOR', 'Monitor', 'Control de asistencia y verificación en campo.'),
    ('INVITADO', 'Invitado', 'Visualización del catálogo de eventos y gestión de su propia inscripción.')
ON CONFLICT (name) DO NOTHING;

-- Asegurar integridad referencial: el rol debe existir en la tabla de roles
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_user_account_roles_role'
    ) THEN
        ALTER TABLE user_account_roles
            ADD CONSTRAINT fk_user_account_roles_role
            FOREIGN KEY (role) REFERENCES roles (name);
    END IF;
END $$;

-- Índice para búsquedas rápidas por rol
CREATE INDEX IF NOT EXISTS idx_user_account_roles_role
    ON user_account_roles (role);
