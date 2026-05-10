$ErrorActionPreference = "Stop"

$messageFile = $args[0]
if (-not $messageFile) {
    Write-Host "[ERRO] commit-msg hook chamado sem argumento (caminho do arquivo de mensagem)." -ForegroundColor Red
    exit 1
}

$prev = $ErrorActionPreference
$ErrorActionPreference = "Continue"
$repoRoot = git rev-parse --show-toplevel 2>&1
$ErrorActionPreference = $prev

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERRO] Nao foi possivel identificar o repo root." -ForegroundColor Red
    exit 1
}

$hookPath = Join-Path $repoRoot ".claude\hooks\universal\conventional-commits.ps1"
if (-not (Test-Path $hookPath)) {
    Write-Host "[ERRO] Hook nao encontrado: $hookPath" -ForegroundColor Red
    exit 1
}

& $hookPath $messageFile
exit $LASTEXITCODE
