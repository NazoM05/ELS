# Fase 1 — Estabilidade e segurança

## Alterações nesta entrega

- CSRF passa a proteger todas as requisições mutáveis por padrão. A exceção é somente `POST /webhooks/mercadopago`, que valida a assinatura HMAC no controller e consulta o pagamento na API antes de alterar o pedido.
- O logout do menu passou de link GET para formulário POST, protegido por CSRF.
- A configuração local continua usando H2 em memória, adequada para desenvolvimento e testes, não para guardar vendas reais.
- Foi adicionado o perfil `prod`, com PostgreSQL, credenciais obrigatórias por variáveis de ambiente, console H2 desativado, SQL init desativado e Hibernate em `validate` (não altera o schema automaticamente).
- Foi adicionada a dependência JDBC do PostgreSQL e o arquivo `.env.example` sem credenciais reais.
- A gestão não permite cancelar diretamente um pedido marcado como PAGO. O sistema ainda não implementa estorno automático; bloquear o cancelamento evita devolver estoque sem estornar o dinheiro.
- Foi adicionado teste de regressão para esse bloqueio.

## Executar em desenvolvimento

O perfil padrão é `dev` e continua usando H2 em memória. Os dados são perdidos ao reiniciar a aplicação.

## Preparação de produção

1. Criar uma base PostgreSQL e um usuário exclusivo da aplicação.
2. Configurar `SPRING_PROFILES_ACTIVE=prod`, `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME` e `DB_PASSWORD` no ambiente seguro.
3. **Não iniciar o perfil `prod` até o schema PostgreSQL estar criado e validado.** Esta entrega ainda não inclui migrações Flyway/Liquibase nem conversão completa do `data.sql` específico do H2. O `ddl-auto=validate` faz a aplicação falhar de forma segura se o schema não corresponder às entidades.
4. Configurar tokens e segredos das integrações no gerenciador de segredos do ambiente, nunca no Git.
5. Publicar somente atrás de HTTPS e configurar o domínio/URL pública do webhook do Mercado Pago.

## Validação

A configuração e os fontes foram inspecionados. A execução de Maven/testes depende de JDK 25 e acesso ao repositório Maven; o ambiente de trabalho disponível possui JDK 21 e não conseguiu baixar Maven. Portanto, os testes automatizados não foram executados nesta entrega. O teste novo foi adicionado, mas precisa ser executado em ambiente compatível.
