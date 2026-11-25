-- =====================================================================
-- Novas colunas da tabela PRODUTO (sintaxe H2).
-- O Hibernate (ddl-auto=update) já cria as colunas a partir da entidade;
-- os ALTER abaixo garantem o mesmo esquema em bancos que já existiam e
-- não fazem nada quando a coluna já está lá (IF NOT EXISTS).
-- =====================================================================
ALTER TABLE produto ADD COLUMN IF NOT EXISTS status BOOLEAN DEFAULT TRUE NOT NULL;
ALTER TABLE produto ADD COLUMN IF NOT EXISTS cor VARCHAR(50);
ALTER TABLE produto ADD COLUMN IF NOT EXISTS colecao VARCHAR(40);
ALTER TABLE produto ADD COLUMN IF NOT EXISTS imagem_frente VARCHAR(255);
ALTER TABLE produto ADD COLUMN IF NOT EXISTS imagem_costas VARCHAR(255);
ALTER TABLE produto ADD COLUMN IF NOT EXISTS imagem_modelo VARCHAR(255);
ALTER TABLE produto ADD COLUMN IF NOT EXISTS imagem_guia_tamanhos VARCHAR(255);
ALTER TABLE produto ADD COLUMN IF NOT EXISTS categoria VARCHAR(20);
ALTER TABLE produto ADD COLUMN IF NOT EXISTS genero VARCHAR(20);
ALTER TABLE produto ADD COLUMN IF NOT EXISTS outlet BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE produto ADD COLUMN IF NOT EXISTS desconto_outlet INTEGER;

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
-- Coleção Tricolor e coleção Urban Culture Oversized
-- Produtos de coleção só aparecem na página da coleção (menu lateral), nunca na Home/Outlet.
-- Ficam antes do estoque e do guia de tamanhos para receberem os dois já na primeira carga.
-- O MERGE permite executar este data.sql novamente sem duplicar os produtos.
-- =====================================================================
MERGE INTO produto (nome, tamanho, preco, tipo, imagem_frente, imagem_costas, status, cor, categoria, genero, colecao) KEY (nome)
VALUES
    ('Manto Imortal Tricolor II', 'P,M,G,GG', 79.90, 'Poliéster', 'manto-imortal-tricolor-ii.svg', 'manto-imortal-tricolor-ii-costas.svg', TRUE, 'Branco', 'CAMISETA', 'UNISSEX', 'TRICOLOR'),
    ('Manto Imortal Tricolor III', 'P,M,G,GG', 79.90, 'Poliéster', 'manto-imortal-tricolor-iii.svg', 'manto-imortal-tricolor-iii-costas.svg', TRUE, 'Preto', 'CAMISETA', 'UNISSEX', 'TRICOLOR'),
    -- Linha esportiva Tricolor (regata, calças, bermudas de jogador, bonés e corta-ventos)
    ('Regata de Treino Tricolor', 'PP,P,M,G,GG,XG', 49.90, 'Poliéster', 'regata-treino-tricolor.svg', 'regata-treino-tricolor-costas.svg', TRUE, 'Azul', 'CAMISETA', 'UNISSEX', 'TRICOLOR'),
    ('Calça Esportiva Tricolor Preta', '36,38,40,42,44,46', 89.90, 'Poliéster', 'calca-esportiva-tricolor-preta.svg', 'calca-esportiva-tricolor-preta-costas.svg', TRUE, 'Preto', 'CALCA', 'UNISSEX', 'TRICOLOR'),
    ('Calça Esportiva Tricolor Azul', '36,38,40,42,44,46', 94.90, 'Poliéster', 'calca-esportiva-tricolor-azul.svg', 'calca-esportiva-tricolor-azul-costas.svg', TRUE, 'Azul', 'CALCA', 'UNISSEX', 'TRICOLOR'),
    ('Calça Esportiva Tricolor Branca', '36,38,40,42,44,46', 89.90, 'Poliéster', 'calca-esportiva-tricolor-branca.svg', 'calca-esportiva-tricolor-branca-costas.svg', TRUE, 'Branco', 'CALCA', 'UNISSEX', 'TRICOLOR'),
    ('Bermuda Jogador Tricolor Branca', '36,38,40,42,44,46', 54.90, 'Poliéster', 'bermuda-jogador-tricolor-branca.svg', 'bermuda-jogador-tricolor-branca-costas.svg', TRUE, 'Branco', 'BERMUDA', 'UNISSEX', 'TRICOLOR'),
    ('Bermuda Jogador Tricolor Preta', '36,38,40,42,44,46', 54.90, 'Poliéster', 'bermuda-jogador-tricolor-preta.svg', 'bermuda-jogador-tricolor-preta-costas.svg', TRUE, 'Preto', 'BERMUDA', 'UNISSEX', 'TRICOLOR'),
    ('Bermuda Jogador Tricolor Azul', '36,38,40,42,44,46', 54.90, 'Poliéster', 'bermuda-jogador-tricolor-azul.svg', 'bermuda-jogador-tricolor-azul-costas.svg', TRUE, 'Azul', 'BERMUDA', 'UNISSEX', 'TRICOLOR'),
    ('Boné Aba Curva Tricolor Azul', 'Único', 39.90, 'Sarja', 'bone-aba-curva-tricolor-azul.svg', 'bone-aba-curva-tricolor-azul-costas.svg', TRUE, 'Azul', 'ACESSORIOS', 'UNISSEX', 'TRICOLOR'),
    ('Boné Aba Reta Tricolor Preto', 'Único', 44.90, 'Sarja', 'bone-aba-reta-tricolor-preto.svg', 'bone-aba-reta-tricolor-preto-costas.svg', TRUE, 'Preto', 'ACESSORIOS', 'UNISSEX', 'TRICOLOR'),
    ('Boné Trucker Tricolor Branco', 'Único', 42.90, 'Poliéster e Tela', 'bone-trucker-tricolor-branco.svg', 'bone-trucker-tricolor-branco-costas.svg', TRUE, 'Branco', 'ACESSORIOS', 'UNISSEX', 'TRICOLOR'),
    ('Corta-Vento Tricolor Preto', 'PP,P,M,G,GG,XG', 119.90, 'Poliéster', 'corta-vento-tricolor-preto.svg', 'corta-vento-tricolor-preto-costas.svg', TRUE, 'Preto', 'JAQUETA', 'UNISSEX', 'TRICOLOR'),
    ('Corta-Vento Tricolor Azul', 'PP,P,M,G,GG,XG', 124.90, 'Poliéster', 'corta-vento-tricolor-azul.svg', 'corta-vento-tricolor-azul-costas.svg', TRUE, 'Azul', 'JAQUETA', 'UNISSEX', 'TRICOLOR'),
    ('Corta-Vento Tricolor Branco com Capuz', 'PP,P,M,G,GG,XG', 129.90, 'Poliéster', 'corta-vento-tricolor-branco.svg', 'corta-vento-tricolor-branco-costas.svg', TRUE, 'Branco', 'JAQUETA', 'UNISSEX', 'TRICOLOR'),
    ('Camiseta Oversized Urban Culture Preta', 'PP,P,M,G,GG,XG', 54.90, 'Algodão', 'camiseta-oversized-urban-culture-preta.svg', 'camiseta-oversized-urban-culture-preta-costas.svg', TRUE, 'Preto', 'CAMISETA', 'UNISSEX', 'URBAN_CULTURE'),
    ('Camiseta Oversized Urban Culture Off-White', 'PP,P,M,G,GG,XG', 54.90, 'Algodão', 'camiseta-oversized-urban-culture-off-white.svg', 'camiseta-oversized-urban-culture-off-white-costas.svg', TRUE, 'Off-white', 'CAMISETA', 'UNISSEX', 'URBAN_CULTURE'),
    ('Camiseta Oversized Urban Culture Chumbo', 'PP,P,M,G,GG,XG', 54.90, 'Algodão', 'camiseta-oversized-urban-culture-chumbo.svg', 'camiseta-oversized-urban-culture-chumbo-costas.svg', TRUE, 'Cinza Chumbo', 'CAMISETA', 'UNISSEX', 'URBAN_CULTURE'),
    ('Camiseta Oversized Urban Culture Azul Marinho', 'PP,P,M,G,GG,XG', 54.90, 'Algodão', 'camiseta-oversized-urban-culture-azul-marinho.svg', 'camiseta-oversized-urban-culture-azul-marinho-costas.svg', TRUE, 'Azul Marinho', 'CAMISETA', 'UNISSEX', 'URBAN_CULTURE'),
    ('Moletom Oversized Urban Culture Preto', 'PP,P,M,G,GG,XG', 94.90, 'Moletom', 'moletom-oversized-urban-culture-preto.svg', 'moletom-oversized-urban-culture-preto-costas.svg', TRUE, 'Preto', 'MOLETOM', 'UNISSEX', 'URBAN_CULTURE'),
    ('Moletom Oversized Urban Culture Off-White', 'PP,P,M,G,GG,XG', 94.90, 'Moletom', 'moletom-oversized-urban-culture-off-white.svg', 'moletom-oversized-urban-culture-off-white-costas.svg', TRUE, 'Off-white', 'MOLETOM', 'UNISSEX', 'URBAN_CULTURE');

