CREATE TABLE anotacao (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL REFERENCES usuario(id),
    titulo VARCHAR(200) NOT NULL,
    conteudo TEXT,
    tipo VARCHAR(50) NOT NULL,
    prioridade VARCHAR(20) NOT NULL DEFAULT 'MEDIA',
    valor_montante NUMERIC(19,2),
    valor_moeda VARCHAR(3),
    data_referencia DATE,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_anotacao_usuario_id ON anotacao(usuario_id);
CREATE INDEX idx_anotacao_tipo ON anotacao(tipo);
CREATE INDEX idx_anotacao_prioridade ON anotacao(prioridade);
