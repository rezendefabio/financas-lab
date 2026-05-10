# .githooks — Entrypoints de git hooks

Esta pasta e apontada por `git config core.hooksPath` (configurado por `scripts/setup.ps1`).

**O que vai aqui:** entrypoints sem extensao (`pre-commit`, `commit-msg`, `pre-push`) que o git invoca diretamente. Cada entrypoint e um wrapper bash minimo que chama o script `.ps1` correspondente.

**O que NAO vai aqui:** logica de validacao. Logica fica em `.claude/hooks/<escopo>/*.ps1` e e invocada pelos entrypoints.

## Padrao de wrapper (referencia para etapas 4.1+)

Arquivo sem extensao (ex: `pre-commit`):

```bash
#!/usr/bin/env bash
set -e
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
powershell -NoProfile -ExecutionPolicy Bypass -File "$SCRIPT_DIR/pre-commit.ps1" "$@"
```

Arquivo `.ps1` companherio (ex: `pre-commit.ps1`):

```powershell
$ErrorActionPreference = "Stop"
# Invoca scripts em .claude/hooks/<escopo>/ conforme aplicavel.
# Exemplo: . "$PSScriptRoot/../.claude/hooks/universal/check-utf8.ps1"
```

## Padrao de orquestrador (1:N) — para hooks `pre-commit`

Diferente do `commit-msg` (que valida uma única regra por commit), o entrypoint `pre-commit` e projetado para invocar **multiplos hooks em sequencia**. Razao: varias validacoes distintas (encoding, blank lines em Markdown, tamanho de docs, etc.) precisam rodar antes de cada commit.

O companheiro `.githooks/pre-commit.ps1` itera sobre um array `$hooks` invocando cada um e agrega os exit codes — se qualquer hook falhar, o commit e bloqueado, mas todos os hooks rodam (nao ha early-exit no primeiro fail). Isso garante que o operador veja todas as violacoes de uma vez, nao uma por commit-tentativa.

Para adicionar um novo hook ao `pre-commit`:

1. Criar `.claude/hooks/universal/<nome>.ps1` (ou `<escopo>/<nome>.ps1`).
2. Acrescentar uma linha ao array `$hooks` em `.githooks/pre-commit.ps1`:

```powershell
$hooks = @(
    (Join-Path $repoRoot ".claude\hooks\universal\encoding-utf8.ps1")
    (Join-Path $repoRoot ".claude\hooks\universal\<novo-hook>.ps1")
)
```

Cada hook e responsavel por ler seu proprio `git diff --cached` e reportar violacoes com mensagens claras. Nao ha contrato compartilhado alem de "exit 0 = ok, exit != 0 = bloqueia".

## Estado atual

Sub-etapa 4.1 estabeleceu o padrao de 3 camadas com `commit-msg`. Sub-etapa 4.2 estreia o `pre-commit` com o padrao orquestrador 1:N.

## Nao tocar manualmente

`setup.ps1` configura `core.hooksPath` automaticamente. Em caso de clone novo ou reset, rodar `.\scripts\setup.ps1`.
