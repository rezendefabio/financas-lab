# scripts/check.ps1
# Gate completo: espelha o que o GitHub Actions CI roda em pull_request.
# Inclui testes + JaCoCo check + Checkstyle + SpotBugs.
# Se este script passa local, CI deve passar tambem.

$ErrorActionPreference = "Stop"

# Instrumentacao de metricas
$MetricsLog = Join-Path (git rev-parse --show-toplevel 2>$null) ".claude\metrics.log"
$StepStart = [DateTimeOffset]::UtcNow
$StepLabel = "check.ps1 (mvn verify)"
$Branch = git branch --show-current 2>$null

# Verifica Docker rodando (Testcontainers precisa).
# Suspende Stop localmente para evitar que stderr nativo do docker vaze (PowerShell + Stop intercepta stderr antes de redirecionamento).
$prev = $ErrorActionPreference
$ErrorActionPreference = "Continue"
docker info 2>&1 | Out-Null
$ErrorActionPreference = $prev
if ($LASTEXITCODE -ne 0) {
    Write-Host "Docker Desktop nao esta rodando. Testcontainers precisa do Docker." -ForegroundColor Red
    exit 1
}

Write-Host "==> Rodando gate completo (mvnw verify)..." -ForegroundColor Cyan
.\mvnw verify
$exit = $LASTEXITCODE

Write-Host ""
if ($exit -eq 0) {
    Write-Host "Gate completo passou. Equivalente ao CI verde." -ForegroundColor Green
} else {
    Write-Host "Gate completo falhou (exit $exit). Veja o output acima." -ForegroundColor Red
}

# Gravar metrica
$StepEnd = [DateTimeOffset]::UtcNow
$DuracaoMs = [long]($StepEnd - $StepStart).TotalMilliseconds
$Entry = [PSCustomObject]@{
    ts         = $StepEnd.ToString("yyyy-MM-ddTHH:mm:ssZ")
    step       = $StepLabel
    branch     = $Branch
    duracao_ms = $DuracaoMs
    exit_code  = $exit
} | ConvertTo-Json -Compress
Add-Content -Path $MetricsLog -Value $Entry -Encoding UTF8
Write-Host "METRICA: $StepLabel concluido em $([math]::Round($DuracaoMs/1000, 1))s" -ForegroundColor DarkGray

exit $exit
