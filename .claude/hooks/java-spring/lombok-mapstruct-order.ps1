$ErrorActionPreference = "Stop"

# Hook so age quando pom.xml esta nos arquivos staged
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

$pomStaged = $stagedFiles | Where-Object {
    $norm = $_ -replace '\\', '/'
    $norm -eq 'pom.xml'
}

if (-not $pomStaged) {
    # pom.xml nao esta no commit -- hook nao se aplica
    exit 0
}

if (-not (Test-Path 'pom.xml')) {
    exit 0
}

$content = [System.IO.File]::ReadAllText('pom.xml', [System.Text.UTF8Encoding]::new($false))

# Extrair bloco annotationProcessorPaths
if ($content -notmatch '(?s)<annotationProcessorPaths>(.*?)</annotationProcessorPaths>') {
    # Bloco nao existe -- hook nao se aplica
    exit 0
}

$block = $Matches[1]

$lombokIdx = $block.IndexOf('lombok')
$mapstructIdx = $block.IndexOf('mapstruct-processor')

if ($lombokIdx -lt 0 -or $mapstructIdx -lt 0) {
    # Um dos dois nao encontrado -- hook nao se aplica
    exit 0
}

if ($mapstructIdx -lt $lombokIdx) {
    # MapStruct aparece antes de Lombok -- violacao
    Write-Host ""
    Write-Host "[ERRO] MapStruct-processor esta declarado antes de Lombok em <annotationProcessorPaths> no pom.xml." -ForegroundColor Red
    Write-Host ""
    Write-Host "Por que esta regra existe (licao 1.4):" -ForegroundColor Cyan
    Write-Host "  - O processador MapStruct precisa enxergar os metodos gerados pelo Lombok" -ForegroundColor Cyan
    Write-Host "    (getters, setters, builders) durante a compilacao." -ForegroundColor Cyan
    Write-Host "  - Se MapStruct for processado antes de Lombok, os metodos ainda nao existem" -ForegroundColor Cyan
    Write-Host "    e o build quebra com erros nao-obvios de 'method not found'." -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Como corrigir:" -ForegroundColor Cyan
    Write-Host "  - Em <annotationProcessorPaths>, mover o bloco <path> do lombok para" -ForegroundColor Cyan
    Write-Host "    ANTES do bloco <path> do mapstruct-processor." -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Exemplo correto:" -ForegroundColor Cyan
    Write-Host "  <annotationProcessorPaths>" -ForegroundColor Cyan
    Write-Host "    <!-- Lombok MUST come before MapStruct -->" -ForegroundColor Cyan
    Write-Host "    <path><groupId>org.projectlombok</groupId>...</path>" -ForegroundColor Cyan
    Write-Host "    <path><groupId>org.mapstruct</groupId>...</path>" -ForegroundColor Cyan
    Write-Host "  </annotationProcessorPaths>" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Override consciente em emergencia: git commit --no-verify" -ForegroundColor Yellow
    Write-Host "(documentar uso em PR body conforme decisoes.md)" -ForegroundColor Yellow
    exit 1
}

exit 0
