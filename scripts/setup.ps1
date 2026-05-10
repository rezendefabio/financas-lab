# scripts/setup.ps1
# Prepara o ambiente local: sobe servicos via Docker Compose,
# baixa dependencias Maven e compila o projeto.
# Ideal para primeira execucao em maquina nova ou reset completo.

$ErrorActionPreference = "Stop"

if (-not (Test-Path .env)) {
    if (Test-Path .env.example) {
        Copy-Item .env.example .env
        Write-Host "AVISO: .env nao encontrado. Criado a partir de .env.example." -ForegroundColor Yellow
        Write-Host "Revise as credenciais em .env antes de usar em ambiente compartilhado." -ForegroundColor Yellow
    } else {
        Write-Host "ERRO: .env nao encontrado e .env.example tambem nao existe." -ForegroundColor Red
        Write-Host "Repositorio parece corrompido. Verifique o clone." -ForegroundColor Red
        exit 1
    }
}

Write-Host "==> Subindo servicos Docker Compose..." -ForegroundColor Cyan
docker compose up -d
if ($LASTEXITCODE -ne 0) {
    Write-Host "Falha ao subir Docker Compose. Docker Desktop esta rodando?" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "==> Baixando dependencias e compilando (sem testes)..." -ForegroundColor Cyan
.\mvnw clean install -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Host "Falha no mvnw clean install." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "==> Configurando core.hooksPath..." -ForegroundColor Cyan
git config core.hooksPath .githooks
if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERRO] Falha ao configurar core.hooksPath." -ForegroundColor Red
    exit 1
}
Write-Host "[OK] core.hooksPath configurado para .githooks/" -ForegroundColor Green

Write-Host ""
Write-Host "Setup concluido com sucesso." -ForegroundColor Green
Write-Host "Proximos passos sugeridos:"
Write-Host "  scripts\dev.ps1                  # subir aplicacao em modo dev"
Write-Host "  scripts\test-integration.ps1     # rodar testes de integracao"
Write-Host "  scripts\check.ps1                # rodar gate completo (CI local)"
