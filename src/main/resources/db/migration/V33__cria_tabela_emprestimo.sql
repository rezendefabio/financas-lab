CREATE TABLE emprestimo (
    id                  UUID            PRIMARY KEY,
    user_id             UUID            NOT NULL REFERENCES usuario(id),
    descricao           VARCHAR(100)    NOT NULL,
    nome_terceiro       VARCHAR(100),
    tipo                VARCHAR(30)     NOT NULL,
    valor_valor         NUMERIC(19,2)   NOT NULL,
    valor_moeda         VARCHAR(3)      NOT NULL,
    data_emprestimo     DATE            NOT NULL,
    quitado             BOOLEAN         NOT NULL DEFAULT FALSE,
    criado_em           TIMESTAMPTZ     NOT NULL,
    atualizado_em       TIMESTAMPTZ     NOT NULL
);
CREATE INDEX idx_emprestimo_user ON emprestimo (user_id);
