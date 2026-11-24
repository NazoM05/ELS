-- LIMPEZA EXPLICITA DE CPFs EM PRODUCAO (PostgreSQL)
-- Execute somente manualmente, com backup recente e em janela de manutencao.
-- Preserva usuarios e demais dados; deixa o campo CPF nulo.
BEGIN;
SELECT COUNT(*) AS usuarios_com_cpf FROM usuario WHERE cpf IS NOT NULL;
UPDATE usuario SET cpf = NULL WHERE cpf IS NOT NULL;
SELECT COUNT(*) AS cpfs_restantes FROM usuario WHERE cpf IS NOT NULL;
-- Revise a previa, faca backup e execute este script somente se concordar com a limpeza.
COMMIT;