-- =====================================================================
-- OUTLET: peças de fim de estoque (máx. 16), com desconto e poucas unidades por tamanho.
-- outlet = TRUE faz o produto aparecer só na página Outlet; desconto_outlet é a porcentagem.
-- O estoque baixo (1 a 5 por tamanho) é definido na mesma carga de estoque, mais abaixo.
-- =====================================================================
MERGE INTO produto (nome, tamanho, preco, tipo, imagem_frente, imagem_costas, status, cor, categoria, genero, outlet, desconto_outlet) KEY (nome)
VALUES
    ('Jaqueta Corta-Vento Vermelha', 'PP,P,M,G,GG,XG', 89.90, 'Poliéster', 'jaqueta-corta-vento-vermelha.svg', 'jaqueta-corta-vento-vermelha-costas.svg', TRUE, 'Vermelho', 'JAQUETA', 'MASCULINO', TRUE, 30),
    ('Jaqueta Corta-Vento Azul Royal', 'PP,P,M,G,GG,XG', 89.90, 'Poliéster', 'jaqueta-corta-vento-azul-royal.svg', 'jaqueta-corta-vento-azul-royal-costas.svg', TRUE, 'Azul Royal', 'JAQUETA', 'MASCULINO', TRUE, 35),
    ('Jaqueta Jeans Preta', 'PP,P,M,G,GG,XG', 119.90, 'Jeans', 'jaqueta-jeans-preta.svg', 'jaqueta-jeans-preta-costas.svg', TRUE, 'Preto', 'JAQUETA', 'UNISSEX', TRUE, 40),
    ('Bermuda Jeans Preta', '36,38,40,42,44,46', 49.90, 'Jeans', 'bermuda-jeans-preta.svg', 'bermuda-jeans-preta-costas.svg', TRUE, 'Preto', 'BERMUDA', 'MASCULINO', TRUE, 30),
    ('Bermuda Cargo Verde', '36,38,40,42,44,46', 54.90, 'Sarja', 'bermuda-cargo-verde.svg', 'bermuda-cargo-verde-costas.svg', TRUE, 'Verde', 'BERMUDA', 'MASCULINO', TRUE, 25),
    ('Short Saia Plissado Azul Marinho', 'PP,P,M,G,GG,XG', 34.90, 'Poliéster', 'short-saia-plissado-azul-marinho.svg', 'short-saia-plissado-azul-marinho-costas.svg', TRUE, 'Azul Marinho', 'SAIA', 'FEMININO', TRUE, 40),
    ('Cropped Canelado Preto', 'PP,P,M,G,GG,XG', 27.90, 'Canelado', 'cropped-canelado-preto.svg', 'cropped-canelado-preto-costas.svg', TRUE, 'Preto', 'BLUSA', 'FEMININO', TRUE, 50),
    ('Cardigan de Tricô Verde', 'PP,P,M,G,GG,XG', 74.90, 'Tricô', 'cardigan-de-trico-verde.svg', 'cardigan-de-trico-verde-costas.svg', TRUE, 'Verde', 'BLUSA', 'FEMININO', TRUE, 35),
    ('Vestido Midi Azul', 'PP,P,M,G,GG,XG', 79.90, 'Viscose', 'vestido-midi-azul.svg', 'vestido-midi-azul-costas.svg', TRUE, 'Azul', 'VESTIDO', 'FEMININO', TRUE, 45),
    ('Moletom com Capuz Lilás', 'PP,P,M,G,GG,XG', 84.90, 'Moletom', 'moletom-com-capuz-lilas.svg', 'moletom-com-capuz-lilas-costas.svg', TRUE, 'Lilás', 'MOLETOM', 'FEMININO', TRUE, 30),
    ('Camisa Militar Preta', 'PP,P,M,G,GG,XG', 24.90, 'Algodão', 'camisa-militar-preta.svg', 'camisa-militar-preta-costas.svg', TRUE, 'Preto', 'CAMISA', 'MASCULINO', TRUE, 20),
    ('Regata Fitness Ciano', 'PP,P,M,G,GG,XG', 22.90, 'Poliéster', 'regata-fitness-ciano.svg', 'regata-fitness-ciano-costas.svg', TRUE, 'Grafite', 'CAMISETA', 'UNISSEX', TRUE, 25),
    ('Calça Jogger Verde Oliva', 'PP,P,M,G,GG,XG', 59.90, 'Moletom', 'calca-jogger-verde-oliva.svg', 'calca-jogger-verde-oliva-costas.svg', TRUE, 'Verde Oliva', 'CALCA', 'UNISSEX', TRUE, 30),
    ('Blusa de Moletom Bordô', 'PP,P,M,G,GG,XG', 69.90, 'Moletom', 'blusa-de-moletom-bordo.svg', 'blusa-de-moletom-bordo-costas.svg', TRUE, 'Bordô', 'MOLETOM', 'UNISSEX', TRUE, 40),
    ('Camiseta Básica Preta', 'PP,P,M,G,GG,XG', 19.90, 'Algodão', 'camiseta-basica-preta.svg', 'camiseta-basica-preta-costas.svg', TRUE, 'Preto', 'CAMISETA', 'UNISSEX', TRUE, 20),
    ('Camisa Social Azul Claro', 'PP,P,M,G,GG,XG', 64.90, 'Algodão', 'camisa-social-azul-claro.svg', 'camisa-social-azul-claro-costas.svg', TRUE, 'Azul Claro', 'CAMISA', 'MASCULINO', TRUE, 35);

