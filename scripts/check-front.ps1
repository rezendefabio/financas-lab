# scripts/check-front.ps1
# Gate de qualidade do frontend: lint + testes + build.
# Roda a partir de qualquer diretorio -- usa caminho absoluto para frontend/.

$ErrorActionPreference = "Stop"

$frontendPath = Join-Path $PSScriptRoot "..\frontend"

Write-Host "==> Gate frontend: lint..." -ForegroundColor Cyan
Set-Location $frontendPath
npm run lint
if ($LASTEXITCODE -ne 0) {
    Write-Host "Lint falhou (exit $LASTEXITCODE)." -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host "==> Gate frontend: testes..." -ForegroundColor Cyan
npm run test:run
if ($LASTEXITCODE -ne 0) {
    Write-Host "Testes falharam (exit $LASTEXITCODE)." -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host "==> Gate frontend: build..." -ForegroundColor Cyan
npm run build
$exit = $LASTEXITCODE

Write-Host ""
if ($exit -eq 0) {
    Write-Host "Gate frontend passou (lint + testes + build)." -ForegroundColor Green
} else {
    Write-Host "Build falhou (exit $exit). Veja o output acima." -ForegroundColor Red
}
exit $exit
