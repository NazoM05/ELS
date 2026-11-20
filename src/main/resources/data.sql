-- =====================================================================
-- Novas colunas da tabela PRODUTO (sintaxe H2).
-- O Hibernate (ddl-auto=update) já cria as colunas a partir da entidade;
-- os ALTER abaixo garantem o mesmo esquema em bancos que já existiam e
-- não fazem nada quando a coluna já está lá (IF NOT EXISTS).
-- =====================================================================
ALTER TABLE produto ADD COLUMN IF NOT EXISTS status BOOLEAN DEFAULT TRUE NOT NULL;
ALTER TABLE produto ADD COLUMN IF NOT EXISTS cor VARCHAR(50);
ALTER TABLE produto ADD COLUMN IF NOT EXISTS imagem_frente VARCHAR(255);
ALTER TABLE produto ADD COLUMN IF NOT EXISTS imagem_costas VARCHAR(255);
ALTER TABLE produto ADD COLUMN IF NOT EXISTS imagem_modelo VARCHAR(255);
ALTER TABLE produto ADD COLUMN IF NOT EXISTS imagem_guia_tamanhos VARCHAR(255);
ALTER TABLE produto ADD COLUMN IF NOT EXISTS categoria VARCHAR(20);
ALTER TABLE produto ADD COLUMN IF NOT EXISTS genero VARCHAR(20);

