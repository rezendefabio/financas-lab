CREATE TABLE emprestimo (
    id              UUID            PRIMARY KEY,
    user_id         UUID            NOT NULL REFERENCES usuario(id),
    descricao       VARCHAR(100)    NOT NULL,
    nome_terceiro   VARCHAR(100),
    tipo            VARCHAR(20)     NOT NULL,
    valor_amount    NUMERIC(19,4)   NOT NULL,
    valor_currency  VARCHAR(3)      NOT NULL DEFAULT 'BRL',
    data_emprestimo DATE            NOT NULL,
    quitado         BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP       NOT NULL,
    updated_at      TIMESTAMP       NOT NULL
);

CREATE INDEX idx_emprestimo_user ON emprestimo (user_id);
CREATE INDEX idx_emprestimo_user_data ON emprestimo (user_id, data_emprestimo);
