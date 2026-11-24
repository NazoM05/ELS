# Personalização do site (Admin > Configurações)

A tela **Configurações** do painel agora tem um menu com uma seção para cada parte do site:

| Seção | O que muda |
|---|---|
| Cor principal e logo | Cor do tema e logo (como antes) |
| Textos do site todo | Nome da loja, avisos da barra do topo, busca, menu lateral, carrinho, filtros, paginação e rodapé |
| Cores de cada item | Cor de cada parte da interface em **todas** as páginas (barra de avisos, topo, botões, cards, menu, carrinho, rodapé, login...) |
| Uma seção por página | Nome da página (aba do navegador), títulos, textos dos botões e as cores dessa página (inclusive uma cor principal só dela) |

- Texto em branco volta ao padrão; nos campos marcados como opcionais (avisos, selos, e-mail do rodapé...), em branco esconde o item.
- Cor em branco segue o padrão. A cor de uma página vence a do site todo.
- A prévia ao lado mostra as cores enquanto você escolhe; os textos aparecem depois de salvar.
- "Restaurar seção" volta só aquela seção ao padrão.

## Como funciona

- `model/CatalogoPersonalizacao.java`: lista fechada de textos e cores (chave, rótulo, padrão, seletor CSS). Para deixar um texto novo editável, adicione o campo aqui e use `${txt['chave']}` no template.
- `service/PersonalizacaoService.java`: guarda só o que difere do padrão em `uploads/config/personalizacao.properties` (o H2 é em memória) e gera o CSS.
- Cada página tem uma classe no `<body>` (`pg-login`, `pg-checkout`...), usada para as cores por página.
- Os textos são impressos com escape e as cores são validadas como `#RRGGBB`; os seletores CSS ficam fixos no código.