-- =====================================================================
-- Carga inicial. MERGE ... KEY (nome): insere o produto novo ou atualiza
-- status, cor, categoria e gênero se o nome já existir (o nome é único), então pode rodar
-- mais de uma vez sem erro. status alterna TRUE/FALSE na ordem da lista.
-- =====================================================================
MERGE INTO produto (nome, tamanho, preco, tipo, imagem_frente, imagem_costas, status, cor, categoria, genero) KEY (nome)
VALUES
    ('Manto Imortal Tricolor', 'P,M,G,GG', 79.90, 'Poliéster', 'manto-imortal-tricolor.svg', 'manto-imortal-tricolor-costas.svg', TRUE, '#0d80bf', 'CAMISETA', 'UNISSEX'),
    ('Camisa Militar', 'PP,P,M,G,GG,XG', 24.90, 'Algodão', 'camisa-militar.svg', 'camisa-militar-costas.svg', TRUE, 'Verde Militar', 'CAMISA', 'MASCULINO'),
    ('Camisa Polo Listrada', 'PP,P,M,G,GG,XG', 39.90, 'Algodão', 'camisa-polo-listrada.svg', 'camisa-polo-listrada-costas.svg', FALSE, 'Azul Marinho', 'CAMISA', 'MASCULINO'),
    ('Camiseta Básica Branca', 'PP,P,M,G,GG,XG', 19.90, 'Algodão', 'camiseta-basica-branca.svg', 'camiseta-basica-branca-costas.svg', TRUE, 'Branco', 'CAMISETA', 'UNISSEX'),
    ('Camiseta Estampa Floral', 'PP,P,M,G,GG,XG', 29.90, 'Malha', 'camiseta-estampa-floral.svg', 'camiseta-estampa-floral-costas.svg', FALSE, 'Rosa Claro', 'CAMISETA', 'FEMININO'),
    ('Camiseta Oversized Preta', 'PP,P,M,G,GG,XG', 44.90, 'Algodão', 'camiseta-oversized-preta.svg', 'camiseta-oversized-preta-costas.svg', TRUE, 'Preto', 'CAMISETA', 'UNISSEX'),
    ('Regata Fitness', 'PP,P,M,G,GG,XG', 22.90, 'Poliéster', 'regata-fitness.svg', 'regata-fitness-costas.svg', FALSE, 'Grafite', 'CAMISETA', 'UNISSEX'),
    ('Blusa de Moletom Cinza', 'PP,P,M,G,GG,XG', 69.90, 'Moletom', 'blusa-de-moletom-cinza.svg', 'blusa-de-moletom-cinza-costas.svg', TRUE, 'Cinza', 'MOLETOM', 'UNISSEX'),
    ('Moletom com Capuz Rosa', 'PP,P,M,G,GG,XG', 84.90, 'Moletom', 'moletom-com-capuz-rosa.svg', 'moletom-com-capuz-rosa-costas.svg', FALSE, 'Rosa', 'MOLETOM', 'FEMININO'),
    ('Jaqueta Jeans', 'PP,P,M,G,GG,XG', 119.90, 'Jeans', 'jaqueta-jeans.svg', 'jaqueta-jeans-costas.svg', TRUE, 'Azul Jeans', 'JAQUETA', 'UNISSEX'),
    ('Jaqueta Corta-Vento', 'PP,P,M,G,GG,XG', 89.90, 'Poliéster', 'jaqueta-corta-vento.svg', 'jaqueta-corta-vento-costas.svg', FALSE, 'Verde Petróleo', 'JAQUETA', 'MASCULINO'),
    ('Calça Jeans Skinny', '36,38,40,42,44,46', 99.90, 'Jeans', 'calca-jeans-skinny.svg', 'calca-jeans-skinny-costas.svg', TRUE, 'Azul Escuro', 'CALCA', 'FEMININO'),
    ('Calça Jeans Reta', '36,38,40,42,44,46', 109.90, 'Jeans', 'calca-jeans-reta.svg', 'calca-jeans-reta-costas.svg', FALSE, 'Azul Jeans Escuro', 'CALCA', 'MASCULINO'),
    ('Calça Jogger', 'PP,P,M,G,GG,XG', 59.90, 'Moletom', 'calca-jogger.svg', 'calca-jogger-costas.svg', TRUE, 'Cinza Chumbo', 'CALCA', 'UNISSEX'),
    ('Calça Alfaiataria Preta', '36,38,40,42,44,46', 94.90, 'Viscose', 'calca-alfaiataria-preta.svg', 'calca-alfaiataria-preta-costas.svg', FALSE, 'Preto', 'CALCA', 'FEMININO'),
    ('Bermuda Cargo', '36,38,40,42,44,46', 54.90, 'Sarja', 'bermuda-cargo.svg', 'bermuda-cargo-costas.svg', TRUE, 'Caqui', 'BERMUDA', 'MASCULINO'),
    ('Bermuda Jeans', '36,38,40,42,44,46', 49.90, 'Jeans', 'bermuda-jeans.svg', 'bermuda-jeans-costas.svg', FALSE, 'Azul Jeans', 'BERMUDA', 'MASCULINO'),
    ('Short Saia Plissado', 'PP,P,M,G,GG,XG', 34.90, 'Poliéster', 'short-saia-plissado.svg', 'short-saia-plissado-costas.svg', TRUE, 'Lilás', 'SAIA', 'FEMININO'),
    ('Saia Midi Floral', 'PP,P,M,G,GG,XG', 57.90, 'Viscose', 'saia-midi-floral.svg', 'saia-midi-floral-costas.svg', FALSE, 'Pêssego', 'SAIA', 'FEMININO'),
    ('Vestido Midi Rosa', 'PP,P,M,G,GG,XG', 79.90, 'Viscose', 'vestido-midi-rosa.svg', 'vestido-midi-rosa-costas.svg', TRUE, 'Rosa', 'VESTIDO', 'FEMININO'),
    ('Vestido Curto Listrado', 'PP,P,M,G,GG,XG', 64.90, 'Malha', 'vestido-curto-listrado.svg', 'vestido-curto-listrado-costas.svg', FALSE, 'Rosa e Branco', 'VESTIDO', 'FEMININO'),
    ('Cropped Canelado', 'PP,P,M,G,GG,XG', 27.90, 'Canelado', 'cropped-canelado.svg', 'cropped-canelado-costas.svg', TRUE, 'Bege', 'BLUSA', 'FEMININO'),
    ('Cardigan de Tricô', 'PP,P,M,G,GG,XG', 74.90, 'Tricô', 'cardigan-de-trico.svg', 'cardigan-de-trico-costas.svg', FALSE, 'Caramelo', 'BLUSA', 'FEMININO'),
    ('Camisa Social Branca', 'PP,P,M,G,GG,XG', 64.90, 'Algodão', 'camisa-social-branca.svg', 'camisa-social-branca-costas.svg', TRUE, 'Branco', 'CAMISA', 'MASCULINO'),
    ('Camisa Xadrez Flanela', 'PP,P,M,G,GG,XG', 52.90, 'Flanela', 'camisa-xadrez-flanela.svg', 'camisa-xadrez-flanela-costas.svg', FALSE, 'Vermelho', 'CAMISA', 'UNISSEX'),
    ('Camiseta Oversized Azul Claro', 'PP,P,M,G,GG,XG', 44.90, 'Algodão', 'camiseta-oversized-azul-claro.svg', 'camiseta-oversized-azul-claro-costas.svg', TRUE, 'Azul Claro', 'CAMISETA', 'UNISSEX'),
    ('Camiseta Oversized Azul Marinho', 'PP,P,M,G,GG,XG', 44.90, 'Algodão', 'camiseta-oversized-azul-marinho.svg', 'camiseta-oversized-azul-marinho-costas.svg', TRUE, 'Azul Marinho', 'CAMISETA', 'UNISSEX'),
    ('Camiseta Oversized Bege com Bolso', 'PP,P,M,G,GG,XG', 46.90, 'Algodão', 'camiseta-oversized-bege-bolso.svg', 'camiseta-oversized-bege-bolso-costas.svg', TRUE, 'Bege', 'CAMISETA', 'UNISSEX'),
    ('Camiseta Oversized Branca', 'PP,P,M,G,GG,XG', 42.90, 'Algodão', 'camiseta-oversized-branca.svg', 'camiseta-oversized-branca-costas.svg', TRUE, 'Branco', 'CAMISETA', 'UNISSEX'),
    ('Camiseta Oversized Cinza Mescla', 'PP,P,M,G,GG,XG', 44.90, 'Algodão', 'camiseta-oversized-cinza-mescla.svg', 'camiseta-oversized-cinza-mescla-costas.svg', TRUE, 'Cinza Mescla', 'CAMISETA', 'UNISSEX'),
    ('Camiseta Oversized Listrada', 'PP,P,M,G,GG,XG', 47.90, 'Algodão', 'camiseta-oversized-listrada.svg', 'camiseta-oversized-listrada-costas.svg', TRUE, 'Listrado', 'CAMISETA', 'UNISSEX'),
    ('Camiseta Oversized Off-White', 'PP,P,M,G,GG,XG', 44.90, 'Algodão', 'camiseta-oversized-off-white.svg', 'camiseta-oversized-off-white-costas.svg', TRUE, 'Off-white', 'CAMISETA', 'UNISSEX'),
    ('Camiseta Oversized Rosa Bebê', 'PP,P,M,G,GG,XG', 44.90, 'Algodão', 'camiseta-oversized-rosa-bebe.svg', 'camiseta-oversized-rosa-bebe-costas.svg', TRUE, 'Rosa Bebê', 'CAMISETA', 'UNISSEX'),
    ('Camiseta Oversized Street Preta', 'PP,P,M,G,GG,XG', 49.90, 'Algodão', 'camiseta-oversized-street-preta.svg', 'camiseta-oversized-street-preta-costas.svg', TRUE, 'Preto', 'CAMISETA', 'UNISSEX'),
    ('Camiseta Oversized Verde Militar', 'PP,P,M,G,GG,XG', 44.90, 'Algodão', 'camiseta-oversized-verde-militar.svg', 'camiseta-oversized-verde-militar-costas.svg', TRUE, 'Verde Militar', 'CAMISETA', 'UNISSEX'),
    ('Camiseta Oversized Vinho', 'PP,P,M,G,GG,XG', 44.90, 'Algodão', 'camiseta-oversized-vinho.svg', 'camiseta-oversized-vinho-costas.svg', TRUE, 'Vinho', 'CAMISETA', 'UNISSEX'),
    ('Moletom Oversized com Capuz Cinza', 'PP,P,M,G,GG,XG', 89.90, 'Moletom', 'moletom-oversized-capuz-cinza.svg', 'moletom-oversized-capuz-cinza-costas.svg', TRUE, 'Cinza', 'MOLETOM', 'UNISSEX'),
    ('Moletom Oversized com Capuz Off-White', 'PP,P,M,G,GG,XG', 89.90, 'Moletom', 'moletom-oversized-capuz-off-white.svg', 'moletom-oversized-capuz-off-white-costas.svg', TRUE, 'Off-white', 'MOLETOM', 'UNISSEX'),
    ('Moletom Oversized Preto', 'PP,P,M,G,GG,XG', 84.90, 'Moletom', 'moletom-oversized-preto.svg', 'moletom-oversized-preto-costas.svg', TRUE, 'Preto', 'MOLETOM', 'UNISSEX'),
    ('Moletom Oversized Verde Oliva', 'PP,P,M,G,GG,XG', 89.90, 'Moletom', 'moletom-oversized-verde-oliva.svg', 'moletom-oversized-verde-oliva-costas.svg', TRUE, 'Verde Oliva', 'MOLETOM', 'UNISSEX');

