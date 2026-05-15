$ErrorActionPreference = "Stop"

# Hook: mvnw sem profile (licao 3.3.1)
# Verifica se scripts/.ps1 staged contem mvnw spring-boot:run sem -Dspring-boot.run.profiles=

$prev = $ErrorActionPreference
$ErrorActionPreference = "Continue"
$stagedRaw = git diff --cached --name-only 2>&1
$diffExitCode = $LASTEXITCODE
$ErrorActionPreference = $prev

if ($diffExitCode -ne 0) {
    Write-Host "[ERRO] Falha ao listar arquivos staged via git diff --cached." -ForegroundColor Red
    exit 1
}

$stagedFiles = @($stagedRaw | Where-Object { $_ -and $_.Trim() })

if ($stagedFiles.Count -eq 0) {
    exit 0
}

# Filtrar arquivos .ps1 em scripts/
$ps1Files = @($stagedFiles | Where-Object {
    $norm = $_ -replace '\\', '/'
    $norm -like 'scripts/*.ps1'
})

if ($ps1Files.Count -eq 0) {
    exit 0
}

$violacoes = @()

foreach ($file in $ps1Files) {
    if (-not (Test-Path $file)) { continue }

    $linhas = [System.IO.File]::ReadAllLines($file, [System.Text.UTF8Encoding]::new($false))

    foreach ($linha in $linhas) {
        if ($linha -match 'mvnw\s+spring-boot:run' -and $linha -notmatch 'spring-boot\.run\.profiles=') {
            $violacoes += "${file}: $($linha.Trim())"
        }
    }
}

$violacoes = @($violacoes)

if ($violacoes.Count -eq 0) {
    exit 0
}

Write-Host ""
Write-Host "[ERRO] mvnw spring-boot:run sem -Dspring-boot.run.profiles= detectado em $($violacoes.Count) linha(s)." -ForegroundColor Red
Write-Host ""
Write-Host "O que acontece (licao 3.3.1):" -ForegroundColor Cyan
Write-Host "  - mvnw spring-boot:run sem profile usa o profile 'default'." -ForegroundColor Cyan
Write-Host "  - O profile 'default' nao tem datasource configurado." -ForegroundColor Cyan
Write-Host "  - Resultado: 'Failed to configure a DataSource' ao subir o backend." -ForegroundColor Cyan
Write-Host ""
Write-Host "Linha(s) com violacao:" -ForegroundColor Red
foreach ($v in $violacoes) {
    Write-Host "  - $v" -ForegroundColor Red
}
Write-Host ""
Write-Host "Como corrigir:" -ForegroundColor Cyan
Write-Host "  - Adicionar -Dspring-boot.run.profiles=dev ao comando mvnw." -ForegroundColor Cyan
Write-Host "  - Exemplo: .\mvnw spring-boot:run -Dspring-boot.run.profiles=dev" -ForegroundColor Cyan
Write-Host ""
Write-Host "Override consciente em emergencia: git commit --no-verify" -ForegroundColor Yellow
Write-Host "(documentar uso em PR body conforme decisoes.md)" -ForegroundColor Yellow
exit 1
