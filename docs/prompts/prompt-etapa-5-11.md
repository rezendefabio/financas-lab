# Prompt -- Sub-etapa 5.11: check-front.ps1 e integracao ao /ship

## Contexto

Gate de qualidade do frontend: roda lint + testes + build antes de abrir PR.
Motivacao: a 5.9 entregou bugs (CORS, origin hardcoded) que passariam pelo
check.ps1 do backend mas nao por um gate frontend. A skill /ship ja roda
`.\scripts\check.ps1` (Passo 1) para o backend -- a 5.11 adiciona um Passo 1.1
condicional que roda o gate frontend quando ha arquivos `frontend/` na branch.

Camada 4. Sem novo bounded context. Dois arquivos modificados, um criado.

---

## O que implementar

### Novo script: `scripts/check-front.ps1`

Seguir o padrao exato de `scripts/check.ps1` (leia antes de criar):
- `$ErrorActionPreference = "Stop"`
- Mensagens com `Write-Host ... -ForegroundColor Cyan/Green/Red`
- Capturar `$LASTEXITCODE` apos cada comando nativo
- `exit $exit` ao final

**Logica:**
```powershell
$ErrorActionPreference = "Stop"

$frontendPath = Join-Path $PSScriptRoot "..\frontend"

Write-Host "==> Gate frontend: lint..." -ForegroundColor Cyan
Set-Location $frontendPath
npm run lint
if ($LASTEXITCODE -ne 0) {
    Write-Host "Lint falhou (exit $LASTEXITCODE)." -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host "==> Gate frontend: testes..." -ForegroundColor Cyan
npm run test:run
if ($LASTEXITCODE -ne 0) {
    Write-Host "Testes falharam (exit $LASTEXITCODE)." -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host "==> Gate frontend: build..." -ForegroundColor Cyan
npm run build
$exit = $LASTEXITCODE

Write-Host ""
if ($exit -eq 0) {
    Write-Host "Gate frontend passou (lint + testes + build)." -ForegroundColor Green
} else {
    Write-Host "Build falhou (exit $exit). Veja o output acima." -ForegroundColor Red
}
exit $exit
```

**Ordem das verificacoes (fail-fast):**
1. `npm run lint` -- mais rapido, falha cedo em erros de codigo
2. `npm run test:run` -- vitest run (sem watch)
3. `npm run build` -- next build (mais lento, pega erros de tipo e config)

**Nota:** o script usa `Set-Location` para entrar em `frontend/` antes de rodar
os comandos npm. Nao usar `cd frontend &&` porque o `$ErrorActionPreference = "Stop"`
nao persiste atraves de subshells.

---

### Atualizar `.claude/skills/ship/SKILL.md`

Leia o arquivo completo antes de editar.

Adicionar **Passo 1.1** entre o Passo 1 (gate backend) e o Passo 2 (push).
O passo e **condicional** -- so roda se houver arquivos `frontend/` na branch:

```markdown
## Passo 1.1 -- Gate frontend (condicional)

Verifique se ha arquivos em `frontend/` entre os commits da branch:

```powershell
$frontendChanged = git diff --name-only main..HEAD | Where-Object { $_ -like "frontend/*" }
```

Se `$frontendChanged` for nao-nulo e nao-vazio: execute o gate frontend:

```powershell
.\scripts\check-front.ps1
```

Se exit code != 0: escreva "ERRO: gate frontend falhou (check-front.ps1 exit
$LASTEXITCODE). Corrija antes de shipar." e termine. Nao faca push.

Se `$frontendChanged` for nulo ou vazio: pule este passo (sem arquivos frontend
na branch, gate nao se aplica).
```

**Cuidado com a formatacao:** o SKILL.md usa headers `##` com linha em branco
antes e depois (hook markdown-blank-lines esta ativo). Respeitar esse padrao ao
inserir o novo bloco.

---

### Atualizar `docs/hooks-pendentes.md`

