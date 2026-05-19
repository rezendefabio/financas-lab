CREATE TABLE audit_log (
    id UUID PRIMARY KEY,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    action VARCHAR(20) NOT NULL,
    user_email VARCHAR(200),
    screen_code VARCHAR(20),
    before_state TEXT,
    after_state TEXT,
    criado_em TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_audit_action CHECK (action IN ('CREATE', 'UPDATE', 'DELETE'))
);

CREATE INDEX idx_audit_log_entity ON audit_log (entity_type, entity_id, criado_em DESC);
CREATE INDEX idx_audit_log_user ON audit_log (user_email, criado_em DESC);
CREATE INDEX idx_audit_log_criado_em ON audit_log (criado_em DESC);
