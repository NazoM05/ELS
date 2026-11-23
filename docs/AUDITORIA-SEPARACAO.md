# Auditoria de acesso – Separação de pedidos (/admin/separacao)

Regra: só os perfis ADMIN e GESTOR abrem a página e leem/alteram os dados dela.

## Situação encontrada
| Quem | Antes | Agora |
|---|---|---|
| Visitante sem login | Ia para o login | Continua indo para o login |
| Cliente (ROLE_USUARIO) | Erro 403 genérico | Vai para a página "Acesso negado"; dados retornam 403 |
| Admin (ROLE_ADMIN) | Acesso | Acesso |
| Gestor (ROLE_GESTOR) | Não existia | Acesso só à separação (demais telas do admin bloqueadas) |

Brechas e problemas corrigidos:
1. CSRF desligado: outro site podia mudar a etapa de um pedido usando a sessão de um admin logado. Agora as ações da separação exigem o token da página.
2. Contas desativadas (ativo = false) ainda conseguiam entrar. Agora são bloqueadas no login.
3. A proteção estava só na URL; agora o controller também confere o perfil (@PreAuthorize).
4. A página de separação não abria (erro do Thymeleaf com "[[" no JavaScript). Corrigido.
5. Não havia página de acesso negado. Criada em /acesso-negado.

Banco de dados: o projeto usa H2 + JPA com um único usuário de banco, então não existe RLS como no Supabase.
A proteção dos dados fica no servidor (Spring Security na URL + no controller). O console H2 segue só para ADMIN.

## Arquivos alterados
- securityconfig/SecurityConfig.java – regra ADMIN/GESTOR, CSRF da separação, página de acesso negado, logout por link mantido
- securityconfig/AcessoUsuario.java – conta desativada não entra
- controller/AdminPedidoController.java (separação; antes AdminSeparacaoController) – @PreAuthorize ADMIN/GESTOR
- controller/AcessoNegadoController.java + templates/acesso-negado.html – novos (+ uma regra em static/css/berzerk.css para a página de uma coluna)
- templates/fragments/fragmentos.html – menu "Área do gestor" só com a separação
- templates/admin/pedido/separacao.html – token CSRF, correção do "[[", gestor vê o código sem link para a ficha do admin
- resources/data.sql – usuário gestor (ROLE_GESTOR, senha de teste 1234)
- model/Usuario.java – comentário dos perfis
- test/.../securityconfig/SeparacaoAcessoTests.java – 15 testes de acesso

## Pendências recomendadas (fora do escopo)
- O resto do site continua sem CSRF (formulários do admin, perfil, checkout).
- Trocar a senha de teste (1234) de admin, user e gestor antes de publicar.
- O teste em src/test/java/com/example/nm/ já falhava antes (pacote sem a aplicação) e pode ser apagado.

---

# Histórico de alterações dos pedidos (/admin/historico)

Cada alteração de pedido (na ficha do pedido ou na tela de separação) grava:
- quem alterou: nome mostrado e login (e-mail) da conta;
- quando: data e hora (com segundos);
- etapa anterior e etapa nova;
- código de rastreio anterior e novo (quando mudou);
- a observação.

A criação do pedido também entra, sem etapa anterior, com o login do cliente.
As colunas do histórico não podem ser alteradas depois de gravadas (updatable = false) e não há tela para editar ou apagar.

Tela "Histórico de alterações" (menu do admin): filtros por pedido, por quem alterou (nome ou e-mail), por etapa e por período, além da opção "Só mudanças de etapa", com 30 registros por página.
A ficha de cada pedido mostra "anterior → nova" e tem o link "Ver no histórico de alterações".
Acesso: só ROLE_ADMIN. O gestor e o cliente vão para "Acesso negado", e o visitante sem login vai para o login.

Arquivos: model/HistoricoPedido.java, model/Pedido.java, service/GestaoPedidoService.java, service/PedidoService.java,
repository/HistoricoPedidoRepository.java (novo), controller/AdminHistoricoController.java (novo),
controller/AdminPedidoController.java, controller/AdminSeparacaoController.java, templates/admin/historico.html (novo),
templates/admin/pedido/detalhes.html, templates/fragments/fragmentos.html, static/css/berzerk.css.
Testes: 3 novos em GestaoPedidoServiceTests e 4 novos em SeparacaoAcessoTests.

Observação: alterações feitas antes desta versão não têm etapa anterior nem login gravados (o banco atual é em memória e começa vazio a cada execução).
