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
pwsh -NoProfile -ExecutionPolicy Bypass -File "$SCRIPT_DIR/pre-commit.ps1" "$@"
```

Arquivo `.ps1` companherio (ex: `pre-commit.ps1`):

```powershell
$ErrorActionPreference = "Stop"
# Invoca scripts em .claude/hooks/<escopo>/ conforme aplicavel.
# Exemplo: . "$PSScriptRoot/../.claude/hooks/universal/check-utf8.ps1"
```

## Estado atual

Nenhum entrypoint funcional ainda. Sub-etapa 4.0 (esta) estabelece apenas a infraestrutura. Entrypoints reais nascem em 4.1+.

## Nao tocar manualmente

`setup.ps1` configura `core.hooksPath` automaticamente. Em caso de clone novo ou reset, rodar `.\scripts\setup.ps1`.
