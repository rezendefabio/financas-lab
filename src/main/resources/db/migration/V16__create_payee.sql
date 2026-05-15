CREATE TABLE payee (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    nome VARCHAR(100) NOT NULL,
    categoria_padrao_id UUID,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_payee_user_nome UNIQUE (user_id, nome)
);
