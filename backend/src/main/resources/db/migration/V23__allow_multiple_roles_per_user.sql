-- V23: permitir que una misma cuenta tenga varios roles.
--
-- La migración V18 impuso un único rol por usuario mediante un trigger y una
-- restricción UNIQUE(user_id). Eso impide cumplir la regla de negocio que
-- permite asignar uno o varios roles a una misma persona, por lo que se
-- eliminan ambas restricciones. La tabla ya tiene como clave primaria
-- compuesta (user_id, role), que impide duplicar el mismo rol.

DROP TRIGGER IF EXISTS trg_user_account_roles_single_role ON user_account_roles;

DROP FUNCTION IF EXISTS enforce_single_user_role();

ALTER TABLE user_account_roles
    DROP CONSTRAINT IF EXISTS uk_user_account_roles_single_user;
