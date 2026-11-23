# Redesign Pix e Cashback

- As páginas administrativas `/admin/pix` e `/admin/cashback` receberam layout responsivo renovado.
- Cashback agora inicia desativado para novas configurações (`ConfiguracaoCashback.ativo=false`).
- Pix recebe como padrão o nome `Nazo Moçambique`, cidade `Porto Alegre`, e usa a variável `PIX_CNPJ` como chave quando `PIX_CHAVE` não estiver definida.
- Não foi inserido um CNPJ fictício: configure `PIX_CNPJ` com o CNPJ real da empresa (apenas números).
- Para personalizar, `PIX_NOME` e `PIX_CIDADE` continuam disponíveis. Uma configuração salva em `uploads/config/pix.properties` prevalece sobre os defaults.

## Variáveis

`PIX_CNPJ=cnpj-real-somente-digitos`
`PIX_NOME=Nazo Moçambique`
`PIX_CIDADE=Porto Alegre`

`PIX_CHAVE`, quando definida, prevalece sobre `PIX_CNPJ`.
