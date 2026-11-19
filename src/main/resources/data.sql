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

-- =====================================================================
-- Carga inicial. MERGE ... KEY (nome): insere o produto novo ou atualiza
-- status e cor se o nome já existir (o nome é único), então pode rodar
-- mais de uma vez sem erro. status alterna TRUE/FALSE na ordem da lista.
-- =====================================================================
MERGE INTO produto (nome, tamanho, preco, tipo, imagem_frente, imagem_costas, status, cor) KEY (nome)
VALUES
    ('Camisa Militar', 'GG', 24.90, 'Algodão', 'camisa-militar.svg', 'camisa-militar-costas.svg', TRUE, 'Verde Militar'),
    ('Camisa Polo Listrada', 'M', 39.90, 'Algodão', 'camisa-polo-listrada.svg', 'camisa-polo-listrada-costas.svg', FALSE, 'Azul Marinho'),
    ('Camiseta Básica Branca', 'G', 19.90, 'Algodão', 'camiseta-basica-branca.svg', 'camiseta-basica-branca-costas.svg', TRUE, 'Branco'),
    ('Camiseta Estampa Floral', 'P', 29.90, 'Malha', 'camiseta-estampa-floral.svg', 'camiseta-estampa-floral-costas.svg', FALSE, 'Rosa Claro'),
    ('Camiseta Oversized Preta', 'GG', 44.90, 'Algodão', 'camiseta-oversized-preta.svg', 'camiseta-oversized-preta-costas.svg', TRUE, 'Preto'),
    ('Regata Fitness', 'M', 22.90, 'Poliéster', 'regata-fitness.svg', 'regata-fitness-costas.svg', FALSE, 'Grafite'),
    ('Blusa de Moletom Cinza', 'G', 69.90, 'Moletom', 'blusa-de-moletom-cinza.svg', 'blusa-de-moletom-cinza-costas.svg', TRUE, 'Cinza'),
    ('Moletom com Capuz Rosa', 'M', 84.90, 'Moletom', 'moletom-com-capuz-rosa.svg', 'moletom-com-capuz-rosa-costas.svg', FALSE, 'Rosa'),
    ('Jaqueta Jeans', 'G', 119.90, 'Jeans', 'jaqueta-jeans.svg', 'jaqueta-jeans-costas.svg', TRUE, 'Azul Jeans'),
    ('Jaqueta Corta-Vento', 'M', 89.90, 'Poliéster', 'jaqueta-corta-vento.svg', 'jaqueta-corta-vento-costas.svg', FALSE, 'Verde Petróleo'),
    ('Calça Jeans Skinny', '38', 99.90, 'Jeans', 'calca-jeans-skinny.svg', 'calca-jeans-skinny-costas.svg', TRUE, 'Azul Escuro'),
    ('Calça Jeans Reta', '42', 109.90, 'Jeans', 'calca-jeans-reta.svg', 'calca-jeans-reta-costas.svg', FALSE, 'Azul Jeans Escuro'),
    ('Calça Jogger', 'M', 59.90, 'Moletom', 'calca-jogger.svg', 'calca-jogger-costas.svg', TRUE, 'Cinza Chumbo'),
    ('Calça Alfaiataria Preta', '40', 94.90, 'Viscose', 'calca-alfaiataria-preta.svg', 'calca-alfaiataria-preta-costas.svg', FALSE, 'Preto'),
    ('Bermuda Cargo', '42', 54.90, 'Sarja', 'bermuda-cargo.svg', 'bermuda-cargo-costas.svg', TRUE, 'Caqui'),
    ('Bermuda Jeans', '40', 49.90, 'Jeans', 'bermuda-jeans.svg', 'bermuda-jeans-costas.svg', FALSE, 'Azul Jeans'),
    ('Short Saia Plissado', 'P', 34.90, 'Poliéster', 'short-saia-plissado.svg', 'short-saia-plissado-costas.svg', TRUE, 'Lilás'),
    ('Saia Midi Floral', 'M', 57.90, 'Viscose', 'saia-midi-floral.svg', 'saia-midi-floral-costas.svg', FALSE, 'Pêssego'),
    ('Vestido Midi Rosa', 'M', 79.90, 'Viscose', 'vestido-midi-rosa.svg', 'vestido-midi-rosa-costas.svg', TRUE, 'Rosa'),
    ('Vestido Curto Listrado', 'P', 64.90, 'Malha', 'vestido-curto-listrado.svg', 'vestido-curto-listrado-costas.svg', FALSE, 'Rosa e Branco'),
    ('Cropped Canelado', 'P', 27.90, 'Canelado', 'cropped-canelado.svg', 'cropped-canelado-costas.svg', TRUE, 'Bege'),
    ('Cardigan de Tricô', 'G', 74.90, 'Tricô', 'cardigan-de-trico.svg', 'cardigan-de-trico-costas.svg', FALSE, 'Caramelo'),
    ('Camisa Social Branca', 'M', 64.90, 'Algodão', 'camisa-social-branca.svg', 'camisa-social-branca-costas.svg', TRUE, 'Branco'),
    ('Camisa Xadrez Flanela', 'G', 52.90, 'Flanela', 'camisa-xadrez-flanela.svg', 'camisa-xadrez-flanela-costas.svg', FALSE, 'Vermelho');
    
    insert into USUARIO (username, senha, role, ativo) values ('admin', '$2a$10$oz8mJWjsca2yd5hdU5pvzO123repm3JNyZ9fxbFzMs5wVaaccsiZG', 'ROLE_ADMIN', TRUE),
    ('user', '$2a$10$oz8mJWjsca2yd5hdU5pvzO123repm3JNyZ9fxbFzMs5wVaaccsiZG','ROLE_USER', TRUE);
