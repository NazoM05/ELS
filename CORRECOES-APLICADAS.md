# Correções aplicadas ao pacote Nazo Moçambique

## Ajustes realizados
- Corrigida a permissão de execução do `mvnw` (Maven Wrapper), para permitir executar `./mvnw` em ambientes Linux/macOS.
- Atualizado o `.gitignore` para excluir artefatos de build, logs, arquivos temporários, metadados de IDE e arquivos `.env` locais, mantendo `.env.example`.
- Preparado o ZIP de entrega sem a pasta `.git` e sem artefatos `target/`, reduzindo o pacote e evitando incluir histórico Git ou classes compiladas/testes antigos.

## Validação
- Java 21 está instalado no ambiente de revisão.
- A execução de `./mvnw -q test` foi tentada, mas o wrapper não conseguiu baixar o Maven 3.9.9 de `repo.maven.apache.org` neste ambiente.
- Portanto, os testes automatizados não foram confirmados nesta revisão.

## Atenção
- Nenhuma configuração de produção, credencial ou arquivo `.env` foi criado.
- Arquivos de código-fonte e recursos da aplicação foram preservados; estas correções são de empacotamento e ambiente de desenvolvimento.


## Ajuste do banner rotativo (20/11/2026)

- O banner rotativo da vitrine principal (`berzerk.html`) agora é exibido somente quando a página atual é a primeira (página 1).
- Ao navegar para páginas 2 ou posteriores, o banner é omitido sem reservar espaço na página.
- A paginação e os filtros permanecem inalterados.


## Ajuste de renderização das URLs da logo (20/11/2026)

- Substituídas expressões Thymeleaf com concatenação/nesting de `${...}` dentro de `@{...}` por parâmetros de caminho nomeados (`@{/logo-site/{arquivo}(arquivo=${site.logoArquivo})}`).
- Aplicado nas telas de login, cadastro, configurações e fragmento de logo compartilhado.
- Essa correção busca evitar falha de processamento das expressões Thymeleaf ao renderizar URLs de logos personalizadas.
- Validação: inspeção estática dos templates. Build Maven/testes não executados, pois o Maven Wrapper não conseguiu baixar Maven 3.9.9 neste ambiente.
