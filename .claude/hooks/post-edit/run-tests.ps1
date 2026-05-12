# .claude/hooks/post-edit/run-tests.ps1
# Hook PostToolUse: roda unit test quando arquivo em */domain/*.java e editado.
# Silencioso se nao ha teste correspondente.

param()

$ErrorActionPreference = "Continue"

$stdin = [Console]::In.ReadToEnd()
if (-not $stdin) { exit 0 }

try {
    $data = $stdin | ConvertFrom-Json
} catch {
    exit 0
}

$filePath = $data.tool_input.file_path
if (-not $filePath) { exit 0 }

# Apenas arquivos em */domain/*.java dentro de src/main/java/
if ($filePath -notmatch 'src[/\\]main[/\\]java[/\\].*[/\\]domain[/\\][^/\\]+\.java$') {
    exit 0
}

$className = [System.IO.Path]::GetFileNameWithoutExtension($filePath)
$testFilePath = $filePath -replace '(src[/\\])main([/\\]java[/\\])', '$1test$2'
$testFilePath = $testFilePath -replace '\.java$', 'Test.java'

if (-not (Test-Path $testFilePath)) {
    exit 0
}

$testClassName = "${className}Test"
Write-Host "[post-edit] Rodando $testClassName..."

[System.Environment]::CurrentDirectory = (Get-Location).Path
$output = & .\mvnw test "-Dtest=$testClassName" 2>&1
$exit = $LASTEXITCODE

if ($exit -eq 0) {
    Write-Host "[post-edit] $testClassName PASSOU"
} else {
    Write-Host "[post-edit] $testClassName FALHOU:"
    $output | ForEach-Object { Write-Host "  $_" }
}

exit 0
