$ErrorActionPreference = "Stop"

# Listar arquivos staged com status M (modificados, nao adicionados)
$prev = $ErrorActionPreference
$ErrorActionPreference = "Continue"
$stagedRaw = git diff --cached --name-only --diff-filter=M 2>&1
$diffExitCode = $LASTEXITCODE
$ErrorActionPreference = $prev

if ($diffExitCode -ne 0) {
    Write-Host "[ERRO] Falha ao listar arquivos staged via git diff --cached --diff-filter=M." -ForegroundColor Red
    exit 1
}

$stagedFiles = $stagedRaw | Where-Object { $_ -and $_.Trim() }

if (-not $stagedFiles -or $stagedFiles.Count -eq 0) {
    exit 0
}

# Filtrar .java modificados sob src/main/java/
$javaFiles = $stagedFiles | Where-Object {
    $norm = $_ -replace '\\', '/'
    $norm -like 'src/main/java/*' -and $norm.EndsWith('.java')
}
$javaFiles = @($javaFiles)

if ($javaFiles.Count -eq 0) {
    exit 0
}

# Para cada .java modificado, verificar @Entity e diff de campos novos
$candidatos = @()

foreach ($file in $javaFiles) {
    if (-not (Test-Path $file)) { continue }

    $content = [System.IO.File]::ReadAllText($file, [System.Text.UTF8Encoding]::new($false))

    # Verificar se contem @Entity
    if (-not ($content -match '(?m)^\s*@Entity\b')) {
        continue
    }

    # Obter diff do arquivo (suspender Stop para git diff)
    $prevInner = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    $diffOutput = git diff --cached -U0 -- $file 2>&1
    $ErrorActionPreference = $prevInner

    if (-not $diffOutput) { continue }

    $diffLines = $diffOutput -split "`n"

    # Filtrar linhas adicionadas (comecam com '+', excluindo '+++')
    $added = $diffLines | Where-Object { $_ -match '^\+[^+]' }

    if (-not $added -or $added.Count -eq 0) { continue }

    # Verificar se alguma linha adicionada corresponde a campo novo
    $temCampoNovo = $false
    foreach ($linha in $added) {
        if ($linha -match 'private\s+\w') {
            $temCampoNovo = $true
            break
        }
        if ($linha -match '@Column') {
            $temCampoNovo = $true
            break
        }
        if ($linha -match '@Id\b') {
            $temCampoNovo = $true
            break
        }
        if ($linha -match '@Embedded\b') {
            $temCampoNovo = $true
            break
        }
    }

    if ($temCampoNovo) {
        $candidatos += $file
    }
}

$candidatos = @($candidatos)

if ($candidatos.Count -eq 0) {
    exit 0
}

# Exibir AVISO (warn -- nao fail)
Write-Host ""
Write-Host "[AVISO] Detectado(s) $($candidatos.Count) arquivo(s) @Entity com possivel(is) campo(s) novo(s)." -ForegroundColor Yellow
Write-Host ""
Write-Host "Arquivo(s) com @Entity modificada e campos suspeitos:" -ForegroundColor Yellow
foreach ($c in $candidatos) {
    Write-Host "  - $c" -ForegroundColor Yellow
}
Write-Host ""
Write-Host "Orientacao:" -ForegroundColor Cyan
Write-Host "  - Se adicionou ou renomeou campo, crie uma migration V<n>__*.sql" -ForegroundColor Cyan
Write-Host "    com ALTER TABLE ADD COLUMN para o(s) campo(s) novo(s)." -ForegroundColor Cyan
Write-Host "  - Se foi apenas refactor sem mudanca de schema, ignore este aviso." -ForegroundColor Cyan
Write-Host ""
Write-Host "Exemplo minimo:" -ForegroundColor Cyan
Write-Host "  -- V10__adiciona_campo_conta.sql" -ForegroundColor Cyan
Write-Host "  ALTER TABLE conta ADD COLUMN campo_teste VARCHAR(255);" -ForegroundColor Cyan
Write-Host ""
Write-Host "(Modo WARN: commit nao bloqueado. Avalie se migration e necessaria.)" -ForegroundColor Yellow

# Modo warn -- nao bloqueia
exit 0
