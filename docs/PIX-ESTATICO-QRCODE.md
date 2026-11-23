# Pix estático com QR Code (valor do subtotal)

Status: `🟡 IMPLEMENTADO — VALIDAÇÃO PENDENTE` (ver `AGENTS.md`, Fase 3).

## O que faz

Na tela do pedido (`/pedido/{id}`), para pedido **Pix aguardando pagamento**, mostra:

- o QR Code do Pix, com o valor do **subtotal dos itens** (sem frete e sem descontos de cashback);
- o código "copia e cola" e o botão **Copiar código**;
- o código do pedido dentro do Pix (campo txid, só letras e números: `EL-261120-7K3Q` vira `EL2611207K3Q`),
  que aparece no comprovante e ajuda a equipe a conferir o pagamento no extrato.

A tela consulta `/pedido/{id}/status` a cada 10 s e se atualiza sozinha quando a equipe muda a situação do pedido.

## Quando aparece

| Situação | Resultado |
|---|---|
| `pix.*` preenchido | QR Code estático com o subtotal. |
| `pix.*` incompleto | Nada aparece (comportamento anterior). |
| Pedido pago, cancelado ou de outra forma de pagamento | Nada aparece. |

## Configuração (variáveis de ambiente; nunca grave a chave no repositório)

| Variável | Propriedade | Exemplo |
|---|---|---|
| `PIX_CHAVE` | `pix.chave` | CPF/CNPJ só com dígitos, e-mail, celular `+5551999999999` ou chave aleatória |
| `PIX_NOME` | `pix.nome` | `Nazo Moçambique` (até 25 caracteres; acentos são removidos) |
| `PIX_CIDADE` | `pix.cidade` | `Porto Alegre` (até 15 caracteres; acentos são removidos) |

No perfil `dev` há valores fictícios (`dev@example.com`) só para o QR Code aparecer; pagamentos reais não
funcionam com eles. No perfil `prod` não há valor padrão.

## Limitações conhecidas

- Pix estático **não avisa** a loja quando é pago. A equipe confere o extrato e muda o pedido para "Pagamento
  confirmado" no painel.
- O valor é o subtotal (pedido do responsável). O frete não entra; se for preciso cobrar o total, troque
  `pedido.getSubtotal()` por `pedido.getTotal()` em `PixEstaticoService.gerar` (e ajuste o texto da tela).
- O QR Code é gerado por `QrCode.java` (ISO/IEC 18004, modo byte, correção M, versões 1 a 40), sem biblioteca externa.
  Se preferir uma biblioteca (ex.: ZXing), basta trocar a chamada em `PixEstaticoService`.

## Testes

`PixBrCodeTests`, `QrCodeTests` e `PixEstaticoServiceTests` (em `src/test/java/.../service`).
Rodar: `./mvnw test -Dtest='PixBrCodeTests,QrCodeTests,PixEstaticoServiceTests'`.
