---
name: write-job
description: Gera scaffold completo de job Spring Batch via subagent job-writer. Cria Reader, Processor, Writer, JobConfig, Listener, Launcher/Scheduler e migration Flyway para tabelas BATCH_*. Argumento: descricao multiline do job.
disable-model-invocation: true
context: fork
agent: job-writer
argument-hint: "nome: X\nbounded-context: Y\ndescricao: Z\ninput: CSV\noutput: Entidade\ndisparo: REST\nchunk-size: 100"
allowed-tools: Read Glob Grep Write Bash
---

Gere o scaffold de job Spring Batch seguindo todas as instrucoes do seu system prompt.

## Input recebido

$ARGUMENTS

## Instrucoes adicionais

- Verificar se spring-boot-starter-batch ja esta no pom.xml antes de adicionar
- Verificar se migration BATCH_* ja existe antes de gerar nova
- Usar chunk-oriented processing (nunca Tasklet)
- Pacote: infrastructure.batch (nao application nem interfaces)
- Verificar compilacao antes de reportar
