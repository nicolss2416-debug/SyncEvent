-- V18: Garantizar un único rol por usuario en user_account_roles y rol INVITADO por defecto

-- 1. Limpiar roles repetidos para cuentas específicas y dejar un solo rol por usuario
DELETE FROM user_account_roles ur
USING user_accounts ua
WHERE ur.user_id = ua.id
  AND ua.email = 'admin@syncevent.com'
  AND ur.role <> 'ADMINISTRADOR';

DELETE FROM user_account_roles ur
USING user_accounts ua
WHERE ur.user_id = ua.id
  AND ua.email = 'operador@syncevent.com'
  AND ur.role <> 'OPERADOR';

DELETE FROM user_account_roles ur
USING user_accounts ua
WHERE ur.user_id = ua.id
  AND ua.email = 'monitor@syncevent.com'
  AND ur.role <> 'MONITOR';

-- Para cualquier otro usuario con múltiples roles (ej. cuentas de participantes/invitados),
-- conservar un único rol priorizando INVITADO cuando exista duplicidad.
DELETE FROM user_account_roles ur
WHERE ur.ctid NOT IN (
    SELECT DISTINCT ON (user_id) ctid
    FROM user_account_roles
    ORDER BY user_id,
             CASE role
                 WHEN 'INVITADO' THEN 1
                 WHEN 'MONITOR' THEN 2
                 WHEN 'OPERADOR' THEN 3
                 WHEN 'ADMINISTRADOR' THEN 4
                 ELSE 5
             END
);

-- 2. Si algún usuario quedó sin rol en user_account_roles, asignarle INVITADO por defecto
INSERT INTO user_account_roles (user_id, role)
SELECT ua.id, 'INVITADO'
FROM user_accounts ua
WHERE NOT EXISTS (
    SELECT 1 FROM user_account_roles ur WHERE ur.user_id = ua.id
);

-- 3. Trigger para que al insertar un nuevo rol para un usuario se reemplace el anterior sin duplicar filas
CREATE OR REPLACE FUNCTION enforce_single_user_role()
RETURNS TRIGGER AS $$
BEGIN
    DELETE FROM user_account_roles
    WHERE user_id = NEW.user_id
      AND role <> NEW.role;
    IF EXISTS (
        SELECT 1 FROM user_account_roles
        WHERE user_id = NEW.user_id
          AND role = NEW.role
    ) THEN
        RETURN NULL;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_user_account_roles_single_role ON user_account_roles;
CREATE TRIGGER trg_user_account_roles_single_role
    BEFORE INSERT ON user_account_roles
    FOR EACH ROW
    EXECUTE FUNCTION enforce_single_user_role();

-- 4. Restricción UNIQUE(user_id) para impedir que un usuario tenga roles repetidos en user_account_roles
ALTER TABLE user_account_roles
    ADD CONSTRAINT uk_user_account_roles_single_user UNIQUE (user_id);
