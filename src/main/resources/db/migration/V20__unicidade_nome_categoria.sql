-- V20: unicidade de nome por usuario e por categoria de sistema
-- Elimina duplicatas existentes, reatribuindo FKs antes de deletar
-- Tabelas com FK para categoria: transacao, orcamento, lancamento_recorrente

-- ===========================================================
-- CATEGORIAS DE USUARIO: reatribuir FKs antes de deletar
-- ===========================================================

-- Passo 1a: reatribuir transacoes que referenciam duplicatas (usuario)
WITH keepers AS (
    SELECT DISTINCT ON (nome, user_id)
           id AS keep_id,
           nome,
           user_id
    FROM categoria
    WHERE user_id IS NOT NULL
    ORDER BY nome, user_id, criado_em ASC, id ASC
)
UPDATE transacao t
SET categoria_id = k.keep_id
FROM keepers k
JOIN categoria c ON c.nome = k.nome AND c.user_id = k.user_id
WHERE t.categoria_id = c.id
  AND c.id <> k.keep_id;

-- Passo 1b: reatribuir orcamentos que referenciam duplicatas (usuario)
WITH keepers AS (
    SELECT DISTINCT ON (nome, user_id)
           id AS keep_id,
           nome,
           user_id
    FROM categoria
    WHERE user_id IS NOT NULL
    ORDER BY nome, user_id, criado_em ASC, id ASC
)
UPDATE orcamento o
SET categoria_id = k.keep_id
FROM keepers k
JOIN categoria c ON c.nome = k.nome AND c.user_id = k.user_id
WHERE o.categoria_id = c.id
  AND c.id <> k.keep_id;

-- Passo 1c: reatribuir lancamentos recorrentes que referenciam duplicatas (usuario)
WITH keepers AS (
    SELECT DISTINCT ON (nome, user_id)
           id AS keep_id,
           nome,
           user_id
    FROM categoria
    WHERE user_id IS NOT NULL
    ORDER BY nome, user_id, criado_em ASC, id ASC
)
UPDATE lancamento_recorrente lr
SET categoria_id = k.keep_id
FROM keepers k
JOIN categoria c ON c.nome = k.nome AND c.user_id = k.user_id
WHERE lr.categoria_id = c.id
  AND c.id <> k.keep_id;

-- Passo 1d: deletar duplicatas de usuario (FKs ja reatribuidas acima)
DELETE FROM categoria
WHERE id NOT IN (
    SELECT DISTINCT ON (nome, user_id) id
    FROM categoria
    WHERE user_id IS NOT NULL
    ORDER BY nome, user_id, criado_em ASC, id ASC
)
AND user_id IS NOT NULL;

-- ===========================================================
-- CATEGORIAS DE SISTEMA: reatribuir FKs antes de deletar
-- ===========================================================

-- Passo 2a: reatribuir transacoes que referenciam duplicatas (sistema)
WITH keepers AS (
    SELECT DISTINCT ON (nome)
           id AS keep_id,
           nome
    FROM categoria
    WHERE system = true
    ORDER BY nome, criado_em ASC, id ASC
)
UPDATE transacao t
SET categoria_id = k.keep_id
FROM keepers k
JOIN categoria c ON c.nome = k.nome AND c.system = true
WHERE t.categoria_id = c.id
  AND c.id <> k.keep_id;

-- Passo 2b: reatribuir orcamentos (sistema)
WITH keepers AS (
    SELECT DISTINCT ON (nome)
           id AS keep_id,
           nome
    FROM categoria
    WHERE system = true
    ORDER BY nome, criado_em ASC, id ASC
)
UPDATE orcamento o
SET categoria_id = k.keep_id
FROM keepers k
JOIN categoria c ON c.nome = k.nome AND c.system = true
WHERE o.categoria_id = c.id
  AND c.id <> k.keep_id;

-- Passo 2c: reatribuir lancamentos recorrentes (sistema)
WITH keepers AS (
    SELECT DISTINCT ON (nome)
           id AS keep_id,
           nome
    FROM categoria
    WHERE system = true
    ORDER BY nome, criado_em ASC, id ASC
)
UPDATE lancamento_recorrente lr
SET categoria_id = k.keep_id
FROM keepers k
JOIN categoria c ON c.nome = k.nome AND c.system = true
WHERE lr.categoria_id = c.id
  AND c.id <> k.keep_id;

-- Passo 2d: deletar duplicatas de sistema (FKs ja reatribuidas acima)
DELETE FROM categoria
WHERE id NOT IN (
    SELECT DISTINCT ON (nome) id
    FROM categoria
    WHERE system = true
    ORDER BY nome, criado_em ASC, id ASC
)
AND system = true;

-- ===========================================================
-- INDEXES DE UNICIDADE
-- ===========================================================

CREATE UNIQUE INDEX IF NOT EXISTS uidx_categoria_nome_user
    ON categoria (nome, user_id)
    WHERE user_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uidx_categoria_nome_system
    ON categoria (nome)
    WHERE system = true;
