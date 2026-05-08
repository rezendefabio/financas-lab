# Prompt — Atualização do `decisoes.md`

Esta não é uma etapa numerada do roadmap. É **manutenção da documentação** entre etapas. O `decisoes.md` ficou desatualizado em relação a decisões tomadas durante as Etapas 1.3, 1.4 e 1.5. Antes de iniciar a Etapa 2.1, atualizar `decisoes.md` é necessário para que ele continue sendo a foto operacional confiável que agentes futuros consultarão.

## Antes de qualquer ação, leia em ordem:

1. `docs/decisoes.md` — versão atual (vai ser atualizada)
2. `docs/adrs.md` — referência canônica das decisões fundadoras
3. `docs/progresso.md` — lições das etapas 1.3, 1.4 e 1.5 (fonte de detalhes técnicos descobertos)

Após ler, apresente um resumo de 5-7 linhas do que entendeu da tarefa antes de propor mudanças.

## Contexto: o que está faltando ser documentado

Durante as Etapas 1.3, 1.4 e 1.5 várias decisões concretas foram tomadas que **ainda não estão em `decisoes.md`**:

- Versões fixas de Spring Boot, MapStruct, JJWT, springdoc, JaCoCo, Testcontainers, Lombok e Maven Wrapper foram escolhidas
- Sistema operacional de desenvolvimento foi declarado (Windows nativo + PowerShell + Docker Desktop)
- Modelo de scripts de automação foi decidido (PowerShell em vez de Makefile)
- Padrão de CI foi estabelecido (GitHub Actions Ubuntu, Temurin 21, cache Maven via setup-java)
- Política de "débito técnico consciente" emergiu organicamente nos PRs

Esta atualização **consolida** essas decisões. Não cria decisão nova — só registra o que já está em vigor.

## Tarefa

### Branch

Criar a branch `chore/atualiza-decisoes-pos-camada-1` a partir de `main` atualizada.

### Modificações no `docs/decisoes.md`

#### 1. Atualizar tabela "Backend" da seção Stack

Substituir a tabela atual por uma com **versões reais fixadas**, não placeholders. Mantenha a estrutura de 3 colunas (Componente | Escolha | Versão), mas com as versões concretas:

| Componente | Escolha | Versão |
|---|---|---|
| Linguagem | Java | 21 (LTS) |
| Framework | Spring Boot | 3.5.14 |
| Build | Maven Wrapper | 3.9.9 (alinhar para 3.9.15+ em etapa futura) |
| Compilação | Java release flag | `<release>21</release>` (não `<source>` + `<target>`) |
| ORM | Spring Data JPA + Hibernate | gerenciado pelo BOM Spring Boot |
| Validação | Hibernate Validator (Bean Validation) | gerenciado pelo BOM Spring Boot |
| Migrations | Flyway + flyway-database-postgresql | gerenciado pelo BOM Spring Boot |
| Mapeamento DTO | MapStruct | 1.6.3 |
| Annotation Processing | Lombok antes de MapStruct (ordem obrigatória) | Lombok 1.18.46 (BOM), MapStruct processor 1.6.3 |
| Auth | Spring Security 6 + JJWT (jjwt-api/impl/jackson) | Spring Security via BOM, JJWT 0.12.7 |
| Banco | PostgreSQL | 16-alpine |
| Cache/Blacklist | Redis | 7-alpine |
| Driver Postgres | postgresql | gerenciado pelo BOM Spring Boot |
| Testes — framework | JUnit 5 + AssertJ + Mockito (uso comedido) | gerenciado pelo BOM Spring Boot |
| Testes — integração | Testcontainers (junit-jupiter + postgresql) | 1.21.4 (gerenciado pelo BOM, não 2.x) |
| Cobertura | JaCoCo Maven Plugin | 0.8.14 |
| Análise estática | SpotBugs + Checkstyle (configuração na Etapa 2.5) | a definir |
| API doc | springdoc-openapi-starter-webmvc-ui | 2.8.17 |

Adicione **logo abaixo da tabela** o seguinte parágrafo curto:

> **Política de versões:** versões fixadas explicitamente em `pom.xml` para dependências não gerenciadas pelo BOM do Spring Boot. Para gerenciadas pelo BOM, deixar sem `<version>` no `pom.xml` (BOM do parent resolve). Atualizações de versão exigem novo PR justificando — não atualizar em massa sem necessidade.

