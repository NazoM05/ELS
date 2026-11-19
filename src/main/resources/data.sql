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
    ('Camisa Militar', 'GG', 24.90, 'Algodão', 'camisa-militar.svg', 'camisa-militar-costas.svg', TRUE, 'Verde Militar', 'CAMISA', 'MASCULINO'),
    ('Camisa Polo Listrada', 'M', 39.90, 'Algodão', 'camisa-polo-listrada.svg', 'camisa-polo-listrada-costas.svg', FALSE, 'Azul Marinho', 'CAMISA', 'MASCULINO'),
    ('Camiseta Básica Branca', 'G', 19.90, 'Algodão', 'camiseta-basica-branca.svg', 'camiseta-basica-branca-costas.svg', TRUE, 'Branco', 'CAMISETA', 'UNISSEX'),
    ('Camiseta Estampa Floral', 'P', 29.90, 'Malha', 'camiseta-estampa-floral.svg', 'camiseta-estampa-floral-costas.svg', FALSE, 'Rosa Claro', 'CAMISETA', 'FEMININO'),
    ('Camiseta Oversized Preta', 'GG', 44.90, 'Algodão', 'camiseta-oversized-preta.svg', 'camiseta-oversized-preta-costas.svg', TRUE, 'Preto', 'CAMISETA', 'UNISSEX'),
    ('Regata Fitness', 'M', 22.90, 'Poliéster', 'regata-fitness.svg', 'regata-fitness-costas.svg', FALSE, 'Grafite', 'CAMISETA', 'UNISSEX'),
    ('Blusa de Moletom Cinza', 'G', 69.90, 'Moletom', 'blusa-de-moletom-cinza.svg', 'blusa-de-moletom-cinza-costas.svg', TRUE, 'Cinza', 'MOLETOM', 'UNISSEX'),
    ('Moletom com Capuz Rosa', 'M', 84.90, 'Moletom', 'moletom-com-capuz-rosa.svg', 'moletom-com-capuz-rosa-costas.svg', FALSE, 'Rosa', 'MOLETOM', 'FEMININO'),
    ('Jaqueta Jeans', 'G', 119.90, 'Jeans', 'jaqueta-jeans.svg', 'jaqueta-jeans-costas.svg', TRUE, 'Azul Jeans', 'JAQUETA', 'UNISSEX'),
    ('Jaqueta Corta-Vento', 'M', 89.90, 'Poliéster', 'jaqueta-corta-vento.svg', 'jaqueta-corta-vento-costas.svg', FALSE, 'Verde Petróleo', 'JAQUETA', 'MASCULINO'),
    ('Calça Jeans Skinny', '38', 99.90, 'Jeans', 'calca-jeans-skinny.svg', 'calca-jeans-skinny-costas.svg', TRUE, 'Azul Escuro', 'CALCA', 'FEMININO'),
    ('Calça Jeans Reta', '42', 109.90, 'Jeans', 'calca-jeans-reta.svg', 'calca-jeans-reta-costas.svg', FALSE, 'Azul Jeans Escuro', 'CALCA', 'MASCULINO'),
    ('Calça Jogger', 'M', 59.90, 'Moletom', 'calca-jogger.svg', 'calca-jogger-costas.svg', TRUE, 'Cinza Chumbo', 'CALCA', 'UNISSEX'),
    ('Calça Alfaiataria Preta', '40', 94.90, 'Viscose', 'calca-alfaiataria-preta.svg', 'calca-alfaiataria-preta-costas.svg', FALSE, 'Preto', 'CALCA', 'FEMININO'),
    ('Bermuda Cargo', '42', 54.90, 'Sarja', 'bermuda-cargo.svg', 'bermuda-cargo-costas.svg', TRUE, 'Caqui', 'BERMUDA', 'MASCULINO'),
    ('Bermuda Jeans', '40', 49.90, 'Jeans', 'bermuda-jeans.svg', 'bermuda-jeans-costas.svg', FALSE, 'Azul Jeans', 'BERMUDA', 'MASCULINO'),
    ('Short Saia Plissado', 'P', 34.90, 'Poliéster', 'short-saia-plissado.svg', 'short-saia-plissado-costas.svg', TRUE, 'Lilás', 'SAIA', 'FEMININO'),
    ('Saia Midi Floral', 'M', 57.90, 'Viscose', 'saia-midi-floral.svg', 'saia-midi-floral-costas.svg', FALSE, 'Pêssego', 'SAIA', 'FEMININO'),
    ('Vestido Midi Rosa', 'M', 79.90, 'Viscose', 'vestido-midi-rosa.svg', 'vestido-midi-rosa-costas.svg', TRUE, 'Rosa', 'VESTIDO', 'FEMININO'),
    ('Vestido Curto Listrado', 'P', 64.90, 'Malha', 'vestido-curto-listrado.svg', 'vestido-curto-listrado-costas.svg', FALSE, 'Rosa e Branco', 'VESTIDO', 'FEMININO'),
    ('Cropped Canelado', 'P', 27.90, 'Canelado', 'cropped-canelado.svg', 'cropped-canelado-costas.svg', TRUE, 'Bege', 'BLUSA', 'FEMININO'),
    ('Cardigan de Tricô', 'G', 74.90, 'Tricô', 'cardigan-de-trico.svg', 'cardigan-de-trico-costas.svg', FALSE, 'Caramelo', 'BLUSA', 'FEMININO'),
    ('Camisa Social Branca', 'M', 64.90, 'Algodão', 'camisa-social-branca.svg', 'camisa-social-branca-costas.svg', TRUE, 'Branco', 'CAMISA', 'MASCULINO'),
    ('Camisa Xadrez Flanela', 'G', 52.90, 'Flanela', 'camisa-xadrez-flanela.svg', 'camisa-xadrez-flanela-costas.svg', FALSE, 'Vermelho', 'CAMISA', 'UNISSEX');

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
