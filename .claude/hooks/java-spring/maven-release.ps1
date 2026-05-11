$ErrorActionPreference = "Stop"

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

# Filtro de aplicabilidade: hook so age se pom.xml estiver no diff
$pomStaged = $stagedFiles | Where-Object { ($_ -replace '\\', '/') -eq 'pom.xml' }

# Forcar contexto array (licao 4.3: PS5.1 desempacota array de 1 elemento)
$pomStaged = @($pomStaged)

if ($pomStaged.Count -eq 0) {
    # pom.xml nao esta no diff - hook nao se aplica
    exit 0
}

# pom.xml esta staged - validar presenca da tag <release>
$pomPath = $pomStaged[0]

if (-not (Test-Path $pomPath)) {
    Write-Host "[ERRO] pom.xml listado no diff mas nao encontrado no working tree." -ForegroundColor Red
    exit 1
}

$content = [System.IO.File]::ReadAllText($pomPath, [System.Text.UTF8Encoding]::new($false))

# Regex: <release> seguido de qualquer conteudo (incluindo quebras), seguido de </release>
# (?s) ativa modo single-line: . casa qualquer caractere, inclusive newline
$hasReleaseTag = $content -match '(?s)<release\s*>.*?</release\s*>'

if (-not $hasReleaseTag) {
    Write-Host ""
    Write-Host "[ERRO] pom.xml staged nao contem tag <release> dentro de <configuration> do maven-compiler-plugin." -ForegroundColor Red
    Write-Host ""
    Write-Host "Por que esta regra existe (licao 1.4):" -ForegroundColor Cyan
    Write-Host "  - Sem <release> explicito, Maven usa default que pode divergir entre dev local e CI." -ForegroundColor Cyan
    Write-Host "  - Resultado: build passa local, quebra em CI (ou vice-versa) por versao Java diferente." -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Como corrigir:" -ForegroundColor Cyan
    Write-Host "  - Adicionar <release>21</release> (ou versao configurada via variavel, ex: `${java.version}) dentro de" -ForegroundColor Cyan
    Write-Host "    <configuration> do plugin maven-compiler-plugin no pom.xml." -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Exemplo minimo:" -ForegroundColor Cyan
    Write-Host "  <plugin>" -ForegroundColor Cyan
    Write-Host "    <artifactId>maven-compiler-plugin</artifactId>" -ForegroundColor Cyan
    Write-Host "    <configuration>" -ForegroundColor Cyan
    Write-Host "      <release>21</release>" -ForegroundColor Cyan
    Write-Host "    </configuration>" -ForegroundColor Cyan
    Write-Host "  </plugin>" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Override consciente em emergencia: git commit --no-verify" -ForegroundColor Yellow
    Write-Host "(documentar uso em PR body conforme decisoes.md)" -ForegroundColor Yellow
    exit 1
}

exit 0
