CREATE TABLE assinatura (
    id                  UUID            PRIMARY KEY,
    user_id             UUID            NOT NULL REFERENCES usuario(id),
    nome                VARCHAR(100)    NOT NULL,
    tipo                VARCHAR(30)     NOT NULL,
    valor_mensal_valor  NUMERIC(19,2)   NOT NULL,
    valor_mensal_moeda  VARCHAR(3)      NOT NULL,
    data_renovacao      DATE            NOT NULL,
    ativa               BOOLEAN         NOT NULL DEFAULT TRUE,
    criado_em           TIMESTAMPTZ     NOT NULL,
    atualizado_em       TIMESTAMPTZ     NOT NULL
);
CREATE INDEX idx_assinatura_user ON assinatura (user_id);
