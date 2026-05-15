$ErrorActionPreference = "Stop"

# Listar arquivos staged (A ou M)
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

# Filtrar .java em src/test/java/
$testJavaFiles = @($stagedFiles | Where-Object {
    $norm = $_ -replace '\\', '/'
    $norm -like 'src/test/java/*' -and $norm.EndsWith('.java')
})

if ($testJavaFiles.Count -eq 0) {
    exit 0
}

$sufixoViolacoes = @()
$abstractViolacoes = @()

foreach ($file in $testJavaFiles) {
    $norm = $file -replace '\\', '/'

    # REGRA 1 -- Sufixo Test
    $className = [System.IO.Path]::GetFileNameWithoutExtension($file)
    $isAbstractBase = $className -like 'Abstract*'
    $hasTestSuffix = $className -like '*Test'
    if (-not $hasTestSuffix -and -not $isAbstractBase) {
        $sufixoViolacoes += $file
    }

    # REGRA 2 -- Abstract em shared
    if ($norm -like '*/shared/*') {
        if (-not (Test-Path $file)) { continue }
        [System.Environment]::CurrentDirectory = (Get-Location).Path
        $content = [System.IO.File]::ReadAllText($file, [System.Text.UTF8Encoding]::new($false))
        if ($content -match '\bpublic class\b' -and $content -notmatch '\bpublic abstract class\b') {
            $abstractViolacoes += $file
        }
    }
}

$sufixoViolacoes = @($sufixoViolacoes)
$abstractViolacoes = @($abstractViolacoes)

$temViolacoes = $false

if ($sufixoViolacoes.Count -gt 0) {
    $temViolacoes = $true
    Write-Host ""
    Write-Host "[ERRO] Classe(s) de teste sem sufixo 'Test' e sem prefixo 'Abstract':" -ForegroundColor Red
    foreach ($v in $sufixoViolacoes) {
        Write-Host "  - $v" -ForegroundColor Red
    }
    Write-Host ""
    Write-Host "Por que esta regra existe (licao 2.1):" -ForegroundColor Cyan
    Write-Host "  - Maven Surefire descobre apenas classes *Test, Test* ou *Tests." -ForegroundColor Cyan
    Write-Host "  - Classe sem sufixo Test nunca e executada -- testes ficam silenciosos." -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Como corrigir:" -ForegroundColor Cyan
    Write-Host "  - Renomear para <Nome>Test.java." -ForegroundColor Cyan
    Write-Host "  - Classes base de teste: usar prefixo Abstract (ex: AbstractIntegrationTest)." -ForegroundColor Cyan
    Write-Host ""
}

if ($abstractViolacoes.Count -gt 0) {
    $temViolacoes = $true
    Write-Host ""
    Write-Host "[ERRO] Classe(s) base em shared/ sem modificador 'abstract':" -ForegroundColor Red
    foreach ($v in $abstractViolacoes) {
        Write-Host "  - $v" -ForegroundColor Red
    }
    Write-Host ""
    Write-Host "Por que esta regra existe (licao 2.1):" -ForegroundColor Cyan
    Write-Host "  - JUnit tenta instanciar classe base sem abstract." -ForegroundColor Cyan
    Write-Host "  - Isso duplica execucoes de teste e pode causar falhas confusas de contexto Spring." -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Como corrigir:" -ForegroundColor Cyan
    Write-Host "  - Adicionar modificador 'abstract' antes de 'class'." -ForegroundColor Cyan
    Write-Host "  - Exemplo: public abstract class AbstractIntegrationTest { ... }" -ForegroundColor Cyan
    Write-Host ""
}

if ($temViolacoes) {
    Write-Host "Override consciente em emergencia: git commit --no-verify" -ForegroundColor Yellow
    Write-Host "(documentar uso em PR body conforme decisoes.md)" -ForegroundColor Yellow
    exit 1
}

exit 0
