CREATE TABLE fatura (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    conta_id UUID NOT NULL,
    nome VARCHAR(100) NOT NULL,
    data_vencimento DATE NOT NULL,
    data_fechamento DATE,
    valor_total_valor NUMERIC(19,2),
    valor_total_moeda VARCHAR(3),
    paga BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em TIMESTAMPTZ NOT NULL,
    atualizado_em TIMESTAMPTZ NOT NULL
);
