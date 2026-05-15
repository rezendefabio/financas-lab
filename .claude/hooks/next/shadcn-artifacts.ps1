$ErrorActionPreference = "Stop"

# Listar arquivos staged (qualquer status)
$prev = $ErrorActionPreference
$ErrorActionPreference = "Continue"
$stagedRaw = git diff --cached --name-only 2>&1
$diffExitCode = $LASTEXITCODE
$ErrorActionPreference = $prev

if ($diffExitCode -ne 0) {
    Write-Host "[ERRO] Falha ao listar arquivos staged via git diff --cached." -ForegroundColor Red
    exit 1
}

$stagedFiles = $stagedRaw | Where-Object { $_ -and $_.Trim() }

if (-not $stagedFiles -or $stagedFiles.Count -eq 0) {
    exit 0
}

$warnings = @()

# AVISO 1 -- button.tsx do shadcn
foreach ($file in $stagedFiles) {
    $norm = $file -replace '\\', '/'
    if ($norm -eq 'frontend/src/components/ui/button.tsx') {
        $warnings += 'BUTTON_TSX'
        break
    }
}

# AVISO 2 -- AGENTS.md / CLAUDE.md em subdiretorio frontend/
foreach ($file in $stagedFiles) {
    $norm = $file -replace '\\', '/'
    $fileName = [System.IO.Path]::GetFileName($norm)
    if (($fileName -eq 'AGENTS.md' -or $fileName -eq 'CLAUDE.md') -and $norm.StartsWith('frontend/')) {
        $warnings += 'AGENTS_CLAUDE_MD'
        break
    }
}

if ($warnings.Count -eq 0) {
    exit 0
}

Write-Host ""

if ($warnings -contains 'BUTTON_TSX') {
    Write-Host "[AVISO] Detectado frontend/src/components/ui/button.tsx no commit." -ForegroundColor Yellow
    Write-Host "  O comando 'npx shadcn@latest init --defaults' instala button.tsx automaticamente." -ForegroundColor Yellow
    Write-Host "  Se nao vai usar o componente Button nesta sub-etapa, remova com:" -ForegroundColor Yellow
    Write-Host "    git restore --staged frontend/src/components/ui/button.tsx" -ForegroundColor Yellow
    Write-Host "    Remove-Item frontend/src/components/ui/button.tsx" -ForegroundColor Yellow
    Write-Host ""
}

if ($warnings -contains 'AGENTS_CLAUDE_MD') {
    Write-Host "[AVISO] Detectado AGENTS.md ou CLAUDE.md dentro de frontend/ no commit." -ForegroundColor Yellow
    Write-Host "  Frameworks como Next.js, Vite e Create React App geram esse arquivo automaticamente." -ForegroundColor Yellow
    Write-Host "  Revise o conteudo. Se for generico de training data, remova." -ForegroundColor Yellow
    Write-Host "  Se tiver instrucoes uteis para o projeto, mantenha e documente a decisao." -ForegroundColor Yellow
    Write-Host ""
}

# Modo warn: nao bloqueia
exit 0