-- =====================================================================
-- ESTOQUE POR TAMANHO. Uma linha em PRODUTO_ESTOQUE para cada tamanho que o produto
-- oferece (coluna tamanho, separada por vírgula). Sem quantidade fixa: o valor é
-- calculado a partir do id e da posição do tamanho, então cada peça tem estoques
-- diferentes (alguns baixos, para a tela de visualização mostrar o alerta).
-- MERGE ... KEY (produto_id, tamanho): pode rodar mais de uma vez sem duplicar.
-- Rodar depois do MERGE dos produtos.
-- =====================================================================
MERGE INTO produto_estoque (produto_id, tamanho, quantidade) KEY (produto_id, tamanho)
SELECT p.id, t.tamanho, MOD(p.id * 7 + t.ordem * 5, 24) + 2
  FROM produto p
 CROSS JOIN (SELECT 'PP' AS tamanho, 1 AS ordem
             UNION ALL SELECT 'P', 2
             UNION ALL SELECT 'M', 3
             UNION ALL SELECT 'G', 4
             UNION ALL SELECT 'GG', 5
             UNION ALL SELECT 'XG', 6
             UNION ALL SELECT '36', 7
             UNION ALL SELECT '38', 8
             UNION ALL SELECT '40', 9
             UNION ALL SELECT '42', 10
             UNION ALL SELECT '44', 11
             UNION ALL SELECT '46', 12) t
 WHERE ',' || REPLACE(p.tamanho, ' ', '') || ',' LIKE '%,' || t.tamanho || ',%';

