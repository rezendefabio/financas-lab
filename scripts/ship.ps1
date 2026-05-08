# scripts/ship.ps1
# Valida tudo localmente e empurra a branch atual para origin.
# NAO cria PR automaticamente — sugere o comando para o operador rodar.

$ErrorActionPreference = "Stop"

# 1. Working tree limpo? (commits feitos)
$status = git status --porcelain
if ($status) {
    Write-Host "Working tree nao esta limpo. Commit ou descarte mudancas antes de ship." -ForegroundColor Red
    Write-Host ""
    Write-Host "Mudancas pendentes:"
    Write-Host $status
    exit 1
}

# 2. Branch atual e remote tracking.
$branch = git rev-parse --abbrev-ref HEAD
if ($branch -eq "main" -or $branch -eq "master") {
    Write-Host "Voce esta em '$branch'. Ship deve rodar a partir de uma feature branch." -ForegroundColor Red
    exit 1
}

Write-Host "==> Branch: $branch" -ForegroundColor Cyan

# 3. Roda check.ps1 (gate completo).
Write-Host "==> Rodando gate completo antes do push..." -ForegroundColor Cyan
& "$PSScriptRoot\check.ps1"
if ($LASTEXITCODE -ne 0) {
    Write-Host "check.ps1 falhou. Push cancelado." -ForegroundColor Red
    exit 1
}

# 4. Push.
Write-Host ""
Write-Host "==> Empurrando '$branch' para origin..." -ForegroundColor Cyan
git push -u origin $branch
if ($LASTEXITCODE -ne 0) {
    Write-Host "git push falhou." -ForegroundColor Red
    exit 1
}

# 5. Sugere comando para criar PR.
Write-Host ""
Write-Host "Push concluido." -ForegroundColor Green
Write-Host ""
Write-Host "Para abrir o PR, execute:" -ForegroundColor Cyan
Write-Host ""
Write-Host "  gh pr create --title `"<titulo do PR>`" --body `"<descricao>`""
Write-Host ""
Write-Host "Ou interativamente:"
Write-Host ""
Write-Host "  gh pr create"
