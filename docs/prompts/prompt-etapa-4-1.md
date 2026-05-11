# Prompt — Etapa 4.1: Hook universal de Conventional Commits (primeiro hook funcional da Camada 3)

> **Nota pós-execução:** o entrypoint prescrito em `.githooks/commit-msg` originalmente chamava `pwsh` (PowerShell Core 7). Durante a execução, descobriu-se que `pwsh` não está instalado neste ambiente — apenas `powershell` (Windows PowerShell 5.1, nativo no Windows). Ajuste aplicado: `pwsh` → `powershell` em `.githooks/commit-msg` e em `.githooks/README.md` (criado na 4.0, referência para sub-etapas seguintes). Scripts `.ps1` confirmados compatíveis com PS5.1. **Sub-etapas seguintes da Camada 3 devem usar `powershell` por default em entrypoints, não `pwsh`.** Lição completa registrada em `docs/progresso.md` (seção "Lições da Sub-etapa 4.1").

## Contexto

A Sub-etapa 4.0 (PR #38) estabeleceu a infraestrutura organizacional da Camada 3: estrutura `.claude/` separada por escopo, `.githooks/` com `core.hooksPath` configurado pelo `setup.ps1`, ADR-009 e ADR-010 registrados, triagem completa do `hooks-pendentes.md`. A Sub-etapa 4.0.1 (PR #39) corrigiu posição do bloco `core.hooksPath` no `setup.ps1` para sobreviver a falha de Docker.

**Esta sub-etapa entrega o primeiro hook funcional do projeto.** Não é só "mais um item da lista" — é a prova de que a infraestrutura criada pela 4.0 funciona end-to-end. Estabelece o **padrão de 3 camadas** que toda sub-etapa seguinte da Camada 3 vai reusar:

```
.githooks/commit-msg                          ← entrypoint sem extensão (wrapper bash)
.githooks/commit-msg.ps1                      ← companheiro PowerShell (fino, delegador)
.claude/hooks/universal/conventional-commits.ps1   ← lógica real (portável)
```

Escolha do Conventional Commits como **primeiro** hook foi calibrada com o operador: usa o entrypoint `commit-msg` (não `pre-commit`) que é o mais simples (um argumento, lê arquivo, valida regex), validação destrutiva é unidimensional (commit válido vs inválido), não interage com diff staged, e é universal absoluto (vale pra qualquer repo). Caso laboratório ideal pra estrear o mecanismo.

Quando esta etapa terminar:

1. Tentar commit com mensagem fora do padrão **bloqueia** localmente.
2. Tentar commit com mensagem válida **passa** sem fricção.
3. `git commit --no-verify` é escape válido documentado em `decisoes.md`.
4. Próximas sub-etapas (4.2 UTF-8, 4.3 Markdown, 4.4 tamanho docs) reusam o padrão de 3 camadas — só adicionam hooks em `.claude/hooks/universal/` e entrypoints em `.githooks/`.

## Padrões que estreiam nesta etapa

1. **Primeiro hook funcional do projeto** — gate mecânico real ativo no fluxo de commit, substituindo prosa instrutiva.
2. **Padrão de 3 camadas estabelecido** — entrypoint bash sem extensão → companheiro `.ps1` em `.githooks/` → hook real em `.claude/hooks/<escopo>/`. Replicável em 4.2+.
3. **Primeiro entrypoint executável** — `.githooks/commit-msg` registrado com bit de execução no git index via `git update-index --chmod=+x` (lição da Etapa 1.5).
4. **Primeira remoção de `.gitkeep`** — `.claude/hooks/universal/.gitkeep` removido quando primeiro arquivo real entra na pasta. Padrão idiomático do git.
5. **Validação destrutiva como condição de "pronto" no roteiro do agente** (já estreou na 4.0.1, agora vira padrão consolidado da Camada 3) — agente reproduz cenário de falha + cenário de sucesso + cenário de bypass antes de declarar etapa concluída.

## Escopo decidido (calibrado com operador antes da redação)

### Conventional Commits — formato aceito

```
<tipo>[(scope)][!]: <descrição>
```

**Tipos permitidos:** `feat`, `fix`, `chore`, `docs`, `test`, `refactor`, `style`, `perf`, `build`, `ci`.

**Scope:** opcional. Lowercase + dígitos + hífen entre parênteses (ex: `feat(transacao):`, `chore(scripts):`, `feat(api-v2):`).

**Breaking change:** opcional, indicado por `!` antes do `:` (ex: `feat!:`, `feat(api)!:`, `chore!:`).

**Descrição:** mínimo 10 caracteres após o `: `.

**Exemplos válidos:**

```
feat: adiciona endpoint de saldo
feat(conta): adiciona endpoint de saldo
fix(setup): corrige posicao do bloco hooks
feat!: muda schema da API de transacoes
feat(api)!: muda schema da API de transacoes
chore(scripts): atualiza setup.ps1
docs: registra licoes da sub-etapa 4.1
```

**Exemplos inválidos (devem ser bloqueados):**

```
wip                                            ← não tem tipo
feature: adiciona endpoint                     ← tipo errado (feature em vez de feat)
feat: x                                        ← descrição com 1 char (mínimo 10)
Update README                                  ← não segue formato
feat (conta): adiciona endpoint                ← espaço entre tipo e scope
```

**Exceções automáticas (passam sem validação):**

- Mensagens começando com `Merge ` (merge commits gerados pelo git automaticamente).
- Mensagens começando com `Revert ` (revert commits gerados pelo git).

**Override consciente:**

`git commit --no-verify` permitido em emergências. Cada uso deve ser registrado no PR body com motivo. Documentado em `decisoes.md`.

### Regex do hook

```
^(feat|fix|chore|docs|test|refactor|style|perf|build|ci)(\([a-z0-9-]+\))?!?: .{10,}$
```

Aplicada à **primeira linha não-vazia, não-comentário** do arquivo de mensagem.

### Arquivos criados e modificados

```
.claude/hooks/universal/conventional-commits.ps1      ← novo (lógica real)
.claude/hooks/universal/.gitkeep                      ← removido
.githooks/commit-msg                                  ← novo (entrypoint bash, sem extensão)
.githooks/commit-msg.ps1                              ← novo (companheiro PowerShell)
docs/decisoes.md                                      ← edição (regra Conventional Commits + override)
docs/hooks-pendentes.md                               ← edição (move item implementado para seção nova)
docs/progresso.md                                     ← edição (lições + sub-etapa + histórico)
docs/prompt-etapa-4-1.md                              ← novo (este próprio prompt)
```

### Conteúdo de `.githooks/commit-msg` (entrypoint bash, sem extensão)

```bash
#!/usr/bin/env bash
set -e
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
pwsh -NoProfile -ExecutionPolicy Bypass -File "$SCRIPT_DIR/commit-msg.ps1" "$@"
```

**Notas críticas:**

1. **Sem extensão no nome.** O git invoca exatamente o nome `commit-msg`. Arquivo com extensão (`commit-msg.sh`) não funciona.
2. **Shebang `#!/usr/bin/env bash`.** Git no Windows usa Git Bash que interpreta shebangs.
3. **Line endings LF.** O `.gitattributes` do projeto cuida disso para arquivos sem extensão. Validar após criação.
4. **Bit de execução no git index** — registrado via `git update-index --chmod=+x .githooks/commit-msg` após `git add`. Sem isso, em Linux/Mac (futuros contribuidores, CI multi-OS) o git não invoca o hook.
5. **Passa `"$@"` para o companheiro** — preserva o argumento (caminho do arquivo de mensagem) recebido do git.

### Conteúdo de `.githooks/commit-msg.ps1` (companheiro PowerShell)

```powershell
$ErrorActionPreference = "Stop"

$messageFile = $args[0]
if (-not $messageFile) {
    Write-Host "[ERRO] commit-msg hook chamado sem argumento (caminho do arquivo de mensagem)." -ForegroundColor Red
    exit 1
}

$repoRoot = git rev-parse --show-toplevel
if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERRO] Nao foi possivel identificar o repo root." -ForegroundColor Red
    exit 1
}

$hookPath = Join-Path $repoRoot ".claude\hooks\universal\conventional-commits.ps1"
if (-not (Test-Path $hookPath)) {
    Write-Host "[ERRO] Hook nao encontrado: $hookPath" -ForegroundColor Red
    exit 1
}

& $hookPath $messageFile
exit $LASTEXITCODE
```

**Notas:**

- Arquivo fino, sem lógica de validação. Responsabilidade: descobrir o repo root, achar o hook em `.claude/hooks/universal/`, invocar com o argumento original, propagar exit code.
- Padrão replicável em 4.2+: cada entrypoint companheiro só descobre e delega.

### Conteúdo de `.claude/hooks/universal/conventional-commits.ps1` (lógica real)

```powershell
$ErrorActionPreference = "Stop"

$messageFile = $args[0]
if (-not (Test-Path $messageFile)) {
    Write-Host "[ERRO] Arquivo de mensagem nao encontrado: $messageFile" -ForegroundColor Red
    exit 1
}

$content = Get-Content $messageFile -Raw -Encoding UTF8
if (-not $content) {
    Write-Host "[ERRO] Arquivo de mensagem vazio." -ForegroundColor Red
    exit 1
}

# Primeira linha nao-vazia, ignorando comentarios (linhas iniciadas por #)
$firstLine = ""
foreach ($line in ($content -split "`n")) {
    $trimmed = $line.Trim()
    if ($trimmed -and -not $trimmed.StartsWith("#")) {
        $firstLine = $trimmed
        break
    }
}

