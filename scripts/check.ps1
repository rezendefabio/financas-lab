# scripts/check.ps1
# Gate completo: espelha o que o GitHub Actions CI roda em pull_request.
# Inclui testes + JaCoCo check + Checkstyle + SpotBugs.
# Se este script passa local, CI deve passar tambem.

$ErrorActionPreference = "Stop"

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
.\mvnw clean verify
$exit = $LASTEXITCODE

Write-Host ""
if ($exit -eq 0) {
    Write-Host "Gate completo passou. Equivalente ao CI verde." -ForegroundColor Green
} else {
    Write-Host "Gate completo falhou (exit $exit). Veja o output acima." -ForegroundColor Red
}
exit $exit
