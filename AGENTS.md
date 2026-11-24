# AGENTS.md — Instruções para IAs (e pessoas) que trabalham neste projeto

Projeto: loja Spring Boot + Thymeleaf (Estilo London Store / Nazo Moçambique).
Leia este arquivo **antes de qualquer alteração visual**.

## Regra principal: CSS centralizado em UM único arquivo

Todo o CSS da aplicação fica em:

    src/main/resources/static/css/site.css

E é carregado em **todas** as páginas por um único fragmento Thymeleaf:

    src/main/resources/templates/fragments/css.html  →  fragmento "css"

### Toda página nova deve ter, dentro do `<head>`:

```html
<th:block th:replace="~{fragments/css :: css}"></th:block>
```

Esse fragmento já inclui: fontes do Google (Anton + Montserrat), o `site.css`
e as cores escolhidas em Admin > Configurações. Não adicione mais nada de CSS no `<head>`.

## É proibido

- Criar outro arquivo `.css` (ex.: `admin.css`, `pagina-x.css`, `novo.css`).
- Colocar blocos `<style>` em templates ou fragmentos.
- Usar atributo `style="..."` no HTML ou em HTML montado por JavaScript
  (template strings, `innerHTML`). Crie uma classe em `site.css`.
- Adicionar `<link rel="stylesheet">` direto nas páginas.
- Usar `@import` de outros arquivos CSS dentro do `site.css`.

## Como adicionar ou alterar estilos

1. Abra `static/css/site.css` e localize a seção certa (índice no topo do arquivo).
   Se for uma tela nova, crie uma seção nova no fim, antes de "Utilitários".
2. Use prefixo da tela nas classes específicas (`sep-`, `adm-`, `detalhes-`,
   `checkout-`, `config-`, ...) para não colidir com outras páginas.
3. Reaproveite as variáveis já existentes (`--primary-color`, `--primary-contrast`,
   `--ink`, `--muted`, `--font-body` ...). Não crie cores fixas para algo que o
   cliente pode personalizar no painel.
4. Depois de alterar o `site.css`, **aumente o número de versão** em
   `fragments/css.html` (`@{/css/site.css(v=N)}` → `v=N+1`) para os navegadores
   não usarem a versão antiga em cache.

## Únicas exceções permitidas (dinâmicas, geradas pelo servidor ou pelo usuário)

- `fragments/css.html`: os dois `<style th:utext>` com as cores do tema
  (`site.cssTema`) e as cores personalizadas (`personalizacaoCss`). Eles ficam
  depois do `site.css` de propósito, para as escolhas do painel sobrescreverem o padrão.
- `th:style` que só define valor vindo do banco (ex.: `--cor-produto`, `--cor`,
  cor de amostra do produto). Toda a regra visual continua no `site.css`; o
  atributo apenas entrega a variável.
- JavaScript que muda estado em tempo de execução (`el.style.display`,
  animação de opacidade, escala da prévia) e a prévia ao vivo de cores em
  Admin > Configurações, que injeta o CSS digitado dentro do iframe de prévia.

Se precisar de qualquer outra exceção, prefira sempre uma classe no `site.css`.

## Checklist antes de terminar uma tarefa

- [ ] Nenhum `<style>` novo em `templates/` (fora de `fragments/css.html`).
- [ ] Nenhum `style="` novo em `templates/` ou em scripts.
- [ ] Nenhum arquivo `.css` novo em `static/`.
- [ ] Toda página nova chama `~{fragments/css :: css}` no `<head>`.
- [ ] Versão `v=` do `site.css` aumentada em `fragments/css.html`, se o CSS mudou.
- [ ] `CssCentralizadoTests` passando.

Comandos de conferência rápidos:

