$ErrorActionPreference = "Stop"

$extensoesMonitoradas = @(".java", ".ts", ".tsx", ".js", ".jsx",
                          ".properties", ".yml", ".yaml", ".json")

$padroes = @(
    @{ Id = "P1"; Regex = "-----BEGIN .*(PRIVATE|RSA) KEY-----";       Desc = "Chave PEM privada" },
    @{ Id = "P2"; Regex = "AKIA[0-9A-Z]{16}";                          Desc = "AWS Access Key ID" },
    @{ Id = "P3"; Regex = "(ghp|ghs|gho|ghu|ghr)_[A-Za-z0-9]{36,}";  Desc = "GitHub token" },
    @{ Id = "P4"; Regex = "sk-[A-Za-z0-9]{32,}";                       Desc = "OpenAI/Anthropic API key" },
    @{ Id = "P5"; Regex = 'password\s*[=:]\s*["''][^\$\{][^"'']{7,}["'']'; Desc = "Password literal" },
    @{ Id = "P6"; Regex = '(secret|api.?key)\s*[=:]\s*["''][^\$\{][^"'']{7,}["'']'; Desc = "Secret/API key literal" }
)

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

$failed = $false

foreach ($file in $stagedFiles) {
    $fileNorm = $file -replace '\\', '/'

    $ext = [System.IO.Path]::GetExtension($file).ToLower()
    if ($extensoesMonitoradas -notcontains $ext) { continue }

    # Exclusoes
    if ($fileNorm -like "*/src/test/*" -or $fileNorm -like "src/test/*") { continue }
    $fileName = [System.IO.Path]::GetFileName($file)
    if ($fileName -like "*.example" -or $fileName -like "*-example.*" -or $fileName -like "*.env.example") { continue }

    $prev = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    $conteudo = git show ":$file" 2>&1
    $showExitCode = $LASTEXITCODE
    $ErrorActionPreference = $prev

    if ($showExitCode -ne 0) { continue }

    $linhas = $conteudo -split "`n"
    $lineNumber = 0

    foreach ($linha in $linhas) {
        $lineNumber++
        foreach ($padrao in $padroes) {
            if ($linha -imatch $padrao.Regex) {
                Write-Host "[$file] linha ${lineNumber}: $($padrao.Desc) [$($padrao.Id)]" -ForegroundColor Yellow
                $failed = $true
            }
        }
    }
}

if ($failed) {
    Write-Host ""
    Write-Host "[secret-scanning] Credenciais literais detectadas. Mova para application.properties" -ForegroundColor Red
    Write-Host "com @Value ou para variavel de ambiente. Para falso positivo documentado," -ForegroundColor Red
    Write-Host "adicione o arquivo a lista de exclusao em .claude/hooks/universal/secret-scanning.ps1." -ForegroundColor Red
    exit 1
}

exit 0