Na secao "Scripts de gate", adicionar entrada para `check-front.ps1`:
- Nome: **check-front.ps1**
- Sub-etapa: 5.11
- Caminho: `scripts/check-front.ps1`
- Comportamento: lint + test:run + build em `frontend/`, fail-fast, condicional no /ship
- Modo: **fail** (qualquer etapa com exit != 0 interrompe e nao faz push)

Se a secao "Scripts de gate" nao existir, criar antes da secao "Hooks implementados".
Leia o arquivo antes de editar.

---

## Validacao destrutiva

Executar os cenarios abaixo. Cada resultado deve ser documentado no body do PR.

**Cenario 1 -- script standalone passa (sem erros):**
```powershell
# Rodar diretamente o script
.\scripts\check-front.ps1
# Esperado: lint OK, testes OK, build OK, exit 0
```

**Cenario 2 -- /ship em branch sem frontend detecta skip:**
```powershell
# Criar branch de teste sem arquivos frontend
git checkout -b test/ship-backend-only
# (nao adicionar nenhum arquivo frontend)
# Verificar na saida do /ship que o Passo 1.1 imprime "sem arquivos frontend"
# ou simplesmente nao executa check-front.ps1
# Limpar: git checkout main && git branch -D test/ship-backend-only
```

**Cenario 3 -- verificar que npm run test:run passa no estado atual:**
```powershell
Set-Location frontend
npm run test:run
# Esperado: todos os testes existentes passando
Set-Location ..
```

Pre-condicao obrigatoria (ADR-011): `$LASTEXITCODE` verificado apos cada
operacao critica. `Test-Path scripts/check-front.ps1` antes do commit.

---

## Fluxo de execucao

```
1. git checkout -b feat/etapa-5-11-check-front

2. Ler antes de implementar:
   - scripts/check.ps1           (padrao do script)
   - .claude/skills/ship/SKILL.md (onde inserir Passo 1.1)
   - docs/hooks-pendentes.md     (formato de documentacao)

3. Criar scripts/check-front.ps1

4. Editar .claude/skills/ship/SKILL.md (inserir Passo 1.1)

5. Editar docs/hooks-pendentes.md (documentar script)

6. Executar os 3 cenarios de validacao destrutiva

7. commit: feat(claude): adiciona check-front.ps1 e integra ao /ship

8. Atualizar docs/progresso.md (registra 5.11)

9. commit: docs(progresso): registra sub-etapa 5.11
   (inclui docs/progresso.md + docs/prompts/prompt-etapa-5-11.md)

10. /ship -> PR com reviews; corrigir apontamentos autonomamente
```

---

## Estrutura de commits (5.11)

```
feat(claude): adiciona check-front.ps1 e integra ao /ship
docs(progresso): registra sub-etapa 5.11
```

---

## Restricoes

- NAO modificar `scripts/check.ps1` (gate backend nao muda).
- NAO rodar check-front.ps1 incondicionalmente no /ship -- deve ser condicional
  para nao penalizar PRs puramente de backend.
- `npm run build` inclui type checking do TypeScript -- nao e redundante com lint.
- Se `npm run test:run` falhar durante o desenvolvimento do script: investigar
  causa antes de commitar (nao contornar com --passWithNoTests ou similar).
- Hook markdown-blank-lines bloqueia headers sem linha em branco -- ao editar
  SKILL.md, garantir linha em branco antes e depois de cada `##`.

---

## Estado esperado ao terminar

- PR aberto com 2 commits acima de main.
- `.\scripts\check-front.ps1` rodando standalone com exit 0.
- `.\scripts\check-front.ps1` nao existente antes, existente depois
  (`Test-Path` confirma).
- Passo 1.1 visivel em `.claude/skills/ship/SKILL.md`.
- docs/hooks-pendentes.md com entrada de check-front.ps1 (5.11).
- docs/progresso.md com 5.11 registrada.

---

## O que NAO fazer ao terminar

- NAO fazer merge sem instrucao do operador.
- NAO rodar /ship mais de uma vez.
- NAO alterar a ordem lint -> test:run -> build (essa ordem e intencional:
  fail-fast do mais rapido para o mais lento).
