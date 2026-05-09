# scripts/dev.ps1
# Sobe ambiente de desenvolvimento: Docker Compose + Spring Boot em foreground.
# Bloqueia o terminal. Ctrl+C para parar a aplicacao.

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

# Verifica Docker rodando antes de tentar subir compose.
# Suspende Stop localmente para evitar que stderr nativo do docker vaze (PowerShell + Stop intercepta stderr antes de redirecionamento).
$prev = $ErrorActionPreference
$ErrorActionPreference = "Continue"
docker info 2>&1 | Out-Null
$ErrorActionPreference = $prev
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
.\mvnw spring-boot:run "-Dspring-boot.run.profiles=dev"
