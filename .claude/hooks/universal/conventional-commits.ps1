$ErrorActionPreference = "Stop"

$messageFile = $args[0]
if (-not (Test-Path $messageFile)) {
    Write-Host "[ERRO] Arquivo de mensagem nao encontrado: $messageFile" -ForegroundColor Red
    exit 1
}

$content = Get-Content $messageFile -Raw -Encoding UTF8
if (-not $content) {
    Write-Host "[ERRO] Arquivo de mensagem vazio." -ForegroundColor Red
    exit 1
}

# Primeira linha nao-vazia, ignorando comentarios (linhas iniciadas por #)
$firstLine = ""
foreach ($line in ($content -split "`n")) {
    $trimmed = $line.Trim()
    if ($trimmed -and -not $trimmed.StartsWith("#")) {
        $firstLine = $trimmed
        break
    }
}

if (-not $firstLine) {
    Write-Host "[ERRO] Mensagem de commit vazia (todas as linhas sao comentarios ou em branco)." -ForegroundColor Red
    exit 1
}

# Excecoes automaticas do git: merge e revert commits passam sem validacao
if ($firstLine.StartsWith("Merge ") -or $firstLine.StartsWith("Revert ")) {
    exit 0
}

# Conventional Commits: <tipo>(scope opcional)!?: <descricao com min 10 chars>
$pattern = '^(feat|fix|chore|docs|test|refactor|style|perf|build|ci)(\([a-z0-9-]+\))?!?: .{10,}$'

if ($firstLine -notmatch $pattern) {
    Write-Host ""
    Write-Host "[ERRO] Mensagem de commit nao segue Conventional Commits." -ForegroundColor Red
    Write-Host ""
    Write-Host "Mensagem rejeitada:" -ForegroundColor Yellow
    Write-Host "  $firstLine" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Formato esperado:" -ForegroundColor Cyan
    Write-Host "  <tipo>[(scope)][!]: <descricao com ao menos 10 caracteres>" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Tipos permitidos: feat, fix, chore, docs, test, refactor, style, perf, build, ci" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Exemplos validos:" -ForegroundColor Cyan
    Write-Host "  feat: adiciona endpoint de saldo" -ForegroundColor Green
    Write-Host "  feat(conta): adiciona endpoint de saldo" -ForegroundColor Green
    Write-Host "  fix(setup): corrige posicao do bloco hooks" -ForegroundColor Green
    Write-Host "  feat!: muda schema da API de transacoes" -ForegroundColor Green
    Write-Host ""
    Write-Host "Override consciente em emergencia: git commit --no-verify" -ForegroundColor Yellow
    Write-Host "(documentar uso em PR body conforme decisoes.md)" -ForegroundColor Yellow
    exit 1
}

exit 0
