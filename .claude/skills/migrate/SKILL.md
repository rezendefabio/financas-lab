---
name: migrate
description: Fluxo completo pos-/feature: gera migration SQL e unit tests para domain POJO do bounded context. Encadeia /write-migration (subagent migration-writer) e /write-test (subagent test-writer). Para se migration falhar. Argumento: nome do bounded context (snake_case).
disable-model-invocation: true
argument-hint: [nome-do-bounded-context]
---

Execute em sequencia para o bounded context `$ARGUMENTS`. Pare e reporte ao operador
se qualquer passo falhar. Nao prossiga para o proximo passo em caso de falha.

## Passo 0 -- Validar argumento

Se `$ARGUMENTS` vazio ou nao casa `^[a-z][a-z0-9_]*$`:
- Reporte: "ERRO: argumento invalido. Informe nome do bounded context em snake_case (ex: conta)."
- Termine.

## Passo 1 -- Verificar que Entity existe

Derive o PascalCase: split por `_`, capitalize primeira letra de cada parte, concatene.
Exemplo: `meu_contexto` -> `MeuContexto`.

Verifique via Glob ou Read que o arquivo existe:
```
src/main/java/com/laboratorio/financas/$ARGUMENTS/infrastructure/persistence/<PascalCase>Entity.java
```

Se nao existir:
- Reporte: "ERRO: Entity nao encontrada. Execute /feature $ARGUMENTS antes de /migrate."
- Termine.

## Passo 2 -- Gerar migration SQL

Invoque a skill `/write-migration $ARGUMENTS`.

Aguarde o relatorio do subagent migration-writer. Se o relatorio indicar erro (Entity nao
encontrada, conflito de versao, ou qualquer falha no Write):
- Reporte: "ERRO no passo 1 (/write-migration): <mensagem de erro do subagent>"
- Termine. Nao invoque /write-test.

## Passo 3 -- Gerar unit tests do domain POJO

Derive o path do domain POJO:
```
src/main/java/com/laboratorio/financas/$ARGUMENTS/domain/<PascalCase>.java
```

Verifique se o arquivo existe. Se nao existir:
- Reporte: "AVISO: domain POJO nao encontrado em <path>. Pulando geracao de tests."
- Prossiga para o Passo 4 (relatorio final) sem invocar /write-test.

Se existir, invoque a skill `/write-test <path-derivado>`.

Aguarde o relatorio do subagent test-writer. Se o relatorio indicar falha de compilacao
ou testes falhando:
- Registre o status como FALHOU no relatorio final, mas NAO reverta a migration.

## Passo 4 -- Relatorio final

Produza:

```
/migrate concluido para: $ARGUMENTS

Passo 1 -- Migration SQL:   <OK | FALHOU: <motivo>>
Passo 2 -- Unit tests:      <OK | FALHOU: <motivo> | PULADO: <motivo>>

Arquivos gerados:
  <path da migration, se gerada>
  <path do arquivo de test, se gerado>

Pendencias manuais:
  - Adicionar FK constraints e indexes na migration conforme necessidade
  - Revisar e complementar os testes gerados

Proximos passos sugeridos:
  git add <arquivos gerados>
  git commit -m "feat(<contexto>): adiciona migration e tests para <nome>"
  /ship   -- apos commit
```