-- O primeiro manto (azul, preto e branco) passa a fazer parte da coleção Tricolor.
UPDATE produto SET colecao = 'TRICOLOR' WHERE nome = 'Manto Imortal Tricolor';

-- =====================================================================
-- ESTOQUE POR TAMANHO. Uma linha em PRODUTO_ESTOQUE para cada tamanho que o produto
-- oferece (coluna tamanho, separada por vírgula). Sem quantidade fixa: o valor é
-- calculado a partir do id e da posição do tamanho, então cada peça tem estoques
-- diferentes (alguns baixos, para a tela de visualização mostrar o alerta).
-- Pode rodar mais de uma vez: só cria o que falta, sem alterar quantidades existentes.
-- Rodar depois do MERGE dos produtos.
-- =====================================================================
-- Só INSERE as linhas que ainda não existem: nunca sobrescreve quantidade já gravada, então
-- baixas de vendas e ajustes feitos pelo admin sobrevivem a reinícios (dev e render).
-- Outlet: de 1 a 5 unidades por tamanho (<= 5 aparece como "poucas unidades"); demais: valor calculado.
INSERT INTO produto_estoque (produto_id, tamanho, quantidade)
SELECT p.id, t.tamanho,
       CASE WHEN p.outlet = TRUE
            THEN MOD(p.id * 3 + LENGTH(t.tamanho) * 2 + ASCII(t.tamanho), 5) + 1
            ELSE MOD(p.id * 7 + t.ordem * 5, 24) + 2 END
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
             UNION ALL SELECT '46', 12
             UNION ALL SELECT 'Único', 13) t
 WHERE ',' || REPLACE(p.tamanho, ' ', '') || ',' LIKE '%,' || t.tamanho || ',%'
   AND NOT EXISTS (SELECT 1 FROM produto_estoque e WHERE e.produto_id = p.id AND e.tamanho = t.tamanho);

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
    

