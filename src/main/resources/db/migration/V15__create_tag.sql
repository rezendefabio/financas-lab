CREATE TABLE tag (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    nome VARCHAR(50) NOT NULL,
    cor VARCHAR(7),
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_tag_user_nome UNIQUE (user_id, nome)
);
