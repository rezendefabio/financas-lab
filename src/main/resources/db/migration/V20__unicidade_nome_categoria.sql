-- V20: unicidade de nome por usuario e por categoria de sistema
-- Elimina duplicatas existentes e cria unique indexes parciais

-- Passo 1: remover duplicatas de categorias de usuario mantendo a mais antiga
DELETE FROM categoria
WHERE id NOT IN (
    SELECT DISTINCT ON (nome, user_id) id
    FROM categoria
    WHERE user_id IS NOT NULL
    ORDER BY nome, user_id, criado_em ASC, id ASC
)
AND user_id IS NOT NULL;

-- Passo 2: remover duplicatas de categorias de sistema mantendo a mais antiga
DELETE FROM categoria
WHERE id NOT IN (
    SELECT DISTINCT ON (nome) id
    FROM categoria
    WHERE system = true
    ORDER BY nome, criado_em ASC, id ASC
)
AND system = true;

-- Passo 3: unique index para categorias de usuario (nome unico por usuario)
CREATE UNIQUE INDEX IF NOT EXISTS uidx_categoria_nome_user
    ON categoria (nome, user_id)
    WHERE user_id IS NOT NULL;

-- Passo 4: unique index para categorias de sistema (nome unico entre sistema)
CREATE UNIQUE INDEX IF NOT EXISTS uidx_categoria_nome_system
    ON categoria (nome)
    WHERE system = true;
