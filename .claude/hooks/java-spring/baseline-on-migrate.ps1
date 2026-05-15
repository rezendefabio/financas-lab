$ErrorActionPreference = "Stop"

# Listar arquivos staged
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

# Filtrar application*.yml em src/main/resources/
$ymlFiles = @($stagedFiles | Where-Object {
    $norm = $_ -replace '\\', '/'
    $norm -match 'src/main/resources/application.*\.(yml|yaml)$'
})

if ($ymlFiles.Count -eq 0) {
    exit 0
}

$violations = @()

foreach ($file in $ymlFiles) {
    if (-not (Test-Path $file)) { continue }

    # Verificar se o arquivo e application-test.yml ou application-dev.yml (permitidos)
    $basename = [System.IO.Path]::GetFileName($file)
    if ($basename -eq 'application-test.yml' -or $basename -eq 'application-test.yaml' -or
        $basename -eq 'application-dev.yml'  -or $basename -eq 'application-dev.yaml') {
        continue
    }

    $content = [System.IO.File]::ReadAllText($file, [System.Text.UTF8Encoding]::new($false))

    if ($content -match 'baseline-on-migrate:\s*true') {
        $violations += $file
    }
}

$violations = @($violations)

if ($violations.Count -eq 0) {
    exit 0
}

# Bloqueio: baseline-on-migrate:true em arquivo que nao seja application-test.yml / application-dev.yml
Write-Host ""
Write-Host "[ERRO] baseline-on-migrate: true detectado em arquivo(s) que afetam producao:" -ForegroundColor Red
Write-Host ""
foreach ($v in $violations) {
    Write-Host "  - $v" -ForegroundColor Red
}
Write-Host ""
Write-Host "Por que esta regra existe (licao 2.1):" -ForegroundColor Cyan
Write-Host "  - baseline-on-migrate: true fora de application-test.yml / application-dev.yml" -ForegroundColor Cyan
Write-Host "    faz o Flyway marcar TODAS as migrations existentes como 'ja executadas' em prod." -ForegroundColor Cyan
Write-Host "  - Resultado: tabelas faltando no schema real -- Flyway ignora silenciosamente" -ForegroundColor Cyan
Write-Host "    migrations que ainda nao foram aplicadas no banco de producao." -ForegroundColor Cyan
Write-Host ""
Write-Host "Como corrigir:" -ForegroundColor Cyan
Write-Host "  - Remover 'baseline-on-migrate: true' do arquivo de producao." -ForegroundColor Cyan
Write-Host "  - Se a intencao e usar baseline apenas em testes, mover a propriedade para" -ForegroundColor Cyan
Write-Host "    src/main/resources/application-test.yml ou application-dev.yml." -ForegroundColor Cyan
Write-Host ""
Write-Host "Override consciente em emergencia: git commit --no-verify" -ForegroundColor Yellow
Write-Host "(documentar uso em PR body conforme decisoes.md)" -ForegroundColor Yellow
exit 1
