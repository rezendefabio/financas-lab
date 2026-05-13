CREATE TABLE orcamento (
    id                    UUID            PRIMARY KEY,
    categoria_id          UUID            NOT NULL,
    valor_limite_valor    NUMERIC(19, 2)  NOT NULL,
    valor_limite_moeda    VARCHAR(3)      NOT NULL,
    mes_ano               DATE            NOT NULL,
    ativo                 BOOLEAN         NOT NULL DEFAULT TRUE,
    criado_em             TIMESTAMPTZ     NOT NULL,
    atualizado_em         TIMESTAMPTZ     NOT NULL,
    CONSTRAINT fk_orcamento_categoria FOREIGN KEY (categoria_id) REFERENCES categoria(id)
);
