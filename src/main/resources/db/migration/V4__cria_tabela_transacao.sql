-- V4: cria tabela transacao
-- Bounded context: transacao
-- Etapa 3.6 da Camada 2

CREATE TABLE transacao (
    id                UUID            PRIMARY KEY,
    tipo              VARCHAR(20)     NOT NULL,
    valor_valor       NUMERIC(19, 2)  NOT NULL,
    valor_moeda       VARCHAR(3)      NOT NULL,
    data_transacao    DATE            NOT NULL,
    descricao         VARCHAR(200)    NOT NULL,
    conta_id          UUID            NOT NULL,
    conta_destino_id  UUID,
    categoria_id      UUID,
    criado_em         TIMESTAMPTZ     NOT NULL,
    atualizado_em     TIMESTAMPTZ     NOT NULL,
    CONSTRAINT fk_transacao_conta             FOREIGN KEY (conta_id)         REFERENCES conta (id),
    CONSTRAINT fk_transacao_conta_destino     FOREIGN KEY (conta_destino_id) REFERENCES conta (id),
    CONSTRAINT fk_transacao_categoria         FOREIGN KEY (categoria_id)     REFERENCES categoria (id),
    CONSTRAINT chk_transacao_valor_positivo   CHECK (valor_valor > 0),
    CONSTRAINT chk_transacao_transferencia    CHECK (
        (tipo = 'TRANSFERENCIA' AND conta_destino_id IS NOT NULL AND conta_id <> conta_destino_id AND categoria_id IS NULL)
        OR
        (tipo <> 'TRANSFERENCIA' AND conta_destino_id IS NULL)
    )
);

CREATE INDEX idx_transacao_conta_data ON transacao (conta_id, data_transacao DESC);
CREATE INDEX idx_transacao_categoria ON transacao (categoria_id);
