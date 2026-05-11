# Prompt — Etapa 2.6.2: Fix de stderr de comando nativo sob `$ErrorActionPreference = "Stop"`

## Contexto

A Etapa 2.6.1 foi concluída e fechada via PR #24. `main` está em `5f249aa`.

Durante validação manual da 2.6.1 (rodar `dev.ps1` com Docker parado), o operador descobriu **bug adicional** no padrão de checagem de Docker:

- Saída no terminal mostra erro nativo do `docker info` + stack trace do PowerShell, em vez da mensagem amigável esperada.
- A mensagem `Write-Host "Docker Desktop nao esta rodando..." -ForegroundColor Red` configurada no script é **engolida** — nunca aparece.
- Funcionalmente o script para com `$LASTEXITCODE = 1` (correto), mas a UX é ruim e poluída.

**Diagnóstico confirmado por testes em terminal direto:**

PowerShell sob `$ErrorActionPreference = "Stop"` intercepta stderr de comandos nativos **antes** que operadores de redirecionamento (`2>&1 | Out-Null`, `2>$null`, `2>&1 > $null`) possam suprimir. Resultado: erro nativo vaza pra tela com stack trace do PowerShell, antes do `if ($LASTEXITCODE -ne 0)` checar o resultado. As 3 tentativas testadas no terminal (Tentativa A, D e variantes de redirecionamento) falharam. **A única solução que funcionou foi suspender `Stop` localmente durante a checagem.**

**Solução validada (Tentativa C):**

```powershell
$prev = $ErrorActionPreference
$ErrorActionPreference = "Continue"
docker info 2>&1 | Out-Null
$ErrorActionPreference = $prev

if ($LASTEXITCODE -ne 0) {
    Write-Host "Docker Desktop nao esta rodando..." -ForegroundColor Red
    exit 1
}
```

Saída limpa, sem stack trace. `$LASTEXITCODE = 1` quando Docker está parado.

Esta etapa aplica esse padrão nos 3 scripts afetados (`dev.ps1`, `test-integration.ps1`, `check.ps1`). Cirúrgica como a 2.6.1.

**`ship.ps1` não está no escopo.** Validação manual confirmou que os caminhos de erro funcionam limpos (working tree sujo + branch == main). Comandos `git` parecem se comportar diferente de `docker` em relação a stderr sob `Stop`, ou os caminhos de erro do `ship.ps1` simplesmente não exercitam stderr de comando nativo. Não tocar.

**`setup.ps1` e `test.ps1` não estão no escopo.** Não têm padrão `docker info`.

## Estado esperado ao iniciar

- Branch atual: `main`, sincronizada com `origin/main`
- `git log --oneline -1` mostra `5f249aa fix: etapa 2.6.1 — exit code correto em scripts PowerShell (#24)`
- `docs/prompt-etapa-2-6-2.md` presente como untracked (este próprio arquivo, anexado pelo operador antes de iniciar). Se não aparecer em `git status`, parar e reportar.
- Working tree sem outras mudanças

Validar com `git status` e `git log --oneline -1` antes de começar.

## Tarefas

### Tarefa 1 — Aplicar padrão "suspender Stop localmente" em `dev.ps1`

Localizar o bloco em `scripts/dev.ps1`:

```powershell
# Verifica Docker rodando antes de tentar subir compose.
docker info 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host "Docker Desktop nao esta rodando. Inicie o Docker e tente novamente." -ForegroundColor Red
    exit 1
}
```

Substituir por:

```powershell
# Verifica Docker rodando antes de tentar subir compose.
# Suspende Stop localmente para evitar que stderr nativo do docker vaze (PowerShell + Stop intercepta stderr antes de redirecionamento).
$prev = $ErrorActionPreference
$ErrorActionPreference = "Continue"
docker info 2>&1 | Out-Null
$ErrorActionPreference = $prev
if ($LASTEXITCODE -ne 0) {
    Write-Host "Docker Desktop nao esta rodando. Inicie o Docker e tente novamente." -ForegroundColor Red
    exit 1
}
```

