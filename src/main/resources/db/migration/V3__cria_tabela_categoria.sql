-- V3: cria tabela categoria
-- Bounded context: categoria
-- Etapa 3.5 da Camada 2

CREATE TABLE categoria (
    id            UUID            PRIMARY KEY,
    nome          VARCHAR(100)    NOT NULL,
    tipo          VARCHAR(20)     NOT NULL,
    criado_em     TIMESTAMPTZ     NOT NULL,
    atualizado_em TIMESTAMPTZ     NOT NULL
);

CREATE INDEX idx_categoria_tipo ON categoria (tipo);