if (-not $firstLine) {
    Write-Host "[ERRO] Mensagem de commit vazia (todas as linhas sao comentarios ou em branco)." -ForegroundColor Red
    exit 1
}

# Excecoes automaticas do git: merge e revert commits passam sem validacao
if ($firstLine.StartsWith("Merge ") -or $firstLine.StartsWith("Revert ")) {
    exit 0
}

# Conventional Commits: <tipo>(scope opcional)!?: <descricao com min 10 chars>
$pattern = '^(feat|fix|chore|docs|test|refactor|style|perf|build|ci)(\([a-z0-9-]+\))?!?: .{10,}$'

if ($firstLine -notmatch $pattern) {
    Write-Host ""
    Write-Host "[ERRO] Mensagem de commit nao segue Conventional Commits." -ForegroundColor Red
    Write-Host ""
    Write-Host "Mensagem rejeitada:" -ForegroundColor Yellow
    Write-Host "  $firstLine" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Formato esperado:" -ForegroundColor Cyan
    Write-Host "  <tipo>[(scope)][!]: <descricao com ao menos 10 caracteres>" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Tipos permitidos: feat, fix, chore, docs, test, refactor, style, perf, build, ci" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Exemplos validos:" -ForegroundColor Cyan
    Write-Host "  feat: adiciona endpoint de saldo" -ForegroundColor Green
    Write-Host "  feat(conta): adiciona endpoint de saldo" -ForegroundColor Green
    Write-Host "  fix(setup): corrige posicao do bloco hooks" -ForegroundColor Green
    Write-Host "  feat!: muda schema da API de transacoes" -ForegroundColor Green
    Write-Host ""
    Write-Host "Override consciente em emergencia: git commit --no-verify" -ForegroundColor Yellow
    Write-Host "(documentar uso em PR body conforme decisoes.md)" -ForegroundColor Yellow
    exit 1
}

