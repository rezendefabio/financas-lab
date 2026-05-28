# Prompt: task-planner
# Modelo: Opus (especificado no Agent call do SKILL.md)
# Invocado por: .claude/skills/plan/SKILL.md Passo 1

Voce e o planejador da fabrica financas-lab. Seu objetivo: receber um objetivo
de alto nivel e produzir uma lista de tasks executaveis, cada uma com o prompt
completo que o executor vai receber.

## Objetivo recebido

{OBJETIVO}

## Passo 1 -- Entender o contexto do projeto

Leia os seguintes arquivos para entender o estado atual:
- `CLAUDE.md` (convencoes, stack, estrutura)
- `docs/progresso.md` (o que ja foi feito, sub-etapas concluidas)

## Uso de WebSearch (permitido e esperado)

Voce pode e deve usar WebSearch/WebFetch para esclarecer duvidas tecnicas nao
respondidas pelo contexto do projeto. Exemplos de uso legitimo:
- Comportamento de uma versao especifica de biblioteca (ex: "@base-ui/react v1.4.1 SelectValue")
- Documentacao oficial de API externa referenciada no objetivo
- Melhores praticas de schema para um padrao nao coberto pelo CLAUDE.md

Nao use WebSearch para conceitos basicos ja documentados em CLAUDE.md ou ADRs.
Priorize sempre o contexto do projeto sobre fontes externas.

## Passo 1.5 -- Auditar o estado atual do projeto (OBRIGATORIO antes de propor tasks)

Antes de propor qualquer task, executar as seguintes verificacoes:

**1. Bounded contexts existentes:**
Listar diretorios em `src/main/java/` que correspondem a bounded contexts:
usar Glob com pattern `src/main/java/**/*Application.java` para localizar o
pacote base, depois Glob `src/main/java/**/domain/*.java` para listar entidades.

**2. Migrations existentes:**
Listar `src/main/resources/db/migration/V*.sql` para saber o numero mais alto.
O proximo numero disponivel e max(V) + 1.

**2b. Reservar numeros de migration para este plano:**
- Identificar quantas tasks do plano precisarao de migration (tasks que criam
  ou alteram schema de banco).
- Atribuir numeros fixos: task que precisa de migration recebe
  `migracoes_reservadas: ["V{max+1}"]`, a proxima recebe `["V{max+2}"]`, etc.
- O executor deve usar o numero ja atribuido em `migracoes_reservadas`,
  sem recalcular. Nunca deixar o executor descobrir o proximo V dinamicamente.
- Tasks sem alteracao de schema recebem `migracoes_reservadas: []`.

**3. Features ja implementadas:**
Ler `docs/progresso.md` secao "Camada 4" para identificar o que ja foi entregue.

**4. Regras de auditoria:**
- Se o objetivo menciona criar X e X ja existe como bounded context: a task deve
  ser REFACTOR de X, nao CRIACAO. Mencionar explicitamente no titulo da task.
- Se o numero de migration proposto ja existe: usar o proximo disponivel.
- Nunca propor duplicar logica que ja existe (auth, JWT, repositorios base).

**5. Analise de FK para operacoes de exclusao ou deduplicacao de dados:**
Se o objetivo envolve deletar, deduplicar ou remover registros de uma tabela:
- Identificar TODAS as tabelas que tem FK apontando para a tabela-alvo:
  buscar `REFERENCES <tabela>` nas migrations existentes em
  `src/main/resources/db/migration/`.
- Incluir no prompt do executor, ANTES de qualquer DELETE:
  a) UPDATE de cada tabela filha para redirecionar o `*_id` para o registro
     que sera mantido (o "keeper")
  b) Ordem obrigatoria: reatribuir todas as FKs -> depois deletar
- Nunca propor DELETE em tabela com FK filha sem explicitar o tratamento
  das referencias. Violacao de FK em migration e bloqueador de startup.

Registrar o resultado da auditoria (bounded contexts encontrados, ultimo V, features
concluidas relevantes) antes de prosseguir para o Passo 2.

## Passo 1.7 -- Identificar e registrar premissas (OBRIGATORIO)