```bash
rg -n '<style' src/main/resources/templates | grep -v fragments/css.html
rg -n '(^|[\s<`])style="' src/main/resources/templates src/main/resources/static --glob '!*.svg' --glob '!site.css'
find src/main/resources/static -name '*.css' ! -name site.css
```

Os três devem voltar vazios.

Há também um teste automático que quebra o build se a regra for violada:
`src/test/java/com/example/nazomocambique/estilo/CssCentralizadoTests.java`
(rode com `./mvnw test -Dtest=CssCentralizadoTests`). Não desative nem afrouxe esse teste.


## Padrão obrigatório de entrega e estrutura do projeto

- Todo arquivo ZIP de entrega deste projeto deve se chamar exatamente `Nazo-Mo-ambique.zip`.
- A pasta raiz dentro do ZIP deve se chamar exatamente `Nazo-Mo-ambique/` sempre que tecnicamente possível.
- Não use sufixos como `-final`, `-implementado`, números de versão ou datas no nome do ZIP.
- Preserve estas regras e este arquivo `AGENTS.md` em todas as próximas versões. Só altere o padrão mediante solicitação explícita do responsável pelo projeto.
- Antes de entregar, valide o nome do ZIP e confirme que os arquivos do projeto estão dentro de `Nazo-Mo-ambique/`.

---

# Plano mestre de desenvolvimento e controle de conclusão

Este plano é a fonte central de acompanhamento do desenvolvimento da Nazo-Mo-ambique.
Toda IA, agente de programação ou pessoa que alterar o projeto deve ler esta seção antes
de começar e atualizá-la ao concluir qualquer etapa.

## 1. Regras obrigatórias de execução (anti-retrabalho)

1. **Consultar antes de implementar:** ler este `AGENTS.md`, verificar o código, os testes,
   a documentação relacionada e o estado atual da tarefa. Não presumir que uma funcionalidade
   está ausente ou pronta apenas pelo nome de uma classe ou tela.
2. **Uma etapa por vez:** trabalhar na menor etapa coerente do plano. Não reimplementar
   algo que já esteja marcado como concluído e tenha evidências válidas.
3. **Atualizar este arquivo no mesmo trabalho:** ao terminar uma etapa, alterar seu status,
   registrar data, resumo objetivo, arquivos/módulos afetados e comandos/testes executados.
   Não deixar a atualização do plano para uma tarefa futura.
4. **Não marcar como concluído só porque o código foi escrito.** Conclusão exige:
   implementação revisada, testes pertinentes executados com sucesso, regressões verificadas
   e documentação/configuração atualizada quando aplicável.
5. **Se não for possível validar:** usar `🟡 IMPLEMENTADO — VALIDAÇÃO PENDENTE`, nunca
   marcar como concluído. Registrar exatamente o bloqueio (ex.: JDK ausente, serviço externo
   indisponível, credenciais de homologação ausentes).
6. **Se já estiver pronto:** não repetir a implementação. Conferir evidências; se válidas,
   manter concluído. Se estiver desatualizado ou quebrado, abrir uma tarefa de correção
   específica, sem apagar o histórico.
7. **Mudança de escopo:** novas solicitações devem ser adicionadas como itens novos, com
   justificativa e fase, antes de começar. Não apagar itens concluídos para acomodar escopo novo.
8. **Dependências e segurança:** nunca inserir tokens, senhas, dados pessoais reais ou segredos
   no código, nos testes, neste arquivo ou no ZIP. Usar variáveis de ambiente e dados fictícios.
9. **Persistência do progresso:** preservar este arquivo em todos os ZIPs e branches. Todo agente
   deve atualizar o mesmo checklist; não criar um segundo plano concorrente.
10. **Entrega:** manter o nome do ZIP `Nazo-Mo-ambique.zip` e a pasta raiz interna
    `Nazo-Mo-ambique/`.

### Legenda única de status

- `⬜ NÃO INICIADO` — ainda não implementado.
- `🟡 IMPLEMENTADO — VALIDAÇÃO PENDENTE` — código existe, mas falta teste, revisão ou validação externa.
- `🟢 CONCLUÍDO E VALIDADO` — critérios de aceite atendidos e evidências registradas.
- `🔴 BLOQUEADO` — depende de decisão, credencial, serviço ou trabalho externo; explicar o motivo.
- `♻️ REABERTO` — item anteriormente concluído que falhou em regressão ou foi invalidado por mudança.

**Regra de marcação:** só trocar para `🟢 CONCLUÍDO E VALIDADO` depois de executar e registrar
os testes. Ao reabrir uma etapa, preservar o motivo e atualizar a evidência.

## 2. Estado inicial conhecido do repositório

Estado registrado a partir da inspeção do ZIP usado para criar este plano, em 2026-11-21.
Não representa certificação de produção.

- Há aplicação Spring Boot com Java 25, Thymeleaf, Spring Security, JPA e perfis H2/PostgreSQL.
- Existem módulos de pedidos, produtos, estoque, usuários, frete/Correios, Asaas,
  cashback e carteira.
- Há testes automatizados em `src/test/java`, incluindo testes de segurança, pedidos,
  frete, Asaas e CSS.
- O documento `docs/FASE-1-ESTABILIDADE-SEGURANCA.md` informa que a proteção CSRF,
  perfil `prod` e bloqueio de cancelamento de pedido pago foram adicionados, mas os testes
  não foram executados naquele ambiente por limitação de JDK 25/acesso ao Maven.
- `IMPLEMENTACAO-CASHBACK-CARTEIRA.md` descreve carteira por lançamentos, configuração
  administrativa, uso no checkout e pedido de estorno integral. O próprio documento alerta
  que falhas de comunicação com Asaas exigem nova tentativa e confirmação externa.
- A regra desejada de CSS centralizado em `src/main/resources/static/css/site.css`
  ainda não está cumprida no estado inspecionado: existem outros arquivos CSS em
  `src/main/resources/static/` e `src/main/resources/static/css/`. Tratar como trabalho
  pendente de auditoria e migração, não como concluído.
- A configuração PostgreSQL ainda precisa de schema/migrações versionadas; o documento
  de Fase 1 alerta para não ativar produção antes de preparar e validar o schema.

## 3. Plano de desenvolvimento por fases

### Fase 0 — Auditoria, baseline e organização do trabalho
Status geral: `🟡 IMPLEMENTADO — VALIDAÇÃO PENDENTE`

- [x] Criar este plano mestre dentro de `AGENTS.md`.
- [x] Definir legenda de status, regra anti-retrabalho e exigência de evidências.
- [ ] Executar inventário funcional e técnico completo; comparar documentação com código.
- [ ] Registrar baseline de build e testes em ambiente com JDK 25.
- [ ] Identificar testes duplicados, obsoletos ou classes de teste fora do pacote principal.
- [ ] Criar registro de decisões técnicas relevantes (ADR) quando houver decisões arquiteturais.

Critério de aceite: inventário versionado, build inicial reproduzível e lista de riscos priorizada.
Evidência inicial: inspeção do conteúdo do ZIP em 2026-11-21; build/testes ainda não executados.

### Fase 1 — Fundação técnica, segurança e qualidade
Status geral: `🟡 IMPLEMENTADO — VALIDAÇÃO PENDENTE`

- [x] Adicionar proteção CSRF por padrão e exceção restrita para webhook assinado do Asaas
      (conforme `docs/FASE-1-ESTABILIDADE-SEGURANCA.md`).
- [x] Alterar logout para POST protegido por CSRF.
- [x] Criar perfil PostgreSQL de produção com credenciais por ambiente e validação do schema.
- [ ] Executar testes de segurança e regressão em JDK 25; corrigir falhas encontradas.
- [ ] Revisar autorização por papel em todas as rotas administrativas e de cliente.
- [ ] Revisar validação de entrada, saída HTML, upload de arquivos e limites de tamanho/tipo.
- [ ] Garantir configuração segura de cookies, sessões, headers, HTTPS e gestão de segredos.
- [ ] Revisar logs para não expor tokens, senhas, dados pessoais ou payloads sensíveis.
- [ ] Adicionar testes de integração para autenticação, autorização, CSRF e webhook inválido.

Critério de aceite: suíte pertinente verde, rotas sensíveis cobertas e checklist de segurança revisado.
Não marcar a fase como concluída antes da execução dos testes.

### Fase 2 — Banco de dados, migrações e persistência de produção
Status geral: `⬜ NÃO INICIADO`

- [ ] Adotar Flyway ou Liquibase (escolher uma ferramenta e documentar a decisão).
- [ ] Criar migração inicial reproduzível para PostgreSQL, incluindo constraints e índices.
- [ ] Converter/revisar dados iniciais atualmente específicos do H2 (`data.sql`) para estratégia
      compatível com desenvolvimento, testes e produção.
- [ ] Definir estratégia segura para dados de demonstração versus dados reais.
- [ ] Validar chaves estrangeiras, unicidade, precisão monetária e timestamps/timezone.
- [ ] Testar instalação do banco do zero e atualização a partir de uma versão anterior.
- [ ] Documentar backup, retenção, restauração e teste periódico de recuperação.

Critério de aceite: banco PostgreSQL novo sobe por migrações; atualização preserva dados;
restauração de backup é testada.

### Fase 3 — Pagamentos, carteira, cashback e estornos
Status geral: `🟡 IMPLEMENTADO — VALIDAÇÃO PENDENTE`

- [x] Implementar configuração administrativa de cashback (ativação, percentual, mínimo e limite),
      conforme `IMPLEMENTACAO-CASHBACK-CARTEIRA.md`.
- [x] Implementar carteira persistida por lançamentos e tela de saldo/histórico.
- [x] Implementar aplicação de saldo no checkout com validação no servidor.
- [x] Implementar crédito após confirmação de pagamento e reversões no cancelamento,
      conforme documentação existente.
- [x] Pix estático: gerar código "copia e cola" (BR Code) e QR Code com o valor do SUBTOTAL do pedido,
      na tela do pedido, quando o Asaas está desligado (item novo, solicitado em 2026-11-21;
      `🟡 IMPLEMENTADO — VALIDAÇÃO PENDENTE`: falta rodar a suíte Maven em JDK 25). Ver `docs/PIX-ESTATICO-QRCODE.md`.
- [ ] Pix estático: decidir com o responsável se o valor deve continuar sendo o subtotal (sem frete) ou
      passar a ser o total; hoje a confirmação do pagamento é manual (o Pix estático não tem webhook).
- [ ] Revisar atomicidade e concorrência: impedir que duas compras gastem o mesmo saldo.
- [ ] Garantir idempotência em todos os eventos de pagamento, crédito, débito e estorno.
- [ ] Criar estados explícitos para estorno solicitado, em processamento, confirmado e falhou.
- [ ] Implementar fila/reprocessamento automático de estornos pendentes, com backoff e limite.
- [ ] Criar rotina de conciliação entre pedidos, pagamentos, reembolsos e lançamentos da carteira.
- [ ] Cobrir pagamentos aprovados tardiamente, eventos repetidos/fora de ordem, reembolso parcial,
      cancelamento simultâneo e indisponibilidade do Asaas.
- [ ] Confirmar regras de negócio para cashback em compra paga parcialmente com carteira,
      frete, descontos, cancelamento parcial e chargeback.
- [ ] Executar testes de integração em homologação do Asaas antes de produção.

Critério de aceite: nenhum saldo duplicado/negativo; cada evento é processado uma única vez;
estorno só aparece como concluído após confirmação do gateway; pendências são reprocessáveis
e auditáveis. Não afirmar que dinheiro foi devolvido apenas porque a solicitação foi enviada.

### Fase 4 — Pedidos, estoque e operação logística
Status geral: `⬜ NÃO INICIADO`

- [ ] Formalizar máquina de estados de pedido e transições permitidas.
- [ ] Garantir reserva/baixa/liberação de estoque de forma transacional e concorrente.
- [ ] Registrar histórico imutável de mudanças de status e responsável.
- [ ] Revisar separação, embalagem, despacho, código de rastreio e confirmação de entrega.
- [ ] Tratar falhas e indisponibilidade dos Correios sem perder o pedido.
- [ ] Implementar cancelamento/troca/devolução conforme política comercial.
- [ ] Criar testes para estoque concorrente, pedidos simultâneos e transições inválidas.

Critério de aceite: estoque consistente, pedido rastreável ponta a ponta e transições protegidas.

### Fase 5 — Área do cliente e experiência de compra
Status geral: `⬜ NÃO INICIADO`

- [ ] Consolidar painel do cliente com pedidos, status, endereços e dados de conta.
- [ ] Implementar favoritos/lista de desejos.
- [ ] Melhorar busca, filtros, ordenação e paginação do catálogo.
- [ ] Revisar página de produto: galeria, variações/tamanhos, disponibilidade e guia de medidas.
- [ ] Simplificar checkout e apresentar resumo transparente de frete, descontos e cashback.
- [ ] Melhorar acessibilidade, responsividade, estados vazios, erros e carregamento.
- [ ] Validar jornada em dispositivos móveis e navegadores modernos.

Critério de aceite: jornada completa de navegação à compra testada em desktop e mobile,
com mensagens claras e acessibilidade básica verificada.

### Fase 6 — Administração, relatórios e gestão
Status geral: `⬜ NÃO INICIADO`

- [ ] Criar dashboard com vendas, pedidos, ticket médio e clientes.
- [ ] Criar relatórios por período, produto, categoria e status.
- [ ] Exibir indicadores de cashback emitido, utilizado, expirado (se houver validade) e estornado.
- [ ] Melhorar gestão de produtos, variações, imagens e estoque por tamanho.
- [ ] Adicionar alertas de estoque baixo e trilha de auditoria administrativa.
- [ ] Revisar permissões de administrador e separador pelo princípio do menor privilégio.
- [ ] Permitir exportações controladas (CSV) sem expor dados além da permissão do usuário.

Critério de aceite: indicadores reconciliáveis com pedidos e pagamentos; operações administrativas
auditadas e protegidas por permissão.

### Fase 7 — Marketing, fidelização e retenção
Status geral: `⬜ NÃO INICIADO`

- [ ] Implementar cupons com validade, limite de uso, elegibilidade e combinação de descontos.
- [ ] Implementar campanhas promocionais agendadas.
- [ ] Integrar regras de cupom, cashback e desconto sem acumulação indevida.
- [ ] Implementar avaliações de produtos com moderação.
- [ ] Implementar recuperação de carrinho abandonado respeitando consentimento e privacidade.
- [ ] Implementar indicação de amigos/programa de fidelidade somente após definir regras antifraude.

Critério de aceite: regras comerciais previsíveis, testadas e configuráveis sem alterar código.

### Fase 8 — Comunicação e automações
Status geral: `⬜ NÃO INICIADO`

- [ ] Padronizar notificações transacionais por e-mail (pedido, pagamento, envio, entrega, estorno).
- [ ] Criar templates e preferências de comunicação do cliente.
- [ ] Implementar notificações assíncronas com retry e prevenção de envio duplicado.
- [ ] Avaliar integração WhatsApp somente após definir provedor, custos, consentimento e templates.
- [ ] Registrar resultado das entregas e permitir reenvio administrativo controlado.

Critério de aceite: mensagens consistentes com o estado real do pedido e falhas recuperáveis.

### Fase 9 — Identidade visual, CSS e acessibilidade
Status geral: `🟡 IMPLEMENTADO — VALIDAÇÃO PENDENTE`

- [ ] Inventariar todos os CSS existentes e suas dependências.
- [ ] Migrar estilos válidos para `static/css/site.css`, removendo duplicações com validação visual.
- [ ] Atualizar todas as páginas para carregar o fragmento `fragments/css :: css`.
- [ ] Remover CSS inline/estilos locais não permitidos, respeitando as exceções documentadas.
- [ ] Atualizar versão de cache do CSS centralizado.
- [ ] Executar `CssCentralizadoTests` e inspeção visual das páginas principais.
- [ ] Melhorar contraste, foco visível, navegação por teclado e textos alternativos.

Critério de aceite: um único CSS de aplicação conforme a regra do projeto, teste automático verde
e nenhuma regressão visual relevante.

### Fase 10 — Observabilidade, desempenho e produção
Status geral: `⬜ NÃO INICIADO`

- [ ] Configurar health checks e métricas sem expor dados sensíveis.
- [ ] Centralizar logs estruturados e correlação por pedido/operação.
- [ ] Configurar alertas para falha de webhook, pagamento, estorno, e-mail e banco.
- [ ] Revisar índices e consultas lentas; medir desempenho antes de otimizar.
- [ ] Definir cache apenas para dados apropriados, sem cachear carteira/checkout de forma insegura.
- [ ] Configurar HTTPS, domínio, CORS se necessário, política de backup e recuperação.
- [ ] Criar pipeline CI para build, testes e verificações estáticas.
- [ ] Executar checklist de homologação e plano de rollback.

Critério de aceite: deploy reproduzível, monitorado, com backup restaurável e procedimento
documentado para incidentes e rollback.

### Fase 11 — Expansão futura e recursos inteligentes
Status geral: `⬜ NÃO INICIADO`

- [ ] Busca aprimorada e sugestões de produtos.
- [ ] Recomendações baseadas em regras e dados consentidos.
- [ ] Combinações/look completo e produtos relacionados.
- [ ] Alerta de reposição de tamanho/produto.
- [ ] Avaliar arquitetura multi-loja/SaaS somente após validar demanda, isolamento de dados,
      faturamento, suporte e custo operacional.

Critério de aceite: cada recurso deve ter hipótese de negócio, métrica de sucesso, avaliação
de privacidade e custo de manutenção antes de ser aprovado.

## 4. Registro obrigatório de conclusão por etapa

Ao concluir qualquer item, adicionar uma entrada no registro abaixo e atualizar a caixa
correspondente na fase. Use data ISO `AAAA-MM-DD`.

Formato:

`AAAA-MM-DD — Fase X / item: resumo do que foi entregue. Evidência: testes/comandos executados
e resultado. Arquivos principais: caminhos. Responsável/agente: identificador, se disponível.`

### Histórico de execução

- 2026-11-21 — Fase 0 / plano mestre: plano e protocolo anti-retrabalho adicionados ao
  `AGENTS.md`. Evidência: revisão estrutural do ZIP e inspeção do código/documentação;
  build e testes não executados. Arquivo principal: `AGENTS.md`.
- 2026-11-21 — Fase 3 / Pix estático com QR Code (subtotal): gerador de BR Code (`PixBrCode`), gerador de
  QR Code em Java puro sem dependências (`QrCode`, nível M, SVG), `PixEstaticoService`, propriedades `pix.*`
  (`PixProperties`/`PixConfig`), seção na tela do pedido e botão copiar. Evidência: a suíte Maven NÃO foi
  executada (sem rede/Maven/JDK 25 no ambiente de trabalho; JDK 21). Foram executados fora do Maven, com os
  fontes reais: 18 testes novos (`PixBrCodeTests`, `QrCodeTests`, `PixEstaticoServiceTests`) via runner mínimo —
  todos passaram — e decodificação com OpenCV de 44 QR Codes (versões 1 a 40, UTF-8) e do SVG do pedido —
  todos idênticos ao texto de origem. Status: 🟡 IMPLEMENTADO — VALIDAÇÃO PENDENTE.
  Arquivos principais: `service/QrCode.java`, `service/PixBrCode.java`, `service/PixEstaticoService.java`,
  `config/PixProperties.java`, `config/PixConfig.java`, `controller/CheckoutController.java`,
  `templates/pedido.html`, `application*.properties`, `docs/PIX-ESTATICO-QRCODE.md`. Sem alteração de CSS
  (reaproveita `.pix-*` do `site.css`; versão `v=` mantida).
- 2026-11-21 — Fases 1, 3 e 9 / baseline: código e documentação existentes identificados,
  mas validação completa pendente. Não promover os status dessas fases para concluído sem
  executar os testes e critérios descritos acima.

## 5. Ritual obrigatório para todo agente em cada tarefa

### Antes de editar
- [ ] Ler este `AGENTS.md` inteiro.
- [ ] Identificar a fase e o item correspondente.
- [ ] Conferir se já está concluído; se estiver, não reimplementar.
- [ ] Inspecionar o código e testes atuais para localizar o ponto correto da alteração.
- [ ] Definir critérios de aceite e testes antes de codificar.

### Depois de editar
- [ ] Executar testes específicos e, quando possível, a suíte completa.
- [ ] Revisar diff para detectar alterações não relacionadas.
- [ ] Atualizar este plano: status, histórico, evidências e arquivos afetados.
- [ ] Atualizar documentação técnica relacionada.
- [ ] Confirmar que `AGENTS.md` foi preservado.
- [ ] Empacotar como `Nazo-Mo-ambique.zip`, com raiz `Nazo-Mo-ambique/`, se houver entrega ZIP.

### Regra de encerramento da tarefa
A resposta final do agente deve informar: o que foi feito, quais itens do plano mudaram de
status, quais testes foram executados e quais limitações permanecem. Se não houve validação,
deve dizer explicitamente que a etapa está implementada, mas ainda não concluída.


### Ajuste visual institucional — contraste e iconografia (2026-11-22)
Status: `🟡 IMPLEMENTADO — VALIDAÇÃO PENDENTE`

- [x] Aplicar cores de texto de alto contraste (#171717 e #454545) nas quatro páginas institucionais.
- [x] Adicionar assinatura iconográfica temática para frete, trocas, privacidade e sobre.
- [x] Reforçar bordas, cartões, links, foco visual e estados de leitura mantendo a identidade preto e branco.
- [x] Preservar animações suaves e respeitar `prefers-reduced-motion`.
- [x] Incrementar a versão de cache do CSS centralizado.
- [ ] Executar `./mvnw test -Dtest=CssCentralizadoTests` e validar visualmente em desktop/mobile.

Arquivos alterados: `templates/institucional/{frete-e-entrega,trocas-e-devolucoes,politica-de-privacidade,sobre}.html`, `static/css/site.css`, `templates/fragments/css.html`.
Validação: revisão estática das alterações; testes automatizados não executados nesta etapa.


## Revelação progressiva durante a rolagem (2026-11-22)
- Adicionado `static/reveal-scroll.js`, carregado globalmente pelo fragmento `templates/fragments/css.html`.
- Blocos de conteúdo, cards de produto e seções institucionais surgem com fade, leve deslocamento vertical e desfoque sutil ao entrarem na área visível.
- A animação ocorre uma única vez por elemento, com pequeno escalonamento nos cards em grade.
- Respeita `prefers-reduced-motion` e mantém o conteúdo visível caso `IntersectionObserver` não esteja disponível.
- Cache do CSS/JS versionado no fragmento global.
