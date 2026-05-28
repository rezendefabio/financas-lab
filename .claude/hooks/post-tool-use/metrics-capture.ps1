# Hook PostToolUse: captura timing de comandos caros (mvn, npm test:run, gh pr create, git push).
# Ativado via settings.json para o evento PostToolUse no Bash tool.
# Input: stdin contem JSON com tool_name, tool_input, tool_response.
# Non-blocking: qualquer erro resulta em exit 0 (falha silenciosa).

param()

$ErrorActionPreference = "SilentlyContinue"

# Ler input do stdin
$rawInput = $input | Out-String
if ([string]::IsNullOrWhiteSpace($rawInput)) { exit 0 }

try {
    $data = $rawInput | ConvertFrom-Json
} catch {
    exit 0
}

# So interessa Bash tool
if ($data.tool_name -ne "Bash") { exit 0 }

$cmd = $data.tool_input.command
if ([string]::IsNullOrWhiteSpace($cmd)) { exit 0 }

# Identificar comandos caros para logar
$label = $null
if ($cmd -match "mvnw?\s+verify")          { $label = "mvn verify" }
elseif ($cmd -match "mvnw?\s+test")        { $label = "mvn test (pontual)" }
elseif ($cmd -match "npm run test:run")    { $label = "npm test:run" }
elseif ($cmd -match "npm run build")       { $label = "npm build" }
elseif ($cmd -match "npm run lint")        { $label = "npm lint" }
elseif ($cmd -match "gh pr create")        { $label = "gh pr create" }
elseif ($cmd -match "git push")            { $label = "git push" }
else { exit 0 }

# Caminho do log
$repoRoot = git rev-parse --show-toplevel 2>$null
if ([string]::IsNullOrWhiteSpace($repoRoot)) { exit 0 }
$MetricsLog = Join-Path $repoRoot ".claude\metrics.log"

$branch = git branch --show-current 2>$null

# Extrair duracao_ms do tool_response se disponivel (pode ser null)
$duracaoMs = $null
if ($data.PSObject.Properties['tool_response']) {
    if ($data.tool_response.PSObject.Properties['duration_ms']) {
        $duracaoMs = $data.tool_response.duration_ms
    }
}

$entry = [PSCustomObject]@{
    ts         = [DateTimeOffset]::UtcNow.ToString("yyyy-MM-ddTHH:mm:ssZ")
    step       = $label
    cmd        = $cmd.Substring(0, [Math]::Min(120, $cmd.Length))
    branch     = $branch
    duracao_ms = $duracaoMs
} | ConvertTo-Json -Compress

Add-Content -Path $MetricsLog -Value $entry -Encoding UTF8
exit 0