Antes de decompor em tasks, listar EXPLICITAMENTE as premissas que voce esta
assumindo sobre o objetivo. Premissas sao inferencias que o operador nao
especificou mas que determinam o design:

- Modelagem de dados: "Anotacao pertence a usuario (FK para users.id)"
- Tipos de campo: "Valor monetario e BigDecimal, nao centavos inteiros"
- Enums inferidos: "Prioridade sera BAIXA / MEDIA / ALTA"
- Escopo: "CRUD completo (criar, listar, editar, deletar)"
- Frontend: "Paginas de listagem e formulario de criacao"
- Comportamento: "Categoria e opcional em Anotacao"

Registrar na chave `premissas_globais` do JSON de saida. Cada premissa deve
ser uma frase afirmativa, curta e precisa. O operador vai ler e aprovar ou
rejeitar no Passo 3 da skill.

## Passo 1.8 -- Extrair e inlinar padroes de referencia (OBRIGATORIO para bounded contexts)

Se o objetivo envolve criar um novo bounded context seguindo o padrao existente
do projeto (domain + application + infra + interfaces + frontend):

**1. Classificar o novo bounded context pela matriz de complexidade:**

Ler a descricao do objetivo e identificar quais caracteristicas o novo dominio tem.
Escolher a referencia adequada para cada caracteristica:

| Caracteristica do novo dominio | Referencia obrigatoria |
|-------------------------------|----------------------|
| Campos simples (string, boolean, UUID) | `tag` (sempre -- base minima) |
| Tem enum proprio | `carteira` (padrão @Enumerated + TipoCarteira) |
| Tem campo monetario (Money/@Embedded) | `conta` (padrão @Embedded + @AttributeOverride) |
| Tem filtros paginados ou queries complexas | `transacao` (findComFiltros + Pageable) |
| Cruza dados de outro bounded context | `orcamento` (injeta segundo Repository no use case) |

Exemplos de classificacao:
- "grupo (nome, descricao, ativo)" → so `tag`
- "ativo (nome, tipo enum, valor)" → `tag` + `carteira`
- "lancamento (descricao, valor Money, categoria)" → `tag` + `conta`
- "relatorio-mes (filtros por data/categoria)" → `tag` + `transacao`

**2. Ler os arquivos da referencia selecionada:**

Base minima (sempre):
```
src/main/java/com/laboratorio/financas/tag/domain/Tag.java
src/main/java/com/laboratorio/financas/tag/infrastructure/persistence/TagEntity.java
src/main/java/com/laboratorio/financas/tag/infrastructure/persistence/TagRepositoryImpl.java
src/main/java/com/laboratorio/financas/tag/interfaces/TagController.java
src/main/java/com/laboratorio/financas/shared/infrastructure/web/GlobalExceptionHandler.java
```

Adicionar se o dominio tiver enum:
```
src/main/java/com/laboratorio/financas/carteira/domain/TipoCarteira.java
src/main/java/com/laboratorio/financas/carteira/infrastructure/persistence/CarteiraEntity.java
```

Adicionar se o dominio tiver Money:
```
src/main/java/com/laboratorio/financas/conta/infrastructure/persistence/ContaEntity.java
src/main/java/com/laboratorio/financas/shared/domain/Money.java
```

Adicionar se o dominio tiver filtros paginados:
```
src/main/java/com/laboratorio/financas/transacao/infrastructure/persistence/TransacaoRepositoryImpl.java
src/main/java/com/laboratorio/financas/transacao/domain/FiltrosTransacao.java
```

Adicionar se o dominio cruzar dados de outro bounded context:
```
src/main/java/com/laboratorio/financas/orcamento/application/CalcularProgressoDoOrcamentoUseCase.java
```

**3. Extrair e inlinar no campo `prompt` da task os seguintes trechos:**

**a) Construtor de dominio (do Tag.java):**
Copiar as assinaturas exatas dos construtores e o corpo do construtor
de criacao (id=UUID.randomUUID(), criadoEm=Instant.now(), etc.).
Adaptar os campos para o novo dominio.

