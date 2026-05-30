CREATE TABLE lembrete (
    id              UUID            PRIMARY KEY,
    user_id         UUID            NOT NULL REFERENCES usuario(id),
    titulo          VARCHAR(100)    NOT NULL,
    descricao       VARCHAR(500),
    data_lembrete   DATE            NOT NULL,
    prioridade      VARCHAR(10)     NOT NULL,
    concluido       BOOLEAN         NOT NULL DEFAULT FALSE,
    criado_em       TIMESTAMP       NOT NULL,
    atualizado_em   TIMESTAMP       NOT NULL
);

CREATE INDEX idx_lembrete_user_data ON lembrete (user_id, data_lembrete);
