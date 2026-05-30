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

Leia APENAS: a classe alvo, classes do mesmo bounded context referenciadas por
imports, e `docs/crud-patterns.md` secao 6 (Testes Java) como referencia de
estilo. **NAO ler testes de outras features** (ContaTest, ContaRepositoryImplTest,
LimiteControllerTest, etc) -- crud-patterns secao 6 ja contem o gabarito
canonico. Gere o arquivo de teste no pacote espelho. Valide via `./mvnw test
-Dtest=<NomeDoTest>`. Reporte resultado no template prescrito.

Se output nao compilar ou testes falharem: reporte o erro literalmente, sem tentar auto-corrigir.