-- =====================================================================
-- GUIA DE TAMANHOS = 4ª posição da galeria (Frente, Verso, Modelo, Guia).
-- Neste projeto a galeria NÃO tem tabela de mídias: são 4 colunas da tabela
-- PRODUTO e a ordem vem de Produto.getGaleria(). Logo, "ordem = 4" é a coluna
-- imagem_guia_tamanhos. Arquivos em static/img (ou uploads/img, que tem prioridade).
-- Escolha por CATEGORIA (enum Categoria):
--   CAMISETA, CAMISA, BLUSA, MOLETOM, JAQUETA -> guia-tamanhos-superior.svg    (tórax / cintura / comprimento / manga)
--   CALCA, BERMUDA                            -> guia-tamanhos-inferior.svg    (cintura / quadril / comprimento / gancho)
--   VESTIDO, SAIA                             -> guia-tamanhos-vestido-saia.svg (busto / cintura / quadril)
-- Só preenche quando o produto ainda não tem guia (o que o admin enviou pelo formulário é mantido).
-- Rodar depois do MERGE (os produtos precisam existir). Pode rodar várias vezes.
-- =====================================================================
UPDATE produto SET imagem_guia_tamanhos = 'guia-tamanhos-superior.svg'
 WHERE categoria IN ('CAMISETA', 'CAMISA', 'BLUSA', 'MOLETOM', 'JAQUETA')
   AND (imagem_guia_tamanhos IS NULL OR TRIM(imagem_guia_tamanhos) = '');

UPDATE produto SET imagem_guia_tamanhos = 'guia-tamanhos-inferior.svg'
 WHERE categoria IN ('CALCA', 'BERMUDA')
   AND (imagem_guia_tamanhos IS NULL OR TRIM(imagem_guia_tamanhos) = '');

UPDATE produto SET imagem_guia_tamanhos = 'guia-tamanhos-vestido-saia.svg'
 WHERE categoria IN ('VESTIDO', 'SAIA')
   AND (imagem_guia_tamanhos IS NULL OR TRIM(imagem_guia_tamanhos) = '');
-- ACESSORIOS fica sem guia (a galeria simplesmente não mostra a 4ª miniatura).
    
    insert into USUARIO (username, senha, role, ativo) values ('admin', '$2a$10$oz8mJWjsca2yd5hdU5pvzO123repm3JNyZ9fxbFzMs5wVaaccsiZG', 'ROLE_ADMIN', TRUE);

    -- Cliente de teste (login: user). Mesma senha do admin.
    insert into USUARIO (nome, username, senha, role, ativo) values ('Usuário', 'user', '$2a$10$oz8mJWjsca2yd5hdU5pvzO123repm3JNyZ9fxbFzMs5wVaaccsiZG', 'ROLE_USUARIO', TRUE);

    -- Gestor (login: gestor, senha de teste 1234). Perfil ROLE_GESTOR: acessa SOMENTE a separação de pedidos (/admin/separacao).
    -- Mesma senha de teste do admin: troque antes de colocar a loja no ar.
    insert into USUARIO (nome, username, senha, role, ativo) values ('Gestor', 'gestor', '$2a$10$oz8mJWjsca2yd5hdU5pvzO123repm3JNyZ9fxbFzMs5wVaaccsiZG', 'ROLE_GESTOR', TRUE);
