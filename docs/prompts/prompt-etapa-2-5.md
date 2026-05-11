# Prompt — Etapa 2.5: Análise estática (Checkstyle + SpotBugs)

## Contexto

A Etapa 2.4 (JaCoCo thresholds) foi concluída e fechada via PR #21. `main` está em `8383658`, working tree limpo.

Esta etapa adiciona dois gates novos ao `mvnw verify`:

- **Checkstyle** — analisa o código-fonte e quebra build em violações de estilo/convenção
- **SpotBugs** — analisa o bytecode e quebra build em padrões suspeitos de bug

A partir deste merge, qualquer PR com violação de estilo (linha muito longa, import não usado, indentação errada) ou padrão suspeito de bug é bloqueado pelo CI antes de chegar à revisão humana.

Objetivo do roadmap: convenções de código aplicadas automaticamente; bugs comuns detectados antes de testes.

Escopo decidido nesta etapa (calibrado com operador antes da redação):

- **Checkstyle: Google Style como base** com override de indentação para **4 espaços** (não 2 do default Google) — alinhado com o que já existe nos arquivos atuais. Linha máxima: 140 chars (mais permissivo que o default 100). Supressões iniciais: regras de Javadoc obrigatório (`JavadocMethod`, `JavadocType`, `JavadocVariable`, `MissingJavadocMethod`, `MissingJavadocType`).
- **Severidade: `error` pra tudo** que não está na lista de supressões. Sem categoria warning silenciosa.
- **SpotBugs: effort=max, threshold=medium** (conforme roadmap). Excludes pra padrões Spring conhecidos (classes `@Configuration`, records de DTO, `FinancasApplication`).
- **Configuração em arquivos externos** (`config/checkstyle/checkstyle.xml`, `config/spotbugs/spotbugs-excludes.xml`) — convenção da indústria, permite IDEs entenderem, `pom.xml` apenas referencia.
- **Phases:** Checkstyle em `validate` (analisa fonte, não bytecode); SpotBugs em `verify` (precisa do `.class`).
- **Ajustes em arquivos `.java` existentes:** **permitidos somente pra atender Checkstyle** (formatação, imports, espaçamento). Proibido alterar nomes de variável, lógica, anotações ou estrutura.
- **Validação destrutiva genuína:** introduzir uma violação Checkstyle proposital + confirmar BUILD FAILURE; reverter; confirmar BUILD SUCCESS. Mesmo procedimento pra SpotBugs (introduzir padrão suspeito proposital). Diferente da 2.4, aqui a validação **vai funcionar** porque os checkers operam sobre código real.

## Estado esperado ao iniciar

- Branch atual: `main`, sincronizada com `origin/main`
- `git log --oneline -1` mostra `8383658 feat: etapa 2.4 — JaCoCo check com thresholds por camada (#21)`
- `docs/prompt-etapa-2-5.md` presente como untracked (este próprio arquivo, anexado pelo operador antes de iniciar). Se não aparecer em `git status`, parar e reportar — significa que o arquivo não foi anexado ou está com nome diferente.
- Working tree sem outras mudanças além do prompt untracked acima
- Docker Desktop rodando (necessário pra Testcontainers no `mvnw verify`)

Validar com `git status` e `git log --oneline -1` antes de começar. Se estado divergir, parar e reportar.

## Pesquisar antes de chutar versões

Antes de qualquer alteração no `pom.xml`, **pesquisar Maven Central** (search.maven.org) ou GitHub releases pra confirmar versão estável atual de:

- `org.apache.maven.plugins:maven-checkstyle-plugin`
- `com.puppycrawl.tools:checkstyle` (a engine, declarada como dependency override do plugin)
- `com.github.spotbugs:spotbugs-maven-plugin`

Versões esperadas (aproximadamente, validar com pesquisa antes de fixar): `maven-checkstyle-plugin 3.x` (BOM Spring Boot pode resolver), `checkstyle 10.x` (ou superior — a versão do plugin é antiga, é importante override), `spotbugs-maven-plugin 4.8.x`. Não confiar em memória — versões mudam.

