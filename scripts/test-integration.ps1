# scripts/test-integration.ps1
# Roda testes de integracao (mvnw verify) com JaCoCo,
# mas pula Checkstyle e SpotBugs para iteracao mais rapida.
# Para o gate completo, use scripts\check.ps1.

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

Write-Host "==> Rodando testes de integracao (mvnw verify, sem analise estatica)..." -ForegroundColor Cyan
.\mvnw verify "-Dcheckstyle.skip=true" "-Dspotbugs.skip=true"
$exit = $LASTEXITCODE

Write-Host ""
if ($exit -eq 0) {
    Write-Host "Testes de integracao passaram." -ForegroundColor Green
} else {
    Write-Host "Testes de integracao falharam (exit $exit)." -ForegroundColor Red
}
exit $exit