#### 2. Adicionar nova seção "Ambiente de desenvolvimento" após "Stack"

Inserir como subseção dentro de "Stack" ou como seção própria, conforme melhor encaixar visualmente:

### Ambiente de desenvolvimento

| Componente | Escolha |
|---|---|
| Sistema operacional dev | Windows nativo (não WSL2) |
| Shell | PowerShell |
| Container runtime | Docker Desktop |
| Versionamento | Git for Windows (`git version 2.45+`) |
| GitHub CLI | `gh` instalado e autenticado |
| Editor recomendado | qualquer editor que respeite `.editorconfig` (a ser criado se necessário) |

**Pegadinhas conhecidas de ambiente Windows** (lista viva, atualizada por etapa):

- PowerShell padrão lê UTF-8 errado sem `-Encoding UTF8` explícito (acentos viram `Ã³`, `Ã§`)
- `Out-File -Encoding UTF8` adiciona BOM por default — `javac` rejeita arquivos `.java` com BOM
- Comandos Unix (`tail`, `head`, `grep`, `sed`, `awk`) **não existem** no PowerShell. Equivalentes: `Select-Object -Last/-First`, `Select-String`
- `python` é o binário padrão (não `python3`)
- Maven Wrapper gerado no Windows não vem com bit de execução no git index. Linux Ubuntu (CI) precisa do bit. Solução: `git update-index --chmod=+x mvnw`

#### 3. Atualizar tabela "Infraestrutura" da seção Stack

Acrescentar 2 linhas à tabela existente:

| Componente | Escolha |
|---|---|
| Deploy | Railway no MVP (Fly.io como alternativa) |
| CI | GitHub Actions |
| Container local | Docker + Docker Compose |
| Observabilidade | Logfire ou Sentry (a definir no MVP) |
| Feature flags | Postergar — flags simples em DB se necessárias no MVP |
| Runner CI | `ubuntu-latest` |
| Java distribution no CI | Temurin 21 (via `actions/setup-java@v4`) |
| Cache CI | Maven via `cache: maven` do `setup-java@v4` (não combinar com `actions/cache@v4` separado) |

#### 4. Adicionar seção nova "Configuração crítica do `pom.xml`" dentro de "Convenções de código"

Logo após a subseção "Spring específico", adicionar:

### Configuração crítica do `pom.xml`

Decisões obrigatórias do `pom.xml` que **não devem ser alteradas sem novo ADR**:

- `<release>21</release>` no `maven-compiler-plugin` (não usar `<source>` + `<target>`)
- `<annotationProcessorPaths>` com **Lombok antes de MapStruct** (inverter quebra build)
- `<compilerArgs>` com `-Amapstruct.defaultComponentModel=spring` (mappers gerados como `@Component`; warning "options were not recognized" é esperado quando ainda não há `@Mapper` no projeto)
- `<scope>` correto:
  - Lombok: `provided`
  - JJWT impl/jackson: `runtime`
  - Test deps: `test`
  - MapStruct: runtime padrão (não declarar scope explícito)
- Spring Boot Maven Plugin com excludes do Lombok no `repackage`
- JaCoCo plugin com `prepare-agent` + `report` (regras de cobertura por camada entram na Etapa 2.4)

#### 5. Atualizar a seção "Convenções operacionais" — substituir item "Comandos atômicos do projeto (alvo)"

A seção atual fala em **Makefile**. Trocar para **scripts PowerShell**, refletindo o ambiente Windows nativo:

### Comandos atômicos do projeto (alvo)

A serem implementados via scripts PowerShell na Etapa 2.6. Padrão da fábrica:

| Comando | Função |
|---|---|
| `.\scripts\setup.ps1` | Sobe Postgres + Redis no Docker, instala deps, roda migrations |
| `.\scripts\dev.ps1` | Sobe backend + frontend em modo desenvolvimento |
| `.\scripts\test.ps1` | Roda testes unitários (rápidos, sem container) |
| `.\scripts\test-integration.ps1` | Roda integration + e2e (com Testcontainers) |
| `.\scripts\check.ps1` | Espelho do que CI roda em PR (`mvnw verify`) |
| `.\scripts\ship.ps1` | `check.ps1` + push + abre PR via `gh` |

#### 6. Adicionar seção nova "Política de débito técnico consciente"

