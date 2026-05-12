# Prompt -- Sub-etapa 4.23: Subagent migration-writer + skill `/write-migration`

## Contexto

Sub-etapa 4.23 da Camada 3. Entrega o subagent `migration-writer` e a skill `/write-migration`.
O subagent le a `*Entity.java` de um bounded context, deriva as colunas SQL a partir das
anotacoes JPA, descobre o proximo numero Flyway e gera o arquivo `V<N>__cria_tabela_<name>.sql`.

Patron: subagent com `context: fork` (igual ao test-writer da 4.17). A 4.23 e o segundo subagent
gerador do projeto. Pre-requisito para a 4.24 (`/migrate` = `/write-migration` + `/write-test`
encadeados).

Lembrete de workflow: passar este prompt como TEXTO ao executor. Commitar o `.md` manualmente
apos o PR estar aberto.

---

## Padroes que estreiam

**Segundo subagent gerador do projeto.** Mesmo padrao do test-writer (4.17): subagent Sonnet
com `context: fork`, skill correspondente com `context: fork` + `agent: migration-writer`.
Nenhuma novidade estrutural -- replicacao categorizada.

**Derivacao de SQL a partir de anotacoes JPA.** Primeiro caso no projeto onde um subagent
le codigo Java e produz artefato SQL. O subagent le `*Entity.java`, interpreta `@Column`,
`@Id`, `@Embedded`, `@AttributeOverride`, `@Enumerated`, e produz `CREATE TABLE`.

---

## Escopo decidido

### Arquivo 1: `.claude/agents/migration-writer.md` (NOVO)

Conteudo completo prescrito abaixo. Use Write para criar com UTF-8 sem BOM.

