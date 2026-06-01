<#
.SYNOPSIS
  Backup completo da fabrica financas-lab: codigo-fonte do projeto + todas as
  configuracoes (locais gitignored + globais ~/.claude).

.DESCRIPTION
  Gera um .zip com timestamp contendo:
    projeto\        -> arvore do repositorio (inclui .git, .claude, docs/prompts;
                       exclui build artifacts pesados e regeneraveis)
    global-claude\  -> memoria da fabrica, settings.json global e config de plugins
    MANIFEST.txt    -> inventario, commit, branch e notas de restauracao

  NAO carrega o que nao precisa: node_modules, target, .next, .turbo, dist, out,
  bin, .testcontainers, .claude\worktrees.

.PARAMETER Destino
  Pasta onde o .zip sera criado. Default: %USERPROFILE%\fabrica-backups

.PARAMETER SemGit
  Se presente, NAO inclui a pasta .git (backup mais leve, sem historico).

.EXAMPLE
  .\scripts\backup-fabrica.ps1
  .\scripts\backup-fabrica.ps1 -Destino D:\backups -SemGit
#>
param(
    [string]$Destino = (Join-Path $env:USERPROFILE 'fabrica-backups'),
    [switch]$SemGit
)

# ADR-011: verificacao explicita de pre/pos-condicao em vez de confiar em
# "rodou sem erro". Nao usar $ErrorActionPreference=Stop por causa da
# incompatibilidade com stderr de comando nativo (lesson 2.6.2 / robocopy).

$projeto      = 'C:\projetos\financas-lab'
$claudeGlobal = Join-Path $env:USERPROFILE '.claude'
$memoria      = Join-Path $claudeGlobal 'projects\c--projetos-financas-lab\memory'

$stamp    = Get-Date -Format 'yyyyMMdd-HHmmss'
$nomeBase = "financas-lab-fabrica-$stamp"
$staging  = Join-Path $env:TEMP $nomeBase
# tar.gz (nao .zip): Compress-Archive do PS5.1 estoura no limite de 260 chars do
# Windows; o tar nativo (libarchive) lida com caminhos longos.
$arquivoFinal = Join-Path $Destino "$nomeBase.tar.gz"

Write-Host "== Backup da fabrica financas-lab =="
Write-Host "Projeto: $projeto"
Write-Host "Destino: $arquivoFinal"
Write-Host ""

# Pre-condicoes
if (-not (Test-Path $projeto)) { throw "Projeto nao encontrado: $projeto" }
New-Item -ItemType Directory -Force -Path $Destino | Out-Null
if (Test-Path $staging) { Remove-Item -Recurse -Force $staging }
New-Item -ItemType Directory -Force -Path $staging | Out-Null

# Info de git (transparencia). Suspende o tratamento de erro localmente para
# nao tropecar em stderr de comando nativo.
$branch = ''; $commit = ''; $sujo = ''
$eapAntigo = $ErrorActionPreference
$ErrorActionPreference = 'SilentlyContinue'
Push-Location $projeto
$branch = (& git rev-parse --abbrev-ref HEAD 2>$null)
$commit = (& git rev-parse HEAD 2>$null)
$sujo   = (& git status --porcelain 2>$null)
Pop-Location
$ErrorActionPreference = $eapAntigo
if ($sujo) { Write-Host "AVISO: working tree com alteracoes nao commitadas (serao incluidas no backup)." }

# --- Balde 1: arvore do projeto (robocopy com exclusoes) ---
Write-Host "Copiando projeto (sem build artifacts)..."
$destProjeto = Join-Path $staging 'projeto'
$excluirDirs = @('target','node_modules','.next','.turbo','dist','out','bin','.testcontainers','worktrees','.fabrica')
if ($SemGit) { $excluirDirs += '.git' }
$args = @($projeto, $destProjeto, '/E', '/NFL', '/NDL', '/NJH', '/NJS', '/NP', '/R:1', '/W:1')
foreach ($d in $excluirDirs) { $args += '/XD'; $args += $d }
& robocopy @args | Out-Null
if ($LASTEXITCODE -ge 8) { throw "robocopy do projeto falhou (exit $LASTEXITCODE)" }

# --- Balde 2: config global ~/.claude ---
Write-Host "Copiando config global (~/.claude: memory, settings, plugins)..."
$destGlobal = Join-Path $staging 'global-claude'
New-Item -ItemType Directory -Force -Path $destGlobal | Out-Null

