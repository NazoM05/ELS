-- Migração do perfil GESTOR para SEPARADOR (rodar UMA vez em produção/PostgreSQL).
-- Em produção o data.sql não roda (spring.sql.init.mode=never), então a conversão é manual.
BEGIN;

UPDATE usuario SET role = 'ROLE_SEPARADOR' WHERE role = 'ROLE_GESTOR';

-- Opcional: renomear o login de teste, se existir e se "separador" ainda estiver livre.
UPDATE usuario SET username = 'separador', nome = 'Separador'
 WHERE username = 'gestor' AND role = 'ROLE_SEPARADOR'
   AND NOT EXISTS (SELECT 1 FROM usuario u2 WHERE u2.username = 'separador');

COMMIT;
