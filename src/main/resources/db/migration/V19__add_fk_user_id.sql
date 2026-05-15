-- V19: adiciona FK constraints de user_id para a tabela usuario
-- Pre-requisito: testes de integracao agora criam usuario real antes dos registros filhos
-- Contexto: V18 omitiu intencionalmente estas FKs por dependencia dos testes de integracao

-- conta.user_id -> usuario.id (nullable: conta pode existir sem usuario)
ALTER TABLE conta ADD CONSTRAINT fk_conta_user_id
    FOREIGN KEY (user_id) REFERENCES usuario(id);

-- categoria.user_id -> usuario.id (nullable: categorias system tem user_id nulo)
ALTER TABLE categoria ADD CONSTRAINT fk_categoria_user_id
    FOREIGN KEY (user_id) REFERENCES usuario(id);

-- tag.user_id -> usuario.id (NOT NULL: toda tag pertence a um usuario)
ALTER TABLE tag ADD CONSTRAINT fk_tag_user_id
    FOREIGN KEY (user_id) REFERENCES usuario(id);

-- payee.user_id -> usuario.id (NOT NULL: todo payee pertence a um usuario)
ALTER TABLE payee ADD CONSTRAINT fk_payee_user_id
    FOREIGN KEY (user_id) REFERENCES usuario(id);

-- transacao.user_id -> usuario.id (nullable: transacao pode existir sem user_id por ora)
ALTER TABLE transacao ADD CONSTRAINT fk_transacao_user_id
    FOREIGN KEY (user_id) REFERENCES usuario(id);