```markdown
---
name: migration-writer
description: Gera arquivo Flyway SQL para bounded context. Le *Entity.java, deriva colunas de anotacoes JPA (@Column, @Id, @Embedded, @AttributeOverride, @Enumerated), descobre proximo numero Flyway via Glob, escreve V<N>__cria_tabela_<name>.sql. Recebe nome do bounded context (snake_case) como argumento.
tools: Read, Glob, Grep, Write
model: sonnet
---

Voce e o `migration-writer` do projeto **financas-lab** -- fabrica AI-native do operador Fabio.
Gera arquivos de migration Flyway SQL a partir de anotacoes JPA do Entity correspondente.
Segundo subagent gerador do projeto (primeiro: test-writer, 4.17).

## Identidade

Gerador de SQL idiomatico PostgreSQL. Le `*Entity.java`, interpreta anotacoes JPA, produz
`CREATE TABLE` correto e versionado via Flyway. Nao toca codigo Java. Nao executa Maven.

## Input

Nome do bounded context em snake_case. Exemplos: `conta`, `categoria`, `transacao`,
`meu_contexto`. Formato valido: `^[a-z][a-z0-9_]*$`.

## O que voce GERA

Arquivo `src/main/resources/db/migration/V<N>__cria_tabela_<name>.sql` com:

- Cabecalho de comentario padrao do projeto
- `CREATE TABLE <table_name> (...)` com todas as colunas do Entity
- Linha de TODO para constraints FK e indexes (que sao domain-specific)

Nao gera: FK constraints, CHECK constraints, indexes. Esses sao domain-specific e devem
ser adicionados manualmente.

## Fluxo de execucao

### Passo 1 -- Validar argumento

Se `$ARGUMENTS` vazio ou nao casa `^[a-z][a-z0-9_]*$`:
- Reporte: "ERRO: argumento invalido. Informe nome do bounded context em snake_case (ex: conta)."
- Termine.

### Passo 2 -- Derivar PascalCase

Algoritmo: split por `_`, capitalize primeira letra de cada parte, concatenar.
- `conta` -> `Conta`
- `meu_contexto` -> `MeuContexto`

### Passo 3 -- Localizar Entity

Path esperado:
```
src/main/java/com/laboratorio/financas/<name>/infrastructure/persistence/<PascalCase>Entity.java
```

Use Read para tentar ler o arquivo. Se nao existir:
- Reporte: "ERRO: Entity nao encontrada em <path>. Execute /feature <name> antes de /write-migration."
- Termine.

### Passo 4 -- Descobrir proximo numero Flyway

Use Glob com pattern `src/main/resources/db/migration/V*.sql`. Extraia o numero de cada
filename (o digito(s) apos `V` e antes de `__`). Calcule max + 1. Se nenhum arquivo
encontrado, use 1.

Exemplo: arquivos V1, V2, V3, V4 encontrados -> proximo e V5.

### Passo 5 -- Ler nome da tabela

No Entity lido, localize `@Table(name = "...")` e extraia o valor. Esse e o nome da tabela
no SQL. Se `@Table` nao existir, use o nome do bounded context (`$ARGUMENTS`) como nome
da tabela.

### Passo 6 -- Parsear campos e gerar colunas

Para cada campo de instancia (nao-estatico, nao-transient) no Entity:

**Caso: @Id**
- Tipo Java UUID + `columnDefinition = "uuid"` -> `<col_name> UUID PRIMARY KEY`

**Caso: @Column simples**
- Extraia `name` do `@Column` -> nome da coluna
- Derive tipo SQL pelo tipo Java (tabela abaixo)
- Derive nullability (regras abaixo)
- Gere: `<col_name> <SQL_TYPE> <NOT NULL se aplicavel>`

**Caso: @Enumerated(EnumType.STRING) + @Column**
- Tipo SQL: `VARCHAR(<length>)` onde length vem do `@Column(length = N)`
- Nullability: mesmas regras

**Caso: @Embedded com @AttributeOverride(s)**
- Ignore o campo Java embeddable em si
- Para CADA `@AttributeOverride(name = "...", column = @Column(name = "X", ...))`:
  - Extraia `name` do @Column aninhado -> nome da coluna
  - Se `@Column` tem `precision` e `scale` -> `NUMERIC(precision, scale)`
  - Se `@Column` tem `length` -> `VARCHAR(length)`
  - Nullability: se `nullable = false` no @Column aninhado -> NOT NULL
  - Gere uma linha por @AttributeOverride

**Campos a ignorar:**
- `protected/private <Classe>()` (construtor vazio para JPA) -- nao e campo
- Metodos (getters/setters) -- nao sao campos

### Tabela de mapeamento Java -> SQL

| Tipo Java | SQL PostgreSQL | Observacao |
|-----------|---------------|------------|
| UUID (+ @Id) | UUID PRIMARY KEY | Sempre sem NOT NULL separado (PK implica) |
| UUID (nao @Id) | UUID | Nullability separada |
| String | VARCHAR(N) | N de `@Column(length = N)`; sem length: VARCHAR(255) |
| boolean (primitive) | BOOLEAN NOT NULL | Primitive = nunca null |
| Boolean (wrapper) | BOOLEAN | Nullability separada |
| Instant | TIMESTAMPTZ | |
| LocalDate | DATE | |
| LocalDateTime | TIMESTAMP | |
| BigDecimal | NUMERIC(P, S) | P e S de `@Column(precision, scale)`; sem: NUMERIC(15, 2) |
| Long / long | BIGINT | |
| Integer / int | INTEGER | |
| Enum (com @Enumerated STRING) | VARCHAR(N) | N de @Column(length) |

### Regras de nullability

Aplicar em ordem de precedencia:

1. Campo @Id -> sem NOT NULL separado (PRIMARY KEY e suficiente)
2. Primitive (`boolean`, `int`, `long`) -> NOT NULL
3. `@NotNull` no campo -> NOT NULL
4. `@Column(nullable = false)` -> NOT NULL
5. Caso contrario: sem NOT NULL (campo nullable)

### Passo 7 -- Montar SQL

Formato prescrito:

```sql
-- V<N>: cria tabela <table_name>
-- Bounded context: <name>
-- Sub-etapa 4.23 da Camada 3

CREATE TABLE <table_name> (
    <coluna1>   <TIPO>   <NOT NULL|>,
    <coluna2>   <TIPO>   <NOT NULL|>,
    ...
);

-- TODO: adicionar constraints FK e indexes conforme necessidade de negocio
```

Alinhamento de colunas: alinhe os nomes, tipos e NOT NULL em colunas para facilitar leitura
(use espacos, nao tabs -- o hook de encoding exige UTF-8 sem TAB em SQL).

### Passo 8 -- Escrever arquivo

Path: `src/main/resources/db/migration/V<N>__cria_tabela_<name>.sql`
Encoding: UTF-8 sem BOM.
Use Write.

### Passo 9 -- Relatorio

Produza o relatorio no template prescrito.

## Template de output

```markdown
# Migration-writer para <name>

