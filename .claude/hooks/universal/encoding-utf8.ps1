$ErrorActionPreference = "Stop"

function Test-IsWhitelisted {
    param([string]$Path)

    $extensions = @(
        '.md', '.java', '.yml', '.yaml', '.xml', '.properties', '.ps1', '.sql',
        '.ts', '.tsx', '.js', '.jsx', '.json', '.css', '.html'
    )

    $nomesExatos = @(
        '.gitignore', '.gitattributes', '.editorconfig', '.env.example'
    )

    $ext = [System.IO.Path]::GetExtension($Path).ToLower()
    $nome = [System.IO.Path]::GetFileName($Path)

    if ($extensions -contains $ext) { return $true }
    if ($nomesExatos -contains $nome) { return $true }

    # Arquivos sem extensao dentro de .githooks/ (entrypoints bash)
    $pathNorm = $Path -replace '\\', '/'
    if ($pathNorm -like '.githooks/*' -and -not $ext) { return $true }

    return $false
}

function Test-FileEncoding {
    param([string]$Path)

    $bytes = [System.IO.File]::ReadAllBytes($Path)

    if ($bytes.Length -eq 0) {
        return @{ ValidUtf8 = $true; HasBom = $false }
    }

    $hasBom = ($bytes.Length -ge 3 -and `
               $bytes[0] -eq 0xEF -and `
               $bytes[1] -eq 0xBB -and `
               $bytes[2] -eq 0xBF)

    if ($hasBom) {
        $contentBytes = $bytes[3..($bytes.Length - 1)]
    } else {
        $contentBytes = $bytes
    }

    # Decodificacao UTF-8 estrita: lanca excecao se houver sequencia invalida
    try {
        $utf8Strict = New-Object System.Text.UTF8Encoding($false, $true)
        $null = $utf8Strict.GetString($contentBytes)
        return @{ ValidUtf8 = $true; HasBom = $hasBom }
    } catch {
        return @{ ValidUtf8 = $false; HasBom = $hasBom }
    }
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
    # Sem arquivos staged elegiveis - nada a validar
    exit 0
}

$problems = @()

foreach ($file in $stagedFiles) {
    if (-not (Test-Path $file)) { continue }
    if (-not (Test-IsWhitelisted $file)) { continue }

    $check = Test-FileEncoding -Path $file

    if (-not $check.ValidUtf8) {
        $problems += [PSCustomObject]@{
            File = $file
            Problem = "Nao e UTF-8 valido (encoding incorreto, possivelmente Latin-1, Windows-1252, etc)"
        }
        continue
    }

    $ext = [System.IO.Path]::GetExtension($file).ToLower()
    if ($ext -eq '.ps1' -and $check.HasBom) {
        $problems += [PSCustomObject]@{
            File = $file
            Problem = "Arquivo .ps1 com BOM (regra: PowerShell exige UTF-8 sem BOM, licao da Etapa 2.6)"
        }
    }
}

if ($problems.Count -gt 0) {
    Write-Host ""
    Write-Host "[ERRO] Validacao de encoding falhou em $($problems.Count) arquivo(s):" -ForegroundColor Red
    Write-Host ""
    foreach ($p in $problems) {
        Write-Host "  - $($p.File)" -ForegroundColor Yellow
        Write-Host "      $($p.Problem)" -ForegroundColor Yellow
    }
    Write-Host ""
    Write-Host "Regra:" -ForegroundColor Cyan
    Write-Host "  - Arquivos de texto devem ser UTF-8 valido." -ForegroundColor Cyan
    Write-Host "  - Arquivos .ps1 devem ser UTF-8 SEM BOM (licao da Etapa 2.6)." -ForegroundColor Cyan
    Write-Host "  - Outros tipos podem ter ou nao ter BOM (passam quando UTF-8 valido)." -ForegroundColor Cyan
    Write-Host "  - Binarios e tipos fora da whitelist sao ignorados." -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Como corrigir:" -ForegroundColor Cyan
    Write-Host "  - Reabrir o arquivo em editor (VSCode, Notepad++, ISE) e salvar como 'UTF-8 sem BOM'." -ForegroundColor Cyan
    Write-Host "  - Em VSCode: clicar no encoding na barra inferior, escolher 'Save with Encoding' > 'UTF-8'." -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Override consciente em emergencia: git commit --no-verify" -ForegroundColor Yellow
    Write-Host "(documentar uso em PR body conforme decisoes.md)" -ForegroundColor Yellow
    exit 1
}

exit 0
