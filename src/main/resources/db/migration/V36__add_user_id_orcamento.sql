ALTER TABLE orcamento ADD COLUMN user_id UUID;

UPDATE orcamento SET user_id = (SELECT id FROM usuario ORDER BY criado_em ASC LIMIT 1)
WHERE user_id IS NULL;

ALTER TABLE orcamento ALTER COLUMN user_id SET NOT NULL;

CREATE INDEX idx_orcamento_user_id ON orcamento (user_id);
