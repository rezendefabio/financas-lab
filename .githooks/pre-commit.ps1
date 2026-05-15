$ErrorActionPreference = "Stop"

$prev = $ErrorActionPreference
$ErrorActionPreference = "Continue"
$repoRoot = git rev-parse --show-toplevel 2>&1
$ErrorActionPreference = $prev

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERRO] Nao foi possivel identificar o repo root." -ForegroundColor Red
    exit 1
}

# Hooks pre-commit executados em sequencia. Adicionar novos hooks aqui em sub-etapas seguintes
# (4.3 Markdown blank lines, 4.4 tamanho de docs, etc).
$hooks = @(
    (Join-Path $repoRoot ".claude\hooks\universal\encoding-utf8.ps1")
    (Join-Path $repoRoot ".claude\hooks\universal\markdown-blank-lines.ps1")
    (Join-Path $repoRoot ".claude\hooks\universal\docs-size.ps1")
    (Join-Path $repoRoot ".claude\hooks\java-spring\maven-release.ps1")
    (Join-Path $repoRoot ".claude\hooks\java-spring\entity-migration.ps1")
    (Join-Path $repoRoot ".claude\hooks\java-spring\entity-migration-modified.ps1")
    (Join-Path $repoRoot ".claude\hooks\java-spring\baseline-on-migrate.ps1")
    (Join-Path $repoRoot ".claude\hooks\java-spring\lombok-mapstruct-order.ps1")
    (Join-Path $repoRoot ".claude\hooks\java-spring\mvnw-profile.ps1")
    (Join-Path $repoRoot ".claude\hooks\java-spring\mvnw-executable.ps1")
    (Join-Path $repoRoot ".claude\hooks\universal\secret-scanning.ps1")
    (Join-Path $repoRoot ".claude\hooks\windows\write-error-exit.ps1")
)

$failed = $false
foreach ($hookPath in $hooks) {
    if (-not (Test-Path $hookPath)) {
        Write-Host "[ERRO] Hook nao encontrado: $hookPath" -ForegroundColor Red
        $failed = $true
        continue
    }
    & $hookPath
    if ($LASTEXITCODE -ne 0) {
        $failed = $true
    }
}

if ($failed) { exit 1 }
exit 0
