# Prompt — Etapa 1.5 da Camada 1

Você está trabalhando no projeto `financas-lab`. Estamos na **Etapa 1.5 da Camada 1** — configurar GitHub Actions CI básico que rode lint + test + build em todo PR e push em `main`.

## Antes de qualquer ação, leia em ordem:

1. `docs/visao.md` — propósito do projeto
2. `docs/decisoes.md` — stack, padrões e regras duras (especialmente seção "Convenções operacionais")
3. `docs/adrs.md` — ADR-001 (Java 21 + Maven), ADR-007 (Testes em três níveis), ADR-008 (modelo financeiro)
4. `docs/progresso.md` — estado atual, especialmente lições da 1.4
5. `docs/roadmap-camada-1.md` — Etapa 1.5 detalhada

Após ler, apresente um resumo de 5-7 linhas do que entendeu da tarefa antes de propor qualquer arquivo.

## Contexto importante

Esta etapa tem **feedback loop lento** comparado às anteriores: cada commit que você empurrar pra branch vai disparar um workflow no GitHub que demora 2-5 minutos pra completar. Não tente "consertar e empurrar" iterativamente sem antes validar localmente. Cada PR errado ao CI desperdiça tempo e minutos do GitHub Actions.

**Princípio operacional desta etapa:** validar localmente o máximo possível antes de empurrar pro CI. Se o `mvnw verify` passa local, deve passar no CI. Se não passa, o CI não vai te ajudar.

## Tarefa

### Branch

Criar a branch `feat/github-actions-ci` a partir de `main` atualizada.

### Arquivo a criar

#### `.github/workflows/ci.yml`

**Triggers:**
- `pull_request` em qualquer branch
- `push` apenas em `main`

**Job único: `build`**

- runs-on: `ubuntu-latest`
- timeout-minutes: 15 (proteção contra travamento)
- steps:

  1. **Checkout do código** (`actions/checkout@v4`)
  
  2. **Setup JDK 21** (`actions/setup-java@v4`):
     - distribution: `temurin`
     - java-version: `21`
     - cache: `maven` (cache automático do `~/.m2/repository`)
  
  3. **Validar wrapper Maven** (`./mvnw --version`) — falha rápida se wrapper estiver corrompido.
  
  4. **Build com testes** (`./mvnw verify`):
     - `verify` roda compile + test + integration-test + jacoco
     - **Aceitar que o teste `FinancasApplicationTests.contextLoads` vai falhar** nesta etapa (documentado na lição 7 da Etapa 1.4 — falta Testcontainers, será resolvido na 2.1)
     - **Solução temporária:** rodar `./mvnw verify -Dmaven.test.skip=false -DfailIfNoTests=false -Dtest='!FinancasApplicationTests'` excluindo o teste que falha esperadamente. Quando 2.1 for implementada, removemos a exclusão.
  
  5. **Upload do relatório JaCoCo** (`actions/upload-artifact@v4`):
     - name: `jacoco-report`
     - path: `target/site/jacoco/`
     - if: `always()` (sobe relatório mesmo se build falhar parcialmente)
     - retention-days: `7`

**Não incluir nesta etapa:**

- Matrix de versões Java (só Java 21 por enquanto — adicionar Java 22+ depois se justificar)
- Cache customizado além do `cache: maven` do setup-java
- Build de frontend (Next.js entra na 2.7)
- Lint customizado (Checkstyle e SpotBugs entram na 2.5)
- Notificações Slack/Discord
- Deploy automático
- Path filters (rodar CI em todo PR é fundação certa; otimização vem depois)

### Decisão sobre o teste excludente

O `-Dtest='!FinancasApplicationTests'` é gambiarra temporária. **Documente isso no PR e no commit message** explicitamente — é débito técnico consciente, não esquecimento. Será removido na Etapa 2.1.

**Alternativa que NÃO recomendo nesta etapa:** desabilitar o autoconfig de DataSource no profile test pra fazer `contextLoads` passar. Isso teria que ser desfeito na 2.1, e altera comportamento padrão do Spring Boot — pior que excluir um teste explicitamente.

### Validações obrigatórias antes de commitar

Execute localmente, na ordem:

1. `./mvnw verify -Dtest='!FinancasApplicationTests' -DfailIfNoTests=false 2>&1 | tail -30` — deve completar com BUILD SUCCESS. Esta é a mesma exata invocação que o CI vai fazer. Se passar local, passa no CI.

2. Confirma que existe `target/site/jacoco/index.html` após o build (artifact que vai ser uploaded).

