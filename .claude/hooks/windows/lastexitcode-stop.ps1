$ErrorActionPreference = "Stop"

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

$ps1Files = @($stagedFiles | Where-Object { $_ -match '\.ps1$' })

if ($ps1Files.Count -eq 0) {
    exit 0
}

$suspeitos = @()

foreach ($file in $ps1Files) {
    if (-not (Test-Path $file)) { continue }

    $content = [System.IO.File]::ReadAllText($file, [System.Text.UTF8Encoding]::new($false))

    $hasStop = $content -match '\$ErrorActionPreference\s*=\s*[''"]Stop[''"]'
    if (-not $hasStop) { continue }

    $hasLastExitCode = $content -match '\$LASTEXITCODE'
    if (-not $hasLastExitCode) { continue }

    $hasSuspensao = $content -match '\$ErrorActionPreference\s*=\s*[''"]Continue[''"]'
    if ($hasSuspensao) { continue }

    $suspeitos += $file
}

if ($suspeitos.Count -gt 0) {
    Write-Host ""
    Write-Host "[AVISO] Combinacao de Stop + LASTEXITCODE sem suspensao local detectada em:" -ForegroundColor Yellow
    foreach ($s in $suspeitos) {
        Write-Host "  - $s" -ForegroundColor Yellow
    }
    Write-Host ""
    Write-Host "Risco: sob Stop, stderr de comando nativo pode lancar excecao terminating" -ForegroundColor Yellow
    Write-Host "antes do codigo atingir o 'if (`$LASTEXITCODE'. O exit code propaga errado." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Padrao correto -- suspensao local:" -ForegroundColor Cyan
    Write-Host "  `$prev = `$ErrorActionPreference" -ForegroundColor Cyan
    Write-Host "  `$ErrorActionPreference = `"Continue`"" -ForegroundColor Cyan
    Write-Host "  <comando nativo>" -ForegroundColor Cyan
    Write-Host "  `$exitCode = `$LASTEXITCODE" -ForegroundColor Cyan
    Write-Host "  `$ErrorActionPreference = `$prev" -ForegroundColor Cyan
    Write-Host "  if (`$exitCode -ne 0) { ... }" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "(Este e um aviso -- o commit nao foi bloqueado. Revise antes de prosseguir.)" -ForegroundColor Yellow
    exit 0
}

exit 0
