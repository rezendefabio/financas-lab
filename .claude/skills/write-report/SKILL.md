---
name: write-report
description: Gera componente React de relatorio impresso (PDF) via subagent report-writer. Recebe descricao multiline do relatorio como argumento.
disable-model-invocation: true
context: fork
agent: report-writer
argument-hint: "nome: X\ndominio: Y\ndados: Z\ncampos: A, B, C\ntitulo: T"
allowed-tools: Read Glob Grep Write Bash
---

Gere o componente de relatorio impresso seguindo todas as instrucoes do seu system prompt.

## Input recebido

$ARGUMENTS

## Instrucoes adicionais

- Ler `frontend/src/shared/lib/formatters.ts` antes de gerar (confirmar assinaturas)
- Nunca usar componentes shadcn dentro do Document
- Nunca usar CSS classes -- apenas StyleSheet.create()
- Exportar o componente publico como named export
- Verificar TypeScript antes de reportar
