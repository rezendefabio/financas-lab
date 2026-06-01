ALTER TABLE anotacao RENAME COLUMN usuario_id TO user_id;
ALTER INDEX idx_anotacao_usuario_id RENAME TO idx_anotacao_user_id;
