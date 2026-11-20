# Frete pelos Correios (opcional)

Por padrão a integração vem **desligada** (`correios.enabled=false`): o checkout continua usando o frete fixo
(R$ 19,90; grátis a partir de R$ 99 no Sul e R$ 250 nas demais regiões) e o prazo por estado de `PrazoEntregaService`.

## Para ligar (precisa de contrato com os Correios)
1. No portal CWS (cws.correios.com.br) > *Gestão de acesso a APIs*, gere o **código de acesso**.
2. Peça ao gestor do contrato para liberar as APIs **Preço (38202)** e **Prazo (38210)** no contrato e no cartão de postagem.
3. Defina as variáveis de ambiente (nunca grave senha no application.properties):

| Variável | Valor |
|---|---|
| `CORREIOS_ENABLED` | `true` |
| `CORREIOS_USUARIO` | CNPJ do contrato, só números |
| `CORREIOS_CODIGO_ACESSO` | código gerado no CWS (não é a senha do Meu Correios) |
| `CORREIOS_CARTAO_POSTAGEM` | número do cartão, como texto (tem zeros à esquerda) |
| `CORREIOS_CEP_ORIGEM` | CEP de onde a loja despacha |
| `CORREIOS_SERVICOS` | opcional; padrão `03298,03220` (PAC e SEDEX contrato; confira os seus no CWS) |
| `CORREIOS_BASE_URL` | opcional; padrão `https://api.correios.com.br` |

## Como funciona
- `GET /frete/cotar?cep=&uf=&item=id:tam:qtd` (exige login) devolve as opções de entrega; a tela do checkout mostra e soma.
- O servidor **recalcula o frete ao confirmar o pedido**; o valor vindo do navegador nunca é usado.
- Se os Correios falharem (rede, credencial, serviço não contratado), o checkout cai para o frete fixo.
- Frete grátis vale para a opção **mais barata** (PAC); SEDEX continua pago.
- Peso e medidas vêm de `EmbalagemFrete` (média por categoria). **Ajuste esses números** com as medidas reais das peças.
- O pedido guarda o nome da forma de entrega e o prazo (`servico_frete`, `prazo_frete_dias`).