Logo após "Convenções operacionais" e antes de "Comandos atômicos":

### Política de débito técnico consciente

Débitos técnicos planejados (decisões temporárias com data de resolução conhecida) **devem ser explícitos**, não silenciosos:

1. **No commit**: mensagem do commit menciona o débito (ex: "ci: exclui FinancasApplicationTests temporariamente até Etapa 2.1").
2. **No PR body**: seção explícita "Débito técnico consciente" descreve o quê, por quê é temporário, e quando será resolvido.
3. **No próprio código** (se possível): comentário acima da linha relevante referenciando a etapa de resolução.
4. **No `progresso.md`**: seção de lições da etapa registra o débito como pendência.

Exemplo real: exclusão de `FinancasApplicationTests` no `ci.yml` da Etapa 1.5, a ser removida na Etapa 2.1 quando Testcontainers configurar o datasource de teste.

**Não confundir débito consciente com bug ou esquecimento.** Débito consciente é decisão; bug é falha; esquecimento é processo ruim.

#### 7. Atualizar seção "Histórico de mudanças"

Adicionar entrada nova mantendo as anteriores intactas. **Atenção:** cada entrada é um bullet separado, não concatenar em um único bullet.

```markdown
### Histórico de mudanças

- **2026-05-08** — Atualização pós-Camada 1 etapas 1.3 a 1.5: versões fixadas no `pom.xml` (Spring Boot 3.5.14, MapStruct 1.6.3, JJWT 0.12.7, springdoc 2.8.17, JaCoCo 0.8.14), seção "Ambiente de desenvolvimento" criada com pegadinhas Windows, configuração crítica do `pom.xml` documentada, scripts PowerShell substituem Makefile, política de débito técnico consciente formalizada.
- **2026-05-06** — Criação inicial. Stack, arquitetura, convenções e modelo financeiro consolidados a partir dos ADRs 001-008.
```

Atualizar também o campo "Última atualização" no topo do documento de `2026-05-06` para `2026-05-08`.

### Validações antes de commitar

1. Mostre o diff completo de `docs/decisoes.md` (não apenas resumo).
2. Confirmar que **nenhuma decisão pré-existente foi removida** acidentalmente. Apenas inserções e substituições nos pontos solicitados.
3. Confirmar que tabelas Markdown estão sintaticamente corretas (separador `|---|---|---|` na segunda linha, todas as linhas com mesmo número de colunas).
4. Confirmar que `git status` mostra apenas `docs/decisoes.md` como modificado.

Se qualquer validação falhar, pare e reporte.

### Commit e PR

Após validações:

1. Mostre `git status` e peça confirmação.
2. Após confirmação:
   - `git add docs/decisoes.md`
   - `git commit -m "docs: atualiza decisoes pos camada 1 com versoes fixas e ambiente"`
3. Push.
4. Abrir PR via `gh` CLI:
   - Title: `docs: atualiza decisoes pos camada 1 com versoes fixas e ambiente`
   - Body com 3 seções:
     - **Summary** — bullets das mudanças
     - **Por que agora** — referência ao débito de documentação acumulado nas etapas 1.3-1.5
     - **Não inclui** — explicitar que NÃO há mudança de decisão; só consolidação

## Restrições importantes

- **Não altere `adrs.md`.** Decisões fundadoras já estão lá. Esta atualização é só do operacional.
- **Não altere `progresso.md`, `roadmap-camada-1.md`, `visao.md`** ou qualquer prompt-etapa-X.md.
- **Não invente decisões** que não foram tomadas. Cada item desta atualização tem fonte rastreável (`adrs.md`, lições do `progresso.md`, `pom.xml` real, `ci.yml` real).
- **Não remova decisões existentes** silenciosamente. Substituições são apenas onde explicitamente pedido.
- **Pergunte antes de cada commit.**
- **Não force push.**
- **Após abrir o PR, PARE.** Não inicie próximas etapas. Não sugira merge automático.

## Observações de ambiente

- Sistema: Windows nativo, PowerShell, Docker Desktop.
- Branch protection ativa em `main` com required check `CI/build`.
- Working tree no início: clean (com `.claude/` untracked).
- `gh` CLI autenticado.
- CI vai rodar automaticamente no PR — para passar, `decisoes.md` é só Markdown, sem efeito em build/test (deve passar trivialmente).
