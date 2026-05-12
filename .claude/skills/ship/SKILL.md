---
name: ship
description: Orquestra entrega de PR: verifica estado, roda check.ps1 (gate completo), push, cria PR com titulo do ultimo commit e lista de commits da branch. Sem argumento necessario.
disable-model-invocation: true
---

Voce deve entregar a branch atual como PR no GitHub. Execute todos os passos em ordem.
Pare e reporte ao operador se qualquer verificacao ou comando falhar.

## Passo 0 -- Verificacoes de seguranca (ADR-011)

Execute cada verificacao via Bash (PowerShell). Se qualquer uma falhar: reporte
exatamente qual falhou e termine sem fazer push ou PR.

**V1 -- branch nao e main:**
```powershell
$branch = git branch --show-current
if ($branch -eq "main") {
    Write-Host "ERRO: voce esta em main. /ship opera apenas em branches de feature."
    exit 1
}
Write-Host "Branch: $branch"
```

**V2 -- working tree limpa:**
```powershell
$status = git status --porcelain
if ($status) {
    Write-Host "ERRO: working tree nao esta limpa. Commite ou descarte as mudancas antes de /ship."
    Write-Host $status
    exit 1
}
Write-Host "Working tree: limpa"
```

**V3 -- ha commits acima de main:**
```powershell
$ahead = git rev-list --count main..HEAD
if ($ahead -eq 0) {
    Write-Host "ERRO: nenhum commit acima de main. Nao ha nada para shipar."
    exit 1
}
Write-Host "Commits acima de main: $ahead"
```

**V4 -- gh CLI disponivel e autenticado:**
```powershell
gh auth status 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERRO: gh CLI nao autenticado. Rode 'gh auth login' antes de /ship."
    exit 1
}
Write-Host "gh CLI: autenticado"
```

## Passo 1 -- Gate completo

```powershell
.\scripts\check.ps1
```

Se exit code != 0: escreva "ERRO: gate falhou (check.ps1 exit $LASTEXITCODE). Corrija
antes de shipar." e termine. Nao faca push.

ATENCAO: check.ps1 exige Docker Desktop rodando (Testcontainers). Se Docker nao
estiver ativo, o gate falhara na verificacao de Docker antes de qualquer teste.

## Passo 2 -- Push

```powershell
$branch = git branch --show-current
git push -u origin $branch
```

Se exit code != 0: escreva "ERRO: push falhou." com o output do git e termine.
Nao crie PR.

## Passo 3 -- Extrair informacoes para o PR

```powershell
$titulo = git log -1 --format="%s"
$commits = git log main..HEAD --oneline
$branch = git branch --show-current
```

## Passo 4 -- Criar PR

Monte o body do PR com a lista de commits e o aviso padrao.
Execute:

```powershell
gh pr create `
  --title $titulo `
  --base main `
  --body "## Commits nesta branch

$commits

---
_Gerado via /ship. Adicione detalhes de decisao e smoke se necessario._"
```

Se exit code != 0: escreva "ERRO: gh pr create falhou." com o output e termine.

## Passo 5 -- Relatorio final

Produza:

```
/ship concluido.

Branch:   <branch>
Gate:     check.ps1 passou
Push:     OK
PR:       <URL retornada pelo gh pr create>
Titulo:   <titulo>
Commits:  <numero> acima de main

Proximos passos sugeridos:
  /review-pr <numero-do-pr>   -- revisao critica antes do merge
  /review-arch <numero-do-pr> -- validacao de ADRs arquiteturais
```