## Tarefas

### Tarefa 1 — Criar arquivo de configuração Checkstyle

**Caminho:** `config/checkstyle/checkstyle.xml`

Estratégia: começar do `google_checks.xml` distribuído com a engine Checkstyle e fazer overrides locais (não copiar e modificar — referenciar). Mas como nem sempre é prático estender (depende de qual maven-checkstyle-plugin), o caminho mais robusto é declarar inline as regras essenciais.

Conteúdo sugerido (validar contra documentação oficial Checkstyle 10.x antes de finalizar):

```xml
<?xml version="1.0"?>
<!DOCTYPE module PUBLIC
        "-//Checkstyle//DTD Checkstyle Configuration 1.3//EN"
        "https://checkstyle.org/dtds/configuration_1_3.dtd">

<!--
    Configuracao Checkstyle do projeto financas-lab.
    Base: Google Java Style com overrides para 4 espacos de indentacao
    (o default Google e 2 espacos, incompativel com o codigo existente
    do projeto, todo escrito em 4 espacos seguindo Spring Initializr).

    Regras de Javadoc obrigatorio estao desabilitadas. Quando houver
    decisao consciente sobre Javadoc no projeto, esta config volta a
    incluir os checks correspondentes.
-->
<module name="Checker">
    <property name="charset" value="UTF-8"/>
    <property name="severity" value="error"/>
    <property name="fileExtensions" value="java, properties, xml"/>

    <!-- Nao tolera tabs em arquivos Java. -->
    <module name="FileTabCharacter">
        <property name="eachLine" value="true"/>
    </module>

    <!-- Linha em branco no final do arquivo (POSIX). -->
    <module name="NewlineAtEndOfFile">
        <property name="lineSeparator" value="lf"/>
    </module>

    <!-- Linha maxima 140 chars (mais permissivo que default 100 do Google). -->
    <module name="LineLength">
        <property name="max" value="140"/>
        <property name="ignorePattern" value="^package.*|^import.*|a href|href|http://|https://|ftp://"/>
    </module>

    <module name="TreeWalker">
        <!-- Indentacao: 4 espacos (override do Google que usa 2). -->
        <module name="Indentation">
            <property name="basicOffset" value="4"/>
            <property name="braceAdjustment" value="0"/>
            <property name="caseIndent" value="4"/>
            <property name="throwsIndent" value="4"/>
            <property name="lineWrappingIndentation" value="8"/>
            <property name="arrayInitIndent" value="4"/>
        </module>

        <!-- Imports: ordem, sem estrelas, sem nao usados, sem duplicados. -->
        <module name="AvoidStarImport"/>
        <module name="UnusedImports">
            <property name="processJavadoc" value="false"/>
        </module>
        <module name="RedundantImport"/>

        <!-- Nomenclatura. -->
        <module name="TypeName"/>
        <module name="MethodName"/>
        <module name="LocalVariableName"/>
        <module name="ConstantName"/>
        <module name="ParameterName"/>
        <module name="MemberName"/>
        <module name="PackageName"/>

        <!-- Estrutura. -->
        <module name="LeftCurly"/>
        <module name="RightCurly"/>
        <module name="NeedBraces"/>
        <module name="EmptyBlock">
            <property name="option" value="text"/>
        </module>

        <!-- Whitespace. -->
        <module name="WhitespaceAround"/>
        <module name="GenericWhitespace"/>
        <module name="MethodParamPad"/>
        <module name="ParenPad"/>
        <module name="NoWhitespaceBefore"/>
        <module name="NoWhitespaceAfter"/>

        <!-- Bugs comuns capturados estaticamente. -->
        <module name="EqualsHashCode"/>
        <module name="MissingSwitchDefault"/>
        <module name="FallThrough"/>
        <module name="EmptyStatement"/>
        <module name="SimplifyBooleanExpression"/>
        <module name="SimplifyBooleanReturn"/>
        <module name="StringLiteralEquality"/>

        <!-- Modificadores. -->
        <module name="ModifierOrder"/>
        <module name="RedundantModifier"/>
    </module>
</module>
```

