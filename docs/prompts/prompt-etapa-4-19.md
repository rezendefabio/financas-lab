# Prompt -- Sub-etapa 4.19: Skill `/feature` para criacao de bounded context

## Contexto

Sub-etapa 4.19 da Camada 3. Entrega a skill `/feature <nome>` -- geradora de estrutura
de bounded context. Primeiro caso de "skill sem subagent" no projeto: skill que instrui
o Claude Code principal a agir diretamente, sem fork para subagent dedicado.

Bounded contexts existentes como referencia: `conta/`, `transacao/`, `categoria/`.
Stack: Java 21, Spring Boot 3.x, JPA/Hibernate, MapStruct, Flyway, Maven.

Estado ao iniciar: branch `main` limpa, PR #64 mergeado (smoke 4.18 concluido).

---

## Padroes que estreiam

**Skill sem subagent (primeiro caso).** Ate aqui, todas as 3 skills (`review-pr`,
`review-arch`, `write-test`) usam `context: fork` + `agent:` para delegar a subagent.
A skill `/feature` nao delega -- Claude Code segue as instrucoes do SKILL.md
diretamente, com ferramentas Write + Bash. O frontmatter NAO tem `context: fork` nem
`agent:`. Categoria: "primeira aplicacao de padrao em eixo novo" (mesma da 4.17).

**Geracao de estrutura vs geracao de codigo.** `test-writer` (4.17/4.18) gera codigo
Java compilavel com logica de negocio real. `/feature` gera apenas esqueleto -- stubs
que compilam mas nao implementam logica. Diferente em natureza: infraestrutura para o
desenvolvedor comecar, nao entrega pronta.

---

## Escopo decidido

### Arquivo 1: `.claude/skills/feature/SKILL.md` (NOVO)

Crie o arquivo com o conteudo abaixo. Atencao: cada `NOME` no corpo do SKILL.md e um
placeholder literal que o Claude Code substituira pelo PascalCase do argumento em
tempo de execucao. Cada `ARG` e substituido pelo argumento bruto (snake_case).

```
---
name: feature
description: Cria estrutura de bounded context com esqueleto basico (domain, application, infrastructure/persistence, interfaces/dto) + 11 arquivos Java stub. Recebe nome do contexto em snake_case minusculo como argumento.
disable-model-invocation: true
argument-hint: [nome-do-bounded-context]
---

Voce deve criar o bounded context `$ARGUMENTS` no projeto financas-lab. Execute todos
os passos em ordem. Pare e reporte ao operador se qualquer pre-condicao falhar.

## Definicoes

Defina internamente antes de qualquer acao:

- `ARG` = `$ARGUMENTS` (ex: `cartao`, `meta_financeira`, `orcamento`)
- `NOME` = PascalCase de `ARG`: capitalize a primeira letra de cada segmento separado
  por underscore e concatene. Exemplos: `cartao` -> `Cartao`,
  `meta_financeira` -> `MetaFinanceira`, `orcamento` -> `Orcamento`.
- Pacote base: `com.laboratorio.financas.ARG`
- Diretorio base: `src/main/java/com/laboratorio/financas/ARG/`

## Passo 0 -- Validacoes (ADR-011)

**Validacao 1 -- formato:**
Verifique se ARG casa com `^[a-z][a-z0-9_]*$`. Se nao casar: escreva
"ERRO: argumento invalido -- use apenas letras minusculas, digitos e underscore,
comecando com letra (ex: /feature cartao, /feature meta_financeira)" e termine.

**Validacao 2 -- existencia:**
Use Bash (PowerShell) para verificar:
```powershell
Test-Path "src/main/java/com/laboratorio/financas/ARG/"
```
Se retornar `True`: escreva "ERRO: bounded context 'ARG' ja existe em
src/main/java/com/laboratorio/financas/ARG/ -- skill abortada para nao sobrescrever
trabalho existente" e termine.

## Passo 1 -- Criar diretorios

```powershell
[System.Environment]::CurrentDirectory = (Get-Location).Path
New-Item -ItemType Directory -Force `
  -Path "src/main/java/com/laboratorio/financas/ARG/domain", `
         "src/main/java/com/laboratorio/financas/ARG/application", `
         "src/main/java/com/laboratorio/financas/ARG/infrastructure/persistence", `
         "src/main/java/com/laboratorio/financas/ARG/interfaces/dto"
