$ErrorActionPreference = "Stop"

$prev = $ErrorActionPreference
$ErrorActionPreference = "Continue"
$staged = git diff --cached --name-only 2>&1
$gitExit = $LASTEXITCODE
$ErrorActionPreference = $prev

if ($gitExit -ne 0) {
    Write-Host "[maven-central-versions] Erro ao obter arquivos staged." -ForegroundColor Red
    exit 1
}

$stagedNorm = @($staged | ForEach-Object { $_ -replace '\\', '/' })
if ($stagedNorm -notcontains "pom.xml") { exit 0 }

# Parsear pom.xml
[xml]$pom = Get-Content "pom.xml" -Raw -Encoding UTF8

# Coletar artefatos com versao explicita
$toCheck = [System.Collections.Generic.List[hashtable]]::new()

foreach ($plugin in @($pom.project.build.plugins.plugin)) {
    if ($plugin.version -and $plugin.version -notmatch '^\$\{') {
        $toCheck.Add(@{ g = $plugin.groupId; a = $plugin.artifactId; v = $plugin.version })
    }
}

# annotationProcessorPaths: o nodo pode estar em qualquer plugin; iterar todos
foreach ($plugin in @($pom.project.build.plugins.plugin)) {
    $paths = $plugin.configuration.annotationProcessorPaths.path
    foreach ($path in @($paths)) {
        if ($path.version -and $path.version -notmatch '^\$\{') {
            $toCheck.Add(@{ g = $path.groupId; a = $path.artifactId; v = $path.version })
        }
    }
}

foreach ($dep in @($pom.project.dependencies.dependency)) {
    if ($dep.version -and $dep.version -notmatch '^\$\{') {
        $toCheck.Add(@{ g = $dep.groupId; a = $dep.artifactId; v = $dep.version })
    }
}

if ($toCheck.Count -eq 0) { exit 0 }

# Consultar Maven Central
$desatualizados = @()
foreach ($artifact in $toCheck) {
    try {
        $url = "https://search.maven.org/solrsearch/select?q=g:$($artifact.g)+AND+a:$($artifact.a)&rows=1&wt=json"
        $prev = $ErrorActionPreference
        $ErrorActionPreference = "Continue"
        $response = Invoke-RestMethod -Uri $url -TimeoutSec 5 2>$null
        $ErrorActionPreference = $prev
        if ($response.response.docs -and $response.response.docs.Count -gt 0) {
            $latest = $response.response.docs[0].latestVersion
            if ($latest -and $latest -ne $artifact.v) {
                $desatualizados += "$($artifact.g):$($artifact.a)  atual=$($artifact.v)  Maven Central=$latest"
            }
        }
    } catch {
        # rede indisponivel ou rate limit -- ignorar silenciosamente
    }
}

if ($desatualizados.Count -gt 0) {
    Write-Host ""
    Write-Host "[AVISO] maven-central-versions: versoes possivelmente desatualizadas em pom.xml:" -ForegroundColor Yellow
    foreach ($item in $desatualizados) {
        Write-Host "  $item" -ForegroundColor Yellow
    }
    Write-Host "Verifique se a versao mais recente e compativel antes de fixar." -ForegroundColor Yellow
    Write-Host ""
}

exit 0
