CREATE TABLE lancamento_recorrente (
    id UUID PRIMARY KEY,
    descricao VARCHAR(200) NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    valor_valor NUMERIC(19,2) NOT NULL,
    valor_moeda VARCHAR(3) NOT NULL,
    conta_id UUID NOT NULL,
    categoria_id UUID,
    periodicidade VARCHAR(20) NOT NULL,
    proxima_ocorrencia DATE NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMPTZ NOT NULL,
    atualizado_em TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_lancamento_recorrente_conta FOREIGN KEY (conta_id) REFERENCES conta(id),
    CONSTRAINT fk_lancamento_recorrente_categoria FOREIGN KEY (categoria_id) REFERENCES categoria(id)
);
