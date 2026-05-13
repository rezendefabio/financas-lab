CREATE TABLE usuario (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    senha_hash VARCHAR(255) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_usuario_email UNIQUE (email)
);
