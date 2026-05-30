CREATE TABLE emprestimo (
    id                UUID            PRIMARY KEY,
    user_id           UUID            NOT NULL REFERENCES usuario(id),
    descricao         VARCHAR(100)    NOT NULL,
    nome_terceiro     VARCHAR(100),
    tipo              VARCHAR(20)     NOT NULL CHECK (tipo IN ('CONCEDIDO', 'RECEBIDO')),
    valor_valor       NUMERIC(19,2)   NOT NULL CHECK (valor_valor > 0),
    valor_moeda       VARCHAR(3)      NOT NULL DEFAULT 'BRL',
    data_emprestimo   DATE            NOT NULL,
    quitado           BOOLEAN         NOT NULL DEFAULT false,
    created_at        TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP       NOT NULL DEFAULT now()
);

CREATE INDEX idx_emprestimo_user_id ON emprestimo (user_id);
