-- V17: refactor transacao para Fase 1
-- Adiciona status, soft delete, transfer pair e tags
-- Bounded context: transacao

-- Adicionar novos campos
ALTER TABLE transacao ADD COLUMN user_id UUID;
ALTER TABLE transacao ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'CLEARED';
ALTER TABLE transacao ADD COLUMN deleted_at TIMESTAMPTZ;
ALTER TABLE transacao ADD COLUMN payee_id UUID;
ALTER TABLE transacao ADD COLUMN transfer_group_id UUID;
ALTER TABLE transacao ADD COLUMN transfer_pair_id UUID;

-- Remover coluna do modelo antigo de transferencia
ALTER TABLE transacao DROP COLUMN IF EXISTS conta_destino_id;

-- Remover constraint que dependia de conta_destino_id (se existir)
ALTER TABLE transacao DROP CONSTRAINT IF EXISTS fk_transacao_conta_destino;
ALTER TABLE transacao DROP CONSTRAINT IF EXISTS chk_transacao_transferencia;

-- Tabela M:N transacao_tag (sem FK constraints por ora: tag ainda nao existe)
CREATE TABLE IF NOT EXISTS transacao_tag (
    transacao_id UUID NOT NULL,
    tag_id       UUID NOT NULL,
    PRIMARY KEY (transacao_id, tag_id)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_transacao_user_id ON transacao (user_id);
CREATE INDEX IF NOT EXISTS idx_transacao_deleted_at ON transacao (deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_transacao_transfer_group ON transacao (transfer_group_id) WHERE transfer_group_id IS NOT NULL;