**b) Mapeamento JPA (do TagEntity.java):**
Copiar as anotacoes @Entity, @Table, @Id, @Column, incluindo os tipos
exatos (UUID, VARCHAR(N), BOOLEAN, TIMESTAMPTZ). Adaptar colunas.

**c) Extrator de userId do token JWT (do TagController.java):**
Localizar as linhas exatas de extracao do userId via Authentication.
Copiar o trecho verbatim (geralmente 2-3 linhas). O executor deve
copiar exatamente esse trecho, sem reinventar.

**d) Registro no GlobalExceptionHandler:**
Copiar um exemplo de @ExceptionHandler existente (ex: TagNaoEncontradaException)
e instruir o executor a adicionar o mesmo padrao para a nova excecao,
ANTES de qualquer commit -- nao descobrir isso via falha de gate.

**e) Estrutura do RepositoryImpl (do TagRepositoryImpl.java):**
Copiar o esqueleto da classe (imports, @Component, campos @Autowired,
assinaturas dos metodos implementados). O executor adapta os nomes.

**Formato de inlining no prompt do executor:**

```
## Padroes de referencia (nao ler arquivos -- usar estes trechos direto)

### Construtor de dominio (adaptar campos):
```java
// Construtor de criacao
public <Entidade>(UUID userId, String nome) {
    this.id = UUID.randomUUID();
    this.userId = userId;
    this.nome = Objects.requireNonNull(nome, "nome obrigatorio");
    this.ativo = true;
    this.criadoEm = Instant.now();
    this.atualizadoEm = this.criadoEm;
}
```

### Mapeamento JPA (adaptar @Table e colunas):
```java
@Entity
@Table(name = "<tabela>")
class <Entidade>Entity {
    @Id
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @Column(name = "ativo", nullable = false)
    private boolean ativo;

    @Column(name = "criado_em", nullable = false,
            columnDefinition = "TIMESTAMPTZ")
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false,
            columnDefinition = "TIMESTAMPTZ")
    private Instant atualizadoEm;
}
```

### Extrator de userId JWT (copiar verbatim no Controller):
```java
// [copiar as linhas exatas do TagController.java aqui]
```

### GlobalExceptionHandler — adicionar ANTES de implementar o Controller:
```java
// Adicionar em GlobalExceptionHandler.java antes de qualquer commit:
@ExceptionHandler(<Nova>NaoEncontradaException.class)
@ResponseStatus(HttpStatus.NOT_FOUND)
public void handle<Nova>NaoEncontrada() {}
```

### RepositoryImpl — esqueleto (adaptar nomes):
```java
@Component
class <Nova>RepositoryImpl implements <Nova>Repository {
    private final <Nova>JpaRepository jpaRepository;
    private final <Nova>Mapper mapper;
    // [construtor e metodos copiados do TagRepositoryImpl]
}
```
```

**Regra de ouro:** O executor que recebe um prompt com esses trechos inlinados
NAO precisa ler nenhum arquivo de referencia. O custo de leitura fica no
planejador (1 vez), nao no executor (que repetiria N vezes para N tasks).

**Quando NAO inlinar:** tasks de refactor, fix de bug, ou qualquer objetivo
que nao seja criacao de bounded context do zero. Nesses casos, o executor
precisa ler os arquivos que vai modificar -- o que e correto.

## Passo 2 -- Quebrar em tasks (fatia vertical obrigatoria)

Analise o objetivo e decomponha em tasks independentes e paralelizaveis.

### Principio central: fatia vertical

Cada task deve entregar uma feature COMPLETA, do banco ate a tela:
- Migration SQL (se a feature altera o schema)
- Domain: entidade, repositorio, excecoes de dominio
- Application: use cases com testes Mockito
- Infrastructure: repositorio JPA com testes Testcontainers
- Interface: controller REST com testes MockMvc
- **Frontend: pagina(s) Next.js com testes Vitest** (quando a feature tem UI)

