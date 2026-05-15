-- V12: refactor conta -- adiciona user_id, saldo_atual e campos de cartao de credito
-- Bounded context: conta
-- Fase 1: Task 002

ALTER TABLE conta ADD COLUMN user_id UUID;
ALTER TABLE conta ADD COLUMN saldo_atual_valor NUMERIC(15,2);
ALTER TABLE conta ADD COLUMN saldo_atual_moeda VARCHAR(3);
ALTER TABLE conta ADD COLUMN limite_credito_valor NUMERIC(15,2);
ALTER TABLE conta ADD COLUMN limite_credito_moeda VARCHAR(3);
ALTER TABLE conta ADD COLUMN dia_fechamento INTEGER;
ALTER TABLE conta ADD COLUMN dia_vencimento INTEGER;
