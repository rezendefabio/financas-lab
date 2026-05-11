$ErrorActionPreference = "Stop"

# Listar arquivos staged com status A (Added)
$prev = $ErrorActionPreference
$ErrorActionPreference = "Continue"
$stagedRaw = git diff --cached --name-only --diff-filter=A 2>&1
$diffExitCode = $LASTEXITCODE
$ErrorActionPreference = $prev

if ($diffExitCode -ne 0) {
    Write-Host "[ERRO] Falha ao listar arquivos staged via git diff --cached --diff-filter=A." -ForegroundColor Red
    exit 1
}

$stagedFiles = $stagedRaw | Where-Object { $_ -and $_.Trim() }

if (-not $stagedFiles -or $stagedFiles.Count -eq 0) {
    exit 0
}

# Filtrar .java novos sob src/main/java/
$javaFiles = $stagedFiles | Where-Object {
    $norm = $_ -replace '\\', '/'
    $norm -like 'src/main/java/*' -and $norm.EndsWith('.java')
}
$javaFiles = @($javaFiles)

if ($javaFiles.Count -eq 0) {
    # Nenhum .java novo - hook nao se aplica
    exit 0
}

# Para cada .java novo, verificar se contem @Entity
$entitiesNovas = @()

foreach ($file in $javaFiles) {
    if (-not (Test-Path $file)) { continue }

    $content = [System.IO.File]::ReadAllText($file, [System.Text.UTF8Encoding]::new($false))

    # @Entity em qualquer contexto (anotacao em linha propria ou inline)
    # Regex tolera whitespace antes, ignora @EntityListeners ou outras anotacoes com prefixo Entity
    if ($content -match '(?m)^\s*@Entity\b') {
        $entitiesNovas += $file
    }
}

$entitiesNovas = @($entitiesNovas)

if ($entitiesNovas.Count -eq 0) {
    # Java novos existem, mas nenhum tem @Entity - hook nao se aplica
    exit 0
}

# Existe @Entity novo. Verificar se ha migration nova.
$migrationsNovas = $stagedFiles | Where-Object {
    $norm = $_ -replace '\\', '/'
    $norm -match '^src/main/resources/db/migration/V\d+__.*\.sql$'
}
$migrationsNovas = @($migrationsNovas)

if ($migrationsNovas.Count -gt 0) {
    # Regra cumprida: ha @Entity novo E ha migration nova
    exit 0
}

# Bloqueio: @Entity novo sem migration nova
Write-Host ""
Write-Host "[ERRO] Detectado(s) $($entitiesNovas.Count) arquivo(s) com @Entity novo(s) no commit, mas nenhuma migration nova em src/main/resources/db/migration/." -ForegroundColor Red
Write-Host ""
Write-Host "Arquivo(s) com @Entity novo(s):" -ForegroundColor Red
foreach ($e in $entitiesNovas) {
    Write-Host "  - $e" -ForegroundColor Red
}
Write-Host ""
Write-Host "Por que esta regra existe (licao 2.1):" -ForegroundColor Cyan
Write-Host "  - Nova @Entity sem migration cria divergencia entre codigo e schema." -ForegroundColor Cyan
Write-Host "  - Hibernate pode tentar criar tabela em runtime, mas isso e proibido em prod (ddl-auto=validate)." -ForegroundColor Cyan
Write-Host "  - Resultado: build local passa, prod quebra ao subir." -ForegroundColor Cyan
Write-Host ""
Write-Host "Como corrigir:" -ForegroundColor Cyan
Write-Host "  - Criar arquivo src/main/resources/db/migration/V<n+1>__<descricao>.sql com CREATE TABLE." -ForegroundColor Cyan
Write-Host "  - Incluir no mesmo commit que a Entity." -ForegroundColor Cyan
Write-Host ""
Write-Host "Exemplo minimo:" -ForegroundColor Cyan
Write-Host "  -- V5__cria_tabela_pagamento.sql" -ForegroundColor Cyan
Write-Host "  CREATE TABLE pagamento (" -ForegroundColor Cyan
Write-Host "    id UUID PRIMARY KEY," -ForegroundColor Cyan
Write-Host "    valor NUMERIC(15,2) NOT NULL" -ForegroundColor Cyan
Write-Host "  );" -ForegroundColor Cyan
Write-Host ""
Write-Host "Override consciente em emergencia: git commit --no-verify" -ForegroundColor Yellow
Write-Host "(documentar uso em PR body conforme decisoes.md)" -ForegroundColor Yellow
exit 1
