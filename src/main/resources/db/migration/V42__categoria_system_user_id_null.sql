-- V42: enforce do invariante "categoria de sistema e global" -- system=true implica user_id NULL.
-- Defensivo para a base real caso algum registro system=true tenha ganho user_id
-- durante a fase em que a API repassava request.system() junto com o userId do token.

UPDATE categoria SET user_id = NULL WHERE system = true;