## Arquivo gerado

`src/main/resources/db/migration/V<N>__cria_tabela_<name>.sql` (<N> linhas).

## Colunas geradas

| Coluna | Tipo SQL | NOT NULL | Fonte no Entity |
|--------|----------|----------|----------------|
| id | UUID PRIMARY KEY | — | @Id @Column(columnDefinition="uuid") |
| ... | ... | ... | ... |

## Pendencias manuais

- **Constraints FK:** <lista de campos UUID nao-PK -- ex: conta_id, categoria_id>
- **Indexes:** nenhum gerado (domain-specific) -- adicionar conforme necessidade

## Decisoes de design

- <ex: campo contaId (UUID, nullable) derivado como UUID sem FK constraint>
- <ex: @Embedded saldoInicial expandido em 2 colunas via @AttributeOverride>
- <ex: proximo numero Flyway: V5 (anterior: V4__cria_tabela_transacao.sql)>
```

## Exemplos de referencia

### Exemplo: ContaEntity -> SQL esperado

Dado `ContaEntity.java` com campos (simplificado):
- `@Id UUID id` + `columnDefinition="uuid"`
- `@NotNull String nome` + `length=100`
- `@Enumerated STRING TipoConta tipo` + `length=30`
- `@Embedded MoneyEmbeddable saldoInicial` com @AttributeOverride valor (precision=19,scale=2) e moeda (length=3)
- `boolean ativa`
- `@NotNull Instant criadoEm`
- `@NotNull Instant atualizadoEm`

SQL esperado:
```sql
-- V5: cria tabela conta
-- Bounded context: conta
-- Sub-etapa 4.23 da Camada 3

CREATE TABLE conta (
    id                    UUID            PRIMARY KEY,
    nome                  VARCHAR(100)    NOT NULL,
    tipo                  VARCHAR(30)     NOT NULL,
    saldo_inicial_valor   NUMERIC(19, 2)  NOT NULL,
    saldo_inicial_moeda   VARCHAR(3)      NOT NULL,
    ativa                 BOOLEAN         NOT NULL,
    criado_em             TIMESTAMPTZ     NOT NULL,
    atualizado_em         TIMESTAMPTZ     NOT NULL
);

-- TODO: adicionar constraints FK e indexes conforme necessidade de negocio
```

### Exemplo: campo UUID FK (nullable)

Campo `@Column(name = "categoria_id", columnDefinition = "uuid") private UUID categoriaId;`
-> `categoria_id UUID` (sem NOT NULL -- nullable FK)

Campo `@Column(name = "conta_id", columnDefinition = "uuid", nullable = false) private UUID contaId;`
-> `conta_id UUID NOT NULL` (sem FK constraint -- adicionar manualmente)

## O que NAO fazer

- **NAO executar Maven.** Nao ha compilacao para validar SQL. Verifique a logica manualmente.
- **NAO gerar FK constraints.** Sao domain-specific e dependem de ordem de criacao de tabelas.
- **NAO gerar indexes.** Sao domain-specific. Deixe o TODO no final.
- **NAO modificar a Entity ou qualquer codigo Java.**
- **NAO tentar auto-corrigir em loop** se encontrar anotacao desconhecida. Documente no relatorio
  como "anotacao nao mapeada: <anotacao>" e trate o campo como nullable sem tipo especifico.
- **NAO sobrescrever migration existente.** Se `V<N>__...` ja existir com o mesmo N calculado,
  reporte o conflito e termine sem escrever.
- **NAO adivinhar campos.** So gera colunas para campos explicitamente anotados com @Column
  ou @AttributeOverride. Campos sem @Column sao ignorados (ex: campos transient).
```

---

### Arquivo 2: `.claude/skills/write-migration/SKILL.md` (NOVO)

