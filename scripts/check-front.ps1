# scripts/check-front.ps1
# Gate de qualidade do frontend: lint + testes + build.
# Roda a partir de qualquer diretorio -- usa caminho absoluto para frontend/.

$ErrorActionPreference = "Stop"

# Instrumentacao de metricas — aponta sempre para o repo principal, mesmo em worktree
$mainRoot = (git worktree list --porcelain 2>$null | Select-String "^worktree " | Select-Object -First 1).Line -replace "^worktree ", ""
$MetricsLog = Join-Path $mainRoot ".claude\metrics.log"
$StepStart = [DateTimeOffset]::UtcNow
$StepLabel = "check-front.ps1 (lint+test+build)"
$Branch = git branch --show-current 2>$null

$frontendPath = Join-Path $PSScriptRoot "..\frontend"

Push-Location $frontendPath

$nodeModulesPath = Join-Path $frontendPath "node_modules"
if (-not (Test-Path $nodeModulesPath)) {
    Write-Host "==> Gate frontend: npm install (node_modules ausente)..." -ForegroundColor Cyan
    npm install
    if ($LASTEXITCODE -ne 0) {
        Write-Host "npm install falhou (exit $LASTEXITCODE)." -ForegroundColor Red
        Pop-Location
        exit $LASTEXITCODE
    }
} else {
    Write-Host "==> Gate frontend: node_modules presente, pulando npm install." -ForegroundColor Gray
}

Write-Host "==> Gate frontend: lint..." -ForegroundColor Cyan
npm run lint
if ($LASTEXITCODE -ne 0) {
    Write-Host "Lint falhou (exit $LASTEXITCODE)." -ForegroundColor Red
    Pop-Location
    exit $LASTEXITCODE
}

Write-Host "==> Gate frontend: testes..." -ForegroundColor Cyan
npm run test:run
if ($LASTEXITCODE -ne 0) {
    Write-Host "Testes falharam (exit $LASTEXITCODE)." -ForegroundColor Red
    Pop-Location
    exit $LASTEXITCODE
}

Write-Host "==> Gate frontend: build..." -ForegroundColor Cyan
npm run build
$exit = $LASTEXITCODE

Pop-Location

Write-Host ""
if ($exit -eq 0) {
    Write-Host "Gate frontend passou (lint + testes + build)." -ForegroundColor Green
} else {
    Write-Host "Build falhou (exit $exit). Veja o output acima." -ForegroundColor Red
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
