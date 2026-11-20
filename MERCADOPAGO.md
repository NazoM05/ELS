# Pagamento por Pix (Mercado Pago)

Por padrão a integração vem **desligada** (`mercadopago.enabled=false`): o checkout continua gravando o pedido
como "Aguardando pagamento" e a equipe confirma o pagamento à mão, como antes.

## Para ligar
1. No painel de desenvolvedores do Mercado Pago, crie uma **aplicação** (tipo pagamentos online) e copie o **Access Token**.
   Comece pelas credenciais de **teste**.
2. Em *Webhooks*, cadastre a URL pública `https://SEU-DOMINIO/webhooks/mercadopago`, marque o evento **Pagamentos**
   e copie a **assinatura secreta**.
3. Defina as variáveis de ambiente (nunca grave chave no application.properties):

| Variável | Valor |
|---|---|
| `MERCADOPAGO_ENABLED` | `true` |
| `MERCADOPAGO_ACCESS_TOKEN` | Access Token da aplicação |
| `MERCADOPAGO_WEBHOOK_SECRET` | assinatura secreta do webhook (sem ela o webhook recusa tudo) |
| `MERCADOPAGO_NOTIFICATION_URL` | opcional; URL do webhook enviada em cada pagamento (precisa ser https público) |
| `MERCADOPAGO_PIX_EXPIRACAO_MINUTOS` | opcional; padrão 60 (o Mercado Pago aceita de 30 minutos a 30 dias) |

Testando no seu computador: o Mercado Pago não alcança `localhost`. Use um túnel (ngrok, cloudflared) e cadastre a URL dele.

## Como funciona
- Ao finalizar a compra com **Pix**, o pedido é gravado normalmente (estoque baixado, "Aguardando pagamento") e, em seguida,
  o servidor cria o pagamento no Mercado Pago. A tela do pedido mostra o QR Code e o código copia e cola.
- Se o Mercado Pago falhar ou o Pix vencer, o pedido continua valendo e a tela oferece o botão **Gerar Pix**.
- Quando o cliente paga, o Mercado Pago chama `POST /webhooks/mercadopago`. O servidor confere a **assinatura**, consulta o
  pagamento na API (o aviso não é usado como prova) e, se estiver aprovado e com o mesmo valor do pedido, passa o pedido para
  "Pagamento confirmado" e registra no histórico. A página do pedido aberta no navegador se atualiza sozinha.
- Avisos repetidos são ignorados. Pagamento aprovado depois de o pedido ser cancelado **não** reabre o pedido: o histórico
  avisa a equipe para devolver o valor.
- O pedido guarda `mpPaymentId`, `mpStatus` e o Pix em uso.

## Arquivos
`config/MercadoPagoProperties`, `config/MercadoPagoConfig`, `service/GatewayPagamento`, `service/MercadoPagoClient`,
`service/MercadoPagoAssinatura`, `service/PagamentoMercadoPagoService`, `controller/MercadoPagoWebhookController`,
`CheckoutController` (gera o Pix, botão "Gerar Pix" e consulta de status), `pedido.html`.
Testes: `MercadoPagoAssinaturaTests`, `PagamentoMercadoPagoServiceTests`.