**Não incluir:** `JavadocMethod`, `JavadocType`, `JavadocVariable`, `MissingJavadocMethod`, `MissingJavadocType`, `SummaryJavadoc` — ficam fora desta etapa por decisão consciente.

### Tarefa 2 — Criar arquivo de excludes SpotBugs

**Caminho:** `config/spotbugs/spotbugs-excludes.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!--
    Filtros SpotBugs do projeto financas-lab.
    Excludes para padroes conhecidos de falso positivo no contexto Spring/JPA.
-->
<FindBugsFilter>
    <!-- Main do Spring Boot, sem logica testavel. -->
    <Match>
        <Class name="com.laboratorio.financas.FinancasApplication"/>
    </Match>

    <!-- Records sao imutaveis por design; SpotBugs nao precisa analisar mutabilidade. -->
    <Match>
        <Bug pattern="EI_EXPOSE_REP,EI_EXPOSE_REP2"/>
        <Class name="~.*Response"/>
    </Match>
    <Match>
        <Bug pattern="EI_EXPOSE_REP,EI_EXPOSE_REP2"/>
        <Class name="~.*Request"/>
    </Match>

    <!--
        Classes @Configuration: metodos @Bean retornando objeto novo
        sao padrao Spring, nao "returned object never used".
    -->
    <Match>
        <Class name="~.*Config"/>
        <Bug pattern="UPM_UNCALLED_PRIVATE_METHOD"/>
    </Match>
</FindBugsFilter>
```

A lista cresce sob demanda. Se aparecer falso positivo novo, adicionar `<Match>` específico (não filtrar amplo).

### Tarefa 3 — Adicionar plugins no `pom.xml`

Localizar a seção `<plugins>` dentro de `<build>`. Adicionar **dois plugins novos** após o JaCoCo (último plugin atual):

**Maven Checkstyle Plugin:**

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-checkstyle-plugin</artifactId>
    <version>3.6.0</version> <!-- VALIDAR no Maven Central antes de fixar -->
    <dependencies>
        <dependency>
            <groupId>com.puppycrawl.tools</groupId>
            <artifactId>checkstyle</artifactId>
            <version>10.20.1</version> <!-- VALIDAR no Maven Central antes de fixar -->
        </dependency>
    </dependencies>
    <configuration>
        <configLocation>${project.basedir}/config/checkstyle/checkstyle.xml</configLocation>
        <consoleOutput>true</consoleOutput>
        <failsOnError>true</failsOnError>
        <failOnViolation>true</failOnViolation>
        <violationSeverity>error</violationSeverity>
        <includeTestSourceDirectory>true</includeTestSourceDirectory>
    </configuration>
    <executions>
        <execution>
            <id>checkstyle-validate</id>
            <phase>validate</phase>
            <goals>
                <goal>check</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

**SpotBugs Maven Plugin:**

