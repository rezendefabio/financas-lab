# scripts/setup.ps1
# Prepara o ambiente local: sobe servicos via Docker Compose,
# baixa dependencias Maven e compila o projeto.
# Ideal para primeira execucao em maquina nova ou reset completo.

$ErrorActionPreference = "Stop"

Write-Host "==> Subindo servicos Docker Compose..." -ForegroundColor Cyan
docker compose up -d
if ($LASTEXITCODE -ne 0) {
    Write-Error "Falha ao subir Docker Compose. Docker Desktop esta rodando?"
    exit 1
}

Write-Host ""
Write-Host "==> Baixando dependencias e compilando (sem testes)..." -ForegroundColor Cyan
.\mvnw clean install -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Error "Falha no mvnw clean install."
    exit 1
}

Write-Host ""
Write-Host "Setup concluido com sucesso." -ForegroundColor Green
Write-Host "Proximos passos sugeridos:"
Write-Host "  scripts\dev.ps1                  # subir aplicacao em modo dev"
Write-Host "  scripts\test-integration.ps1     # rodar testes de integracao"
Write-Host "  scripts\check.ps1                # rodar gate completo (CI local)"
