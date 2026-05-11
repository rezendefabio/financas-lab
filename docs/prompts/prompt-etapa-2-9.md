# Prompt — Etapa 2.9: Fix do `setup.ps1` para criar `.env` automaticamente quando ausente

## Contexto

A Etapa 2.8 foi concluída e fechada via PR #27 (wrap-up da Camada 1). Camada 1 está marcada como ✅ Concluída em `progresso.md`. Antes de abrir a Camada 2, esta etapa resolve um débito técnico descoberto na validação destrutiva da própria 2.8: `setup.ps1` passa silenciosamente quando `.env` está ausente.

O problema concreto: Docker Compose interpreta variáveis de ambiente referenciadas em `docker-compose.yml` mas ausentes no shell como strings vazias. Sem `.env`, os containers de Postgres e Redis sobem com credenciais vazias. `mvn -DskipTests` (último passo do `setup.ps1`) não testa conexão. Resultado: setup "conclui com sucesso" mas o ambiente é inutilizável para dev real. CI nunca detectaria — secrets são injetados no runner.

A retrospectiva da Camada 1 elevou isso a princípio consolidado:

> **Princípio 8** — Scripts de setup devem ser à prova de ambiente zero. `setup.ps1` (e equivalentes) devem validar pré-condições visíveis (`.env` presente, Docker rodando) com mensagem clara antes de executar — não confiar que o ambiente está correto porque "normalmente está".

Esta etapa implementa esse princípio para o caso `.env` ausente. Validação de Docker rodando já foi feita na Etapa 2.6.2 (padrão "suspender Stop localmente") — fora de escopo aqui.

Escopo decidido nesta etapa (calibrado com operador antes da redação):

- **Comportamento desejado** quando `setup.ps1` ou `dev.ps1` rodam:
  - `.env` existe → seguir normalmente, sem mensagem (caso comum, não poluir saída).
  - `.env` ausente, `.env.example` presente → copiar `.env.example` para `.env`, exibir aviso amarelo, seguir.
  - `.env` ausente, `.env.example` também ausente → falhar com mensagem vermelha clara, exit 1 (repositório corrompido).
- **Padrão de mensagem de erro:** `Write-Host -ForegroundColor Red` + `exit 1` (regra consolidada na 2.6.1, nunca `Write-Error`).
- **Padrão de aviso:** `Write-Host -ForegroundColor Yellow` (não bloqueia, apenas informa).
- **Aplicar nos dois scripts que sobem Docker Compose:** `setup.ps1` e `dev.ps1`. `test.ps1`, `test-integration.ps1`, `check.ps1`, `ship.ps1` não sobem Docker — fora de escopo.
- **Sobre extrair função vs duplicar bloco:** decidir caso a caso baseado em legibilidade. Bloco curto (~10 linhas) duplicado é aceitável; se ficar > 15 linhas, extrair. Justificar no PR body.
- **Sem testes automatizados de PowerShell.** Projeto não tem suíte de testes para `.ps1`. Validação é manual destrutiva (operador, definida abaixo).
- **Encoding UTF-8 sem BOM** nos scripts modificados. Validar com `xxd` antes de commitar.

## Estado esperado ao iniciar

- Branch atual: `main`, sincronizada com `origin/main`
- `git log --oneline -1` mostra commit do squash da Etapa 2.8 com referência a PR #27
- `docs/prompt-etapa-2-9.md` presente como untracked (este próprio arquivo, anexado pelo operador antes de iniciar). Se não aparecer em `git status`, parar e reportar.
- Working tree sem outras mudanças
- `scripts/setup.ps1` e `scripts/dev.ps1` presentes (criados na Etapa 2.6, ajustados em 2.6.1 e 2.6.2)
- `.env.example` versionado na raiz; `.env` em `.gitignore`

Validar com:

