ALTER TABLE lancamento_recorrente ADD COLUMN user_id UUID;

UPDATE lancamento_recorrente SET user_id = (SELECT id FROM usuario ORDER BY criado_em ASC LIMIT 1)
WHERE user_id IS NULL;

ALTER TABLE lancamento_recorrente ALTER COLUMN user_id SET NOT NULL;

CREATE INDEX idx_lancamento_recorrente_user_id ON lancamento_recorrente (user_id);
