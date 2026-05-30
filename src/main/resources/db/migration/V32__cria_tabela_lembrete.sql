-- V32: cria tabela lembrete
-- Lembretes pessoais do usuario autenticado.

CREATE TABLE lembrete (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID            NOT NULL REFERENCES usuario(id),
    titulo          VARCHAR(100)    NOT NULL,
    descricao       VARCHAR(500),
    data_lembrete   DATE            NOT NULL,
    prioridade      VARCHAR(10)     NOT NULL,
    concluido       BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP
);

CREATE INDEX idx_lembrete_user_data ON lembrete (user_id, data_lembrete);