```bash
git status
git log --oneline -1
ls scripts/setup.ps1 scripts/dev.ps1 .env.example
grep -E "^\.env$" .gitignore
```

Se algum dos dois primeiros itens divergir, parar e reportar.

## Tarefas

### Tarefa 1 — Validar pré-requisitos

```bash
git status
git log --oneline -3
ls scripts/setup.ps1 scripts/dev.ps1 .env.example
grep -nE "^\.env$|^\.env\b" .gitignore
```

Esperado:
- Working tree limpo, exceto `docs/prompt-etapa-2-9.md` untracked
- Último commit em main referencia Etapa 2.8 / PR #27
- Os três arquivos existem
- `.env` (ou `.env*`) está no `.gitignore`

Se qualquer item divergir, parar e reportar.

### Tarefa 2 — Criar branch de trabalho

```bash
git checkout -b fix/setup-env-auto
```

### Tarefa 3 — Modificar `scripts/setup.ps1`

Adicionar bloco de validação/criação de `.env` **no início do script**, após o cabeçalho/comentários e após `$ErrorActionPreference = "Stop"`, **antes** da chamada `docker compose up -d`.

Comportamento do bloco:

1. Se `.env` existe → não fazer nada, prosseguir.
2. Se `.env` não existe e `.env.example` existe → copiar `.env.example` para `.env`, imprimir aviso amarelo informando criação automática e recomendando revisão das credenciais antes de uso compartilhado.
3. Se `.env` não existe e `.env.example` também não existe → imprimir mensagem vermelha clara informando que repositório está corrompido (qualquer clone deve ter `.env.example`), `exit 1`.

Esboço de referência (adaptar à sintaxe e estilo dos scripts existentes):

```powershell
if (-not (Test-Path .env)) {
    if (Test-Path .env.example) {
        Copy-Item .env.example .env
        Write-Host "AVISO: .env nao encontrado. Criado a partir de .env.example." -ForegroundColor Yellow
        Write-Host "Revise as credenciais em .env antes de usar em ambiente compartilhado." -ForegroundColor Yellow
    } else {
        Write-Host "ERRO: .env nao encontrado e .env.example tambem nao existe." -ForegroundColor Red
        Write-Host "Repositorio parece corrompido. Verifique o clone." -ForegroundColor Red
        exit 1
    }
}
```

**Importante:** mensagens sem acentos (mesmo motivo de outros scripts do projeto — Windows + encoding UTF-8 sem BOM evita risco de `Ã£`/`Ã§` em terminais que leiam errado). Usar "nao", "porem", "configuracao" etc.

Não alterar nenhuma outra linha do `setup.ps1`.

Após edição, confirmar encoding UTF-8 sem BOM:

```bash
xxd scripts/setup.ps1 | head -1
```

Esperado: primeiros bytes **não** são `EF BB BF`. Se forem, recriar com encoding correto via tool `Write` nativa (lição da Etapa 2.6).

### Tarefa 4 — Modificar `scripts/dev.ps1`

Aplicar o mesmo bloco da Tarefa 3, na mesma posição relativa: após `$ErrorActionPreference = "Stop"` e quaisquer cabeçalhos, **antes** da checagem de Docker rodando (introduzida na Etapa 2.6.2 com o padrão "suspender Stop localmente em torno de `docker info`").

**Ordem fixa, não negociável:** o bloco do `.env` vem **antes** da checagem de Docker. Razão: `.env` ausente é falha mais barata de detectar — não exige Docker rodando para descobrir. Se Docker estiver parado e `.env` também ausente, o operador recebe a mensagem do `.env` primeiro, resolve, e só então cai na checagem de Docker. Caminho inverso confunde o diagnóstico.

Decisão sobre extrair função vs duplicar:

