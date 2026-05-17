# Detecta se frontend/package.json mudou no ultimo merge e roda npm install.
# Roda em modo warn (exit 0 sempre) -- nunca bloqueia.

$changed = git diff-tree -r --name-only --no-commit-id ORIG_HEAD HEAD 2>$null |
           Where-Object { $_ -eq "frontend/package.json" }

if (-not $changed) {
    exit 0
}

Write-Host ""
Write-Host "==> frontend/package.json mudou no merge. Rodando npm install..." -ForegroundColor Cyan

$frontendPath = Join-Path (git rev-parse --show-toplevel) "frontend"

Push-Location $frontendPath
try {
    npm install
    if ($LASTEXITCODE -ne 0) {
        Write-Host "AVISO: npm install falhou. Rode manualmente: cd frontend && npm install" -ForegroundColor Yellow
    } else {
        Write-Host "==> npm install concluido." -ForegroundColor Green
    }
} finally {
    Pop-Location
}

exit 0
