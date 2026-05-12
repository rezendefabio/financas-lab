---
name: write-test
description: Gera unit tests para classe de dominio (POJO em */domain/) via subagent test-writer (Sonnet). Recebe path da classe alvo como argumento. Output: arquivo de teste + relatorio.
disable-model-invocation: true
context: fork
agent: test-writer
argument-hint: [path-da-classe-alvo]
allowed-tools: Bash(./mvnw *) Bash(cat *) Bash(ls *)
---

Gere unit tests para a classe em `$ARGUMENTS` seguindo todas as instrucoes do seu system prompt.

Leia a classe alvo, classes referenciadas, e `ContaTest.java` como referencia de estilo. Gere o arquivo de teste no pacote espelho. Valide via `./mvnw test -Dtest=<NomeDoTest>`. Reporte resultado no template prescrito.

Se output nao compilar ou testes falharem: reporte o erro literalmente, sem tentar auto-corrigir.