- Como o bloco tem ~10 linhas, duplicar é aceitável e mantém scripts auto-contidos (princípio do projeto: scripts atômicos, sem dependência cruzada).
- Se decidir extrair (ex: função em arquivo `scripts/_lib.ps1` dot-source), justificar no PR body. Atenção: dot-source adiciona complexidade nova ao projeto, e o padrão dos `.ps1` atuais é zero dependência entre scripts. Default recomendado: **duplicar**.

Confirmar encoding após edição:

```bash
xxd scripts/dev.ps1 | head -1
```

### Tarefa 5 — Atualizar `README.md`

Verificar se há seção descrevendo setup inicial / primeiros passos:

```bash
grep -niE "setup|primeiros passos|getting started" README.md | head -20
```

**Se existir seção sobre setup que faz sentido atualizar:** adicionar nota curta de que `.env` é criado automaticamente a partir de `.env.example` no primeiro `setup.ps1` (ou `dev.ps1`).

**Se não existir essa seção, ou se a seção de comandos for puramente referencial (tabela):** não inventar seção nova. Pular esta tarefa e registrar no PR body que README não precisou de alteração.

Reportar a decisão tomada.

### Tarefa 6 — Atualizar `docs/decisoes.md`

**6a.** Localizar a seção "Comandos atômicos do projeto" (criada na Etapa 2.6). Adicionar nota curta abaixo da tabela de comandos (ou em parágrafo adjacente) sobre criação automática do `.env`. Sugestão de redação:

```markdown
**Criação automática de `.env`:** `setup.ps1` e `dev.ps1` criam `.env` automaticamente a partir de `.env.example` quando `.env` está ausente, com aviso amarelo. Se `.env.example` também estiver ausente, o script falha com mensagem clara (repositório corrompido). Padrão alinhado ao princípio "scripts de setup à prova de ambiente zero" (consolidado na retrospectiva da Camada 1).
```

**6b.** Adicionar entrada no **"Histórico de mudanças"** no fim do arquivo, no topo da lista:

```markdown
- **2026-05-08** — Etapa 2.9 concluída: `setup.ps1` e `dev.ps1` criam `.env` automaticamente a partir de `.env.example` quando ausente. Resolve débito técnico descoberto na Etapa 2.8. Mergeado via PR #XX.
```

(O `#XX` será substituído pelo número real do PR na fase de pós-criação.)

### Tarefa 7 — Atualizar `docs/hooks-pendentes.md`

Localizar seção "Hooks de setup / ambiente". O primeiro item da seção é:

> **`setup.ps1` deve detectar `.env` ausente antes de subir containers.** (Etapa 2.8) Clone novo sem `.env` resulta em containers com credenciais vazias [...]

Esse hook está sendo **resolvido** nesta etapa (não é mais hook pendente, é comportamento implementado). Remover esse item completo.

Atualizar campo "Última atualização" no topo do arquivo: `2026-05-08 (consolidado durante Etapa 2.8, item de .env resolvido na Etapa 2.9)`.

Se a seção "Hooks de setup / ambiente" ficar vazia após a remoção, manter o título da seção com nota tipo `(Nenhum item ativo no momento.)` em vez de remover a seção inteira — mantém estrutura previsível pra próximas adições.

### Tarefa 8 — Atualizar `docs/progresso.md`

**8a.** Atualizar campo "Última atualização" no topo: `2026-05-08 (Etapa 2.9 — fix do .env)`.

**8b.** Não alterar status da Camada 1 (continua ✅ Concluída). Esta etapa é fix de débito técnico da Camada 1, não reabre.

**8c.** Adicionar nova seção **"Lições da Etapa 2.9"** logo antes de **"Lições da Etapa 2.8"** (mantendo ordem decrescente):

```markdown
## Lições da Etapa 2.9

### Candidatos a hook (automatizar em etapas futuras)

(Preencher com lições reais — ou `(Nenhum novo nesta etapa.)` se nada surgir.)

### Lições de ambiente

(Preencher com lições reais — ou `(Nenhum novo nesta etapa.)` se nada surgir.)
```

