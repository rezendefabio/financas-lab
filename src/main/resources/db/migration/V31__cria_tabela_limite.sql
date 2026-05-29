-- V31: cria tabela limite
-- Regra de limite de gasto definida pelo usuario, escopo por usuario.

CREATE TABLE limite (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID            NOT NULL,
    nome            VARCHAR(100)    NOT NULL,
    tipo            VARCHAR(30)     NOT NULL,
    valor           NUMERIC(19,2)   NOT NULL,
    moeda           VARCHAR(3)      NOT NULL,
    ativo           BOOLEAN         NOT NULL DEFAULT TRUE,
    criado_em       TIMESTAMPTZ     NOT NULL DEFAULT now(),
    atualizado_em   TIMESTAMPTZ     NOT NULL DEFAULT now()
);

-- Indice de busca por usuario (sem FK para outros bounded contexts).
CREATE INDEX idx_limite_user_id ON limite (user_id);
