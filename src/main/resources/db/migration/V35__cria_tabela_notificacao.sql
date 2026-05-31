CREATE TABLE notificacao (
    id              UUID            PRIMARY KEY,
    user_id         UUID            NOT NULL REFERENCES usuario(id),
    tipo            VARCHAR(30)     NOT NULL,
    referencia_id   UUID            NOT NULL,
    titulo          VARCHAR(100)    NOT NULL,
    descricao       VARCHAR(300)    NOT NULL,
    descartada      BOOLEAN         NOT NULL DEFAULT FALSE,
    criado_em       TIMESTAMPTZ     NOT NULL,
    atualizado_em   TIMESTAMPTZ     NOT NULL,
    -- chave natural: uma notificacao por (usuario, tipo, referencia).
    -- A reconciliacao faz upsert por essa chave.
    CONSTRAINT uq_notificacao_chave UNIQUE (user_id, tipo, referencia_id)
);

CREATE INDEX idx_notificacao_user ON notificacao (user_id);

-- referencia_id aponta para orcamento(id) OU meta(id) conforme o tipo -- sem FK
-- explicita (referencia polimorfica entre bounded contexts; integridade via
-- aplicacao, e a linha de origem pode ser removida sem orfanizar a UI: a
-- reconciliacao deleta a notificacao quando a condicao se resolve).
