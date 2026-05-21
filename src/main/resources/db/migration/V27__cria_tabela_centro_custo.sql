-- V27: cria tabela centro_custo
-- Agrupador classificatorio ortogonal a categoria, escopo por usuario.

CREATE TABLE centro_custo (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    nome VARCHAR(100) NOT NULL,
    descricao VARCHAR(255),
    ativo BOOLEAN NOT NULL DEFAULT true,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Unicidade case-insensitive de nome por usuario (espelha padrao V20 para categoria)
CREATE UNIQUE INDEX ux_centro_custo_user_lower_nome
    ON centro_custo (user_id, lower(nome));