3. Validar sintaxe do YAML:
   - `python -c "import yaml; yaml.safe_load(open('.github/workflows/ci.yml'))"` se Python disponível
   - Ou via web: cole conteúdo em https://www.yamllint.com/
   - Mas idealmente: GitHub Actions tem schema validation no editor web — vale visualizar uma vez

4. `git status` — apenas o `.github/workflows/ci.yml` como novo. Sem modificações em outros arquivos.

Se qualquer validação falhar, **pare e reporte. Não tente consertar criativamente.**

### Commit, PR e configuração de status check obrigatório

Após validações:

1. Mostre `git status` e peça confirmação.

2. Commit:
   - `git add .github/workflows/ci.yml`
   - `git commit -m "ci: adiciona workflow github actions com build e upload jacoco"`

3. Push.

4. Abrir PR via `gh` CLI:
   - Title: `ci: adiciona workflow github actions com build e upload jacoco`
   - Body com 4 seções:
     - **Summary** — bullets do que foi adicionado
     - **Comportamento esperado** — o que o CI valida em cada PR
     - **Débito técnico consciente** — explicar a exclusão do `FinancasApplicationTests` e quando será removida (Etapa 2.1)
     - **Validações executadas** — outputs locais

5. **IMPORTANTE — após abrir o PR, PARE.** Não inicie configuração de branch protection com status check obrigatório (essa é tarefa minha/usuário, não sua, porque exige interface web do GitHub e o status check só fica disponível após o primeiro CI rodar).

## Restrições importantes

- **Não criar pasta `src/`** além da estrutura existente.
- **Não modificar `pom.xml`** nesta etapa. CI consome o `pom.xml` como está.
- **Não criar Testcontainers config.** Etapa 2.1.
- **Não criar `.github/dependabot.yml` ou outras configs do GitHub.** Foco único: CI básico.
- **Não tentar instalar dependências adicionais (`apt install ...`)** no workflow — `setup-java@v4` já provê tudo.
- **Não usar versões com `@main` ou `@master`** em actions. Sempre versão fixa (`@v4`).
- **Não inventar steps adicionais** ("upload to codecov", "send slack notification", etc).
- **Pergunte antes de cada commit.**
- **Não force push.**
- **Não inicie próximas etapas ou configurações no GitHub web após abrir o PR — essa é decisão do usuário.**

## Pegadinhas conhecidas

1. **Versões de actions:** `actions/checkout@v4`, `actions/setup-java@v4`, `actions/upload-artifact@v4`. Não usar `@v3` ou `@latest`. `@v3` ainda funciona mas está deprecated; `@v4` é o atual.

2. **Cache do Maven com `setup-java@v4`:** o parâmetro `cache: maven` faz o trabalho. **Não combine** com `actions/cache@v4` separado pra Maven — duplica e pode dar conflito.

3. **`./mvnw` em workflow Linux:** o wrapper precisa ser executável. Geralmente já vem assim, mas se der "permission denied", adicionar step `chmod +x ./mvnw` antes do uso.

4. **`-Dtest='!Pattern'` em PowerShell vs bash:** sintaxe difere. PowerShell precisa escape de `!`. No CI (Linux bash), `-Dtest='!FinancasApplicationTests'` funciona direto. **Não copia comando do PowerShell pro YAML sem ajuste.**

5. **JaCoCo report sem testes:** se o build for executado e nenhum teste rodar (todos excluídos), JaCoCo gera warning "Skipping JaCoCo execution due to missing execution data file". O upload-artifact com `if: always()` vai upload diretório vazio — não é erro, mas vale notar.

6. **Branch protection ativa em `main`:** push direto está bloqueado, então o workflow tem que ser empurrado via PR. O primeiro PR vai rodar o CI sem que ele seja "required check" ainda — isso é correto, configuramos o "required" depois do primeiro CI verde.

## Observações de ambiente

- Sistema: Windows nativo, PowerShell, Docker Desktop.
- Disponível: Java 21.0.11, Maven Wrapper 3.9.9, Docker 29.0.1, gh CLI autenticado.
- Branch protection ativa em `main`.
- Repo público (GitHub Actions ilimitado).
- Working tree no início: clean (com `.claude/` untracked).
- ADR-008 (modelo financeiro): cada execução do CI consome minutos de GitHub Actions, mas em repo público o limite é ilimitado. Mesmo assim, evitar PRs desnecessários — ciclo de iteração rápido vem da validação local antes do push.
