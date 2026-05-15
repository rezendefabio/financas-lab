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

# Filtrar arquivos .ps1 (qualquer path)
$ps1Files = @($stagedFiles | Where-Object {
    $_ -match '\.ps1$'
})

if ($ps1Files.Count -eq 0) {
    exit 0
}

$suspeitos = @()

foreach ($file in $ps1Files) {
    if (-not (Test-Path $file)) { continue }

    $content = [System.IO.File]::ReadAllText($file, [System.Text.UTF8Encoding]::new($false))
    $lines = $content -split "`n"

    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($lines[$i] -match 'Write-Error') {
            $windowEnd = [Math]::Min($i + 5, $lines.Count - 1)
            for ($j = $i + 1; $j -le $windowEnd; $j++) {
                if ($lines[$j] -match '\bexit\b') {
                    $suspeitos += "$file (linha $($i + 1))"
                    break
                }
            }
        }
    }
}

if ($suspeitos.Count -gt 0) {
    Write-Host ""
    Write-Host "[AVISO] Padrao Write-Error seguido de exit detectado em:" -ForegroundColor Yellow
    foreach ($s in $suspeitos) {
        Write-Host "  - $s" -ForegroundColor Yellow
    }
    Write-Host ""
    Write-Host "Problema: Write-Error sob `$ErrorActionPreference = 'Stop' lanca excecao terminating" -ForegroundColor Yellow
    Write-Host "antes de atingir o exit N seguinte. O exit code propaga errado quando o script" -ForegroundColor Yellow
    Write-Host "e invocado com dot-source." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Substituicao recomendada:" -ForegroundColor Cyan
    Write-Host "  Write-Host -ForegroundColor Red `"<mensagem>`"" -ForegroundColor Cyan
    Write-Host "  exit N" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "(Este e um aviso -- o commit nao foi bloqueado. Revise antes de prosseguir.)" -ForegroundColor Yellow
    exit 0
}

exit 0
