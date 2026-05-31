ALTER TABLE meta ADD COLUMN user_id UUID;

UPDATE meta SET user_id = (SELECT id FROM usuario ORDER BY criado_em ASC LIMIT 1)
WHERE user_id IS NULL;

ALTER TABLE meta ALTER COLUMN user_id SET NOT NULL;

CREATE INDEX idx_meta_user_id ON meta (user_id);
