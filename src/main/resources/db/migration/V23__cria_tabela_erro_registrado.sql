CREATE TABLE erro_registrado (
    id UUID PRIMARY KEY,
    codigo VARCHAR(12) NOT NULL,
    operacao VARCHAR(100),
    classe_erro VARCHAR(200),
    mensagem VARCHAR(500),
    stack_trace TEXT,
    criado_em TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_erro_registrado_codigo UNIQUE (codigo)
);

CREATE INDEX idx_erro_registrado_criado_em ON erro_registrado(criado_em);
