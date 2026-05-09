-- V2: cria tabela conta
-- Bounded context: conta
-- Etapa 3.3 da Camada 2

CREATE TABLE conta (
    id                    UUID            PRIMARY KEY,
    nome                  VARCHAR(100)    NOT NULL,
    tipo                  VARCHAR(30)     NOT NULL,
    saldo_inicial_valor   NUMERIC(19, 2)  NOT NULL,
    saldo_inicial_moeda   VARCHAR(3)      NOT NULL,
    ativa                 BOOLEAN         NOT NULL,
    criado_em             TIMESTAMPTZ     NOT NULL,
    atualizado_em         TIMESTAMPTZ     NOT NULL
);

CREATE INDEX idx_conta_ativa ON conta (ativa);