exit 0
```

**Notas críticas:**

1. **Sem acentos** em mensagens (lição consolidada de scripts PowerShell).
2. **Encoding UTF-8 sem BOM** (lição da Etapa 2.6).
3. **`Write-Host` em vez de `Write-Error`** (lição da Etapa 2.6.1).
4. **Regex única** — não fragmentar em múltiplas validações em sequência. Match falha → mostra mensagem completa de ajuda → exit 1.
5. **`-Encoding UTF8`** em `Get-Content` evita problemas com caracteres especiais na mensagem do operador (mesmo que o hook não os use, o operador pode escrever em português com acentos — o hook não rejeita por acento, só por formato).
6. **Não validar body do commit.** Só primeira linha (subject). Body fica livre.

### Atualização de `docs/decisoes.md`

Localizar a seção "Camada 3 — Configuração do Claude Code" criada na 4.0. Adicionar nova subseção **antes** da subseção "Claude Code hooks nativos":

```markdown
### Conventional Commits (Sub-etapa 4.1)

**Tipos permitidos:** feat, fix, chore, docs, test, refactor, style, perf, build, ci.

**Formato:** `<tipo>[(scope)][!]: <descricao>` com pelo menos 10 caracteres na descricao.

**Scope:** opcional. Lowercase + digitos + hifen entre parenteses. Convencao do projeto usa nome do modulo (`feat(transacao):`, `chore(scripts):`).

**Breaking change:** indicado por `!` apos scope (`feat!:` ou `feat(api)!:`).

**Excecoes automaticas:** mensagens iniciadas por `Merge ` ou `Revert ` (geradas pelo git) passam sem validacao.

**Override consciente:** `git commit --no-verify` e escape valido em emergencias (bug critico em producao, hotfix que justifica pular validacao). Cada invocacao deve ser registrada no PR body com motivo. Sem polica automatica — disciplina por norma.

**Hook implementado em:** `.claude/hooks/universal/conventional-commits.ps1`, invocado por `.githooks/commit-msg` (entrypoint bash) -> `.githooks/commit-msg.ps1` (companheiro PowerShell).
```

Adicionar entrada no histórico (final do arquivo):

```markdown
- **2026-MM-DD** — Sub-etapa 4.1 concluida: primeiro hook funcional do projeto. Conventional Commits implementado em 3 camadas (entrypoint bash `.githooks/commit-msg` -> companheiro `.githooks/commit-msg.ps1` -> hook `.claude/hooks/universal/conventional-commits.ps1`). Tipos permitidos: feat, fix, chore, docs, test, refactor, style, perf, build, ci. Scope opcional, breaking change via `!`, descricao minima 10 chars. Override `--no-verify` documentado como escape valido. Validacao destrutiva confirmou bloqueio de mensagem invalida + bypass por --no-verify. Mergeado via PR #XX.
```

Substituir `2026-MM-DD` pela data real da execução.

### Atualização de `docs/hooks-pendentes.md`

**Operação A — Mover item implementado.**

Localizar a linha do Conventional Commits na seção "Hooks Markdown / docs":

```markdown
- **Conventional Commits.** (Etapa 1.1) Validar mensagem de commit (`feat:`, `fix:`, `chore:`, `docs:`, etc).
```

**Remover** essa linha da seção "Hooks Markdown / docs".

**Operação B — Criar nova seção "Hooks implementados".**

Adicionar nova seção ao **final** do arquivo, logo após "Débitos de configuração":

```markdown
## Hooks implementados

