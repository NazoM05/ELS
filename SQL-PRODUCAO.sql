-- Perfil prod usa ddl-auto=validate: rode antes de subir esta versão.
ALTER TABLE pedido ADD COLUMN servico_frete_codigo VARCHAR(10);
ALTER TABLE pedido ADD COLUMN correios_id_pre_postagem VARCHAR(40);
ALTER TABLE pedido ADD COLUMN correios_codigo_objeto VARCHAR(13);
ALTER TABLE pedido ADD COLUMN etiqueta_gerada_em TIMESTAMP;