```xml
<plugin>
    <groupId>com.github.spotbugs</groupId>
    <artifactId>spotbugs-maven-plugin</artifactId>
    <version>4.8.6.6</version> <!-- VALIDAR no Maven Central antes de fixar -->
    <configuration>
        <effort>Max</effort>
        <threshold>Medium</threshold>
        <failOnError>true</failOnError>
        <excludeFilterFile>${project.basedir}/config/spotbugs/spotbugs-excludes.xml</excludeFilterFile>
        <plugins>
            <!-- find-sec-bugs intencionalmente nao incluido nesta etapa. -->
        </plugins>
    </configuration>
    <executions>
        <execution>
            <id>spotbugs-verify</id>
            <phase>verify</phase>
            <goals>
                <goal>check</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

**Crítico:** os números de versão acima são placeholders. **Pesquisar Maven Central** antes de fixar.

### Tarefa 4 — Rodar `mvnw clean verify` e tratar violações

```powershell
cd C:\projetos\financas-lab
.\mvnw clean verify
```

**Cenário esperado:** alguma violação Checkstyle vai aparecer, porque o código atual nunca passou por gate de estilo. Possíveis causas:

- Linhas em branco ausentes em fim de arquivo
- Imports não usados
- Espaçamento entre operadores
- Indentação não-uniforme em alguma classe específica

**Como tratar cada caso:**

1. **Violação genuína** (código pode ser corrigido sem mudança de comportamento): editar o arquivo `.java` afetado, **somente formatação**. Permitido nesta etapa.
2. **Violação artificial** (regra do Checkstyle absurda no contexto): parar e reportar ao operador antes de mexer no `checkstyle.xml`. Não relaxar a regra silenciosamente.
3. **SpotBugs falso positivo** que merece supressão: parar e reportar ao operador antes de adicionar exclude amplo. Adicionar exclude estreito (com `<Class>` específico) é aceitável.

**Regra dura:** **alterações em `.java` existentes só pra atender Checkstyle** — formatação, imports, espaçamento. **Proibido**:

- Alterar nomes de variável/método/classe
- Adicionar ou remover lógica
- Alterar anotações Spring/JPA
- Mudar visibilidade (public/private/protected)
- Refatorar estrutura

Se o Checkstyle pedir algo que exija mudança proibida acima, parar e reportar.

Esperado ao final desta tarefa:

- BUILD SUCCESS
- `Tests run: 5` (mesmo número da 2.4)
- Logs do Checkstyle e SpotBugs com 0 violações

### Tarefa 5 — Validação destrutiva (Checkstyle)

Esta validação **não vai pro commit**.

1. Editar **localmente** algum `.java` introduzindo uma violação clara: a maneira mais limpa é adicionar uma linha com 200 caracteres de comentário em algum arquivo. Exemplo, no topo de `HealthcheckResponse.java`, antes do `package`:
   ```java
   // aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
   ```
2. Rodar `.\mvnw clean verify`. Esperado: **BUILD FAILURE** com mensagem do Checkstyle indicando `LineLength` (linha excede 140 chars).
3. Reverter via `git checkout <arquivo afetado>`. Confirmar via `git diff` que voltou ao estado limpo.
4. Rodar `.\mvnw clean verify`. Esperado: **BUILD SUCCESS**.

### Tarefa 6 — Validação destrutiva (SpotBugs)

Esta validação **não vai pro commit**.

1. Editar **localmente** algum método pra introduzir um padrão que SpotBugs detecta, em threshold medium. Exemplo simples: adicionar método em `HealthcheckController.java`:
   ```java
   public String exemploNullDereference() {
       String s = null;
       return s.toLowerCase();
   }
   ```
2. Rodar `.\mvnw clean verify`. Esperado: **BUILD FAILURE** com mensagem do SpotBugs (`NP_ALWAYS_NULL` ou similar).
3. Reverter via `git checkout <arquivo afetado>`. Confirmar via `git diff`.
4. Rodar `.\mvnw clean verify`. Esperado: **BUILD SUCCESS**.

Se SpotBugs **não detectar** o null dereference (passa o build), reportar — pode indicar que o threshold ou effort não está pegando.

### Tarefa 7 — Atualizar `decisoes.md`

**7a.** Na tabela de stack (seção "Backend"), substituir a linha de Análise estática:

De:
```markdown
| Análise estática | SpotBugs + Checkstyle (configuração na Etapa 2.5) | a definir |
```

Para (com versões reais observadas durante a Tarefa 3):
```markdown
| Análise estática (estilo) | maven-checkstyle-plugin + Checkstyle engine | <versões reais observadas> |
| Análise estática (bugs) | spotbugs-maven-plugin | <versão real observada> |
```

**7b.** Em **"Configuração crítica do `pom.xml`"**, adicionar bullet após o JaCoCo:

```markdown
- Checkstyle plugin: phase `validate`, severidade `error`, configuração externa em `config/checkstyle/checkstyle.xml`. Base Google Style com overrides: indentação 4 espaços (default Google é 2), linha 140 chars (default Google é 100), regras de Javadoc obrigatório suprimidas.
- SpotBugs plugin: phase `verify`, effort `max`, threshold `medium`, exclude filter em `config/spotbugs/spotbugs-excludes.xml`. Excludes iniciais: `FinancasApplication`, records de `*Response`/`*Request` (EI_EXPOSE_REP), classes `*Config` (UPM_UNCALLED_PRIVATE_METHOD).
```

**7c.** Adicionar nova subseção curta em **"Convenções de código"**, após "Cobertura mínima por camada (JaCoCo)":

```markdown
### Análise estática

