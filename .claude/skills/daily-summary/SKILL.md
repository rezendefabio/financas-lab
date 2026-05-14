---
name: daily-summary
description: Routine Tier 1 que gera um resumo diario do projeto: PRs mergeados, PRs abertos, CI do main, commits recentes. Invocar uma vez -- o loop se auto-agenda via ScheduleWakeup.
disable-model-invocation: true
---

Voce e o daily-summary do projeto financas-lab. Verifique se e hora de gerar
o resumo do dia e agende a proxima verificacao.

No inicio de cada iteracao, capturar contexto usando o Bash tool com o comando
`pwd` para obter $repoRoot. Definir tambem:
`$stateFile = "$repoRoot/.claude/daily-summary.state"` e `$agora = Get-Date`

## Verificar se e hora de gerar

```powershell
$ultimaExecucao = if (Test-Path $stateFile) {
    [datetime](Get-Content $stateFile | ConvertFrom-Json).ultimaExecucao
} else {
    [datetime]::MinValue
}

$horasDesdeUltima = ($agora - $ultimaExecucao).TotalHours
```

Se `$horasDesdeUltima -lt 20`: reportar "daily-summary: proximo resumo em
$([math]::Round(20 - $horasDesdeUltima, 1))h" e ir direto para Passo 4
(agendar proxima iteracao). Nao gerar resumo.

## Iteracao (apenas se >= 20h desde ultima execucao)

### Passo 1 -- Coletar dados

```powershell
# PRs mergeados hoje
$mergedToday = gh pr list --state merged --limit 20 `
    --json number,title,mergedAt,headRefName | ConvertFrom-Json `
    | Where-Object { [datetime]$_.mergedAt -gt $agora.Date }

# PRs ainda abertos
$openPRs = gh pr list --state open `
    --json number,title,headRefName,mergeable | ConvertFrom-Json

# CI do main (ultimo run)
$mainCI = gh run list --branch main --limit 1 `
    --json conclusion,status,displayTitle | ConvertFrom-Json | Select-Object -First 1

# Commits em main hoje
$commitsHoje = git log origin/main --since="$($agora.Date.ToString('yyyy-MM-dd'))" `
    --oneline
```

### Passo 2 -- Gerar resumo

Exibir:

```
==================================================
[daily-summary] {DATA} {HH:MM}
==================================================

MAIN CI: {conclusion/status do mainCI}

PRs MERGEADOS HOJE ({count}):
{para cada PR: #N titulo}

PRs ABERTOS ({count}):
{para cada PR: #N titulo [CONFLICTING se mergeable==CONFLICTING]}

COMMITS EM MAIN HOJE:
{lista de commits ou "nenhum commit hoje"}
==================================================
```

### Passo 3 -- Salvar state

```powershell
@{ ultimaExecucao = $agora.ToString('o') } | ConvertTo-Json | Set-Content $stateFile
```

### Passo 4 -- Agendar proxima iteracao

Usar ScheduleWakeup:
- delaySeconds: 3600
- reason: "verificacao periodica do daily-summary (gera 1x/dia)"
- prompt: `<<autonomous-loop-dynamic>>`