Itens originalmente listados em "Hooks Markdown / docs" ou outras seções, agora implementados e ativos no projeto. Mantidos aqui como histórico de progresso da Camada 3.

- **Conventional Commits** (Sub-etapa 4.1, PR #XX). Implementado em `.claude/hooks/universal/conventional-commits.ps1`, invocado via `.githooks/commit-msg` no evento `commit-msg`. Tipos: feat, fix, chore, docs, test, refactor, style, perf, build, ci. Scope opcional, breaking change via `!`, descricao minima 10 chars. Excecoes automaticas: merge e revert commits. Override consciente: `git commit --no-verify` documentado em `decisoes.md`.
```

**Operação C — Atualizar data.**

Linha "Última atualização" no topo:

```markdown
**Última atualização:** 2026-MM-DD (Sub-etapa 4.1 — Conventional Commits implementado)
```

### Atualização de `docs/progresso.md`

**A.** Atualizar "Última atualização": `2026-MM-DD (Sub-etapa 4.1 — Conventional Commits implementado)`.

**B.** Na seção "Camada 3 — Configuração do Claude Code", subseção "Sub-etapas concluídas", adicionar:

```markdown
- **4.1 — Hook universal de Conventional Commits** (2026-MM-DD): primeiro hook funcional do projeto. Estabelece padrao de invocacao em 3 camadas (entrypoint bash sem extensao -> companheiro `.ps1` -> hook em `.claude/hooks/<escopo>/`). Valida mensagem de commit contra Conventional Commits (10 tipos permitidos, scope opcional, breaking change via `!`, descricao minima 10 chars). Excecoes automaticas: merge e revert. Override `--no-verify` documentado em `decisoes.md`. Validacao destrutiva manual confirma bloqueio de mensagem invalida + bypass por `--no-verify`. PR #XX.
```

**C.** Adicionar seção "Lições da Sub-etapa 4.1" após as da 4.0.1:

```markdown
## Licoes da Sub-etapa 4.1

### Candidatos a hook (automatizar em etapas futuras)

(A preencher se houver durante execucao.)

### Licoes de ambiente

(A preencher se houver durante execucao. Esperado pelo menos: primeiro hook mecanico ativo no projeto — fim da era "vigilancia humana sobre padrao consolidado". Validacao destrutiva manual provou o gate funciona.)
```

**D.** Adicionar entrada no histórico geral:

```markdown
- **2026-MM-DD** — Sub-etapa 4.1 concluida: primeiro hook funcional. Conventional Commits ativo via `commit-msg` hook. Padrao de 3 camadas (entrypoint bash -> companheiro `.ps1` -> hook universal) estabelecido como referencia para sub-etapas seguintes da Camada 3. Mergeado via PR #XX.
```

### Versionar este próprio prompt

`docs/prompt-etapa-4-1.md`.

## Estado esperado ao iniciar

- Branch atual: `main`, sincronizada com `origin/main`.
- `git log --oneline -1` mostra commit `adc97a3` (squash da 4.0.1) ou superior.
- `git config core.hooksPath` retorna `.githooks`.
- `docs/prompt-etapa-4-1.md` presente como **untracked** (operador colocou antes de iniciar).
- Working tree limpo (exceto o prompt).
- `.claude/hooks/universal/` contém apenas `.gitkeep`.
- `.githooks/` contém apenas `README.md`.

Validar com:

```bash
git status
git log --oneline -3
ls docs/prompt-etapa-4-1.md
git config core.hooksPath
ls .claude/hooks/universal/
ls .githooks/
```

Se algum item divergir, **parar e reportar**.

## Tarefas

### Tarefa 1 — Validar pré-requisitos

Rodar comandos da seção "Estado esperado ao iniciar". Se algum item divergir, parar e reportar.

### Tarefa 2 — Criar branch de trabalho

```bash
git checkout -b feat/etapa-4-1-conventional-commits-hook
```

### Tarefa 3 — Antes de escrever, ler arquivos vivos

```bash
cat .githooks/README.md
cat .claude/hooks/universal/.gitkeep 2>&1 | head -3
cat docs/decisoes.md
cat docs/hooks-pendentes.md
cat docs/progresso.md
cat .gitattributes
```

**Confirmar:**
- `.githooks/README.md` descreve o padrão de wrapper esperado (do prompt da 4.0).
- `.claude/hooks/universal/.gitkeep` está vazio (esperado).
- `decisoes.md` tem seção "Camada 3 — Configuração do Claude Code" com subseções existentes da 4.0.
- `hooks-pendentes.md` tem o item de Conventional Commits na seção "Hooks Markdown / docs".
- `.gitattributes` tem regra LF para arquivos sem extensão (importante para `.githooks/commit-msg`).

Se alguma divergência, **parar e reportar**.

### Tarefa 4 — Criar o hook universal `.claude/hooks/universal/conventional-commits.ps1`

Conteúdo conforme seção "Conteúdo de `.claude/hooks/universal/conventional-commits.ps1`" do escopo decidido. Encoding UTF-8 sem BOM. Sem acentos nas mensagens.

### Tarefa 5 — Remover `.gitkeep` da pasta `.claude/hooks/universal/`

```bash
git rm .claude/hooks/universal/.gitkeep
```

Padrão idiomático: `.gitkeep` existe apenas para preservar pasta vazia no git. Quando primeiro arquivo real entra, `.gitkeep` sai.

### Tarefa 6 — Criar o companheiro `.githooks/commit-msg.ps1`

Conteúdo conforme seção "Conteúdo de `.githooks/commit-msg.ps1`". Encoding UTF-8 sem BOM. Sem acentos.

### Tarefa 7 — Criar o entrypoint `.githooks/commit-msg` (sem extensão)

Conteúdo conforme seção "Conteúdo de `.githooks/commit-msg`". 

**Critérios técnicos críticos:**

1. **Nome sem extensão.** Exatamente `commit-msg` (não `commit-msg.sh`, não `commit-msg.txt`).
2. **Line endings LF.** O `.gitattributes` deve cuidar disso automaticamente para arquivos sem extensão, mas validar após criação:
   ```bash
   file .githooks/commit-msg
   ```
   Esperado: saída inclui "ASCII text" ou similar, sem "with CRLF line terminators". Se aparecer CRLF, converter para LF antes de commitar.
3. **Bit de execução no git index** — depois de `git add`, rodar:
   ```bash
   git update-index --chmod=+x .githooks/commit-msg
   ```
   Validar com:
   ```bash
   git ls-files --stage .githooks/commit-msg
   ```
   Esperado: saída começa com `100755` (executável). Se começar com `100644` (não executável), o `chmod=+x` não aplicou — parar e reportar.

### Tarefa 8 — Validação destrutiva (cenário 1: mensagem válida passa)

**Pré-condição:** todos os arquivos da Tarefa 4-7 criados e adicionados ao stage (`git add`).

Criar arquivo temporário para teste, commitar com mensagem válida, confirmar sucesso:

```bash
echo "validacao do hook" > test-hook-validation.tmp
git add test-hook-validation.tmp
git commit -m "chore(test): validacao manual do hook conventional commits"
```

Esperado: commit aceito. `git log --oneline -1` mostra o novo commit.

Se commit for rejeitado, parar e reportar (regex pode estar incorreta, hook pode não estar sendo invocado).

### Tarefa 9 — Validação destrutiva (cenário 2: mensagem inválida bloqueia)

Modificar o arquivo temporário, tentar commitar com mensagem inválida:

```bash
echo "modificacao" >> test-hook-validation.tmp
git add test-hook-validation.tmp
git commit -m "wip"
```

**Esperado:** commit **rejeitado**. Output mostra a mensagem de ajuda do hook (formato esperado, tipos permitidos, exemplos, instrução de override). Exit code do `git commit` é diferente de zero.

Reportar a saída completa do hook na resposta final.

Se commit for aceito apesar da mensagem inválida, parar e reportar — hook não está sendo invocado ou regex está errada.

### Tarefa 10 — Validação destrutiva (cenário 3: override por --no-verify)

Tentar commitar a mesma mensagem inválida com `--no-verify`:

```bash
git commit --no-verify -m "wip"
```

**Esperado:** commit aceito (hook foi bypassado).

### Tarefa 11 — Validação destrutiva (cenário 4: exceções automáticas)

**Não reproduzir merge ou revert reais.** Em vez disso, validar via execução direta do hook com arquivo de mensagem simulado:

```powershell
# Cenario 4a: simular mensagem de merge
"Merge branch 'feature/x' into main" | Out-File -Encoding utf8 -NoNewline test-merge-msg.tmp
pwsh -File .claude/hooks/universal/conventional-commits.ps1 test-merge-msg.tmp
$resultMerge = $LASTEXITCODE

# Cenario 4b: simular mensagem de revert
'Revert "feat: adiciona feature X"' | Out-File -Encoding utf8 -NoNewline test-revert-msg.tmp
pwsh -File .claude/hooks/universal/conventional-commits.ps1 test-revert-msg.tmp
$resultRevert = $LASTEXITCODE

Remove-Item test-merge-msg.tmp, test-revert-msg.tmp

Write-Host "Cenario 4a (merge, esperado 0): $resultMerge"
Write-Host "Cenario 4b (revert, esperado 0): $resultRevert"
```

Ambos devem retornar exit code 0 (passam sem validação). Se algum retornar 1, hook está rejeitando exceções automáticas — parar e reportar.

### Tarefa 12 — Limpeza dos commits de teste

Os cenários 8, 9 (rejeitado, não cria commit), 10 criaram **2 commits** indesejados na branch atual (cenários 8 e 10). Removê-los:

```bash
git log --oneline -5
git reset --hard HEAD~2
Remove-Item test-hook-validation.tmp -ErrorAction SilentlyContinue
git status
git log --oneline -3
```

**Atenção crítica:**

1. **`git reset --hard` SOMENTE na branch da etapa**, NUNCA em `main`. Validar com `git branch --show-current` antes do reset que está em `feat/etapa-4-1-conventional-commits-hook`.
2. Se a branch tem mais ou menos commits que esperado (porque cenário 8 falhou em alguma forma e não criou commit, ou cenário 10 já criou commit antes da limpeza), ajustar o número após `HEAD~`. Validar com `git log --oneline -5` antes.
3. **Não usar `--no-verify` na limpeza** — limpeza não é "emergência", é parte normal do roteiro destrutivo.

Esperado após limpeza:
- `git status` working tree limpo.
- `git log --oneline -1` mostra commit antes dos testes destrutivos (provavelmente squash da 4.0.1 ou commit anterior na branch da etapa).
- Arquivo `test-hook-validation.tmp` removido.

### Tarefa 13 — Editar `docs/decisoes.md`

Adicionar subseção "Conventional Commits (Sub-etapa 4.1)" na seção "Camada 3 — Configuração do Claude Code", **antes** da subseção "Claude Code hooks nativos". Conteúdo conforme escopo.

Adicionar entrada no histórico (final do arquivo). Substituir `2026-MM-DD` pela data real.

### Tarefa 14 — Editar `docs/hooks-pendentes.md`

Operações A, B, C conforme escopo decidido:
- A: remover linha do Conventional Commits da seção "Hooks Markdown / docs".
- B: criar seção "Hooks implementados" no final, com a primeira entrada.
- C: atualizar data no topo.

### Tarefa 15 — Editar `docs/progresso.md`

Operações A, B, C, D conforme escopo decidido. Substituir `2026-MM-DD` pela data real.

### Tarefa 16 — Versionar este próprio prompt

`git add docs/prompt-etapa-4-1.md`.

### Tarefa 17 — Validação final antes de commitar

```bash
git status
ls .claude/hooks/universal/
ls .githooks/
git ls-files --stage .githooks/commit-msg
file .githooks/commit-msg
```

Esperado:
- `git status` mostra 7 arquivos modificados/novos (lista abaixo).
- `.claude/hooks/universal/` contém `conventional-commits.ps1` (não mais `.gitkeep`).
- `.githooks/` contém `README.md`, `commit-msg`, `commit-msg.ps1`.
- `git ls-files --stage .githooks/commit-msg` começa com `100755`.
- `file .githooks/commit-msg` confirma ASCII/UTF-8 com LF.

**Arquivos esperados em git status:**

- **Novos:** `.claude/hooks/universal/conventional-commits.ps1`, `.githooks/commit-msg`, `.githooks/commit-msg.ps1`, `docs/prompt-etapa-4-1.md`.
- **Removidos:** `.claude/hooks/universal/.gitkeep`.
- **Modificados:** `docs/decisoes.md`, `docs/hooks-pendentes.md`, `docs/progresso.md`.

**`check.ps1` opcional** — esta etapa não toca em código Java, mas vale rodar para confirmar suite intocada:

```bash
.\scripts\check.ps1
```

Se falhar por motivo não relacionado (Docker, etc.), reportar mas seguir.

## Restrições e freios

1. **Não criar outros hooks.** Esta etapa entrega **apenas** o Conventional Commits. UTF-8, Markdown, tamanho de docs ficam para 4.2, 4.3, 4.4.

2. **Não criar entrypoints `pre-commit`, `pre-push`, ou outros.** Apenas `commit-msg`.

3. **Não criar subagents, skills, ou CLAUDE.md.** Continuam fora do escopo desta sub-etapa.

4. **Não tocar em scripts existentes** (`setup.ps1`, `dev.ps1`, `test.ps1`, `check.ps1`, `ship.ps1`, `test-integration.ps1`). Funcionam como estão.

5. **Não tocar em `src/`, `pom.xml`, `application*.yml`, `frontend/`, `docker-compose.yml`, migrations.**

6. **Não tocar em `.gitignore`, `.gitattributes`.** Configurações da 4.0 estão corretas.

7. **Não tocar em ADRs (`docs/adrs.md`).** ADR-009 já prescreve o padrão. Esta etapa apenas implementa o primeiro caso.

8. **Não introduzir dependências externas** (husky, pre-commit framework Python, etc.). Continua sendo PowerShell puro conforme ADR-009.

9. **Encoding UTF-8 sem BOM** em todos os arquivos. `.ps1` especialmente sensível.

10. **Line endings LF** em `.githooks/commit-msg` (entrypoint sem extensão). `.gitattributes` deve cuidar disso; validar.

11. **Sem acentos** nas mensagens de output do hook.

12. **Não usar `Write-Error` + `exit`.** Padrão: `Write-Host -ForegroundColor Red` + `exit 1`.

13. **Bit de execução no git index** via `git update-index --chmod=+x` é **obrigatório** para `.githooks/commit-msg`. Sem isso, hook não roda em ambientes não-Windows. Lição da Etapa 1.5.

14. **Linhas em branco antes e depois de headers Markdown** nos docs editados.

15. **Lógica de validação fica em `.claude/hooks/universal/conventional-commits.ps1`.** Entrypoint companheiro `.githooks/commit-msg.ps1` é fino — só descobre o hook e delega. Não duplicar regex no companheiro.

16. **Não validar body do commit.** Apenas primeira linha (subject). Body livre conforme prática.

17. **`git reset --hard` apenas na branch da etapa.** Nunca em `main`. Validar com `git branch --show-current` antes.

18. **Validação destrutiva COMPLETA (cenários 8, 9, 10, 11) é gate de "pronto".** Etapa só está concluída após todos os 4 cenários passarem como esperado. Reportar saídas no PR body.

19. **Não tomar decisão silenciosa em zona limítrofe.** Se algum cenário destrutivo produzir comportamento inesperado (commit aceito quando deveria bloquear, ou vice-versa), parar e reportar — não silenciar com workaround.

20. **Não sugerir próxima etapa espontaneamente.** Termina com PR aberto, CI verde, aguardando autorização explícita do operador.

21. **Antes de escrever cada arquivo, ler arquivos vivos** (Tarefa 3). Padrões observados > prompt.

## Estrutura de commits

Branch: `feat/etapa-4-1-conventional-commits-hook`

**Commit 1** — `feat(claude): adiciona hook universal de conventional commits`
- `.claude/hooks/universal/conventional-commits.ps1` (novo)
- `.claude/hooks/universal/.gitkeep` (removido)

**Commit 2** — `feat(githooks): adiciona entrypoint commit-msg + companheiro powershell`
- `.githooks/commit-msg` (novo, bit de execução setado)
- `.githooks/commit-msg.ps1` (novo)

**Commit 3** — `docs: registra conventional commits ratificado e override --no-verify`
- `docs/decisoes.md`
- `docs/hooks-pendentes.md`

**Commit 4** — `docs: registra sub-etapa 4.1 em progresso e versiona prompt`
- `docs/progresso.md`
- `docs/prompt-etapa-4-1.md`

## Validação antes de abrir PR

```bash
.\scripts\check.ps1
git status
git log --oneline -5
git config core.hooksPath
git ls-files --stage .githooks/commit-msg
```

Esperado:
- `check.ps1` passa.
- Working tree limpo.
- 4 commits novos.
- `core.hooksPath` retorna `.githooks`.
- `.githooks/commit-msg` registrado como `100755`.

## PR

Título: `feat: sub-etapa 4.1 — hook universal de conventional commits (primeiro hook funcional)`

Body sugerido:

```markdown
## Summary

Implementa o **primeiro hook funcional** do projeto: Conventional Commits via `commit-msg`. Marca a transição de "vigilância humana sobre padroes consolidados" para "gate mecanico automatico no fluxo de commit".

Esta sub-etapa **estabelece o padrao de 3 camadas** que sera reusado por toda a Camada 3:

\```
.githooks/commit-msg                                  <- entrypoint sem extensao (wrapper bash)
.githooks/commit-msg.ps1                              <- companheiro PowerShell (fino, delegador)
.claude/hooks/universal/conventional-commits.ps1     <- logica real (portavel)
\```

Quando esta sub-etapa fechar, qualquer commit local com mensagem fora do formato e bloqueado. Override consciente via `git commit --no-verify` documentado em `decisoes.md`.

### Comportamento do hook

**Formato aceito:**

\```
<tipo>[(scope)][!]: <descricao com min 10 chars>
\```

- **Tipos permitidos:** feat, fix, chore, docs, test, refactor, style, perf, build, ci.
- **Scope:** opcional, lowercase + digitos + hifen entre parenteses.
- **Breaking change:** `!` antes do `:`.
- **Excecoes automaticas:** mensagens iniciadas por `Merge ` ou `Revert ` (geradas pelo git).
- **Override:** `git commit --no-verify` em emergencias, com motivo registrado no PR body.

### Validacao destrutiva manual

Quatro cenarios executados antes do commit final:

1. **Mensagem valida passa** — `chore(test): validacao manual do hook conventional commits` aceito.
2. **Mensagem invalida bloqueia** — `wip` rejeitado com mensagem de ajuda exibida.
3. **Override bypassa** — `git commit --no-verify -m "wip"` aceito.
4. **Excecoes automaticas passam** — execucao direta do hook com mensagens `Merge ...` e `Revert ...` retorna exit 0.

Todos os 4 cenarios passaram conforme esperado.

### Por que Conventional Commits primeiro

Calibrado com operador antes da redacao do prompt. Quatro vantagens objetivas:

1. Usa entrypoint `commit-msg` (1 argumento, le arquivo, valida regex) — mais simples que `pre-commit` (multi-arquivo, le diff).
2. Validacao destrutiva e unidimensional (mensagem boa vs ruim).
3. Nao interage com diff staged.
4. Universal absoluto — vale para qualquer repo, qualquer linguagem.

Caso laboratorio ideal para estrear o mecanismo de 3 camadas.

### Mudancas

- `.claude/hooks/universal/conventional-commits.ps1`: logica real do hook. Regex `^(feat|fix|chore|docs|test|refactor|style|perf|build|ci)(\([a-z0-9-]+\))?!?: .{10,}$` aplicada a primeira linha nao-vazia, nao-comentario do arquivo de mensagem. Excecoes para `Merge ` e `Revert `. Sem acentos, encoding UTF-8 sem BOM.
- `.claude/hooks/universal/.gitkeep`: removido (primeiro arquivo real na pasta).
- `.githooks/commit-msg`: entrypoint bash sem extensao. Bit de execucao registrado no git index via `git update-index --chmod=+x`. Line endings LF.
- `.githooks/commit-msg.ps1`: companheiro fino. Descobre repo root, localiza o hook em `.claude/hooks/universal/`, delega com o argumento original, propaga exit code.
- `docs/decisoes.md`: subsecao "Conventional Commits" adicionada em "Camada 3 — Configuracao do Claude Code". Entrada no historico.
- `docs/hooks-pendentes.md`: item Conventional Commits movido de "Hooks Markdown / docs" para nova secao "Hooks implementados". Data atualizada.
- `docs/progresso.md`: sub-etapa 4.1 registrada em "Sub-etapas concluidas" da Camada 3. Licoes da 4.1. Entrada no historico.

### Validacao destrutiva pos-merge sugerida

1. Em qualquer branch (nao main):
   - `git commit --allow-empty -m "feat: teste pos-merge do hook"` — aceito.
   - `git commit --allow-empty -m "wip"` — rejeitado.
   - `git commit --allow-empty --no-verify -m "wip"` — aceito (override).
   - `git reset --hard HEAD~2` para limpar.
2. Confirmar mensagens de ajuda do hook sao claras (formato, tipos, exemplos, instrucao de override).

### Proximo passo

Sub-etapa 4.2 (hook universal de encoding UTF-8 via `pre-commit`). Estabelece o segundo entrypoint do padrao de 3 camadas e introduz validacao multi-arquivo a partir de `git diff --cached`. Decisao fora deste PR.
```

## Pós-criação do PR

1. Abrir PR via `gh pr create`.
2. Capturar o número.
3. Editar `docs/decisoes.md`, `docs/hooks-pendentes.md`, `docs/progresso.md` substituindo `PR #XX` por `PR #<numero-real>`.
4. Commit: `docs: atualiza numero do PR no historico`.
5. Push.
6. Esperar CI verde.
7. **Aguardar autorização explícita do operador antes do merge.**

## Estado esperado ao terminar

- Branch `feat/etapa-4-1-conventional-commits-hook` empurrada com 5 commits (4 + 1 update do PR).
- PR aberto, CI verde, **não mergeado**.
- `main` ainda no squash da 4.0.1.
- Working tree limpo.
- Arquivos `test-hook-validation.tmp`, `test-merge-msg.tmp`, `test-revert-msg.tmp` **removidos** (limpeza da Tarefa 12).
- `git ls-files --stage .githooks/commit-msg` retorna `100755`.
- Reportar com `git log --oneline -5`, `git status`, `gh pr view --json number,state,statusCheckRollup`, e as saidas dos 4 cenarios destrutivos.

## O que NÃO fazer ao terminar

- Não mergear o PR.
- Não criar prompt da Sub-etapa 4.2.
- Não criar hooks de UTF-8, Markdown, ou tamanho de docs.
- Não criar subagents, skills, CLAUDE.md.
- Não tocar em scripts existentes além dos arquivos prescritos.
- Não tocar em `.gitignore`, `.gitattributes`, ADRs.
- Não deixar arquivos de validação destrutiva (`test-*.tmp`) na branch.
- Não deixar commits de validação destrutiva (`chore(test):...`, `wip`) no histórico — limpar via `git reset --hard`.
- Não sugerir "próximo passo" espontaneamente.
- Não relaxar regex do hook se aparecer falso positivo — reportar e calibrar com operador.
- Não duplicar lógica de validação entre `.githooks/commit-msg.ps1` e `.claude/hooks/universal/conventional-commits.ps1`.
