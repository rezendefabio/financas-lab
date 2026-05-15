-- V18: FK consolidation -- adiciona FK constraints faltantes e indexes
-- FK de user_id para usuario omitidas nesta migracao: testes de integracao
-- criam registros com user_id arbitrario (nao presente em usuario). Sera
-- enderecado quando os testes de integracao forem adaptados para usar
-- fixtures de usuario reais.

-- transacao.payee_id -> payee.id (nullable)
ALTER TABLE transacao ADD CONSTRAINT fk_transacao_payee
    FOREIGN KEY (payee_id) REFERENCES payee(id);

-- transacao_tag.transacao_id -> transacao.id
ALTER TABLE transacao_tag ADD CONSTRAINT fk_transacao_tag_transacao
    FOREIGN KEY (transacao_id) REFERENCES transacao(id);

-- transacao_tag.tag_id -> tag.id
ALTER TABLE transacao_tag ADD CONSTRAINT fk_transacao_tag_tag
    FOREIGN KEY (tag_id) REFERENCES tag(id);

-- Indexes para colunas user_id sem index (melhoram queries de listagem por usuario)
CREATE INDEX IF NOT EXISTS idx_conta_user_id ON conta(user_id);
CREATE INDEX IF NOT EXISTS idx_categoria_user_id ON categoria(user_id);
CREATE INDEX IF NOT EXISTS idx_transacao_payee_id ON transacao(payee_id);
CREATE INDEX IF NOT EXISTS idx_tag_user_id ON tag(user_id);
CREATE INDEX IF NOT EXISTS idx_payee_user_id ON payee(user_id);