**Regra dura:** só registrar lições **realmente observadas** durante a execução. Se nada digno surgir, deixar `(Nenhum novo nesta etapa.)` — não inventar.

**8d.** Adicionar entrada no **"Histórico de mudanças deste documento"** no topo da lista:

```markdown
- **2026-05-08** — Etapa 2.9 concluída: `setup.ps1` e `dev.ps1` criam `.env` automaticamente a partir de `.env.example` quando ausente. Débito técnico da Camada 1 (descoberto na 2.8) resolvido. Mergeado via PR #XX.
```

### Tarefa 9 — Atualizar `docs/retrospectiva-camada-1.md` (ajuste pontual)

Localizar a seção "Para a Camada 2" do arquivo. O último bullet é:

> Fix do `setup.ps1` para criar/validar `.env` de `.env.example` (débito técnico da Camada 1)

Substituir por:

> ~~Fix do `setup.ps1` para criar/validar `.env` de `.env.example` (débito técnico da Camada 1)~~ — **resolvido na Etapa 2.9 (PR #XX).**

Manter o riscado para preservar o histórico real do que estava na lista quando a Camada 1 foi fechada — não apagar.

### Tarefa 10 — Versionar este próprio prompt

Confirmar que `docs/prompt-etapa-2-9.md` está em disco como untracked e incluir no commit de docs (Tarefa 11, Commit 2).

### Tarefa 11 — Validação local antes de commitar

```bash
# Encoding sem BOM nos scripts modificados:
xxd scripts/setup.ps1 | head -1
xxd scripts/dev.ps1 | head -1
# (Esperado: nenhum começa com EF BB BF)

# Sintaxe PowerShell válida nos dois scripts (parse-only, não executa):
powershell.exe -NoProfile -Command "[void][System.Management.Automation.Language.Parser]::ParseFile('scripts\setup.ps1', [ref]\$null, [ref]\$null); Write-Host 'setup.ps1 OK'"
powershell.exe -NoProfile -Command "[void][System.Management.Automation.Language.Parser]::ParseFile('scripts\dev.ps1', [ref]\$null, [ref]\$null); Write-Host 'dev.ps1 OK'"

# Confirmar que .env continua no .gitignore:
grep -nE "^\.env" .gitignore

# Confirmar que .env NÃO está sendo versionado por engano:
git ls-files | grep -E "^\.env$" || echo "OK: .env nao versionado"

# Working tree esperado: scripts modificados + docs modificados + prompt untracked
git status
```

Se sintaxe falhar em qualquer script, parar e reportar — não tentar adivinhar correção.

**Validação destrutiva real (Cenários A/B/C/D)** é responsabilidade do **operador** após o merge, em ambiente Windows com Docker. O agente roda em ambiente Linux (`bash_tool`) sem Docker disponível e não consegue exercitar os cenários reais. Documentar no PR body que validação destrutiva é pós-merge.

## Restrições e freios

1. **Não tocar em arquivos fora do escopo.** Arquivos permitidos:
   - `scripts/setup.ps1`
   - `scripts/dev.ps1`
   - `README.md` (somente se Tarefa 5 decidir alterar)
   - `docs/decisoes.md`
   - `docs/hooks-pendentes.md`
   - `docs/progresso.md`
   - `docs/retrospectiva-camada-1.md`
   - `docs/prompt-etapa-2-9.md` (este arquivo, versionar)

2. **Não tocar em `scripts/test.ps1`, `scripts/test-integration.ps1`, `scripts/check.ps1`, `scripts/ship.ps1`.** Esses não sobem Docker Compose, não têm o problema. Aplicar o bloco neles seria scope creep.

3. **Não tocar em `docker-compose.yml`, `pom.xml`, `application.yml`, `src/main/...`, `src/test/...`, `frontend/...`, `.github/...`.** Esta etapa é exclusivamente sobre dois scripts e atualização de docs.

4. **Não relaxar `$ErrorActionPreference = "Stop"`** dos scripts. Padrão consolidado nas Etapas 2.6.1 e 2.6.2.

5. **Usar `Write-Host -ForegroundColor` (não `Write-Error`)** para mensagens de erro/aviso. Regra da Etapa 2.6.1 — `Write-Error` + `exit N` sob `Stop` propaga exit code falso em sessão direta.

6. **Encoding UTF-8 sem BOM** em ambos os scripts modificados. Se a tool `Write` ou edição via `str_replace` introduzir BOM, recriar o arquivo do zero com encoding correto. Padrão consolidado da Etapa 2.6.

7. **Sem acentos nas mensagens dos scripts.** "nao", "configuracao", "porem". Decisão alinhada com o resto dos scripts.

8. **Não introduzir validação de Docker rodando como parte desta etapa.** Já existe na 2.6.2. Adicionar de novo seria scope creep.

9. **Não tentar genericizar para múltiplos arquivos `.env.*`** (`.env.development`, `.env.staging`, etc). Atualmente só existe `.env`. Genericização sem necessidade é overengineering.

10. **Não criar arquivo `.env` versionado no repositório acidentalmente.** `.env` está em `.gitignore` — confirmar via `git status` que não aparece como tracked após edições. Se aparecer, é bug.

11. **Não criar testes automatizados em PowerShell.** Projeto não tem suíte de testes `.ps1`. Introduzir agora é fora de escopo. Validação é manual destrutiva (operador, pós-merge).

12. **`bash_tool` é bash, não PowerShell.** Para invocar PowerShell, `powershell.exe -Command ...` ou `powershell.exe -NoProfile -File ...`. Lição registrada desde 2.5.

13. **Lições da Etapa 2.9 só registram observações reais.** Se Tarefa 8c ficar com `(Nenhum novo nesta etapa.)`, tudo bem. Não inventar lições.

14. **Não antecipar Camada 2.** Sem rascunhar próximas etapas. Sem sugerir "Money primeiro" ou similar. Camada 2 abre em discussão separada com o operador.

## Estrutura de commits

Branch: `fix/setup-env-auto`

Commits atômicos, em ordem:

**Commit 1** — `fix(scripts): cria .env automaticamente em setup.ps1 e dev.ps1 quando ausente`
- `scripts/setup.ps1`
- `scripts/dev.ps1`

**Commit 2** — `docs: registra etapa 2.9 (fix do .env) em decisoes, hooks-pendentes, progresso e retrospectiva`
- `README.md` (apenas se Tarefa 5 decidir alterar)
- `docs/decisoes.md`
- `docs/hooks-pendentes.md`
- `docs/progresso.md`
- `docs/retrospectiva-camada-1.md`
- `docs/prompt-etapa-2-9.md`

## Validação antes de abrir PR

```bash
# Encoding e sintaxe (já feitos na Tarefa 11):
xxd scripts/setup.ps1 | head -1
xxd scripts/dev.ps1 | head -1
powershell.exe -NoProfile -Command "[void][System.Management.Automation.Language.Parser]::ParseFile('scripts\setup.ps1', [ref]\$null, [ref]\$null); 'OK'"
powershell.exe -NoProfile -Command "[void][System.Management.Automation.Language.Parser]::ParseFile('scripts\dev.ps1', [ref]\$null, [ref]\$null); 'OK'"

# Working tree esperado: limpo após os 2 commits
git status
git log --oneline -3
```

## PR

Título: `fix: etapa 2.9 — setup.ps1 e dev.ps1 criam .env automaticamente quando ausente`

Body sugerido (ajustar com observações reais):

```markdown
## Summary

Implementa a Etapa 2.9 do roadmap: resolve débito técnico descoberto na validação destrutiva da Etapa 2.8 (PR #27). `setup.ps1` e `dev.ps1` agora criam `.env` automaticamente a partir de `.env.example` quando `.env` está ausente, exibindo aviso amarelo. Se `.env.example` também estiver ausente, o script falha com mensagem clara (repositório corrompido).

### Mudanças

- `scripts/setup.ps1`: bloco de validação/criação de `.env` antes da chamada ao Docker Compose.
- `scripts/dev.ps1`: mesmo bloco, mesma posição relativa.
- `docs/decisoes.md`: nota sobre comportamento na seção "Comandos atômicos do projeto" + entrada no histórico.
- `docs/hooks-pendentes.md`: item do `.env` removido da seção "Hooks de setup / ambiente" (resolvido).
- `docs/progresso.md`: lições da Etapa 2.9 + entrada no histórico.
- `docs/retrospectiva-camada-1.md`: bullet do débito do `.env` marcado como resolvido (preservado riscado).
- `docs/prompt-etapa-2-9.md`: prompt versionado.

### Decisão sobre duplicar vs extrair função

<descrever decisão real tomada na Tarefa 4 — "duplicado por simplicidade" ou "extraído em ..." com justificativa>

### Validação

- Encoding UTF-8 sem BOM nos dois scripts: confirmado via `xxd`.
- Sintaxe PowerShell válida nos dois scripts: confirmado via `[Parser]::ParseFile`.
- `.env` continua em `.gitignore` e não foi versionado.
- **Validação destrutiva (Cenários A/B/C/D)** é responsabilidade do operador pós-merge, em Windows com Docker. Cenários documentados no prompt:
  - A: `.env` ausente, `.env.example` presente → criação automática + aviso amarelo
  - B: `.env` e `.env.example` ausentes → mensagem vermelha + exit 1
  - C: `.env` presente → seguir sem mensagem
  - D: `dev.ps1` com `.env` ausente → mesma criação automática

### Próximo passo

Camada 2 (Arquitetura otimizada para agentes) — fora do escopo deste PR. Abre em discussão separada após este merge.
```

## Pós-criação do PR

1. Abrir o PR via `gh pr create`.
2. Capturar o número.
3. Editar `docs/decisoes.md`, `docs/progresso.md` e `docs/retrospectiva-camada-1.md` substituindo `Mergeado via PR #XX` (e `(PR #XX)` na retrospectiva) por `Mergeado via PR #<numero-real>` (e `(PR #<numero-real>)`).
4. Commit: `docs: atualiza numero do PR no historico`
5. Push.
6. Esperar CI verde.
7. **Aguardar autorização explícita do operador antes do merge.**

## Estado esperado ao terminar

- Branch `fix/setup-env-auto` empurrada para origin com 3 commits (2 + 1 de update do número do PR)
- PR aberto, CI verde, **não mergeado**
- `main` local ainda aponta pro squash da 2.8 (operador faz merge depois)
- Working tree limpo
- `setup.ps1` e `dev.ps1` com bloco de criação automática do `.env`
- `docs/hooks-pendentes.md` sem o item do `.env` na seção de setup
- `docs/decisoes.md`, `docs/progresso.md` e `docs/retrospectiva-camada-1.md` atualizados, com PR número real registrado
- Prompt versionado em `docs/prompt-etapa-2-9.md`

Reportar ao operador o estado final com `git log --oneline -3`, `git status`, `gh pr view --json number,state,statusCheckRollup` e parar.

## O que NÃO fazer ao terminar

- Não mergear o PR. Aguardar autorização explícita do operador.
- Não criar prompt da próxima etapa.
- Não rascunhar Camada 2.
- Não tocar em backend nem frontend.
- Não adicionar validação de Docker rodando (já existe na 2.6.2).
- Não sugerir "próximo passo" espontaneamente após o merge eventual.
- Não atualizar `progresso.md` marcando algo da Camada 2 — esta etapa é exclusivamente fix de débito da Camada 1.