**Manter intactos:**
- Comentário original (a primeira linha `# Verifica Docker rodando...` permanece — só é adicionado o segundo comentário com a explicação técnica).
- `docker info 2>&1 | Out-Null` (não muda o comando, só envolve com suspensão de Stop).
- A mensagem do `Write-Host` (idêntica).
- O `exit 1`.

### Tarefa 2 — Aplicar mesmo padrão em `test-integration.ps1`

Localizar o bloco em `scripts/test-integration.ps1`:

```powershell
# Verifica Docker rodando (Testcontainers precisa).
docker info 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host "Docker Desktop nao esta rodando. Testcontainers precisa do Docker." -ForegroundColor Red
    exit 1
}
```

Substituir pelo mesmo padrão:

```powershell
# Verifica Docker rodando (Testcontainers precisa).
# Suspende Stop localmente para evitar que stderr nativo do docker vaze (PowerShell + Stop intercepta stderr antes de redirecionamento).
$prev = $ErrorActionPreference
$ErrorActionPreference = "Continue"
docker info 2>&1 | Out-Null
$ErrorActionPreference = $prev
if ($LASTEXITCODE -ne 0) {
    Write-Host "Docker Desktop nao esta rodando. Testcontainers precisa do Docker." -ForegroundColor Red
    exit 1
}
```

### Tarefa 3 — Aplicar mesmo padrão em `check.ps1`

Localizar o bloco em `scripts/check.ps1`:

```powershell
# Verifica Docker rodando (Testcontainers precisa).
docker info 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host "Docker Desktop nao esta rodando. Testcontainers precisa do Docker." -ForegroundColor Red
    exit 1
}
```

Substituir pelo mesmo padrão (idêntico ao da `test-integration.ps1`):

```powershell
# Verifica Docker rodando (Testcontainers precisa).
# Suspende Stop localmente para evitar que stderr nativo do docker vaze (PowerShell + Stop intercepta stderr antes de redirecionamento).
$prev = $ErrorActionPreference
$ErrorActionPreference = "Continue"
docker info 2>&1 | Out-Null
$ErrorActionPreference = $prev
if ($LASTEXITCODE -ne 0) {
    Write-Host "Docker Desktop nao esta rodando. Testcontainers precisa do Docker." -ForegroundColor Red
    exit 1
}
```

### Tarefa 4 — Validação básica (sem destrutiva — operador valida manualmente depois)

Confirmar que as 3 substituições foram aplicadas corretamente:

```bash
# Esperado: cada arquivo deve ter EXATAMENTE 1 ocorrência de "ErrorActionPreference = \"Continue\""
grep -c 'ErrorActionPreference = "Continue"' scripts/dev.ps1 scripts/test-integration.ps1 scripts/check.ps1
# Esperado: cada um retorna 1
```

```bash
# Esperado: cada arquivo deve ter EXATAMENTE 1 "$ErrorActionPreference = $prev"
grep -c 'ErrorActionPreference = \$prev' scripts/dev.ps1 scripts/test-integration.ps1 scripts/check.ps1
# Esperado: cada um retorna 1
```

```bash
# Confirmar que NÃO entrou Continue em outros scripts:
grep -l 'ErrorActionPreference = "Continue"' scripts/*.ps1
# Esperado: APENAS os 3 acima (dev.ps1, test-integration.ps1, check.ps1)
```

```bash
# Encoding sem BOM:
xxd scripts/dev.ps1 | head -1
xxd scripts/test-integration.ps1 | head -1
xxd scripts/check.ps1 | head -1
# Esperado: cada um começa com 23 20 (# ), não EF BB BF
```

**Validação destrutiva real (Docker parado) é responsabilidade do operador**, executada manualmente em PowerShell após o merge. O agente não pode parar Docker do operador, e tentar simular outros caminhos é validação enviesada (igual o erro da 2.6.1).

### Tarefa 5 — Atualizar `decisoes.md`

**5a.** Localizar a subseção **"Scripts PowerShell"** existente (criada na 2.6.1). Adicionar bullet adicional **antes do último bullet** ("Manter `$ErrorActionPreference = "Stop"` no topo dos scripts..."):

