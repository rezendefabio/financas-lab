---
name: write-migration
description: Gera migration Flyway SQL para bounded context via subagent migration-writer. Le *Entity.java, deriva colunas de anotacoes JPA, descobre proximo numero Flyway, escreve V<N>__cria_tabela_<name>.sql. Argumento: nome do bounded context em snake_case.
disable-model-invocation: true
context: fork
agent: migration-writer
argument-hint: [nome-do-bounded-context]
allowed-tools: Read Glob Grep Write
---

Gere a migration Flyway SQL para o bounded context `$ARGUMENTS` seguindo todas as instrucoes
do seu system prompt.

Leia `<PascalCase>Entity.java`, derive as colunas das anotacoes JPA, descubra o proximo
numero Flyway via Glob em `src/main/resources/db/migration/V*.sql`, e escreva o arquivo
`V<N>__cria_tabela_<name>.sql`. Produza o relatorio no template prescrito.

Se Entity nao existir ou argumento invalido: reporte o erro literal e termine.
