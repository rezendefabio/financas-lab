# scripts/test.ps1
# Ciclo rapido: roda os testes via Surefire.
# NAO executa JaCoCo check, Checkstyle nem SpotBugs.
# Para o gate completo (igual ao CI), use scripts\check.ps1.

$ErrorActionPreference = "Stop"

Write-Host "==> Rodando testes (mvnw test)..." -ForegroundColor Cyan
.\mvnw test
$exit = $LASTEXITCODE

Write-Host ""
if ($exit -eq 0) {
    Write-Host "Testes passaram." -ForegroundColor Green
} else {
    Write-Host "Testes falharam (exit $exit)." -ForegroundColor Red
}
exit $exit
