# scripts/dev.ps1
# Sobe ambiente de desenvolvimento: Docker Compose + Spring Boot em foreground.
# Bloqueia o terminal. Ctrl+C para parar a aplicacao.

$ErrorActionPreference = "Stop"

# Verifica Docker rodando antes de tentar subir compose.
docker info 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host "Docker Desktop nao esta rodando. Inicie o Docker e tente novamente." -ForegroundColor Red
    exit 1
}

Write-Host "==> Garantindo que servicos Docker estao up..." -ForegroundColor Cyan
docker compose up -d
if ($LASTEXITCODE -ne 0) {
    Write-Host "Falha ao subir Docker Compose." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "==> Iniciando Spring Boot (Ctrl+C para parar)..." -ForegroundColor Cyan
.\mvnw spring-boot:run
