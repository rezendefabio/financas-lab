# Prompt — Etapa 4.2.1: Padrões de validação destrutiva (lição pós-merge da 4.2)

## Contexto

A Sub-etapa 4.2 (PR #41) entregou o hook de encoding UTF-8 e foi mergeada em `main`. Smoke test pós-merge revelou um problema **não no código**, mas no **método de validação**:

`[System.IO.File]::WriteAllText("arquivo.md", ..., encoding)` com path relativo em PowerShell grava em `[System.Environment]::CurrentDirectory`, **não em `$PWD`**. Quando a sessão PowerShell faz `cd` para entrar no repo, `$PWD` é atualizado mas `Environment.CurrentDirectory` permanece no diretório original (tipicamente `C:\Users\<user>\`). Resultado:

- `WriteAllText` cria arquivo em `C:\Users\<user>\`.
- `git add arquivo.md` (rodando em `C:\projetos\...`) falha com `pathspec did not match any files`.
- `git commit` reporta `nothing to commit, working tree clean`.
- Observador desatento conclui "cenário passou" quando o hook nem foi invocado.

**Conferência empírica:** após smoke test pós-merge falho do operador, `Get-ChildItem C:\Users\rezen\test-*.*` veio vazio — confirmando que o **agente do Claude Code não caiu nesse gotcha** durante a validação destrutiva da branch da 4.2. Os 5 cenários da branch foram reais. Mas o operador caiu em smoke test pós-merge, evidenciando que o risco existe e pode atingir qualquer sessão futura — agente ou humano.

**Categoria do achado:** não é bug de código. É padrão de validação que precisa virar regra formal antes de 4.3+. O risco real não é só esse gotcha específico do PowerShell — é o **princípio mais geral**: "comando rodou sem erro visível" ≠ "cenário foi exercitado".

Esta sub-etapa **não toca em código**. Registra a lição completa em quatro lugares (ADR-011, `decisoes.md`, `hooks-pendentes.md`, `progresso.md`), formaliza o padrão de validação destrutiva para sub-etapas seguintes, e versiona este próprio prompt.

## Padrões que estreiam nesta etapa

1. **Primeiro ADR de processo, não arquitetural.** ADR-001 a ADR-010 são sobre stack, layout, débitos. ADR-011 é sobre **como validar destrutivamente** — decisão de método, não de produto.
2. **Princípio "comando rodou sem erro ≠ cenário exercitado" formalizado.** Toda validação destrutiva da Camada 3 em diante segue o padrão prescrito no ADR-011.
3. **Sub-etapa doc-only sem precedente similar.** Diferente da 4.0.1 (que era fix de código), a 4.2.1 é puramente registro de aprendizado.

## Escopo decidido (calibrado com operador antes da redação)

### Arquivos modificados/criados

```
docs/adrs.md                    ← edição (apenda ADR-011)
docs/decisoes.md                ← edição (subseção + histórico)
docs/hooks-pendentes.md         ← edição (nota de cuidado + data)
docs/progresso.md               ← edição (lição adicional + sub-etapa + histórico)
docs/prompt-etapa-4-2-1.md      ← novo (este próprio prompt)
```

**Sem mudança de código.** Sem hooks novos. Sem ajuste em hooks existentes.

### ADR-011 — Conteúdo a apendar em `docs/adrs.md`

Inserir após ADR-010:

```markdown
## ADR-011 — Padroes de validacao destrutiva

**Status:** Aceito
**Data:** 2026-MM-DD

### Contexto

Validacao destrutiva (executada na branch da etapa ou em smoke test pos-merge) e instrumento de qualidade de primeira linha do projeto. Camada 1 estabeleceu o principio: "validacao destrutiva manual e nao-negociavel; encontrou 3 bugs que toda automacao validou como verde" (retrospectiva da Camada 1).

Sub-etapa 4.2 expos uma armadilha do metodo: validacao que parece passar pode nao ter exercitado o hook. Em smoke test pos-merge da 4.2, operador executou tres cenarios destrutivos. Todos reportaram "comando rodou sem erro visivel" — mas `git status` mostrava working tree limpo em todos. Hook nao foi invocado em nenhum cenario. Causa raiz: `[System.IO.File]::WriteAllText` com path relativo em PowerShell grava em `[System.Environment]::CurrentDirectory`, nao em `$PWD`. Quando a sessao faz `cd` para entrar no repo, esses dois caminhos divergem. Arquivo e criado em `C:\Users\<user>\`, invisivel ao git rodando em `C:\projetos\...`.

Investigacao posterior confirmou que o agente do Claude Code **nao caiu** nesse gotcha durante a validacao destrutiva da branch da 4.2 (provavel: agente foi spawnado ja dentro do diretorio do repo, sincronizando ambos automaticamente). Mas o operador caiu, evidenciando que o risco e real em sessoes onde `$PWD` e `Environment.CurrentDirectory` divergem.

O ponto critico nao e o gotcha especifico do PowerShell. E o **principio mais geral**: "comando rodou sem erro" e premissa fraca para concluir "cenario foi exercitado". Sem verificacao explicita de pre-condicao, validacao destrutiva produz falsos positivos sem alerta.

### Decisao

Toda validacao destrutiva (na branch da etapa ou em smoke test pos-merge) **deve incluir verificacao explicita de pre-condicao** antes de cada cenario.

**Padroes obrigatorios:**

1. **Apos criar arquivo de teste:** `Test-Path .\arquivo` (ou equivalente). Esperado: `True`. Se `False`, parar e investigar — arquivo nao foi criado onde esperado.
2. **Antes de `git commit`:** rodar `git status` e confirmar que ha arquivo staged. Se sair `nothing to commit, working tree clean`, parar — cenario nao tem entrada.
3. **Apos comando que deveria falhar:** verificar `$LASTEXITCODE` (PowerShell) ou similar. Esperar codigo `!= 0`. Se vier `0`, cenario nao reproduziu o erro esperado.
4. **Para `[System.IO.File]::WriteAllText` com path relativo em PowerShell:** sincronizar previamente `[System.Environment]::CurrentDirectory = (Get-Location).Path`, OU usar caminho absoluto (`"$PWD\arquivo"`). Sem sincronizacao, arquivo pode ser gravado em diretorio diferente do `git`.

**Reportar resultado de cada pre-condicao no PR body** da sub-etapa. Nao basta listar "cenarios validados" — listar tambem as pre-condicoes verificadas e seus valores observados.

### Alternativas consideradas

- **Confiar em "o comando nao deu erro visivel"** — rejeitada. Foi exatamente a hipotese que falhou no smoke test pos-merge da 4.2. Falsos positivos sao silenciosos por natureza; nao ha mecanismo de detecao se nao houver verificacao explicita.
- **Forcar uso de caminhos absolutos em todos os scripts de validacao** — considerada e parcialmente adotada. Mais robusto, mas verboso. Sincronizacao previa de `Environment.CurrentDirectory` resolve o caso PowerShell sem afetar legibilidade.
- **Criar tooling automatico** (linter de scripts de validacao destrutiva) — rejeitada como prematura. Padrao primeiro, automacao depois quando justificar.
- **Limitar validacao destrutiva a agente apenas** (que nao caiu no gotcha) — rejeitada. Operador precisa validar pos-merge em ambiente real; nao pode delegar essa responsabilidade.

### Consequencias

**Aceitas:**

- Prompts de validacao destrutiva ficam mais verbose (cenarios com `Test-Path`, `git status` explicitos).
- Operador e agente seguem o mesmo padrao — sem atalhos por familiaridade do operador com o ambiente.

**Ganhos:**

- Zero falsos positivos silenciosos em validacao destrutiva.
- Padrao replicavel para sub-etapas seguintes da Camada 3 (4.3, 4.4) e qualquer hook futuro.
- Aprendizado registrado em ADR formal — nao se perde em prosa de retrospectiva.
- Reforco do principio consolidado da Camada 1: "validacao destrutiva e instrumento de qualidade de primeira linha". O gotcha mostrou que o principio precisa de gates explicitos para ser eficaz.
```

### Atualização de `docs/decisoes.md`

Adicionar nova subseção sob "Camada 3 — Configuração do Claude Code", **após** "Padrao orquestrador 1:N para `pre-commit` (Sub-etapa 4.2)" e **antes** de "Claude Code hooks nativos":

```markdown
### Padroes de validacao destrutiva (Sub-etapa 4.2.1)

Ratificado em ADR-011. Toda validacao destrutiva — na branch da etapa ou em smoke test pos-merge — segue tres regras:

1. **Pre-condicao explicita apos cada criacao de arquivo:** `Test-Path` (ou equivalente). Se `False`, parar.
2. **`git status` antes de `git commit`** para confirmar arquivo staged. Se `nothing to commit`, parar.
3. **Verificacao de exit code apos comando que deveria falhar.** Esperar codigo `!= 0`; se vier `0`, cenario nao reproduziu erro esperado.

**Para PowerShell + `[System.IO.File]::WriteAllText` com path relativo:** sincronizar previamente:

\```powershell
[System.Environment]::CurrentDirectory = (Get-Location).Path
\```

Ou usar path absoluto (`"$PWD\arquivo"`). Sem isso, arquivo e gravado em diretorio que pode divergir do `$PWD` (gotcha confirmado em smoke test pos-merge da 4.2).

**Reportar pre-condicoes verificadas no PR body** — nao basta listar "cenarios validados". Listar tambem o valor observado em cada pre-condicao. Falsos positivos silenciosos sao detectados apenas por verificacao explicita.

**Aplica retroativamente:** sub-etapas 4.3+ devem incluir esses gates no roteiro de validacao destrutiva. Sub-etapas 4.0 a 4.2 ja mergeadas nao sao revistas — confirmacao empirica posterior (smoke test pos-merge da 4.2 corrigido) validou que o codigo dessas sub-etapas esta correto.
```

Adicionar entrada no histórico (final do arquivo):

```markdown
- **2026-MM-DD** — Sub-etapa 4.2.1 concluida: registra padroes de validacao destrutiva. ADR-011 formalizado. Licao descoberta em smoke test pos-merge da 4.2 onde `[System.IO.File]::WriteAllText` com path relativo em PowerShell criou arquivos em `[System.Environment]::CurrentDirectory` (`C:\Users\rezen\`) em vez de `$PWD` (`C:\projetos\financas-lab\`). Hook nao foi invocado. Comando rodou sem erro visivel. Conferencia empirica (`Get-ChildItem C:\Users\rezen\test-*.*` vazio) confirmou que agente do Claude Code nao caiu nesse gotcha — risco existe apenas em sessoes onde `$PWD` e `Environment.CurrentDirectory` divergem. Sub-etapa doc-only — sem mudanca de codigo. Mergeado via PR #XX.
```

### Atualização de `docs/hooks-pendentes.md`

**Operação A** — Adicionar nova seção **antes** de "Hooks de processo":

```markdown
## Notas de cuidado para validacao destrutiva

Itens que nao sao hooks automatizaveis mas precisam ser observados em scripts e prompts futuros. Formalizados em ADR-011.

- **`[System.IO.File]::WriteAllText` com path relativo em PowerShell** grava em `[System.Environment]::CurrentDirectory`, nao em `$PWD`. Quando sessao fez `cd` para entrar no repo, esses caminhos divergem. Sincronizar previamente com `[System.Environment]::CurrentDirectory = (Get-Location).Path` ou usar path absoluto. Sem isso, arquivo vai parar em diretorio invisivel ao `git`. (Descoberto em smoke test pos-merge da 4.2, registrado na 4.2.1.)
- **`git commit` retornando `nothing to commit, working tree clean`** em validacao destrutiva e sinal de falso positivo, nao de "cenario nao se aplica". Indica que `git add` falhou silenciosamente — arquivo nao foi staged. Rodar `git status` antes de cada `git commit` em cenarios destrutivos e padrao consolidado.
- **`Test-Path` apos `WriteAllText`** e padrao obrigatorio para validacao destrutiva conforme ADR-011. Se retornar `False`, parar e investigar — nao prosseguir com `git add`/`git commit`.
```

**Operação B** — Atualizar data no topo:

```markdown
**Última atualização:** 2026-MM-DD (Sub-etapa 4.2.1 — padroes de validacao destrutiva)
```

### Atualização de `docs/progresso.md`

**A.** Atualizar "Última atualização": `2026-MM-DD (Sub-etapa 4.2.1 — padroes de validacao destrutiva)`.

**B.** Na seção "Camada 3 — Configuração do Claude Code", subseção "Sub-etapas concluídas", adicionar em ordem cronológica (após 4.2):

```markdown
- **4.2.1 — Padroes de validacao destrutiva** (2026-MM-DD): sub-etapa doc-only registrando licao descoberta em smoke test pos-merge da 4.2. `[System.IO.File]::WriteAllText` com path relativo em PowerShell grava em `[System.Environment]::CurrentDirectory` (nao em `$PWD`), produzindo falso positivo silencioso quando sessao fez `cd`. ADR-011 formaliza padroes de validacao destrutiva: `Test-Path` apos criar arquivo, `git status` antes de `git commit`, verificacao de exit code, sincronizacao de `Environment.CurrentDirectory`. Aplica retroativamente a sub-etapas 4.3+; 4.0-4.2 nao sao revistas (smoke test corrigido confirmou codigo correto). PR #XX.
```

**C.** Localizar "Lições da Sub-etapa 4.2" (já existente, criada na 4.2). **Adicionar terceira lição** em "Licoes de ambiente":

```markdown
3. **`[System.IO.File]::WriteAllText` com path relativo em PowerShell grava em `[System.Environment]::CurrentDirectory`, nao em `$PWD`.** Descoberto em smoke test pos-merge (operador, nao agente). Quando sessao PowerShell faz `cd` para entrar no repo apos abrir, `$PWD` e atualizado mas `Environment.CurrentDirectory` permanece no diretorio original (tipicamente home do usuario). Arquivo criado por `WriteAllText("arquivo.md", ...)` vai parar em `C:\Users\<user>\arquivo.md`; `git add arquivo.md` rodando em `C:\projetos\financas-lab\` falha com `pathspec did not match`; `git commit` reporta `nothing to commit, working tree clean`. Observador desatento conclui "cenario passou" quando hook nem foi invocado. **Conferencia empirica:** `Get-ChildItem C:\Users\rezen\test-*.*` veio vazio apos validacao destrutiva da branch da 4.2 — confirmando que agente do Claude Code nao caiu nesse gotcha (provavel: agente foi spawnado ja dentro do repo, sincronizando `Environment.CurrentDirectory`). Risco real existe em sessoes onde `$PWD` e `Environment.CurrentDirectory` divergem. **Categoria da licao:** padrao de validacao, nao bug de codigo. Resolvido na Sub-etapa 4.2.1 com ADR-011 formalizando "comando rodou sem erro != cenario exercitado" como principio de validacao destrutiva.
```

**D.** Adicionar seção "Lições da Sub-etapa 4.2.1" após as da 4.2:

```markdown
## Licoes da Sub-etapa 4.2.1

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum diretamente — padrao de validacao destrutiva e procedimento, nao codigo automatizavel facil. Nota de cuidado registrada em `hooks-pendentes.md`.)

### Licoes de ambiente

1. **Primeiro ADR de processo do projeto.** ADR-001 a ADR-010 sao decisoes de stack, layout, debitos. ADR-011 e o primeiro sobre **metodo de validacao**. Padrao de ADR funciona igualmente bem para decisoes de processo — vale considerar em sub-etapas futuras quando padroes operacionais aparecerem.
2. **Operador caiu, agente nao.** Risco assimetrico entre sessoes que iniciaram no repo (agente) e sessoes que fizeram `cd` para entrar (operador). ADR-011 cobre os dois casos com mesmo padrao — sem privilegio de "agente sabe o ambiente, pode pular o gate".
3. **Falso positivo silencioso e o pior tipo de bug em validacao.** "Nada deu erro visivel" e a mensagem mais perigosa quando o objetivo era reproduzir erro. ADR-011 ataca exatamente isso com verificacao de pre-condicao explicita.
```

**E.** Adicionar entrada no histórico geral:

```markdown
- **2026-MM-DD** — Sub-etapa 4.2.1 concluida: padroes de validacao destrutiva formalizados em ADR-011. Sub-etapa doc-only. Licao descoberta em smoke test pos-merge da 4.2: `[System.IO.File]::WriteAllText` com path relativo em PowerShell pode gravar em diretorio invisivel ao git. Mergeado via PR #XX.
```

### Versionar este próprio prompt

`docs/prompt-etapa-4-2-1.md` no commit de docs.

## Estado esperado ao iniciar

- Branch atual: `main`, sincronizada com `origin/main`.
- `git log --oneline -1` mostra commit `7cb3fec` (squash da 4.2) ou superior.
- `git config core.hooksPath` retorna `.githooks`.
- `docs/prompt-etapa-4-2-1.md` presente como **untracked**.
- Working tree limpo (exceto o prompt).
- Hooks ativos: Conventional Commits + encoding UTF-8.

**Pré-requisito de ambiente** (lição da 4.1):

- `powershell` (Windows PowerShell 5.1) disponível.

**Pré-requisito da validação destrutiva desta etapa:**

- Apesar de esta sub-etapa ser doc-only e **não exercitar** os padrões prescritos no ADR-011, vale validar que o roteiro continua funcionando. Não há cenário destrutivo de hook nesta sub-etapa — apenas confirmação de que os arquivos `.md` editados passam pelo hook de encoding UTF-8 (auto-validação implícita nos commits 3 e 4).

Validar com:

```bash
git status
git log --oneline -3
ls docs/prompt-etapa-4-2-1.md
git config core.hooksPath
```

Se algum item divergir, **parar e reportar**.

## Tarefas

### Tarefa 1 — Validar pré-requisitos

Rodar comandos da seção "Estado esperado ao iniciar". Se divergir, parar e reportar.

### Tarefa 2 — Criar branch

```bash
git checkout -b docs/etapa-4-2-1-padroes-validacao-destrutiva
```

Prefixo `docs/` em vez de `feat/`/`fix/`. Justificativa: sub-etapa exclusivamente documental, sem código novo ou correção de código. O Conventional Commits ativo aceita esse prefixo apenas em **mensagem de commit**, não em nome de branch — branches são livres por convenção interna. Manter padrão.

### Tarefa 3 — Antes de editar, ler arquivos vivos

```bash
cat docs/adrs.md
cat docs/decisoes.md
cat docs/hooks-pendentes.md
cat docs/progresso.md
```

**Confirmar especialmente:**

- `adrs.md` tem ADR-010 como último. ADR-011 entra após.
- `decisoes.md` tem subseção "Padrao orquestrador 1:N para `pre-commit` (Sub-etapa 4.2)" e subseção "Claude Code hooks nativos" — nova subseção "Padroes de validacao destrutiva" entra **entre** essas duas.
- `hooks-pendentes.md` tem seção "Hooks de processo" — nova seção "Notas de cuidado para validacao destrutiva" entra **antes** dela.
- `progresso.md` tem "Lições da Sub-etapa 4.2" — terceira lição entra em "Licoes de ambiente" existente. Nova seção "Lições da Sub-etapa 4.2.1" entra **após** as da 4.2.

Se alguma divergência, **parar e reportar**.

### Tarefa 4 — Apendar ADR-011 em `docs/adrs.md`

Conteúdo conforme seção "ADR-011" do escopo decidido. Posicionar após ADR-010. Substituir `2026-MM-DD` pela data real.

### Tarefa 5 — Editar `docs/decisoes.md`

Adicionar subseção "Padroes de validacao destrutiva (Sub-etapa 4.2.1)" entre as subseções existentes conforme escopo.

Adicionar entrada no histórico (final do arquivo). Substituir `2026-MM-DD`.

### Tarefa 6 — Editar `docs/hooks-pendentes.md`

Operações A e B conforme escopo.

### Tarefa 7 — Editar `docs/progresso.md`

Operações A, B, C, D, E conforme escopo. **Atenção à ordem cronológica em "Sub-etapas concluídas":** 4.0 → 4.0.1 → 4.1 → 4.2 → 4.2.1. Substituir `2026-MM-DD`.

### Tarefa 8 — Versionar este próprio prompt

`git add docs/prompt-etapa-4-2-1.md`.

### Tarefa 9 — Validação local

```bash
git status
git diff --cached
```

Esperado:
- 5 arquivos modificados/novos:
  - **Modificados:** `docs/adrs.md`, `docs/decisoes.md`, `docs/hooks-pendentes.md`, `docs/progresso.md`.
  - **Novo:** `docs/prompt-etapa-4-2-1.md`.

**Confirmar via `git diff --cached`** que nenhum arquivo de código está modificado. Esta sub-etapa é doc-only.

### Tarefa 10 — Commit 1 (ADR-011)

```bash
git add docs/adrs.md
git commit -m "docs: registra ADR-011 sobre padroes de validacao destrutiva"
```

Hook de encoding UTF-8 ativo valida o `.md`. Esperado: aceito.

### Tarefa 11 — Commit 2 (atualizações operacionais)

```bash
git add docs/decisoes.md docs/hooks-pendentes.md docs/progresso.md
git commit -m "docs: registra licao 4.2 sobre validacao destrutiva silenciosa em progresso decisoes e hooks-pendentes"
```

**Atenção:** mensagem com 99 chars no subject. Conventional Commits valida ≥10 chars; sem limite superior no projeto. Vale conferir.

### Tarefa 12 — Commit 3 (prompt versionado)

```bash
git add docs/prompt-etapa-4-2-1.md
git commit -m "docs: versiona prompt da sub-etapa 4.2.1"
```

### Tarefa 13 — Validação final antes de push

```bash
git status
git log --oneline -5
```

Esperado:
- Working tree limpo.
- 3 commits novos.
- Mensagens em Conventional Commits válido (hook aprovou).

**`check.ps1` opcional** — esta etapa não toca em código Java, mas confirma suite intocada se rodado.

## Restrições e freios

1. **Doc-only.** Não tocar em código (`.ps1`, `.java`, `.bash`, etc.). Apenas `.md` em `docs/`.

2. **Não criar hooks novos.** Nem em `.claude/hooks/`, nem em `.githooks/`.

3. **Não modificar hooks existentes** (`encoding-utf8.ps1`, `conventional-commits.ps1`, entrypoints, companheiros).

4. **Não modificar `setup.ps1` ou outros scripts.** Conteúdo do ADR-011 documenta padrão, mas implementação fica para sub-etapas futuras (ou nunca — pode permanecer só como nota de cuidado).

5. **Não modificar ADRs anteriores (001 a 010).** Apenas apendar ADR-011.

6. **Não revisar sub-etapas 4.0 a 4.2** retroativamente. Smoke test pós-merge corrigido da 4.2 confirmou que o código dessas sub-etapas está correto. ADR-011 aplica adiante.

7. **Encoding UTF-8 sem BOM** em todos os arquivos `.md`. Hook próprio valida.

8. **Linhas em branco antes e depois de headers Markdown** nos docs editados.

9. **Acentos permitidos em `.md`** (regra é "sem acentos em código", não em documentação). Mas evitar acentos em **conteúdo do ADR-011** especificamente — replicar estilo do projeto que mistura mensagens de hook (sem acento) e prosa de doc (com acento). Verificar coerência com ADRs anteriores.

10. **Não tomar decisão silenciosa em zona limítrofe.** Se alguma seção do `decisoes.md` tiver estrutura diferente do esperado, ou se algum item já estiver registrado, parar e reportar.

11. **Não sugerir próxima etapa espontaneamente.**

12. **Antes de escrever cada arquivo, ler arquivo vivo** (Tarefa 3).

13. **Ordem cronológica em `progresso.md`:** 4.0 → 4.0.1 → 4.1 → 4.2 → 4.2.1. Não inverter.

14. **Hook de encoding UTF-8 deve aprovar todos os commits desta sub-etapa.** Se algum for rejeitado, há problema de encoding no `.md` editado — parar e investigar.

15. **Não fazer `git reset --hard` nesta etapa.** Não há validação destrutiva. Sem cenários de teste = sem necessidade de limpeza.

## Estrutura de commits

Branch: `docs/etapa-4-2-1-padroes-validacao-destrutiva`

**Commit 1** — `docs: registra ADR-011 sobre padroes de validacao destrutiva`
- `docs/adrs.md`

**Commit 2** — `docs: registra licao 4.2 sobre validacao destrutiva silenciosa em progresso decisoes e hooks-pendentes`
- `docs/decisoes.md`
- `docs/hooks-pendentes.md`
- `docs/progresso.md`

**Commit 3** — `docs: versiona prompt da sub-etapa 4.2.1`
- `docs/prompt-etapa-4-2-1.md`

## Validação antes de abrir PR

```bash
.\scripts\check.ps1
git status
git log --oneline -4
```

Esperado:
- `check.ps1` passa.
- Working tree limpo.
- 3 commits novos.

## PR

Título: `docs: sub-etapa 4.2.1 — padroes de validacao destrutiva (ADR-011)`

Body sugerido:

```markdown
## Summary

Sub-etapa **doc-only** registrando licao descoberta em smoke test pos-merge da 4.2. Sem mudanca de codigo.

### O que aconteceu

Smoke test pos-merge da 4.2 (executado pelo operador apos merge do PR #41) revelou armadilha do metodo de validacao destrutiva em PowerShell. `[System.IO.File]::WriteAllText("arquivo.md", ..., encoding)` com path relativo grava em `[System.Environment]::CurrentDirectory`, NAO em `$PWD`. Quando sessao PowerShell fez `cd` para entrar no repo, esses dois caminhos divergiram (`Environment.CurrentDirectory` permaneceu em `C:\Users\rezen\`). Resultado: arquivos de teste destrutivo foram criados em `C:\Users\rezen\`; `git add` rodando em `C:\projetos\financas-lab\` falhou com `pathspec did not match`; `git commit` reportou `nothing to commit`. Observador desatento concluiria "cenarios passaram" quando hook nem foi invocado.

### Conferencia empirica

Operador rodou `Get-ChildItem C:\Users\rezen\test-*.*` apos descobrir o problema. Resultado: **vazio**. Significa que o agente do Claude Code **nao caiu** nesse gotcha durante a validacao destrutiva da branch da 4.2. Os 5 cenarios da branch foram reais. Hipotese provavel: agente foi spawnado ja dentro do diretorio do repo, sincronizando `Environment.CurrentDirectory` automaticamente. Risco existe apenas em sessoes onde `$PWD` e `Environment.CurrentDirectory` divergem (caso do operador, que abriu PowerShell na home e fez `cd`).

### Por que ADR formal

Primeiro ADR de **processo** do projeto. ADR-001 a ADR-010 cobrem stack, layout, debitos. ADR-011 cobre **metodo de validacao destrutiva** — decisao operacional, nao arquitetural.

Razao para formalizar: principio "comando rodou sem erro != cenario exercitado" precisa de gates explicitos em prompts de validacao destrutiva da Camada 3 em diante. Sem isso, falsos positivos silenciosos continuam possiveis — sub-etapa 4.2 mostrou que o operador caiu mesmo conhecendo o ambiente.

### O que muda adiante

Toda validacao destrutiva em sub-etapas 4.3+ deve incluir:

1. `Test-Path` apos criar arquivo de teste — esperado `True`.
2. `git status` antes de `git commit` — esperado arquivo staged.
3. Verificacao de `$LASTEXITCODE` apos comando que deveria falhar — esperado `!= 0`.
4. Para `[System.IO.File]::WriteAllText` com path relativo: sincronizar previamente `[System.Environment]::CurrentDirectory = (Get-Location).Path` OU usar path absoluto.

PR body de cada sub-etapa deve **reportar valores observados em cada pre-condicao**, nao apenas listar "cenarios validados".

### Aplicacao retroativa

ADR-011 **nao revisa sub-etapas 4.0 a 4.2 ja mergeadas**. Smoke test pos-merge corrigido da 4.2 (com `Test-Path` e `[System.Environment]::CurrentDirectory = (Get-Location).Path`) confirmou que o codigo dessas sub-etapas esta correto. Apenas o **metodo de validacao** muda dai em diante.

### Mudancas

- `docs/adrs.md`: apenda ADR-011 (Padroes de validacao destrutiva). Primeiro ADR de processo.
- `docs/decisoes.md`: subsecao "Padroes de validacao destrutiva (Sub-etapa 4.2.1)" sob "Camada 3". Entrada no historico.
- `docs/hooks-pendentes.md`: nova secao "Notas de cuidado para validacao destrutiva" com 3 itens (gotcha do `WriteAllText`, sinal de falso positivo do `git commit`, `Test-Path` obrigatorio). Data atualizada.
- `docs/progresso.md`: terceira licao em "Licoes de ambiente" da 4.2 (descricao completa do gotcha). Sub-etapa 4.2.1 em "Sub-etapas concluidas". Nova secao "Licoes da Sub-etapa 4.2.1". Entrada no historico.
- `docs/prompt-etapa-4-2-1.md`: prompt versionado.

### Validacao destrutiva

Nao aplicavel — sub-etapa doc-only. Verificacao implicita: os 3 commits passam pelo hook de Conventional Commits e pelo hook de encoding UTF-8 ja ativos. Se algum commit for rejeitado, e sinal de problema de encoding ou formato — investigar antes de seguir.

### Proximo passo

Sub-etapa 4.3 (hook universal de blank lines em Markdown via `pre-commit`). Roteiro de validacao destrutiva sera **o primeiro a aplicar ADR-011 desde a primeira linha** — usara `Test-Path`, `git status` explicito, e sincronizacao de `Environment.CurrentDirectory`. Decisao fora deste PR.
```

## Pós-criação do PR

1. Abrir PR via `gh pr create`.
2. Capturar o número.
3. Editar `docs/adrs.md`, `docs/decisoes.md`, `docs/hooks-pendentes.md`, `docs/progresso.md` substituindo `PR #XX` por `PR #<numero-real>` e `2026-MM-DD` pela data real.
4. Commit: `docs: atualiza numero do PR no historico`.
5. Push.
6. Esperar CI verde.
7. **Aguardar autorização explícita do operador antes do merge.**

## Estado esperado ao terminar

- Branch `docs/etapa-4-2-1-padroes-validacao-destrutiva` empurrada com 4 commits (3 + 1 update do PR).
- PR aberto, CI verde, **não mergeado**.
- `main` ainda no squash da 4.2.
- Working tree limpo.
- Reportar com `git log --oneline -5`, `git status`, `gh pr view --json number,state,statusCheckRollup`.

## O que NÃO fazer ao terminar

- Não mergear o PR.
- Não criar prompt da Sub-etapa 4.3.
- Não tocar em código (hooks, scripts, src/, frontend/, pom.xml, etc.).
- Não revisar ADRs anteriores (001 a 010).
- Não retro-aplicar ADR-011 a sub-etapas já mergeadas — apenas adiante.
- Não criar exemplos de validação destrutiva no ADR-011 que toquem o sistema real (sem `Test-Path` em arquivos reais, sem `git status` na branch atual — só prosa explicativa).
- Não sugerir "próximo passo" espontaneamente.
- Não relaxar regra "Test-Path obrigatório" se aparecer caso onde "parece desnecessário" — é regra dura.
- Não criar hooks que automatizem ADR-011 (decisão futura, fora do escopo).