```

Verifique com `Test-Path` que os 4 diretorios foram criados. Se algum ausente:
reporte qual falhou e termine.

## Passo 2 -- Criar os 11 arquivos

Use a ferramenta Write para cada arquivo. Substitua `NOME` e `ARG` pelos valores
definidos no inicio. Codificacao: UTF-8 sem BOM.

### Arquivo 1: src/main/java/com/laboratorio/financas/ARG/domain/NOME.java

```java
package com.laboratorio.financas.ARG.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class NOME {

    private final UUID id;
    private final String nome;
    private final Instant criadoEm;

    public NOME(String nome) {
        this(UUID.randomUUID(), nome, Instant.now());
    }

    public NOME(UUID id, String nome, Instant criadoEm) {
        Objects.requireNonNull(id, "id nao pode ser nulo");
        Objects.requireNonNull(nome, "nome nao pode ser nulo");
        Objects.requireNonNull(criadoEm, "criadoEm nao pode ser nulo");
        if (nome.isBlank()) {
            throw new IllegalArgumentException("nome nao pode ser vazio");
        }
        this.id = id;
        this.nome = nome;
        this.criadoEm = criadoEm;
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NOME other)) {
            return false;
        }
        return this.id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "NOME{id=" + id + ", nome='" + nome + "'}";
    }
}
```

### Arquivo 2: src/main/java/com/laboratorio/financas/ARG/domain/NOMERepository.java

```java
package com.laboratorio.financas.ARG.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NOMERepository {

    NOME salvar(NOME domain);

    Optional<NOME> buscarPorId(UUID id);

    List<NOME> listarTodos();
}
```

### Arquivo 3: src/main/java/com/laboratorio/financas/ARG/domain/NOMENaoEncontradaException.java

```java
package com.laboratorio.financas.ARG.domain;

import java.util.UUID;

public class NOMENaoEncontradaException extends RuntimeException {

    private final UUID id;

    public NOMENaoEncontradaException(UUID id) {
        super("ARG nao encontrado: " + id);
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}
```

### Arquivo 4: src/main/java/com/laboratorio/financas/ARG/application/CriarNOMEUseCase.java

```java
package com.laboratorio.financas.ARG.application;

import com.laboratorio.financas.ARG.domain.NOME;
import com.laboratorio.financas.ARG.domain.NOMERepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CriarNOMEUseCase {

    private final NOMERepository repository;

    public CriarNOMEUseCase(NOMERepository repository) {
        this.repository = repository;
    }

    public record Comando(String nome) { }

    @Transactional
    public NOME executar(Comando comando) {
        NOME novo = new NOME(comando.nome());
        return repository.salvar(novo);
    }
}
```

### Arquivo 5: src/main/java/com/laboratorio/financas/ARG/infrastructure/persistence/NOMEEntity.java

```java
package com.laboratorio.financas.ARG.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ARG")
public class NOMEEntity {

