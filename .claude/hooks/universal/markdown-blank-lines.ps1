$ErrorActionPreference = "Stop"

function Test-IsHeaderLine {
    param([string]$Line)

    # Header Markdown de nivel 2 a 6: ##, ###, ####, #####, ######
    # Seguido de espaco e conteudo. Permite indentacao com espacos antes (alguns editores).
    return ($Line -match '^\s{0,3}#{2,6}\s+\S')
}

function Test-IsBlankLine {
    param([string]$Line)

    # Linha em branco: vazia ou apenas espacos/tabs
    return ($Line -match '^\s*$')
}

function Get-MarkdownViolations {
    param([string]$FilePath)

    $lines = [System.IO.File]::ReadAllLines($FilePath, [System.Text.UTF8Encoding]::new($false))
    if ($null -eq $lines -or $lines.Count -eq 0) { return @() }

    $violations = @()
    $inCodeBlock = $false

    for ($i = 0; $i -lt $lines.Count; $i++) {
        $line = $lines[$i]

        # Toggle de bloco de codigo (linha que comeca com ```)
        if ($line -match '^\s{0,3}```') {
            $inCodeBlock = -not $inCodeBlock
            continue
        }

        if ($inCodeBlock) { continue }

        if (-not (Test-IsHeaderLine $line)) { continue }

        # Validar linha anterior (se existir)
        if ($i -gt 0) {
            $prevLine = $lines[$i - 1]
            if (-not (Test-IsBlankLine $prevLine)) {
                $violations += [PSCustomObject]@{
                    File = $FilePath
                    Line = $i + 1  # 1-indexed para humanos
                    Type = 'sem linha em branco ANTES'
                    Header = $line.TrimEnd()
                }
            }
        }
        # Se $i -eq 0 (header na primeira linha), passa por fronteira implicita.

        # Validar linha seguinte (se existir)
        if ($i -lt $lines.Count - 1) {
            $nextLine = $lines[$i + 1]
            if (-not (Test-IsBlankLine $nextLine)) {
                $violations += [PSCustomObject]@{
                    File = $FilePath
                    Line = $i + 1
                    Type = 'sem linha em branco DEPOIS'
                    Header = $line.TrimEnd()
                }
            }
        }
        # Se $i -eq $lines.Count - 1 (header na ultima linha), passa por fronteira implicita.
    }

    return $violations
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

$allViolations = @()

foreach ($file in $stagedFiles) {
    if (-not (Test-Path $file)) { continue }

    $ext = [System.IO.Path]::GetExtension($file).ToLower()
    if ($ext -ne '.md') { continue }

    $violations = @(Get-MarkdownViolations -FilePath $file)
    if ($violations.Count -gt 0) {
        $allViolations += $violations
    }
}

if ($allViolations.Count -gt 0) {
    Write-Host ""
    Write-Host "[ERRO] Validacao de blank lines em Markdown falhou em $($allViolations.Count) ocorrencia(s):" -ForegroundColor Red
    Write-Host ""
    foreach ($v in $allViolations) {
        Write-Host "  $($v.File):$($v.Line)  ($($v.Type))" -ForegroundColor Yellow
        Write-Host "    > $($v.Header)" -ForegroundColor Yellow
    }
    Write-Host ""
    Write-Host "Regra:" -ForegroundColor Cyan
    Write-Host "  - Headers Markdown de nivel 2-6 (##, ###, ####, #####, ######) devem ter linha em branco antes E depois." -ForegroundColor Cyan
    Write-Host "  - Headers de nivel 1 (#) sao ignorados (tipicamente titulo do documento)." -ForegroundColor Cyan
    Write-Host "  - Header na primeira linha do arquivo nao precisa de linha em branco antes (fronteira implicita)." -ForegroundColor Cyan
    Write-Host "  - Header na ultima linha do arquivo nao precisa de linha em branco depois (fronteira implicita)." -ForegroundColor Cyan
    Write-Host "  - Headers dentro de blocos de codigo sao ignorados." -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Como corrigir:" -ForegroundColor Cyan
    Write-Host "  - Editar o arquivo .md e inserir linha em branco onde apontado." -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Override consciente em emergencia: git commit --no-verify" -ForegroundColor Yellow
    Write-Host "(documentar uso em PR body conforme decisoes.md)" -ForegroundColor Yellow
    exit 1
}

exit 0