- **Checkstyle severidade `error` para tudo.** Sem categoria warning silenciosa. Cada supressão é decisão consciente registrada em `config/checkstyle/checkstyle.xml`.
- **SpotBugs excludes estreitos.** Filtros sempre com `<Class>` específico ou `<Bug pattern>` específico. Filtros amplos (ex: pacote inteiro) exigem novo ADR.
- **Javadoc não é obrigatório nesta fase.** Regras `JavadocMethod`, `JavadocType`, `JavadocVariable`, `MissingJavadocMethod`, `MissingJavadocType` ficam desabilitadas. Reativação fica como decisão futura.
```

**7d.** Adicionar linha no **"Histórico de mudanças"** no fim do arquivo, no topo da lista:

```markdown
- **2026-05-08** — Etapa 2.5 concluída: Checkstyle e SpotBugs ativados como gates do `mvnw verify`. Configuração externa em `config/`, severidade `error`, validação destrutiva confirmada para ambos.
```

### Tarefa 8 — Atualizar `progresso.md`

**8a.** Atualizar campo "Última atualização" no topo: `2026-05-08 (Etapa 2.5)`.

**8b.** Marcar como `[x]` na seção da Camada 1 o critério `Checkstyle + SpotBugs configurados`.

**8c.** Adicionar nova seção **"Lições da Etapa 2.5"** logo antes de **"Lições da Etapa 2.4"** (mantendo ordem decrescente). Conteúdo: candidatos a hook e lições de ambiente que **realmente forem observados durante a execução**. Padrão pra preencher quando nada digno emergir:

```markdown
## Lições da Etapa 2.5

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum novo nesta etapa.)

### Licoes de ambiente

