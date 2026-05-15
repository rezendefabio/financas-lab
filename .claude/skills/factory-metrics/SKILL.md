---
name: factory-metrics
description: Routine Tier 1 que coleta metricas da fabrica de software: tempo spec->PR, PRs/dia, taxa de correcao autonoma, taxa de bloqueador humano. Armazena em .claude/factory-metrics.json e exibe relatorio semanal. Invocar uma vez -- o loop se auto-agenda via ScheduleWakeup.
disable-model-invocation: true
---

Voce e o factory-metrics do projeto financas-lab. Verifique se e hora de coletar
metricas da fabrica e agende a proxima verificacao.

No inicio de cada iteracao, capturar contexto usando o Bash tool com o comando
`pwd` para obter $repoRoot. Definir tambem:
`$stateFile = "$repoRoot/.claude/factory-metrics.json"` e `$agora = Get-Date`

## Passo 0 -- Capturar contexto

```powershell
$repoRoot = (pwd).Path
$stateFile = "$repoRoot/.claude/factory-metrics.json"
$agora = Get-Date
```

## Passo 1 -- Verificar se e hora de coletar

```powershell
$state = if (Test-Path $stateFile) {
    Get-Content $stateFile | ConvertFrom-Json
} else {
    [PSCustomObject]@{ ultimaExecucao = [datetime]::MinValue.ToString('o'); prs = @() }
}

$ultimaExecucao = [datetime]$state.ultimaExecucao
$horasDesdeUltima = ($agora - $ultimaExecucao).TotalHours
```

Se `$horasDesdeUltima -lt 20`: reportar "factory-metrics: proximo em
$([math]::Round(20 - $horasDesdeUltima, 1))h" e ir direto para Passo 5
(agendar proxima iteracao). Nao coletar metricas.

## Iteracao (apenas se >= 20h desde ultima execucao)

### Passo 2 -- Coletar PRs mergeados nos ultimos 7 dias ainda nao no state

```powershell
$dataLimite = $agora.AddDays(-7).ToString('yyyy-MM-ddTHH:mm:ssZ')

$prsMergeados = gh pr list --state merged --limit 50 `
    --json number,title,headRefName,createdAt,mergedAt `
    | ConvertFrom-Json `
    | Where-Object {
        [datetime]$_.mergedAt -gt [datetime]$dataLimite -and
        ($_.headRefName -match '^(feat|fix|docs)/')
    }

# Filtrar apenas PRs ainda nao no state
$prsRegistrados = @($state.prs | ForEach-Object { $_.pr_number })
$prsNovos = @($prsMergeados | Where-Object { $_.number -notin $prsRegistrados })
```

### Passo 3 -- Calcular metricas para cada PR novo

```powershell
foreach ($pr in $prsNovos) {
    # Obter commits do PR
    $commitsData = gh pr view $pr.number --json commits | ConvertFrom-Json
    $todosCommits = @($commitsData.commits)
    $primeiroCommit = $todosCommits | Sort-Object authoredDate | Select-Object -First 1

    # tempo_spec_pr_min: diferenca entre abertura do PR e primeiro commit (proxy de "work to PR").
    # Negativo = clock skew ou PR aberto antes do primeiro commit -> registrado como null.
    $tempoMin = if ($primeiroCommit) {
        $diff = ([datetime]$pr.createdAt - [datetime]$primeiroCommit.authoredDate).TotalMinutes
        if ($diff -lt 0) { $null } else { [math]::Round($diff, 0) }
    } else { $null }

    $commitsFix = @($todosCommits | Where-Object {
        $_.messageHeadline -match '^fix\('
    }).Count

    $prBody = gh pr view $pr.number --json body | ConvertFrom-Json
    $teveBloqueador = [bool]($prBody.body -match 'BLOQUEADOR')

    # teve_correcao_autonoma: heuristica -- commits fix( presentes E mais de 1 commit total.
    # Detecta "iteracao com correcao" mas nao distingue correcao por feedback humano vs autonoma.
    $entrada = [PSCustomObject]@{
        pr_number              = $pr.number
        titulo                 = $pr.title
        branch                 = $pr.headRefName
        aberto_em              = $pr.createdAt
        mergeado_em            = $pr.mergedAt
        tempo_spec_pr_min      = $tempoMin
        commits_total          = $todosCommits.Count
        commits_fix            = $commitsFix
        teve_correcao_autonoma = ($commitsFix -gt 0 -and $todosCommits.Count -gt 1)
        teve_bloqueador        = $teveBloqueador
    }
    $state.prs += $entrada
}
```

### Passo 4 -- Exibir relatorio

Calcular metricas a partir de `$state.prs` filtrado para os ultimos 7 dias:

```powershell
$prs7d = @($state.prs | Where-Object {
    [datetime]$_.mergeado_em -gt $agora.AddDays(-7)
})

$tempos = @($prs7d | Where-Object { $_.tempo_spec_pr_min -ne $null } |
    ForEach-Object { $_.tempo_spec_pr_min })

$mediaTempo = if ($tempos.Count -gt 0) {
    [math]::Round(($tempos | Measure-Object -Average).Average, 0)
} else { 'N/A' }

$medianaTempo = if ($tempos.Count -gt 0) {
    $sorted = $tempos | Sort-Object
    $mid = [math]::Floor($sorted.Count / 2)
    if ($sorted.Count % 2 -eq 0) {
        [math]::Round(($sorted[$mid-1] + $sorted[$mid]) / 2, 0)
    } else { $sorted[$mid] }
} else { 'N/A' }

$correcaoCount = @($prs7d | Where-Object { $_.teve_correcao_autonoma }).Count
$bloqueadorCount = @($prs7d | Where-Object { $_.teve_bloqueador }).Count
$totalPrs7d = $prs7d.Count
$prsDia = if ($totalPrs7d -gt 0) { [math]::Round($totalPrs7d / 7, 1) } else { 0 }

$correcaoPct = if ($totalPrs7d -gt 0) {
    [math]::Round($correcaoCount * 100 / $totalPrs7d, 0)
} else { 0 }

$bloqueadorPct = if ($totalPrs7d -gt 0) {
    [math]::Round($bloqueadorCount * 100 / $totalPrs7d, 0)
} else { 0 }

$top3 = @($prs7d | Where-Object { $_.tempo_spec_pr_min -ne $null } |
    Sort-Object tempo_spec_pr_min | Select-Object -First 3)

$totalHistorico = @($state.prs).Count
```

Exibir relatorio no formato:

```
==================================================
[factory-metrics] {DATA} {HH:MM}
==================================================

METRICAS DOS ULTIMOS 7 DIAS
PRs de fabrica analisados: N

Tempo medio spec->PR:      XX min
Tempo mediano spec->PR:    XX min
PRs com correcao autonoma: N/N (XX%)
PRs com bloqueador humano: N/N (XX%)
PRs/dia (media 7d):        X.X

TOP 3 mais rapidos:
  #N titulo: XX min
  ...

ACUMULADO HISTORICO (desde inicio):
Total PRs registrados: N
==================================================
```

### Passo 5 -- Salvar state e agendar proxima iteracao

```powershell
$state.ultimaExecucao = $agora.ToString('o')
$state | ConvertTo-Json -Depth 5 | Set-Content $stateFile
```

Em seguida usar ScheduleWakeup:
- delaySeconds: 3600
- reason: "verificacao periodica do factory-metrics (coleta 1x/dia)"
- prompt: `<<autonomous-loop-dynamic>>`
