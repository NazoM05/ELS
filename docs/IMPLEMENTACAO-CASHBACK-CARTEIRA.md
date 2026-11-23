# Cashback e carteira — integração Asaas

- O cashback do pedido é creditado uma única vez quando o pagamento é confirmado pela consulta à API do Asaas ou quando a equipe altera o pedido para `PAGO`.
- O identificador do pagamento Asaas é armazenado como texto (`pay_...`), não como número.
- O webhook não é tratado como prova de pagamento: o backend consulta a cobrança na API, confere a referência externa e o valor total antes de atualizar o pedido.
- O identificador do pedido é usado como `externalReference` para localizar uma cobrança criada antes de uma falha de comunicação e reduzir risco de duplicidade.
- Cancelamento de pedido com pagamento recebido solicita estorno integral pela API do Asaas. Se o pagamento for recebido depois do cancelamento, o pedido não é reaberto e o sistema solicita o estorno.

Consulte `ASAAS.md` para variáveis de ambiente, Sandbox e configuração do Webhook.
