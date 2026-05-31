UPDATE categoria SET user_id = (SELECT id FROM usuario ORDER BY criado_em ASC LIMIT 1)
WHERE user_id IS NULL AND system = false;
