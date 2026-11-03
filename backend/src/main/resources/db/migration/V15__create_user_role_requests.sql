CREATE TABLE user_role_requests (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES user_accounts(id),
    requested_role VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    requested_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    resolved_by UUID REFERENCES user_accounts(id),
    resolved_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT ck_user_role_requests_role
        CHECK (requested_role IN ('ADMINISTRADOR', 'OPERADOR', 'MONITOR')),
    CONSTRAINT ck_user_role_requests_status
        CHECK (status IN ('PENDIENTE', 'APROBADA', 'RECHAZADA'))
);

CREATE INDEX idx_user_role_requests_pending
    ON user_role_requests (status, requested_at);

CREATE INDEX idx_user_role_requests_user
    ON user_role_requests (user_id, requested_at DESC);
