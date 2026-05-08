-- V1__schema_inicial.sql
--
-- Migration inicial do schema. Cria a tabela placeholder __healthcheck que
-- existe apenas para validar que o pipeline Flyway -> Postgres -> Spring
-- funciona end-to-end. Pode ser referenciada pelo endpoint de healthcheck
-- da Etapa 2.3.
--
-- Convencao: tabelas com prefixo `__` (dois underscores) sao tecnicas/
-- internas, nao representam dominio de negocio.

CREATE TABLE __healthcheck (
    id          BIGSERIAL PRIMARY KEY,
    verificado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Insere uma linha placeholder para que SELECT count(*) > 0 seja sempre
-- verdadeiro mesmo sem inserts da aplicacao.
INSERT INTO __healthcheck DEFAULT VALUES;
