# Integração Asaas — Nazo Moçambique

A integração utiliza exclusivamente o Asaas para Pix dinâmico. O ambiente padrão é o Sandbox; nenhum pagamento real é processado enquanto `ASAAS_ENABLED=false` ou enquanto forem usadas as credenciais Sandbox.

## Variáveis de ambiente

Copie `.env.example` para seu ambiente de execução e configure:

- `ASAAS_ENABLED=true`
- `ASAAS_BASE_URL=https://api-sandbox.asaas.com/v3`
- `ASAAS_API_KEY`: chave API gerada no painel Sandbox.
- `ASAAS_WEBHOOK_TOKEN`: token aleatório entre 32 e 255 caracteres, sem espaços. Não use a API Key como token do webhook.
- `ASAAS_PIX_EXPIRACAO_MINUTOS=60` (prazo de contingência usado se a API não informar expiração).

Não versione `.env`, API Keys ou tokens.

## Webhook

Depois de publicar a aplicação em HTTPS, cadastre `https://SEU-DOMINIO/webhooks/asaas` no painel Sandbox. Configure o mesmo token em `ASAAS_WEBHOOK_TOKEN` e habilite pelo menos `PAYMENT_CONFIRMED`, `PAYMENT_RECEIVED` e `PAYMENT_REFUNDED`. O Asaas envia o token no header `asaas-access-token`.

O endpoint consulta a cobrança diretamente na API antes de alterar o pedido. Só aceita status `CONFIRMED` ou `RECEIVED`, confere o valor e a referência externa do pedido, e evita creditar cashback duas vezes. O pedido cancelado não é reaberto; se houver recebimento posterior, o sistema solicita estorno.

## Plano B: sincronização do status (quando o aviso do webhook não chega)

Além do webhook, o sistema confere o pagamento direto na API do Asaas, só para pedidos Pix em "Aguardando pagamento" que já têm cobrança:

- Ao abrir `/pedido/{id}` e a cada consulta de `/pedido/{id}/status` (a tela consulta a cada 5 s), no máximo uma ida ao Asaas por pedido a cada 15 s.
- A tarefa agendada `PagamentoAsaasService.sincronizarPeriodicamente()` roda a cada 2 minutos (`asaas.sincronizacao-intervalo-ms` muda o intervalo) para os pedidos Pix pendentes das últimas 24 h.

Se o Asaas não responder, o pedido mantém o status do banco e nada quebra. A confirmação usa a mesma regra do webhook (consulta a cobrança, confere valor e referência, não repete cashback), então os dois caminhos podem coexistir.

## Fluxo Pix

1. O sistema cadastra o cliente no Asaas.
2. Cria uma cobrança `billingType=PIX` com valor e referência do pedido.
3. Consulta `/payments/{id}/pixQrCode` para obter imagem Base64 e Pix Copia e Cola.
4. O webhook recebe eventos e consulta `/payments/{id}` para confirmar a situação.

## Testes locais

O Asaas precisa alcançar um endpoint público HTTPS. Para testes locais, exponha temporariamente a porta da aplicação com um túnel HTTPS (por exemplo, Cloudflare Tunnel ou ngrok) e cadastre a URL pública. Sandbox e Produção têm credenciais e webhooks independentes.

## Observações

A API do Asaas exige que a conta tenha condições de receber Pix. A disponibilidade do Sandbox e os dados de clientes de teste devem ser conferidos no painel/documentação oficial. O projeto acadêmico não precisa de CNPJ para desenvolver e executar a lógica local; a habilitação de recebimentos reais depende dos requisitos cadastrais e de aprovação do provedor.

## Referências oficiais consultadas

- [Criar cobrança Pix](https://docs.asaas.com/docs/cobrancas-via-pix)
- [Consultar QR Code Pix](https://docs.asaas.com/reference/obter-qr-code-para-pagamentos-via-pix)
- [Receber eventos no Webhook](https://docs.asaas.com/docs/receba-eventos-do-asaas-no-seu-endpoint-de-webhook)
- [Eventos de cobrança](https://docs.asaas.com/docs/webhook-para-cobrancas)
- [FAQ do Sandbox](https://docs.asaas.com/docs/faq-sandbox-1)

O Sandbox pode ter restrições em alguns fluxos de QR Code Pix, conforme a FAQ oficial. Se o QR Code não puder ser pago no Sandbox, isso não significa necessariamente que o endpoint de produção terá o mesmo comportamento; valide as limitações antes da apresentação final.
