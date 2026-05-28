# scripts/show-metrics.ps1
# Exibe metricas de execucao da fabrica acumuladas em .claude/metrics.log
# Uso: .\scripts\show-metrics.ps1 [-Branch <nome>] [-Ultimas <N>]

param(
    [string]$Branch = "",
    [int]$Ultimas = 50
)

$LogPath = Join-Path (git rev-parse --show-toplevel) ".claude\metrics.log"

if (-not (Test-Path $LogPath)) {
    Write-Host "Nenhuma metrica encontrada em $LogPath" -ForegroundColor Yellow
    exit 0
}

$entries = Get-Content $LogPath -Encoding UTF8 |
    Where-Object { -not [string]::IsNullOrWhiteSpace($_) } |
    ForEach-Object {
        try { $_ | ConvertFrom-Json } catch { $null }
    } |
    Where-Object { $_ -ne $null }

if ($Branch) {
    $entries = $entries | Where-Object { $_.branch -eq $Branch }
}

$entries = @($entries | Select-Object -Last $Ultimas)

Write-Host "`n=== Metricas da Fabrica ===" -ForegroundColor Cyan
Write-Host "Arquivo: $LogPath | Entradas exibidas: $($entries.Count)`n"

$entries | ForEach-Object {
    $dur = if ($_.duracao_ms) { "$([math]::Round($_.duracao_ms/1000, 1))s" } else { "N/D" }
    $exitInfo = if ($_.PSObject.Properties['exit_code']) { " [exit=$($_.exit_code)]" } else { "" }
    Write-Host "[$($_.ts)] $($_.branch) | $($_.step) | $dur$exitInfo" -ForegroundColor White
}

# Sumario por step
Write-Host "`n=== Sumario por Step ===" -ForegroundColor Cyan
$entries |
    Where-Object { $_.duracao_ms } |
    Group-Object step |
    ForEach-Object {
        $avg = [math]::Round(($_.Group | Measure-Object duracao_ms -Average).Average / 1000, 1)
        $max = [math]::Round(($_.Group | Measure-Object duracao_ms -Maximum).Maximum / 1000, 1)
        Write-Host "  $($_.Name): avg=${avg}s  max=${max}s  count=$($_.Count)"
    }