```markdown
---
name: write-migration
description: Gera migration Flyway SQL para bounded context via subagent migration-writer. Le *Entity.java, deriva colunas de anotacoes JPA, descobre proximo numero Flyway, escreve V<N>__cria_tabela_<name>.sql. Argumento: nome do bounded context em snake_case.
disable-model-invocation: true
context: fork
agent: migration-writer
argument-hint: [nome-do-bounded-context]
allowed-tools: Read Glob Grep Write
---

Gere a migration Flyway SQL para o bounded context `$ARGUMENTS` seguindo todas as instrucoes
do seu system prompt.

Leia `<PascalCase>Entity.java`, derive as colunas das anotacoes JPA, descubra o proximo
numero Flyway via Glob em `src/main/resources/db/migration/V*.sql`, e escreva o arquivo
`V<N>__cria_tabela_<name>.sql`. Produza o relatorio no template prescrito.

Se Entity nao existir ou argumento invalido: reporte o erro literal e termine.
```

---

### Arquivo 3: `docs/progresso.md` (EDITAR)

Leia o arquivo antes de editar. Aplique as 4 mudancas abaixo via Edit.

**Mudanca 1 -- linha "Ultima atualizacao":**

Substitua o valor atual por:
```
**Última atualização:** 2026-05-12 (Sub-etapa 4.23 -- Subagent migration-writer + skill /write-migration)
```

**Mudanca 2 -- marcar `/write-migration` como concluido** (secao "Criterios de pronto"):

Substitua:
```
- [ ] Subagent migration-writer + skill `/write-migration`
```
Por:
```
- [x] Subagent migration-writer + skill `/write-migration` -- concluido 4.23
```

**Mudanca 3 -- adicionar 4.23 em "Sub-etapas concluidas"** (logo antes da entrada 4.22):

```
- **4.23 -- Subagent migration-writer + skill `/write-migration`** (2026-05-12):
  segundo subagent gerador do projeto (primeiro: test-writer, 4.17). Le `*Entity.java`,
  interpreta anotacoes JPA (`@Column`, `@Id`, `@Embedded`, `@AttributeOverride`,
  `@Enumerated`), descobre proximo numero Flyway via Glob, gera `V<N>__cria_tabela_<name>.sql`.
  Nao gera FK constraints nem indexes (domain-specific). Padrao: subagent Sonnet com
  `context: fork` (mesma categoria do test-writer). Pre-requisito para a 4.24 (`/migrate`
  encadeia migration-writer + test-writer). PR #69.
```

**Mudanca 4 -- adicionar em "Historico de mudancas":**

```
- **2026-05-12** -- Sub-etapa 4.23 concluida: subagent `migration-writer` em
  `.claude/agents/migration-writer.md` + skill `/write-migration` em
  `.claude/skills/write-migration/SKILL.md`. Derivacao SQL de anotacoes JPA.
  Pre-requisito para 4.24. CLAUDE.md NAO atualizado. PR #69.
```

---

### Arquivo 4: `docs/decisoes-claude-code.md` (EDITAR)

Leia o arquivo antes de editar. Adicione subsecao antes do "Historico de mudancas"
(linha em branco antes e depois de cada `##`):

```
## Sub-etapa 4.23 -- Subagent migration-writer

### Subagent vs skill direta

Decisao: subagent (mesmo padrao do test-writer, 4.17) -- nao skill direta (padrao da 4.19).
Criterio confirmado na calibracao da 4.23: derivacao Java -> SQL exige raciocinio sobre tipos
e anotacoes, nao e sequencia procedural simples. Skill direta e adequada para sequencias
de comandos shell com logica simples; subagent e adequado para raciocinio de dominio.

### Escopo do SQL gerado

Decisao deliberada: migration-writer gera apenas `CREATE TABLE` basico.

- **FK constraints:** NAO geradas. Dependem de ordem de criacao de tabelas e de conhecimento
  do schema completo -- informacao que o subagent nao tem de forma confiavel.
- **Indexes:** NAO gerados. Domain-specific; o desenvolvedor conhece os padroes de query.
- **CHECK constraints:** NAO geradas. Logica de negocio; risco de erro silencioso alto.

Resultado: SQL gerado e sempre correto (sem FK/CHECK errados), mas incompleto (exige
complemento manual). Trade-off aceito: corretude > completude automatica.

### Versao Flyway

Numeracao automatica (max das migrations existentes + 1). Colisao impossivel durante uso
normal (um desenvolvedor, sem migrations paralelas). Se colisao ocorrer: migration-writer
reporta o conflito e nao sobrescreve.
```

Adicione ao "Historico de mudancas":

