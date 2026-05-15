-- V13: adiciona user_id, flag system e seed de categorias de sistema ao bounded context categoria

ALTER TABLE categoria ADD COLUMN IF NOT EXISTS user_id UUID;
ALTER TABLE categoria ADD COLUMN IF NOT EXISTS system BOOLEAN NOT NULL DEFAULT false;

-- Marcar categorias existentes do seed V10 como sistema
UPDATE categoria SET system = true WHERE user_id IS NULL;

-- Seed de categorias de sistema com tipo NEUTRAL (transferencias/ajustes)
-- UUIDs fixos garantem idempotencia via ON CONFLICT
INSERT INTO categoria (id, nome, tipo, system, criado_em, atualizado_em)
VALUES
    ('00000000-0000-0000-0000-000000000001', 'Transferencia entre contas', 'NEUTRAL', true, now(), now()),
    ('00000000-0000-0000-0000-000000000002', 'Pagamento de fatura', 'NEUTRAL', true, now(), now()),
    ('00000000-0000-0000-0000-000000000003', 'Ajuste de saldo', 'NEUTRAL', true, now(), now())
ON CONFLICT (id) DO NOTHING;