```markdown
- **Suspender `Stop` localmente em checagens com comando nativo + redirecionamento.** Sob `$ErrorActionPreference = "Stop"`, comandos nativos (`docker`, `git`, `mvn`, etc) que escrevem em stderr fazem o PowerShell vazar o erro pra tela com stack trace, **antes** que operadores de redirecionamento (`2>&1`, `2>$null`, `2>&1 > $null`) possam suprimir. Não há sintaxe de redirecionamento que evite. Padrão correto:
  ```powershell
  $prev = $ErrorActionPreference
  $ErrorActionPreference = "Continue"
  docker info 2>&1 | Out-Null
  $ErrorActionPreference = $prev

  if ($LASTEXITCODE -ne 0) {
      Write-Host "mensagem clara do erro" -ForegroundColor Red
      exit 1
  }
  ```
  Aplicar este padrão sempre que o script chamar comando nativo com intenção de checar `$LASTEXITCODE` em vez de tratar como erro fatal.
```

**5b.** Adicionar linha no **"Histórico de mudanças"** no fim do arquivo, no topo da lista:

```markdown
- **2026-05-08** — Etapa 2.6.2 concluída: fix de UX em `dev.ps1`/`test-integration.ps1`/`check.ps1` — `docker info 2>&1 | Out-Null` sob `Stop` vazava stderr nativo + stack trace do PowerShell, engolindo a mensagem amigável. Aplicado padrão "suspender Stop localmente" em torno do `docker info`. Regra adicionada na seção "Scripts PowerShell".
```

### Tarefa 6 — Atualizar `progresso.md`

**6a.** Atualizar campo "Última atualização" no topo: `2026-05-08 (Etapa 2.6.2)`.

**6b.** Adicionar nova seção **"Lições da Etapa 2.6.2"** logo antes de **"Lições da Etapa 2.6.1"** (mantendo ordem decrescente):

