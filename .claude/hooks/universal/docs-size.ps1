$ErrorActionPreference = "Stop"

# Limite de linhas para alerta de tamanho de docs (modo warn — nao bloqueia commit).
$LIMITE_LINHAS = 800

function Test-IsDocsMarkdown {
    param([string]$Path)

    # Apenas .md em docs/ (qualquer nivel de profundidade).
    $pathNorm = $Path -replace '\\', '/'
    $ext = [System.IO.Path]::GetExtension($pathNorm).ToLower()

    if ($ext -ne '.md') { return $false }
    if (-not ($pathNorm -like 'docs/*')) { return $false }

    return $true
}

function Get-LineCount {
    param([string]$Path)

    $lines = [System.IO.File]::ReadAllLines($Path, [System.Text.UTF8Encoding]::new($false))
    if ($null -eq $lines) { return 0 }
    return $lines.Count
}

# Listar arquivos staged (Added, Copied, Modified)
$prev = $ErrorActionPreference
$ErrorActionPreference = "Continue"
$stagedRaw = git diff --cached --name-only --diff-filter=ACM 2>&1
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

$alerts = @()

foreach ($file in $stagedFiles) {
    if (-not (Test-Path $file)) { continue }
    if (-not (Test-IsDocsMarkdown $file)) { continue }

    $lineCount = Get-LineCount -Path $file
    if ($lineCount -gt $LIMITE_LINHAS) {
        $alerts += [PSCustomObject]@{
            File = $file
            Lines = $lineCount
            Excess = $lineCount - $LIMITE_LINHAS
        }
    }
}

# Forcar contexto array (licao 4.3: PS5.1 desempacota array de 1 elemento)
$alerts = @($alerts)

if ($alerts.Count -gt 0) {
    Write-Host ""
    Write-Host "[ALERTA] Tamanho de docs acima do limite ($LIMITE_LINHAS linhas) em $($alerts.Count) arquivo(s):" -ForegroundColor Yellow
    Write-Host ""
    foreach ($a in $alerts) {
        Write-Host "  $($a.File)" -ForegroundColor Yellow
        Write-Host "    Linhas: $($a.Lines) (excede limite em $($a.Excess) linhas)" -ForegroundColor Yellow
    }
    Write-Host ""
    Write-Host "Recomendacao:" -ForegroundColor Cyan
    Write-Host "  - Docs em 'docs/' com mais de $LIMITE_LINHAS linhas tendem a virar enciclopedia." -ForegroundColor Cyan
    Write-Host "  - Considerar dividir em arquivos menores ou extrair secoes maduras para ADRs." -ForegroundColor Cyan
    Write-Host "  - Este e um alerta - commit prossegue normalmente. Nao precisa de --no-verify." -ForegroundColor Cyan
    Write-Host ""
}

# Modo warn: hook nunca bloqueia.
exit 0
