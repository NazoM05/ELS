# Aba de Pedidos

Toda venda finalizada no /checkout gera um **código de pedido** (ex.: `EL-261120-7K3Q`):
`EL` + data da compra (aammdd) + 4 caracteres aleatórios, sem letras/números que se confundem (0/O, 1/I/L).
O cliente vê o código na tela "Pedido confirmado"; a equipe usa o mesmo código no painel.

## Onde fica
Menu lateral > Opções de administrador > **Pedidos** (`/admin/pedidos`, só para ADMIN).

- Abas por etapa: Em aberto (padrão), Todos, Aguardando pagamento, Pagamento confirmado, Em separação, Enviado, Entregue, Cancelado.
- Busca no topo: digitar filtra a tabela (código, cliente, e-mail ou cidade); "Abrir pedido" abre direto pelo código completo (aceita minúsculas e sem hífens).
- Página do pedido (`/admin/pedidos/{codigo}`): itens, valores, cliente/entrega, histórico e o formulário de acompanhamento (etapa, código de rastreio, observação). Cada salvamento vira uma linha no histórico com o nome de quem fez.

## Regras
- Pedido novo começa em "Aguardando pagamento".
- Cancelar devolve as peças ao estoque (uma vez só) e o pedido não volta para outra etapa.
- Rastreio: só letras, números e hífen, até 40 caracteres. Observação: até 300 caracteres.
- Os textos das telas novas podem ser trocados em Configurações (seções "Admin: pedidos" e "Admin: acompanhar pedido").

Arquivos: `model/StatusPedido`, `model/HistoricoPedido`, `service/CodigoPedido`, `service/GestaoPedidoService`,
`controller/AdminPedidoController`, `templates/admin/pedido/*`. Testes: `CodigoPedidoTests`, `GestaoPedidoServiceTests`.
