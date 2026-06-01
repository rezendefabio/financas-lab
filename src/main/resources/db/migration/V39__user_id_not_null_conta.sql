UPDATE conta SET user_id = (SELECT id FROM usuario ORDER BY criado_em ASC LIMIT 1)
WHERE user_id IS NULL;
ALTER TABLE conta ALTER COLUMN user_id SET NOT NULL;
CREATE INDEX IF NOT EXISTS idx_conta_user_id ON conta (user_id);