(Nenhuma nova nesta etapa.)
```

**Regra dura:** só registrar lições **realmente observadas**. Configuração de Checkstyle ou SpotBugs costuma esconder pegadinhas reais — se aparecer uma, registrar honestamente. Ex.: regra que se mostrou impossível de aplicar, exclude que precisou ser mais amplo do que o esperado, comportamento diferente entre Windows e Linux CI, falso positivo SpotBugs em record (records são novidade pro SpotBugs antigo).

**8d.** Adicionar entrada no **"Histórico de mudanças deste documento"** no topo da lista:

```markdown
- **2026-05-08** — Etapa 2.5 concluída: Checkstyle (`validate`) e SpotBugs (`verify`) integrados como gates obrigatórios do `mvnw verify`. Configuração externa, severidade `error`, validação destrutiva confirmada. Mergeado via PR #XX.
```

### Tarefa 9 — Versionar este próprio prompt

Confirmar que `docs/prompt-etapa-2-5.md` está em disco como untracked e incluir no commit de docs.

## Restrições e freios

1. **Não tocar em arquivos fora do escopo.** Arquivos permitidos:
   - `pom.xml`
   - `config/checkstyle/checkstyle.xml` (novo)
   - `config/spotbugs/spotbugs-excludes.xml` (novo)
   - `src/main/java/**/*.java` e `src/test/java/**/*.java` — **somente formatação para atender Checkstyle**, conforme Tarefa 4
   - `docs/decisoes.md`
   - `docs/progresso.md`
   - `docs/prompt-etapa-2-5.md` (este arquivo)

2. **"Necessidade técnica direta" não é exceção válida à Restrição 1.** Lição da 2.2 + 2.4: tentar resolver gap silenciosamente (mexer em `application.yml`, `ci.yml`, `AbstractIntegrationTest`, comandos Unix em ambiente Windows) é o padrão exato que essa restrição existe pra bloquear.

3. **Em arquivos `.java` existentes: SOMENTE formatação.** Permitido: indentação, espaçamento, ordem de imports, remoção de imports não-usados, adição de newline final. **Proibido:** alterar nome de variável, método, classe, anotação, visibilidade, lógica, ou qualquer coisa que mude semântica do código. Se Checkstyle pedir mudança que exija isso, parar e reportar.

4. **Não desabilitar regra Checkstyle silenciosamente.** Se uma regra do `checkstyle.xml` proposto se mostrar problemática, parar e reportar. Não comentar regra. Não baixar severidade. Não adicionar `@SuppressWarnings`. Decisão de relaxar regra é consciente, calibrada com operador, registrada em `decisoes.md`.

5. **Não adicionar plugins SpotBugs adicionais.** `find-sec-bugs`, `fb-contrib` e similares ficam fora desta etapa. Esta etapa é o SpotBugs core, com effort=max e threshold=medium.

6. **Não criar `application-dev.yml`, `application-prod.yml`, `application-ci.yml`.** Fora do escopo.

7. **Não tocar em `ci.yml`.** O CI atual roda `mvnw verify`, então Checkstyle (em `validate`) e SpotBugs (em `verify`) já são executados automaticamente.

8. **Não antecipar Etapa 2.6 (Scripts PowerShell).** Não criar nenhum script `.ps1`. Validações desta etapa são feitas com `mvnw verify` direto.

9. **Comandos PowerShell, não Unix.** `Select-Object -Last N` (não `tail -N`); `Select-Object -First N` (não `head -N`); `Select-String <padrão>` (não `grep`). Lição já registrada em `decisoes.md` desde a 2.1, **repetida na 2.4** — não repetir novamente nesta etapa.

10. **Validar conteúdo bruto antes de commitar.** Para `.xml` (Checkstyle/SpotBugs config): confirmar que está bem-formado e não tem BOM. Para `.java` ajustados: `Get-Content <path> -Encoding UTF8` confirmar que ajuste foi mínimo. Para `pom.xml`: `git diff pom.xml` deve mostrar apenas as adições dos dois plugins.

11. **Pesquisar versões antes de fixar no `pom.xml`.** Maven Central, GitHub releases. Não chutar.

## Estrutura de commits

Branch: `feat/static-analysis`

Commits atômicos, em ordem:

**Commit 1** — `feat: adiciona configuracao Checkstyle (Google Style com overrides)`
- `config/checkstyle/checkstyle.xml`

**Commit 2** — `feat: adiciona configuracao SpotBugs com excludes para Spring/records`
- `config/spotbugs/spotbugs-excludes.xml`

**Commit 3** — `feat: ativa Checkstyle (validate) e SpotBugs (verify) no pom.xml`
- `pom.xml`

**Commit 4** (apenas se houve ajustes em arquivos `.java`) — `style: ajustes de formatacao para atender Checkstyle`
- arquivos `.java` modificados

Se nenhum ajuste em `.java` foi necessário, pular Commit 4.

**Commit 5** — `docs: registra etapa 2.5 (Checkstyle + SpotBugs) em decisoes e progresso`
- `docs/decisoes.md`
- `docs/progresso.md`
- `docs/prompt-etapa-2-5.md`

## Validação antes de abrir PR

```powershell
git status                                                          # working tree limpo
git log --oneline -6                                                # commits novos visiveis
.\mvnw clean verify                                                 # BUILD SUCCESS, Tests run: 5
Get-Content docs/progresso.md -Encoding UTF8 | Select-String "Etapa 2.5"
```

## PR

Título: `feat: etapa 2.5 — Checkstyle e SpotBugs como gates de mvnw verify`

Body sugerido (ajustar com números/observações reais da execução):

```markdown
## Summary

