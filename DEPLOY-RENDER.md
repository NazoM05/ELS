# Deploy no Render (plano gratuito)

O Render não tem ambiente Java pronto, então o projeto sobe por Docker (`Dockerfile` na raiz). O arquivo `render.yaml` descreve o serviço.

## Antes de subir
1. Crie o repositório no GitHub como **privado**: o `data.sql` contém o usuário admin (e-mail, CPF e hash da senha).
2. Envie com `git push` (não arraste arquivos pelo site). O `.gitignore` já deixa de fora `target/`, `dados/`, `uploads/` e `secrets-local.properties`.
3. Gere uma **API Key nova** no painel Sandbox do Asaas (a antiga já passou por arquivo de projeto e conversa) e um token de webhook novo, de 32+ caracteres.

## Criar o serviço
1. Render > New > **Blueprint** > escolha o repositório. O Render lê o `render.yaml` e pede `KEY_ASAAS` e `ASAAS_WEBHOOK_TOKEN`.
   (Alternativa manual: New > Web Service > Docker > Free, e cadastre as variáveis do `render.yaml`.)
2. Aguarde o build (alguns minutos). A URL será `https://nazo-mo-ambique.onrender.com` (ou parecida).
3. No painel Sandbox do Asaas, cadastre o webhook `https://SUA-URL.onrender.com/webhooks/asaas` com o **mesmo token**, eventos `PAYMENT_CONFIRMED`, `PAYMENT_RECEIVED` e `PAYMENT_OVERDUE`.

## Como funciona neste perfil (`render`)
- Banco **H2 em memória**: a cada início o `data.sql` recria os produtos, o admin e o cliente de teste. **Pedidos, cadastros novos e uploads do admin somem** quando o serviço reinicia ou hiberna (15 min sem acesso).
- O `data.sql` usa `MERGE ... KEY`, que só existe no H2. Para usar PostgreSQL seria preciso converter esse arquivo (perfil `prod`).
- Imagens dos produtos devem ficar em `src/main/resources/static/img` (vão no repositório).
- Console H2 desligado.

## Antes de apresentar
- Abra o site uma vez para acordar o serviço (leva cerca de 1 minuto).
- Faça um pedido de teste, gere o Pix e confirme o pagamento no painel Sandbox.
