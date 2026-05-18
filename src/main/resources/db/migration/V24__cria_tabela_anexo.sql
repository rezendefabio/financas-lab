CREATE TABLE anexo (
    id UUID PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    tipo_conteudo VARCHAR(100) NOT NULL,
    tamanho BIGINT NOT NULL,
    chave_armazenamento VARCHAR(500) NOT NULL,
    entidade_tipo VARCHAR(50) NOT NULL,
    entidade_id UUID NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_anexo_entidade ON anexo (entidade_tipo, entidade_id);