-- =====================================================================
-- Coleção Vice City
-- Produtos da coleção fornecidos em files.zip. As imagens (frente e costas) ficam em static/img.
-- Produtos com colecao = VICE_CITY só aparecem na página da coleção (menu lateral).
-- O MERGE permite executar este data.sql novamente sem duplicar os produtos.
-- =====================================================================
MERGE INTO produto (nome, tamanho, preco, tipo, imagem_frente, imagem_costas, status, cor, categoria, genero, colecao) KEY (nome)
VALUES
    ('Camiseta Neon Sunset', 'PP,P,M,G,GG,XG', 39.90, 'Algodão', 'camiseta-neon-sunset.svg', 'camiseta-neon-sunset-costas.svg', TRUE, 'Roxo Noturno', 'CAMISETA', 'UNISSEX', 'VICE_CITY'),
    ('Camiseta Palmeiras Miami', 'PP,P,M,G,GG,XG', 42.90, 'Algodão', 'camiseta-palmeiras-miami.svg', 'camiseta-palmeiras-miami-costas.svg', FALSE, 'Turquesa Neon', 'CAMISETA', 'UNISSEX', 'VICE_CITY'),
    ('Camisa Havaiana Hibisco', 'PP,P,M,G,GG,XG', 64.90, 'Viscose', 'camisa-havaiana-hibisco.svg', 'camisa-havaiana-hibisco-costas.svg', TRUE, 'Rosa Flamingo', 'CAMISA', 'UNISSEX', 'VICE_CITY'),
    ('Regata Ocean Drive', 'PP,P,M,G,GG,XG', 27.90, 'Poliéster', 'regata-ocean-drive.svg', 'regata-ocean-drive-costas.svg', FALSE, 'Turquesa e Rosa', 'CAMISETA', 'UNISSEX', 'VICE_CITY'),
    ('Moletom Neon Nights', 'PP,P,M,G,GG,XG', 89.90, 'Moletom', 'moletom-neon-nights.svg', 'moletom-neon-nights-costas.svg', TRUE, 'Roxo Noturno', 'MOLETOM', 'UNISSEX', 'VICE_CITY'),
    ('Jaqueta Bomber Flamingo', 'PP,P,M,G,GG,XG', 129.90, 'Poliéster', 'jaqueta-bomber-flamingo.svg', 'jaqueta-bomber-flamingo-costas.svg', FALSE, 'Rosa Flamingo', 'JAQUETA', 'UNISSEX', 'VICE_CITY'),
    ('Jaqueta Corta-Vento Sunset', 'PP,P,M,G,GG,XG', 99.90, 'Poliéster', 'jaqueta-corta-vento-sunset.svg', 'jaqueta-corta-vento-sunset-costas.svg', TRUE, 'Laranja Pôr do Sol', 'JAQUETA', 'UNISSEX', 'VICE_CITY'),
    ('Calça Jogger Sunset', '36,38,40,42,44,46', 72.90, 'Moletom', 'calca-jogger-sunset.svg', 'calca-jogger-sunset-costas.svg', FALSE, 'Roxo Noturno', 'CALCA', 'UNISSEX', 'VICE_CITY'),
    ('Bermuda Tropical Neon', '36,38,40,42,44,46', 59.90, 'Poliéster', 'bermuda-tropical-neon.svg', 'bermuda-tropical-neon-costas.svg', TRUE, 'Azul Noite', 'BERMUDA', 'UNISSEX', 'VICE_CITY'),
    ('Bermuda Jeans Neon', '36,38,40,42,44,46', 64.90, 'Jeans', 'bermuda-jeans-neon.svg', 'bermuda-jeans-neon-costas.svg', FALSE, 'Azul Néon', 'BERMUDA', 'UNISSEX', 'VICE_CITY'),
    ('Vestido Midi Coral Beach', 'PP,P,M,G,GG,XG', 84.90, 'Viscose', 'vestido-midi-coral-beach.svg', 'vestido-midi-coral-beach-costas.svg', TRUE, 'Coral', 'VESTIDO', 'UNISSEX', 'VICE_CITY'),
    ('Vestido Curto Sunset Stripes', 'PP,P,M,G,GG,XG', 69.90, 'Malha', 'vestido-curto-sunset-stripes.svg', 'vestido-curto-sunset-stripes-costas.svg', FALSE, 'Degradê Sunset', 'VESTIDO', 'UNISSEX', 'VICE_CITY'),
    ('Cropped Canelado Turquesa', 'PP,P,M,G,GG,XG', 31.90, 'Canelado', 'cropped-canelado-turquesa.svg', 'cropped-canelado-turquesa-costas.svg', TRUE, 'Turquesa Neon', 'BLUSA', 'UNISSEX', 'VICE_CITY'),
    ('Saia Midi Hibisco Noite', 'PP,P,M,G,GG,XG', 61.90, 'Viscose', 'saia-midi-hibisco-noite.svg', 'saia-midi-hibisco-noite-costas.svg', FALSE, 'Roxo e Rosa', 'SAIA', 'UNISSEX', 'VICE_CITY'),
    ('Camiseta Retrô 86', 'PP,P,M,G,GG,XG', 44.90, 'Algodão', 'camiseta-retro-86.svg', 'camiseta-retro-86-costas.svg', TRUE, 'Branco Off-White', 'CAMISETA', 'UNISSEX', 'VICE_CITY');

    -- Desenvolvimento (H2): remove CPFs antigos do banco persistente local.
    -- IDs internos e username continuam identificando contas; CPF nao e necessario aqui.
    UPDATE USUARIO SET cpf = NULL WHERE cpf IS NOT NULL;

    -- MERGE por username: data.sql roda em todo startup dev e preserva a conta de teste.
    MERGE INTO USUARIO (username, senha, role, ativo, email, cpf) KEY (username)
    VALUES ('admin', '$2a$10$oz8mJWjsca2yd5hdU5pvzO123repm3JNyZ9fxbFzMs5wVaaccsiZG', 'ROLE_ADMIN', TRUE, 'thiagomfreitas05@gmail.com', NULL);

    -- Cliente de teste (login: user). Mesma senha do admin.
    MERGE INTO USUARIO (nome, username, senha, role, ativo) KEY (username)
    VALUES ('Usuário', 'user', '$2a$10$oz8mJWjsca2yd5hdU5pvzO123repm3JNyZ9fxbFzMs5wVaaccsiZG', 'ROLE_USUARIO', TRUE);

    -- Migração do perfil antigo (ROLE_GESTOR -> ROLE_SEPARADOR) para bancos H2 persistentes já existentes.
    UPDATE USUARIO SET role = 'ROLE_SEPARADOR' WHERE role = 'ROLE_GESTOR';
    UPDATE USUARIO SET username = 'separador', nome = 'Separador'
     WHERE username = 'gestor' AND role = 'ROLE_SEPARADOR'
       AND NOT EXISTS (SELECT 1 FROM (SELECT username FROM USUARIO) u2 WHERE u2.username = 'separador');

    -- Separador (login: separador, senha de teste 1234). Perfil ROLE_SEPARADOR: acessa SOMENTE a separação de pedidos (/admin/separacao).
    -- Mesma senha de teste do admin: troque antes de colocar a loja no ar.
    MERGE INTO USUARIO (nome, username, senha, role, ativo) KEY (username)
    VALUES ('Separador', 'separador', '$2a$10$oz8mJWjsca2yd5hdU5pvzO123repm3JNyZ9fxbFzMs5wVaaccsiZG', 'ROLE_SEPARADOR', TRUE);
