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

$unixCmds = @('tail', 'head', 'grep', 'sed', 'awk')
$ocorrencias = @()

foreach ($file in $ps1Files) {
    if (-not (Test-Path $file)) { continue }

    $content = [System.IO.File]::ReadAllText($file, [System.Text.UTF8Encoding]::new($false))
    $lines = $content -split "`n"

    for ($i = 0; $i -lt $lines.Count; $i++) {
        $line = $lines[$i]
        if ($line -match '^\s*#') { continue }
        foreach ($cmd in $unixCmds) {
            if ($line -match "\b$cmd\b") {
                $ocorrencias += "$file (linha $($i + 1)): $cmd"
                break
            }
        }
    }
}

if ($ocorrencias.Count -gt 0) {
    Write-Host ""
    Write-Host "[AVISO] Comandos Unix detectados em scripts .ps1:" -ForegroundColor Yellow
    foreach ($o in $ocorrencias) {
        Write-Host "  - $o" -ForegroundColor Yellow
    }
    Write-Host ""
    Write-Host "Problema: tail, head, grep, sed, awk nao existem no PowerShell nativo." -ForegroundColor Yellow
    Write-Host "Scripts .ps1 que os usam falham em Windows sem Git Bash no PATH." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Equivalentes PowerShell:" -ForegroundColor Cyan
    Write-Host "  tail -N arquivo   ->  Get-Content arquivo | Select-Object -Last N" -ForegroundColor Cyan
    Write-Host "  head -N arquivo   ->  Get-Content arquivo | Select-Object -First N" -ForegroundColor Cyan
    Write-Host "  grep pattern      ->  Select-String -Pattern 'pattern'" -ForegroundColor Cyan
    Write-Host "  sed / awk         ->  -replace ou pipeline PS" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "(Este e um aviso -- o commit nao foi bloqueado. Revise antes de prosseguir.)" -ForegroundColor Yellow
    exit 0
}

exit 0