Implementa a Etapa 2.5 do roadmap: análise estática ativada como gate obrigatório do `mvnw verify`. A partir deste merge, qualquer violação de estilo (Checkstyle) ou padrão suspeito de bug (SpotBugs) bloqueia o build.

### Mudanças

- `config/checkstyle/checkstyle.xml`: configuração Google Style com overrides (4 espaços de indentação, 140 chars, sem Javadoc obrigatório). Severidade `error`.
- `config/spotbugs/spotbugs-excludes.xml`: filtros para `FinancasApplication`, records `*Response`/`*Request` (EI_EXPOSE_REP), classes `*Config` (UPM_UNCALLED_PRIVATE_METHOD).
- `pom.xml`: dois plugins novos. Checkstyle em `validate`, SpotBugs em `verify` (effort max, threshold medium).
- `*.java`: <listar arquivos ajustados, se houve, com descrição curta — ex.: "remoção de imports não usados em SecurityConfig.java"; ou: "nenhum ajuste necessário, código já passa pelas regras">.
- `decisoes.md`: registra versões dos plugins, política de severidade error, política de excludes estreitos para SpotBugs.
- `progresso.md`: marca critério Checkstyle + SpotBugs como concluído, registra lições.

### Validação local

- `mvnw clean verify` local: BUILD SUCCESS, Tests run: 5
- Checkstyle: 0 violações
- SpotBugs: 0 padrões suspeitos detectados

### Validação destrutiva

Confirmação de que ambos os gates funcionam — não commitada:

**Checkstyle:**
1. Adicionei linha de comentário com 200 chars em `<arquivo>`
2. `mvnw clean verify` retornou BUILD FAILURE com violação `LineLength`
3. Revertido via `git checkout`; build voltou a SUCCESS

**SpotBugs:**
1. Adicionei método com null dereference em `<arquivo>`
2. `mvnw clean verify` retornou BUILD FAILURE com `NP_ALWAYS_NULL` (ou padrão similar)
3. Revertido via `git checkout`; build voltou a SUCCESS

### Decisões de escopo

- **Severidade `error` pra tudo.** Sem warning silenciosa.
- **Javadoc não é obrigatório nesta fase.** Regras de Javadoc estão desabilitadas; reativação fica como decisão futura.
- **Excludes SpotBugs sempre estreitos.** `<Class>` ou `<Bug pattern>` específico, nunca pacote inteiro.

### Próximo passo

Etapa 2.6 (Scripts PowerShell) — fora do escopo deste PR.
```

## Pós-criação do PR

Antes de mergear, fazer commit adicional **na mesma branch** corrigindo o `#XX` do `progresso.md` pelo número real do PR:

1. Abrir o PR via `gh pr create`.
2. Capturar o número do PR retornado pelo comando.
3. Editar `docs/progresso.md` substituindo `Mergeado via PR #XX` por `Mergeado via PR #<numero-real>`.
4. Commit: `docs: atualiza numero do PR no historico do progresso.md`
5. Push na mesma branch.
6. Esperar CI verde.
7. **Aguardar autorização explícita do operador antes do merge.**

## Estado esperado ao terminar

- `main` local sincronizada com `origin/main`, incluindo squash commit da 2.5
- `git status` limpo
- `mvnw clean verify` passa local com Checkstyle e SpotBugs verdes
- `docs/progresso.md` reflete 2.5 concluída, número real do PR no histórico
- Branch `feat/static-analysis` deletada local e remotamente

Reportar ao operador o estado final com `git log --oneline -3` e `git status`. Parar.

## O que NÃO fazer ao terminar

- Não criar prompt da 2.6
- Não criar scripts PowerShell em `scripts/`
- Não tocar em `pom.xml` pra adicionar mais plugins, ajustar Surefire, mexer em dependência
- Não criar `application-dev.yml`, `application-prod.yml`, ou qualquer outro arquivo
- Não sugerir "próximo passo" espontaneamente. Fim de etapa = parada explícita.