    @Id
    @Column(name = "id", columnDefinition = "uuid", nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @NotNull
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected NOMEEntity() {
        // Construtor protected exigido pelo JPA.
    }

    public NOMEEntity(UUID id, String nome, Instant criadoEm) {
        this.id = id;
        this.nome = nome;
        this.criadoEm = criadoEm;
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
```

### Arquivo 6: src/main/java/com/laboratorio/financas/ARG/infrastructure/persistence/NOMEJpaRepository.java

```java
package com.laboratorio.financas.ARG.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NOMEJpaRepository extends JpaRepository<NOMEEntity, UUID> {
}
```

### Arquivo 7: src/main/java/com/laboratorio/financas/ARG/infrastructure/persistence/NOMERepositoryImpl.java

```java
package com.laboratorio.financas.ARG.infrastructure.persistence;

import com.laboratorio.financas.ARG.domain.NOME;
import com.laboratorio.financas.ARG.domain.NOMERepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class NOMERepositoryImpl implements NOMERepository {

    private final NOMEJpaRepository jpaRepository;
    private final NOMEMapper mapper;

    public NOMERepositoryImpl(NOMEJpaRepository jpaRepository, NOMEMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public NOME salvar(NOME domain) {
        NOMEEntity entity = mapper.toEntity(domain);
        NOMEEntity salva = jpaRepository.save(entity);
        return mapper.toDomain(salva);
    }

    @Override
    public Optional<NOME> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<NOME> listarTodos() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }
}
```

### Arquivo 8: src/main/java/com/laboratorio/financas/ARG/infrastructure/persistence/NOMEMapper.java

```java
package com.laboratorio.financas.ARG.infrastructure.persistence;

import com.laboratorio.financas.ARG.domain.NOME;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NOMEMapper {

    default NOMEEntity toEntity(NOME domain) {
        if (domain == null) {
            return null;
        }
        return new NOMEEntity(domain.getId(), domain.getNome(), domain.getCriadoEm());
    }

    default NOME toDomain(NOMEEntity entity) {
        if (entity == null) {
            return null;
        }
        return new NOME(entity.getId(), entity.getNome(), entity.getCriadoEm());
    }
}
```

### Arquivo 9: src/main/java/com/laboratorio/financas/ARG/interfaces/NOMEController.java

```java
package com.laboratorio.financas.ARG.interfaces;

import com.laboratorio.financas.ARG.application.CriarNOMEUseCase;
import com.laboratorio.financas.ARG.interfaces.dto.CriarNOMERequest;
import com.laboratorio.financas.ARG.interfaces.dto.NOMEResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// TODO: ajustar /api/ARGs para plural correto em pt-BR (ex: /api/cartoes)
@RestController
@RequestMapping("/api/ARGs")
public class NOMEController {

    private final CriarNOMEUseCase criarNOMEUseCase;

    public NOMEController(CriarNOMEUseCase criarNOMEUseCase) {
        this.criarNOMEUseCase = criarNOMEUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NOMEResponse criar(@RequestBody @Valid CriarNOMERequest request) {
        // TODO: implementar
        throw new UnsupportedOperationException("Nao implementado");
    }
}
```

### Arquivo 10: src/main/java/com/laboratorio/financas/ARG/interfaces/dto/CriarNOMERequest.java

```java
package com.laboratorio.financas.ARG.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CriarNOMERequest(
        @NotBlank
        @Size(max = 100)
        String nome
) { }
```

### Arquivo 11: src/main/java/com/laboratorio/financas/ARG/interfaces/dto/NOMEResponse.java

```java
package com.laboratorio.financas.ARG.interfaces.dto;

import java.time.Instant;
import java.util.UUID;

public record NOMEResponse(
        UUID id,
        String nome,
        Instant criadoEm
) { }
```

## Passo 3 -- Verificar criacao

Execute:
```powershell
Get-ChildItem -Recurse "src/main/java/com/laboratorio/financas/ARG/" | Select-Object FullName
```

Confirme que os 11 arquivos existem. Se algum ausente: reporte qual falta e nao emita
o relatorio de sucesso.

## Passo 4 -- Relatorio final

Produza o seguinte relatorio (substituindo ARG e NOME pelos valores reais):

```
Bounded context 'ARG' criado.

Estrutura (11 arquivos):
  domain/NOME.java
  domain/NOMERepository.java
  domain/NOMENaoEncontradaException.java
  application/CriarNOMEUseCase.java
  infrastructure/persistence/NOMEEntity.java
  infrastructure/persistence/NOMEJpaRepository.java
  infrastructure/persistence/NOMERepositoryImpl.java
  infrastructure/persistence/NOMEMapper.java
  interfaces/NOMEController.java
  interfaces/dto/CriarNOMERequest.java
  interfaces/dto/NOMEResponse.java

ATENCAO -- antes do primeiro commit:
  1. Crie a migration Flyway V<n>__create_ARG_table.sql
     (hook 4.7 bloqueia commit com @Entity novo sem migration)
  2. Ajuste /api/ARGs para o plural correto no NOMEController.java
  3. Preencha os TODO com logica de negocio especifica do dominio
```
```

---

### Arquivo 2: `docs/progresso.md` (EDITAR)

**Mudanca 1 -- linha "Ultima atualizacao"** (proximo ao topo do arquivo):

Substitua:
```
**Última atualização:** 2026-05-11 (Sub-etapa 4.13 ...
```
Por:
```
**Última atualização:** 2026-05-12 (Sub-etapa 4.19 -- Skill /feature para criacao de bounded context)
```

**Mudanca 2 -- marcar smoke 4.18 como concluido** (secao "Criterios de pronto"):

Substitua:
```
- [ ] Smoke pos-merge da 4.18 validando integration tests com cobaia real (`calcularTotaisPorConta`).
```
Por:
```
- [x] Smoke pos-merge da 4.18 validando integration tests com cobaia real (`calcularTotaisPorConta`) -- concluido 2026-05-12 via PR #64. 11 novos @Test acrescentados (4 para calcularTotaisPorConta + 7 para listarComFiltros), 21/21 passando. Uma assertion corrigida (COALESCE retorna zero, nao null). Fix entregue em PR separado (fix cirurgico de smoke).
```

**Mudanca 3 -- marcar `/feature` como concluido** (secao "Criterios de pronto"):

Substitua:
```
- [ ] Skill `/feature <nome>` (cria estrutura de bounded context)
```
Por:
```
- [x] Skill `/feature <nome>` (cria estrutura de bounded context) -- concluido 4.19
```

**Mudanca 4 -- adicionar 4.19 em "Sub-etapas concluidas"** (logo antes da sub-etapa 4.18):

Adicione o seguinte bloco ANTES da entrada da 4.18 (mantenha ordem decrescente):

```
- **4.19 -- Skill `/feature <nome>` (skill geradora direta sem subagent)** (2026-05-12):
  primeira skill sem `context: fork` e sem `agent:` do projeto. Claude Code principal
  segue as instrucoes do `.claude/skills/feature/SKILL.md` diretamente, usando Write +
  Bash. Recebe nome do bounded context em snake_case minusculo; valida formato
  (`^[a-z][a-z0-9_]*$`) e existencia previa; computa PascalCase; cria 4 diretorios +
  11 arquivos Java stub (domain: POJO + Repository + Exception; application: UseCase;
  infrastructure/persistence: Entity + JpaRepository + RepositoryImpl + Mapper;
  interfaces/dto: Controller + 2 DTOs). Esqueleto compila, mas exige migration Flyway
  antes do primeiro commit (hook 4.7). Smoke parcial honesto: verificacao de criacao dos
  11 arquivos + compilacao (`mvn compile`); `mvn verify` fica como responsabilidade do
  desenvolvedor apos criar migration. Categoria: "primeira aplicacao de padrao em eixo
  novo" (mesma da 4.17 -- skill sem subagent vs skill-com-fork). PR #65.
```

**Mudanca 5 -- adicionar em "Historico de mudancas"** (no final do arquivo):

Adicione o seguinte antes do ultimo item (mantenha ordem decrescente):

```
- **2026-05-12** -- Sub-etapa 4.19 concluida: skill `/feature <nome>` em
  `.claude/skills/feature/SKILL.md`. Primeira skill sem subagent do projeto. Gera
  estrutura de bounded context (4 dirs + 11 arquivos Java stub). Validacao de
  argumento + existencia previa. Esqueleto compilavel, developer adiciona migration +
  logica. Smoke parcial honesto. Categoria "primeira aplicacao de padrao em eixo novo".
  Smoke 4.18 marcado como concluido (PR #64). CLAUDE.md NAO atualizado (convencao de
  skills ja registrada na 4.11). PR #65.
```

---

### Arquivo 3: `docs/decisoes-claude-code.md` (EDITAR)

Leia o arquivo antes de editar. Localize a secao de sub-etapas mais recente (4.18) e
adicione um novo bloco logo ANTES do "Historico de mudancas" (final do arquivo).

Adicione a secao abaixo. Garanta linha em branco antes do `##` e depois do `##`
(hook 4.3):

```
## Sub-etapa 4.19 -- Skill `/feature` sem subagent

### Decisao: skill direta vs skill-com-fork

ADR-012 (4.10/4.11) prescreveu o padrao `skill orquestradora -> subagent dedicado`
para tarefas que exigem fork de contexto. A 4.19 introduz variante nova: **skill
direta sem subagent**. Claude Code principal le o `SKILL.md` e executa as instrucoes
com seus proprios tools (Write + Bash), sem fork.

Criterio de escolha entre os dois padroes:

| Padrao | Quando usar |
|---|---|
| Skill com fork (ADR-012) | Tarefa exige isolamento de contexto, modelo diferente do principal, ou tools mais restritas |
| Skill direta (4.19) | Tarefa e procedural (sequencia de Write + Bash), sem necessidade de isolamento, sem logica de negocio emergente |

`/feature` e claramente procedural: valida argumento, cria diretorios, escreve 11
arquivos de template fixo, reporta resultado. Nao ha raciocinio de dominio que
justifique isolamento em subagent.

### Frontmatter da skill direta

```yaml
name: feature
disable-model-invocation: true
argument-hint: [nome-do-bounded-context]
```

Ausencia de `context: fork` e `agent:` e intencional e distingue a skill direta das
skills-com-fork existentes. Sem `allowed-tools` restrito -- Claude Code usa seus tools
normais (Write, Bash) conforme prescrito pelo corpo do SKILL.md.

### Categoria operacional

"Primeira aplicacao de padrao em eixo novo" -- mesma da 4.17 (primeiro subagent
gerador). A 4.19 estreia o eixo "skill sem subagent", distinto de:
- skills-com-fork (review-pr 4.11, review-arch 4.12, write-test 4.17)
- subagents revisores (pr-reviewer 4.9, architect-reviewer 4.12)
- subagents geradores (test-writer 4.17)

### Smoke parcial honesto

Cobaia natural nao disponivel (operador nao tem bounded context novo para produzir
agora). Smoke verifica: (1) skill executa sem erro; (2) 11 arquivos criados com
conteudo correto; (3) `mvn compile` passa. `mvn verify` e responsabilidade do
desenvolvedor apos criar migration Flyway para a Entity gerada (hook 4.7 bloqueia
commit sem migration). Padrao registrado pela segunda vez (primeira: 4.17).

### Aviso para uso real

O skeleton gerado inclui `@Entity` em `NOMEEntity.java`. Hook 4.7 bloqueia commit de
arquivo Java novo com `@Entity` sem migration Flyway correspondente. Desenvolvedor
deve criar `V<n>__create_ARG_table.sql` antes de commitar o bounded context.
```

Adicione ao "Historico de mudancas" do decisoes-claude-code.md:

```
- **2026-05-12** -- Sub-etapa 4.19 concluida: skill `/feature <nome>` direta (sem
  subagent). Padrao novo: skill procedural usa Write + Bash sem fork. Criterio de
  escolha entre skill-com-fork e skill-direta registrado. Categoria "primeira aplicacao
  de padrao em eixo novo". Smoke parcial honesto (segunda aplicacao do padrao). PR #65.
```

---

## Estado esperado ao iniciar

```powershell
git branch --show-current   # deve retornar: main
git status                  # deve retornar: nothing to commit, working tree clean
git log --oneline -3        # deve mostrar PR #64 no topo
```

Se qualquer condicao falhar: pare e reporte antes de continuar.

---

## Tarefas

### Tarefa 1 -- Verificar estado inicial (ADR-011)

```powershell
git branch --show-current
git status
git log --oneline -3
```

Confirme branch `main`, working tree limpa, ultimo commit = PR #64. Se divergir: pare
e reporte.

### Tarefa 2 -- Criar branch

```powershell
git checkout -b feat/etapa-4-19-feature-skill
git branch --show-current   # deve retornar: feat/etapa-4-19-feature-skill
```

### Tarefa 3 -- Criar `.claude/skills/feature/SKILL.md`

Pre-condicao:
```powershell
Test-Path ".claude/skills/feature/"  # deve retornar: False
```

Crie o diretorio e o arquivo:
```powershell
New-Item -ItemType Directory -Path ".claude/skills/feature/"
```

Use Write para criar `.claude/skills/feature/SKILL.md` com o conteudo prescrito na
secao "Arquivo 1" acima. Codificacao UTF-8 sem BOM.

Pos-condicao:
```powershell
Test-Path ".claude/skills/feature/SKILL.md"  # deve retornar: True
(Get-Content ".claude/skills/feature/SKILL.md" -TotalCount 3) -join "`n"
# deve mostrar as 3 primeiras linhas do frontmatter (---, name: feature, description:...)
```

### Tarefa 4 -- Smoke do `/feature`

Execute a skill simulando a invocacao com argumento `teste419`:

**4a. Validacao de argumento invalido (cenario destrutivo 1):**
Tente criar com argumento invalido (maiuscula): se a skill incluiu corretamente a
validacao, o argumento `TesteInvalido` deve ser rejeitado. Valide manualmente que
a logica de validacao de formato esta no SKILL.md (leia o arquivo e confirme que
`^[a-z][a-z0-9_]*$` aparece no Passo 0).

**4b. Geracao real com `teste419`:**
Siga o SKILL.md manualmente para ARG=`teste419`, NOME=`Teste419`:

- Verifique que `src/main/java/com/laboratorio/financas/teste419/` NAO existe.
- Crie os 4 diretorios:
  ```powershell
  [System.Environment]::CurrentDirectory = (Get-Location).Path
  New-Item -ItemType Directory -Force `
    -Path "src/main/java/com/laboratorio/financas/teste419/domain", `
           "src/main/java/com/laboratorio/financas/teste419/application", `
           "src/main/java/com/laboratorio/financas/teste419/infrastructure/persistence", `
           "src/main/java/com/laboratorio/financas/teste419/interfaces/dto"
  ```
- Crie os 11 arquivos Java com conteudo correto (NOME=`Teste419`, ARG=`teste419`,
  pacote=`com.laboratorio.financas.teste419`).

**4c. Verificar criacao:**
```powershell
Get-ChildItem -Recurse "src/main/java/com/laboratorio/financas/teste419/" | Select-Object Name
```
Deve listar os 11 arquivos.

**4d. Verificar compilacao (smoke de compilacao, nao build completo):**
```powershell
.\mvnw compile -q
```
Deve retornar exit code 0. Se falhar: reporte o erro literal e pare (nao tente
auto-corrigir).

NOTA: `mvn verify` (testes completos) vai falhar porque `@Entity` existe sem migration
Flyway. Isso e esperado e nao e criterio de smoke. O smoke valida apenas compilacao.

**4e. Cleanup do teste:**
```powershell
Remove-Item -Recurse -Force "src/main/java/com/laboratorio/financas/teste419/"
```
Confirme remocao:
```powershell
Test-Path "src/main/java/com/laboratorio/financas/teste419/"  # deve retornar: False
```

**4f. Criterios de sucesso do smoke:**

- [ ] Validacao de formato documentada no SKILL.md (verificacao visual)
- [ ] 11 arquivos criados corretamente com ARG=`teste419`
- [ ] `mvn compile` retorna exit 0
- [ ] Cleanup removeu tudo

Se qualquer criterio falhar: reporte o que falhou com o erro literal. Nao tente
auto-corrigir. Abrir 4.19.1 se necessario.

### Tarefa 5 -- Primeiro commit

```powershell
git add ".claude/skills/feature/SKILL.md"
git status  # confirme que so o SKILL.md esta staged
```

Commit:
```
feat(.claude): adiciona skill /feature para criacao de bounded context

Primeira skill sem subagent do projeto. Claude Code executa diretamente
(Write + Bash) sem fork para subagent dedicado. Recebe nome do bounded
context em snake_case minusculo, valida formato e existencia previa,
gera 4 diretorios + 11 arquivos Java stub compilaveis. Requer migration
Flyway antes do primeiro commit (hook 4.7).
```

Verifique exit code. Se hook bloquear: leia a mensagem de erro e corrija.

### Tarefa 6 -- Atualizar `docs/progresso.md`

Leia o arquivo antes de editar:
```powershell
Get-Content "docs/progresso.md" -Encoding UTF8 | Select-Object -First 10
```

Aplique as 5 mudancas prescritas na secao "Arquivo 2" acima, em ordem. Use Edit
para cada mudanca. Nao altere nenhum trecho alem dos prescritos.

Pos-condicao: confirme que "4.19" aparece no arquivo:
```powershell
Select-String "4.19" "docs/progresso.md" | Select-Object -First 3
```

### Tarefa 7 -- Segundo commit

```powershell
git add "docs/progresso.md"
git status
```

Commit:
```
docs(progresso): registra sub-etapa 4.19 e marca smoke 4.18 como concluido
```

### Tarefa 8 -- Atualizar `docs/decisoes-claude-code.md`

Leia o final do arquivo:
```powershell
Get-Content "docs/decisoes-claude-code.md" -Encoding UTF8 | Select-Object -Last 20
```

Adicione a secao "Sub-etapa 4.19" prescrita na secao "Arquivo 3" acima, antes do
"Historico de mudancas". Use Edit. Garanta linha em branco antes e depois de cada
header `##` (hook 4.3).

Pos-condicao:
```powershell
Select-String "4.19" "docs/decisoes-claude-code.md" | Select-Object -First 3
```

### Tarefa 9 -- Terceiro commit

```powershell
git add "docs/decisoes-claude-code.md"
git status
```

Commit:
```
docs(decisoes): registra padrao skill sem subagent inaugurado na 4.19
```

### Tarefa 10 -- Validacao pre-PR

```powershell
git log --oneline -3
# deve mostrar os 3 commits da 4.19

git diff main --name-only
# deve mostrar exatamente:
#   .claude/skills/feature/SKILL.md
#   docs/decisoes-claude-code.md
#   docs/progresso.md
```

Verifique encoding dos arquivos editados:
```powershell
$bytes = [System.IO.File]::ReadAllBytes("docs/progresso.md")
$bytes[0..2] -join "," # nao deve ser "239,187,191" (BOM)
```

Verifique que "4.19" aparece em ambos os docs:
```powershell
Select-String "4.19" "docs/progresso.md" | Measure-Object | Select-Object -Expand Count
Select-String "4.19" "docs/decisoes-claude-code.md" | Measure-Object | Select-Object -Expand Count
```
Ambos devem retornar > 0.

---

## Restricoes e freios

- NAO tente compilar ou testar os arquivos Java gerados alem de `mvn compile`.
  `mvn verify` vai falhar (Entity sem migration) -- isso e esperado para smoke parcial.
- NAO remova os arquivos do contexto `teste419` antes de validar todos os criterios
  do Passo 4f.
- NAO adicione arquivos de migration Flyway no escopo desta sub-etapa. A migration e
  responsabilidade do desenvolvedor que invocar `/feature` em uso real.
- NAO modifique o CLAUDE.md do projeto. Convencao de skills ja registrada na 4.11.
- NAO edite os bounded contexts existentes (conta, transacao, categoria, shared).
- Se qualquer hook bloquear o commit: leia a mensagem de erro, corrija sem usar
  `--no-verify`, reporte o que foi corrigido.
- Smoke falhou? Reporte exatamente o que falhou. Nao tente auto-corrigir em loop.
  Abrir 4.19.1 e decisao do operador.

---

## Estrutura de commits

3 commits, nesta ordem:

```
feat(.claude): adiciona skill /feature para criacao de bounded context
docs(progresso): registra sub-etapa 4.19 e marca smoke 4.18 como concluido
docs(decisoes): registra padrao skill sem subagent inaugurado na 4.19
```

---

## Validacao antes do PR

Execute em sequencia:

```powershell
git log --oneline feat/etapa-4-19-feature-skill ^main
# deve mostrar exatamente 3 commits

git diff main --name-only
# deve mostrar exatamente 3 arquivos

git diff main -- .claude/skills/feature/SKILL.md | Select-String "^+" | Measure-Object | Select-Object -Expand Count
# deve retornar > 100 (SKILL.md tem conteudo substancial)

Select-String "disable-model-invocation: true" ".claude/skills/feature/SKILL.md"
# deve retornar match

Select-String "context: fork" ".claude/skills/feature/SKILL.md"
# NAO deve retornar match (skill direta, sem fork)

Select-String "smoke 4.18" "docs/progresso.md"
# deve retornar match (marcado como concluido)

Select-String "\- \[x\] Skill.*feature" "docs/progresso.md"
# deve retornar match

Select-String "skill.*sem.*subagent" "docs/decisoes-claude-code.md" -CaseSensitive:$false
# deve retornar match
```

---

## PR body

```
## Sub-etapa 4.19 -- Skill `/feature` para criacao de bounded context

### O que muda

- `.claude/skills/feature/SKILL.md` (novo): primeira skill sem subagent do projeto.
  Instrui Claude Code a criar bounded context com 4 diretorios + 11 arquivos Java stub.
- `docs/progresso.md`: smoke 4.18 marcado como concluido, 4.19 registrada.
- `docs/decisoes-claude-code.md`: padrao skill-direta vs skill-com-fork documentado.

### Categoria operacional

"Primeira aplicacao de padrao em eixo novo" -- mesmo da sub-etapa 4.17 (primeiro
subagent gerador). A 4.19 estreia o eixo "skill sem subagent" (skill procedural
direta), distinto de todos os pares skill+subagent anteriores.

### Smoke

Parcial honesto. Validado: formato do argumento, criacao dos 11 arquivos com
NOME=`Teste419`, compilacao (`mvn compile` exit 0), cleanup. NAO validado: `mvn
verify` (esperado falhar sem migration Flyway -- responsabilidade do desenvolvedor
em uso real). Padrao registrado pela segunda vez (primeira: 4.17).

### Aviso para uso real

Arquivos gerados incluem `@Entity`. Hook 4.7 bloqueia commit sem migration
Flyway correspondente. Criar `V<n>__create_<nome>_table.sql` antes de commitar.
```

---

## Pos-criacao do PR

Reporte ao operador:

1. URL do PR aberto.
2. Output completo dos criterios de validacao pre-PR (cada Select-String).
3. Resultado do smoke (criterios 4f satisfeitos ou nao, com evidencia).
4. Se algum criterio falhou: descreva o que falhou com o erro literal.

---

## Estado esperado ao terminar

- Branch `feat/etapa-4-19-feature-skill` com 3 commits acima de `main`.
- Working tree limpa (contexto `teste419` removido no smoke).
- PR #65 aberto, CI rodando.
- `.claude/skills/feature/SKILL.md` existente com conteudo correto.
- `progresso.md` com smoke 4.18 marcado, 4.19 registrada, criterio `/feature` marcado.
- `decisoes-claude-code.md` com secao 4.19 adicionada.

---

## O que NAO fazer ao terminar

- NAO fazer merge sem instrucao explicita do operador.
- NAO modificar nenhum bounded context existente.
- NAO criar arquivos de migration ou testes para o bounded context gerado.
- NAO tentar rodar `mvn verify` (vai falhar sem migration -- esperado).
- NAO abrir 4.19.1 sem relatar primeiro ao operador o que falhou.