```markdown
## Lições da Etapa 2.6.2

### Candidatos a hook (automatizar em etapas futuras)

1. Detectar comando nativo (`docker`, `git`, `mvn`, etc) seguido de `if ($LASTEXITCODE -ne 0)` em arquivos `.ps1` **sem** ser precedido por suspensão local de `$ErrorActionPreference`. Indica risco do mesmo bug que esta etapa corrigiu. Hook leve futuro: `grep -B1 -A2 "if (\$LASTEXITCODE" scripts/*.ps1` revisado caso a caso.

### Lições de ambiente

1. **`$ErrorActionPreference = "Stop"` + comando nativo + redirecionamento de stderr é incompatível em PowerShell.** Os operadores de redirecionamento (`2>&1`, `2>$null`, `2>&1 > $null`, `2>&1 | Out-Null`) **não são aplicados antes** do `Stop` interceptar stderr de comando nativo. Resultado: erro vaza pra tela com stack trace do PowerShell. Testado: 3 variantes de redirecionamento falharam. Única solução prática: suspender `Stop` localmente durante a checagem.
2. **Validação manual continua descobrindo bugs que automação não pega.** A 2.6.1 corrigiu um bug parecido (`Write-Error` + `exit` sob `Stop`), e foi descoberta na validação manual destrutiva. A 2.6.2 corrigiu outro bug do mesmo padrão raiz (`Stop` + comando que escreve stderr), descoberto também em validação manual destrutiva (rodar `dev.ps1` com Docker parado). Conclusão reforçada: validação manual destrutiva é instrumento de qualidade de primeira linha, não opcional.
3. **Diagnóstico via teste em terminal direto > inferência.** A solução final foi descoberta por reprodução isolada no terminal (3 tentativas, comparação de comportamento com/sem `Stop`). Sem reprodução isolada, teria ficado tentando varia com `2>&1` infinitamente. Padrão pra debugar comportamento confuso de PowerShell: reproduzir linha-a-linha no terminal direto antes de mexer em script.
```

**6c.** Adicionar entrada no **"Histórico de mudanças deste documento"** no topo da lista:

```markdown
- **2026-05-08** — Etapa 2.6.2 concluída: fix de UX em checagem de Docker nos scripts `.ps1`. Aplicado padrão "suspender `Stop` localmente" em `dev.ps1`/`test-integration.ps1`/`check.ps1`. Regra adicionada em `decisoes.md`. Mergeado via PR #XX.
```

### Tarefa 7 — Versionar este próprio prompt

Confirmar que `docs/prompt-etapa-2-6-2.md` está em disco como untracked e incluir no commit de docs.

## Restrições e freios

1. **Não tocar em arquivos fora do escopo.** Arquivos permitidos:
   - `scripts/dev.ps1` (substituição)
   - `scripts/test-integration.ps1` (substituição)
   - `scripts/check.ps1` (substituição)
   - `docs/decisoes.md`
   - `docs/progresso.md`
   - `docs/prompt-etapa-2-6-2.md` (este arquivo)

2. **Não tocar em `setup.ps1`, `test.ps1`, `ship.ps1`.** Os três não têm padrão `docker info` afetado por este bug. Confirmar via `grep "docker info" scripts/*.ps1` que apenas `dev.ps1`, `test-integration.ps1` e `check.ps1` aparecem — se aparecer outro, parar e reportar.

3. **Não tocar em `pom.xml`, `ci.yml`, `README.md`, `application.yml`, ou qualquer outro arquivo.** Etapa cirúrgica.

4. **Não tentar "padrões alternativos" do tipo `try/catch`, função helper, ou wrapper.** O padrão escolhido é o validado em terminal direto. Outras abordagens (que parecem mais elegantes em vácuo) podem ter sutis problemas que a Tentativa C não tem. Em particular: **não criar `Test-DockerRunning` em arquivo compartilhado** — a 2.6 explicitamente decidiu contra shared lib em scripts.

5. **Não mudar mensagens de `Write-Host`, exit codes, comentários originais ou ordem dos demais comandos.**

6. **Não tocar em `$ErrorActionPreference = "Stop"` no topo dos scripts.** Mantém. A regra adicionada em `decisoes.md` documenta por quê — só é suspenso **localmente** em torno do `docker info`.

7. **Encoding obrigatório UTF-8 sem BOM.** Validar com `xxd scripts/<arquivo>.ps1 | head -1` antes de commitar — primeiros bytes `23 20` (`# `), não `EF BB BF`.

8. **Não executar `.ps1` no `bash_tool` para validar.** Validação destrutiva real (Docker parado) é do operador. O agente faz apenas validação estática (grep, encoding). Tentar simular Docker parado dentro do bash_tool é caminho de bugs (não dá pra parar o Docker do host de dentro do agente).

9. **`bash_tool` é bash, não PowerShell.** Comandos de validação do agente em sintaxe POSIX/bash.

10. **Não antecipar Etapa 2.7 (Next.js).** Não criar `frontend/`. Não rodar `create-next-app`.

11. **Não inventar correções para `ship.ps1`.** Os comandos `git` em `ship.ps1` parecem se comportar diferente de `docker` em relação a stderr sob `Stop` — validação manual da 2.6.1 confirmou que os caminhos de erro funcionam limpos. Aplicar o mesmo padrão preventivamente em `ship.ps1` é mexer em código funcional sem necessidade. Se aparecer problema futuro, vira 2.6.3.

## Estrutura de commits

Branch: `fix/docker-check-stderr`

Commits atômicos, em ordem:

**Commit 1** — `fix: suspende Stop localmente em checagem de docker info nos scripts .ps1`
- `scripts/dev.ps1`
- `scripts/test-integration.ps1`
- `scripts/check.ps1`

**Commit 2** — `docs: registra etapa 2.6.2 (fix stderr docker check) em decisoes e progresso`
- `docs/decisoes.md`
- `docs/progresso.md`
- `docs/prompt-etapa-2-6-2.md`

## Validação antes de abrir PR

```bash
# 3 ocorrências de "Continue" (exatamente uma em cada script):
grep -c 'ErrorActionPreference = "Continue"' scripts/dev.ps1 scripts/test-integration.ps1 scripts/check.ps1

# 3 ocorrências de "$prev":
grep -c 'ErrorActionPreference = \$prev' scripts/dev.ps1 scripts/test-integration.ps1 scripts/check.ps1

# Confirma que outros scripts NÃO foram tocados:
grep -l 'ErrorActionPreference = "Continue"' scripts/*.ps1
# Esperado: apenas dev.ps1, test-integration.ps1, check.ps1

# Confirma que docker info aparece nos 3 esperados e em mais nenhum:
grep -l "docker info" scripts/*.ps1
# Esperado: apenas dev.ps1, test-integration.ps1, check.ps1

# Encoding limpo:
xxd scripts/dev.ps1 | head -1
xxd scripts/test-integration.ps1 | head -1
xxd scripts/check.ps1 | head -1

# Working tree esperado:
git status
git log --oneline -3
```

## PR

Título: `fix: etapa 2.6.2 — UX limpa em checagem de Docker nos scripts PowerShell`

Body sugerido:

```markdown
## Summary

Fix de UX descoberto em validação manual da Etapa 2.6.1: rodar `dev.ps1` com Docker parado fazia o stderr nativo do `docker info` vazar pra tela junto com stack trace do PowerShell, engolindo a mensagem amigável `"Docker Desktop nao esta rodando..."`.

Causa raiz: `$ErrorActionPreference = "Stop"` + comando nativo + redirecionamento de stderr é incompatível em PowerShell. O `Stop` intercepta stderr **antes** que `2>&1 | Out-Null` (ou similar) possa suprimir.

### Mudanças

3 substituições mecânicas em 3 scripts. Aplicado padrão "suspender `Stop` localmente":

```powershell
$prev = $ErrorActionPreference
$ErrorActionPreference = "Continue"
docker info 2>&1 | Out-Null
$ErrorActionPreference = $prev
```

- `dev.ps1` (1 ocorrência)
- `test-integration.ps1` (1 ocorrência)
- `check.ps1` (1 ocorrência)
- `setup.ps1`, `test.ps1`, `ship.ps1` não tocados (não têm o padrão).
- `decisoes.md`: bullet adicional na seção "Scripts PowerShell" formalizando a regra.
- `progresso.md`: lições registradas.

### Diagnóstico

Validado em terminal direto. 3 alternativas de redirecionamento (`2>$null`, `2>&1 > $null`, `2>&1 | Out-Null`) falharam — todas vazavam stderr + stack trace sob `Stop`. Apenas suspender `Stop` localmente funcionou. Comportamento conhecido (e documentado como confuso) do PowerShell.

### Validação

- Substituições confirmadas via grep.
- Encoding sem BOM via xxd.
- **Validação destrutiva (Docker parado) é do operador**, manualmente em PowerShell, após o merge.

### Por que isso importa

Bug de UX, não de funcionalidade — o script ainda parava com exit 1. Mas a saída poluída + mensagem amigável engolida quebra a "documentação executável" da fábrica: scripts que devem ser auto-explicativos estavam mostrando dump técnico em vez de mensagem clara. Vale o ajuste pequeno.

### Próximo passo

Etapa 2.7 (inicializar Next.js) — fora do escopo deste PR.
```

## Pós-criação do PR

1. Abrir o PR via `gh pr create`.
2. Capturar o número.
3. Editar `docs/progresso.md` substituindo `Mergeado via PR #XX` por `Mergeado via PR #<numero-real>`.
4. Commit: `docs: atualiza numero do PR no historico do progresso.md`
5. Push.
6. Esperar CI verde.
7. **Aguardar autorização explícita do operador antes do merge.**

## Estado esperado ao terminar

- `main` local sincronizada com `origin/main`, incluindo squash commit da 2.6.2
- `git status` limpo
- 3 scripts em `scripts/` com padrão "suspender Stop localmente" aplicado
- `decisoes.md` com bullet adicional na seção "Scripts PowerShell"
- `progresso.md` reflete 2.6.2 concluída, número real do PR no histórico
- Branch `fix/docker-check-stderr` deletada local e remotamente

Reportar ao operador o estado final com `git log --oneline -3` e `git status`. Parar.

## O que NÃO fazer ao terminar

- Não criar prompt da 2.7
- Não criar `frontend/`, não rodar `create-next-app`
- Não tocar em `ship.ps1`, `setup.ps1` ou `test.ps1`
- Não criar funções helper / shared lib
- Não sugerir "próximo passo" espontaneamente