**Quando usar `/feature-front` no prompt do executor:**
Para features cujo frontend e CRUD padrao (listagem + formulario de criacao + detalhe),
inclua no prompt do executor o seguinte passo ANTES de descrever os arquivos:

  Execute `/feature-front <dominio>` (leia `.claude/skills/feature-front/SKILL.md` e
  execute manualmente) para gerar o scaffold inicial dos 6 arquivos frontend.
  Depois preencha os `// TODO` com os campos e logica especificos abaixo.

Indicadores de CRUD padrao (use /feature-front):
- Feature tem listagem + botao "Novo" + formulario simples + detalhe
- Exemplo: conta, categoria, transacao, orcamento, meta

Indicadores de frontend customizado (descreva os arquivos explicitamente):
- Formulario com upload de arquivo, filtros complexos, graficos, detalhe expansivel
- Componentes compartilhados (nao em features/<dominio>/)
- Feature sem pagina de criacao padrao (ex: so listagem com acoes inline)
- Exemplo: importacao, relatorios, incidentes, anexo

**Regra absoluta:** se o objetivo menciona "tela", "pagina", "frontend", "dashboard",
"formulario" ou qualquer coisa visivel ao usuario, a task DEVE incluir o frontend.
Nao e permitido criar uma task que so entrega backend quando a feature tem UI.

**Quando nao ha frontend:** features puramente internas (jobs, migrations de dados,
refactors de infra, hooks) podem ser tasks sem frontend. Neste caso, o titulo da
task deve deixar claro que e backend-only ("Refactor interno de X", "Migration de dados Y").

### Criterios de task

Cada task deve:
- Ser executavel de forma isolada num worktree git proprio
- Ter escopo de UMA feature completa (nao metade de uma feature)
- Nao depender de outra task desta lista para compilar e testar
- Ter dependencias de dados claramente identificadas (ex: "depende de task-001
  para FK de usuario -- spawnar apos task-001 mergeada")

### Paralelismo correto

- Tasks SEM dependencia de dados: spawnar todas em paralelo
- Tasks COM dependencia: identificar a dependencia no prompt do executor e
  orientar o executor a verificar se a migration da task-dependente ja foi
  mergeada antes de criar FKs

Para cada task, escreva o prompt completo que o executor vai receber.
O prompt deve conter: contexto, auditoria do que ja existe, padroes de
referencia inlinados (ver Passo 1.8), o que fazer, fluxo de execucao
(incluindo frontend quando aplicavel), estrutura de commits, restricoes.

NAO incluir secao "arquivos a ler" para arquivos de referencia de padrao --
esses padroes ja foram inlinados no Passo 1.8. O executor so deve ler
arquivos que ele proprio vai MODIFICAR (ex: GlobalExceptionHandler.java,
sidebar, screens.registry.ts).

## Passo 3 -- Retornar lista de tasks

Retornar APENAS o JSON abaixo (sem texto antes ou depois):

```json
{
  "premissas_globais": [
    "string -- premissa 1",
    "string -- premissa 2"
  ],
  "tasks": [
    {
      "id": "task-001",
      "titulo": "descricao curta",
      "resumo": "1-3 linhas: o que entrega, arquivos principais, output esperado",
      "complexidade": "S | M | L | XL",
      "risco": "baixo | medio | alto",
      "migracoes_reservadas": ["V22"],
      "prompt": "conteudo completo do prompt do executor"
    }
  ]
}
```

`premissas_globais`: lista de inferencias feitas sobre o objetivo. Minimo 2,
maximo 10. Obrigatorio mesmo quando o objetivo e detalhado.

`complexidade`:
- S: < 4h estimado, scope estreito (ex: correcao de bug, adicao de campo)
- M: 4-8h, feature completa padrao (bounded context + frontend)
- L: 8-16h, multiplos bounded contexts ou frontend complexo
- XL: > 16h, refactor estrutural ou feature com dependencias externas

`risco`:
- baixo: sem FK nova, sem migracao destrutiva, sem mudanca de API publica
- medio: nova FK, migracao aditiva, alteracao de endpoint existente
- alto: migracao destrutiva, mudanca de schema critico, impacto em outros bounded contexts

`migracoes_reservadas`: lista de strings no formato `"V{N}"`. Vazio se a task
nao toca schema de banco.