if (Test-Path $memoria) {
    & robocopy $memoria (Join-Path $destGlobal 'memory') '/E' '/NFL' '/NDL' '/NJH' '/NJS' '/NP' | Out-Null
    if ($LASTEXITCODE -ge 8) { throw "robocopy da memory falhou (exit $LASTEXITCODE)" }
} else {
    Write-Host "AVISO: pasta de memory nao encontrada: $memoria"
}

$settingsGlobal = Join-Path $claudeGlobal 'settings.json'
if (Test-Path $settingsGlobal) { Copy-Item $settingsGlobal (Join-Path $destGlobal 'settings.json') -Force }

$destPlugins = Join-Path $destGlobal 'plugins'
New-Item -ItemType Directory -Force -Path $destPlugins | Out-Null
foreach ($f in @('installed_plugins.json','known_marketplaces.json','blocklist.json')) {
    $src = Join-Path $claudeGlobal "plugins\$f"
    if (Test-Path $src) { Copy-Item $src (Join-Path $destPlugins $f) -Force }
}
$mkt = Join-Path $claudeGlobal 'plugins\marketplaces'
if (Test-Path $mkt) {
    & robocopy $mkt (Join-Path $destPlugins 'marketplaces') '/E' '/NFL' '/NDL' '/NJH' '/NJS' '/NP' | Out-Null
}

# --- MANIFEST ---
$incluiGit = if ($SemGit) { 'NAO' } else { 'sim' }
$manifest = @"
Backup da fabrica financas-lab
==============================
Gerado em : $stamp
Branch    : $branch
Commit    : $commit
.git incl.: $incluiGit
Working tree com alteracoes nao commitadas: $([bool]$sujo)

Conteudo do arquivo
-------------------
  projeto\              arvore do repositorio (codigo, .git, .claude, docs/prompts,
                        scripts). Inclui os arquivos gitignored: .claude\settings.json,
                        .claude\factory-metrics.json, .claude\metrics.log, etc.
  global-claude\memory\ memoria da fabrica (~/.claude/projects/.../memory)
  global-claude\settings.json   config global do Claude Code
  global-claude\plugins\        config de plugins instalados

Excluido (regeneravel): $($excluirDirs -join ', ')

Restauracao
-----------
  1. Descompactar:  tar -xzf $nomeBase.tar.gz
  2. projeto\  -> restaurar em C:\projetos\financas-lab
     (ou: git clone do remoto + copiar de volta os arquivos gitignored de projeto\.claude\
      e projeto\docs\prompts\).
  3. global-claude\memory\      -> %USERPROFILE%\.claude\projects\c--projetos-financas-lab\memory\
  4. global-claude\settings.json -> %USERPROFILE%\.claude\settings.json
  5. Rodar scripts\setup.ps1 para reconfigurar core.hooksPath e os hooks.

ATENCAO: os settings.json (de projeto e global) podem conter tokens/segredos.
Trate este arquivo como SENSIVEL. Nao suba em repositorio publico nem em nuvem aberta.
"@
Set-Content -Path (Join-Path $staging 'MANIFEST.txt') -Value $manifest -Encoding UTF8

# --- Compactar (tar nativo do Windows -- lida com caminhos longos) ---
# Forcar o tar nativo (System32). Se apenas "tar" for usado, o PATH pode resolver
# para o tar do Git/MSYS, que interpreta "C:\..." como host:path e falha.
Write-Host "Compactando..."
$tarExe = Join-Path $env:SystemRoot 'System32\tar.exe'
if (-not (Test-Path $tarExe)) { throw "tar nativo nao encontrado em $tarExe (requer Windows 10 1803+)" }
if (Test-Path $arquivoFinal) { Remove-Item -Force $arquivoFinal }
& $tarExe -czf "$arquivoFinal" -C "$staging" "." 2>$null
if ($LASTEXITCODE -ge 2) { throw "tar falhou (exit $LASTEXITCODE)" }

# Pos-condicao
if (-not (Test-Path $arquivoFinal)) { throw "Falha: backup nao foi criado em $arquivoFinal" }
$tamMB = [math]::Round((Get-Item $arquivoFinal).Length / 1MB, 1)
Remove-Item -Recurse -Force $staging

Write-Host ""
Write-Host "OK. Backup criado:"
Write-Host "  $arquivoFinal  ($tamMB MB)"
Write-Host "Restaurar com: tar -xzf $nomeBase.tar.gz"
Write-Host "Lembrete: contem settings.json (possiveis segredos). Guarde com seguranca."
