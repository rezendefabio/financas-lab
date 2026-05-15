$ErrorActionPreference = "Stop"

# Hook: mvnw sem bit de execucao no git index
# mvnw sem modo 100755 no indice git faz o CI Linux falhar com Permission denied.

$prev = $ErrorActionPreference
$ErrorActionPreference = "Continue"
$lsOutput = git ls-files --stage mvnw 2>&1
$lsExitCode = $LASTEXITCODE
$ErrorActionPreference = $prev

if ($lsExitCode -ne 0) {
    Write-Host "[ERRO] Falha ao executar git ls-files --stage mvnw." -ForegroundColor Red
    exit 1
}

$lsOutput = @($lsOutput | Where-Object { $_ -and $_.Trim() })

if ($lsOutput.Count -eq 0) {
    # mvnw nao esta no indice -- hook nao se aplica
    exit 0
}

# Formato da saida: <modo> <sha> <stage> <path>
# Exemplo: 100755 abc123 0       mvnw
$primeiraLinha = $lsOutput[0].Trim()
$campos = $primeiraLinha -split '\s+'
$modo = $campos[0]

if ($modo -eq '100755') {
    exit 0
}

Write-Host ""
Write-Host "[ERRO] mvnw nao tem bit de execucao no indice git (modo atual: $modo, esperado: 100755)." -ForegroundColor Red
Write-Host ""
Write-Host "O que acontece:" -ForegroundColor Cyan
Write-Host "  - Localmente no Windows nao ha impacto (filesystem ignora bit de execucao)." -ForegroundColor Cyan
Write-Host "  - No CI Linux: './mvnw' falha com 'Permission denied'." -ForegroundColor Cyan
Write-Host "  - Resultado: build do CI quebra para todos os contribuidores." -ForegroundColor Cyan
Write-Host ""
Write-Host "Como corrigir:" -ForegroundColor Cyan
Write-Host "  - git update-index --chmod=+x mvnw" -ForegroundColor Cyan
Write-Host "  - git add mvnw" -ForegroundColor Cyan
Write-Host "  - Incluir mvnw no mesmo commit ou em commit anterior." -ForegroundColor Cyan
Write-Host ""
Write-Host "Override consciente em emergencia: git commit --no-verify" -ForegroundColor Yellow
Write-Host "(documentar uso em PR body conforme decisoes.md)" -ForegroundColor Yellow
exit 1
