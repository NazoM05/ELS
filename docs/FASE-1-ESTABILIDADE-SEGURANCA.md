# Estabilidade e segurança — atualização Asaas

- CSRF permanece habilitado por padrão. A exceção pública é somente `POST /webhooks/asaas`, autenticado pelo header `asaas-access-token`.
- O webhook consulta o estado atual da cobrança na API Asaas antes de atualizar o pedido.
- A integração usa Sandbox por padrão e credenciais fornecidas por variáveis de ambiente.
- Não versione chaves API nem tokens do webhook.
- Para testar o webhook localmente, exponha a aplicação por HTTPS usando um túnel e cadastre a URL pública no painel Sandbox.