```
- **2026-05-12** -- Sub-etapa 4.23 concluida: subagent `migration-writer` (segundo gerador
  do projeto). Derivacao Java -> SQL de anotacoes JPA. Escopo deliberadamente basico
  (sem FK/CHECK/indexes). Numeracao Flyway automatica. Pre-requisito para 4.24. PR #69.
```

---

## Estado esperado ao iniciar

```powershell
git branch --show-current   # deve retornar: main
git status                  # deve retornar: nothing to commit, working tree clean
git log --oneline -1        # deve mostrar squash do PR #68 (4.22) no topo
```

Se qualquer condicao falhar: pare e reporte.

---

## Tarefas

### Tarefa 1 -- Verificar estado inicial (ADR-011)

```powershell
git branch --show-current
git status
git log --oneline -1
```

### Tarefa 2 -- Criar branch

```powershell
git checkout -b feat/etapa-4-23-migration-writer
git branch --show-current   # deve retornar: feat/etapa-4-23-migration-writer
```

### Tarefa 3 -- Criar `.claude/agents/migration-writer.md`

Pre-condicao:
```powershell
Test-Path ".claude/agents/migration-writer.md"   # deve retornar: False
```

Use Write para criar `.claude/agents/migration-writer.md` com o conteudo prescrito.
Encoding UTF-8 sem BOM.

Pos-condicao:
```powershell
Test-Path ".claude/agents/migration-writer.md"   # deve retornar: True
Select-String "migration-writer" ".claude/agents/migration-writer.md"  # deve ter match
Select-String "model: sonnet" ".claude/agents/migration-writer.md"     # deve ter match
```

### Tarefa 4 -- Criar `.claude/skills/write-migration/SKILL.md`

Pre-condicao:
```powershell
Test-Path ".claude/skills/write-migration/"   # deve retornar: False
```

```powershell
New-Item -ItemType Directory -Path ".claude/skills/write-migration/"
```

Use Write para criar `.claude/skills/write-migration/SKILL.md` com o conteudo prescrito.
Encoding UTF-8 sem BOM.

Pos-condicao:
```powershell
Test-Path ".claude/skills/write-migration/SKILL.md"          # deve retornar: True
Select-String "context: fork" ".claude/skills/write-migration/SKILL.md"     # deve ter match
Select-String "agent: migration-writer" ".claude/skills/write-migration/SKILL.md"  # deve ter match
```

### Tarefa 5 -- Primeiro commit

```powershell
git add ".claude/agents/migration-writer.md" ".claude/skills/write-migration/SKILL.md"
git status
# deve mostrar exatamente esses 2 arquivos staged
```

Commit (scope `claude` sem ponto -- licao 4.19):
```
feat(claude): adiciona subagent migration-writer e skill /write-migration
```

### Tarefa 6 -- Smoke: `/write-migration conta`

Execute a skill:
```
/write-migration conta
```

O subagent deve:
1. Validar argumento (`conta` e snake_case valido)
2. Derivar PascalCase: `Conta`
3. Ler `src/main/java/com/laboratorio/financas/conta/infrastructure/persistence/ContaEntity.java`
4. Fazer Glob em `src/main/resources/db/migration/V*.sql` -> encontrar V1-V4 -> proximo: V5
5. Parsear campos de ContaEntity e gerar SQL
6. Escrever `src/main/resources/db/migration/V5__cria_tabela_conta.sql`

**Verificacao do conteudo gerado** -- confirme que o arquivo contem:
```powershell
Get-Content "src/main/resources/db/migration/V5__cria_tabela_conta.sql" -Encoding UTF8
```

Conteudo esperado (ordem e alinhamento podem variar, colunas devem estar presentes):
- `id` como `UUID PRIMARY KEY`
- `nome` como `VARCHAR(100) NOT NULL`
- `tipo` como `VARCHAR(30) NOT NULL`
- `saldo_inicial_valor` como `NUMERIC(19, 2) NOT NULL`
- `saldo_inicial_moeda` como `VARCHAR(3) NOT NULL`
- `ativa` como `BOOLEAN NOT NULL`
- `criado_em` como `TIMESTAMPTZ NOT NULL`
- `atualizado_em` como `TIMESTAMPTZ NOT NULL`
- Linha de TODO para FK/indexes

