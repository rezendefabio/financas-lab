CREATE TABLE meta (
    id UUID PRIMARY KEY,
    nome VARCHAR(200) NOT NULL,
    valor_alvo_valor NUMERIC(19,2) NOT NULL,
    valor_alvo_moeda VARCHAR(3) NOT NULL,
    valor_atual_valor NUMERIC(19,2) NOT NULL DEFAULT 0,
    valor_atual_moeda VARCHAR(3) NOT NULL,
    prazo DATE NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'EM_ANDAMENTO',
    criado_em TIMESTAMPTZ NOT NULL,
    atualizado_em TIMESTAMPTZ NOT NULL
);