**Criterios de sucesso do smoke:**
- [ ] Arquivo `V5__cria_tabela_conta.sql` criado
- [ ] Todas as 8 colunas presentes com tipos corretos
- [ ] `saldo_inicial_valor` e `saldo_inicial_moeda` derivados de @AttributeOverride
- [ ] `ativa` como `BOOLEAN NOT NULL` (campo primitive boolean)
- [ ] Relatorio exibido pelo subagent

**Apos verificacao -- deletar o arquivo de smoke** (evita migration duplicada):
```powershell
Remove-Item "src/main/resources/db/migration/V5__cria_tabela_conta.sql"
Test-Path "src/main/resources/db/migration/V5__cria_tabela_conta.sql"   # deve retornar: False
git status   # deve retornar: nothing to commit, working tree clean
```

Se qualquer criterio falhar: reporte erro literal. Nao tente auto-corrigir.

### Tarefa 7 -- Atualizar docs

Aplique as mudancas prescritas nos Arquivos 3 e 4 (`progresso.md`, `decisoes-claude-code.md`).
Leia cada arquivo antes de editar. Nao altere nenhum trecho alem dos prescritos.

Pos-condicao:
```powershell
Select-String "4.23" "docs/progresso.md" | Measure-Object | Select-Object -Expand Count
# deve retornar > 0

Select-String "4.23" "docs/decisoes-claude-code.md" | Measure-Object | Select-Object -Expand Count
# deve retornar > 0
```

### Tarefa 8 -- Segundo e terceiro commits

```powershell
git add "docs/progresso.md"
git status
```
Commit:
```
docs(progresso): registra sub-etapa 4.23 e skill /write-migration como concluida
```

```powershell
git add "docs/decisoes-claude-code.md"
git status
```
Commit:
```
docs(decisoes): registra subagent migration-writer e decisoes de geracao SQL
```

### Tarefa 9 -- Validacao pre-ship

```powershell
git log --oneline feat/etapa-4-23-migration-writer ^main
# deve mostrar exatamente 3 commits

git diff main --name-only
# deve mostrar exatamente:
#   .claude/agents/migration-writer.md
#   .claude/skills/write-migration/SKILL.md
#   docs/decisoes-claude-code.md
#   docs/progresso.md

git status
# deve retornar: nothing to commit, working tree clean

Select-String "context: fork" ".claude/skills/write-migration/SKILL.md"
# deve ter match

Select-String "agent: migration-writer" ".claude/skills/write-migration/SKILL.md"
# deve ter match

Select-String "model: sonnet" ".claude/agents/migration-writer.md"
# deve ter match

Select-String "AttributeOverride" ".claude/agents/migration-writer.md"
# deve ter match (instrucao de @Embedded presente)
```

### Tarefa 10 -- Entregar via `/ship`

```
/ship
```

---

## Restricoes e freios

- NAO usar scope `.claude` em commits -- usar `claude` sem ponto (licao 4.19).
- NAO modificar o CLAUDE.md.
- NAO editar arquivos Java do projeto.
- NAO commitar `V5__cria_tabela_conta.sql` -- e artefato de smoke, deve ser deletado.
- NAO rodar `check.ps1` ou `mvnw` manualmente alem do que `/ship` ja faz.
- Smoke falhou? Reporte erro literal. Nao tente auto-corrigir em loop.
- Se hook bloquear commit: leia a mensagem, corrija sem `--no-verify`.

---

## Estrutura de commits

```
feat(claude): adiciona subagent migration-writer e skill /write-migration
docs(progresso): registra sub-etapa 4.23 e skill /write-migration como concluida
docs(decisoes): registra subagent migration-writer e decisoes de geracao SQL
```

---

## Estado esperado ao terminar

- PR #69 aberto (via `/ship`).
- Working tree limpa.
- `.claude/agents/migration-writer.md` existente e versionado.
- `.claude/skills/write-migration/SKILL.md` existente e versionado.
- `docs/progresso.md` e `docs/decisoes-claude-code.md` atualizados.
- `V5__cria_tabela_conta.sql` DELETADO (era artefato de smoke).
- Migrations existentes (V1-V4) intactas.

---

## O que NAO fazer ao terminar

- NAO fazer merge sem instrucao do operador.
- NAO commitar o arquivo de prompt na branch (operador faz manualmente).
- NAO rodar `/ship` mais de uma vez.
