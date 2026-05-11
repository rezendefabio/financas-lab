# Prompt — Etapa 2.4: JaCoCo com thresholds por camada

## Contexto

A Etapa 2.3 (healthcheck endpoint + SecurityConfig) foi concluída e fechada via PR #20. `main` está em `682f83a`, working tree limpo.

Esta etapa adiciona ao JaCoCo a execução `check` com regras de cobertura — fazendo com que `mvnw verify` (e o CI, por consequência) **falhe** se cobertura cair abaixo dos thresholds definidos em `decisoes.md`. Essa é a peça que transforma JaCoCo de "relatório bonito" em "gate real de qualidade".

Objetivo do roadmap: cobertura medida e CI falha se cobertura cair abaixo dos thresholds.

Escopo decidido nesta etapa (calibrado com operador antes da redação):

- **Estratégia de regras vazias (caminho B):** aplicar **agora** apenas o threshold global agregado (75%) e o threshold de `infrastructure` (60%) — esses são os pacotes que têm classes hoje. Os 3 outros (`domain` 90%, `application` 80%, `interfaces` 70%) ficam **comentados no `pom.xml`** com TODO referenciando "ativar quando primeira classe deste pacote for adicionada na Camada 2". Mais honesto que simular regra sem aplicação.
- **`SecurityConfig` incluído nos thresholds** (não excluído). A pegadinha do filtro de Security é exatamente onde mais bug aparece — excluir tira o incentivo de testar. O `HealthcheckControllerTest.deveBloquearEndpointNaoMapeadoComUnauthorized()` já cobre o caminho principal.
- **Exclusões enxutas:** apenas `FinancasApplication.class` (main do Spring Boot, sem lógica testável). Nada mais.
- **Validação destrutiva local (caminho B):** rodar `mvnw verify` localmente forçando threshold absurdo (ex: 99%) e confirmar BUILD FAILURE; reverter para o valor correto e confirmar BUILD SUCCESS. Documentar no PR body. Sem PR descartável.

## Estado esperado ao iniciar

- Branch atual: `main`, sincronizada com `origin/main`
- `git log --oneline -1` mostra `682f83a feat: etapa 2.3 — endpoint healthcheck com SecurityConfig minimo (#20)`
- `docs/prompt-etapa-2-4.md` presente como untracked (este próprio arquivo, anexado pelo operador antes de iniciar). Se não aparecer em `git status`, parar e reportar — significa que o arquivo não foi anexado ou está com nome diferente.
- Working tree sem outras mudanças além do prompt untracked acima
- Docker Desktop rodando (necessário pra Testcontainers no `mvnw verify`)

Validar com `git status` e `git log --oneline -1` antes de começar. Se estado divergir, parar e reportar.

## Tarefas

### Tarefa 1 — Adicionar execução `check` ao JaCoCo no `pom.xml`

**Caminho:** `pom.xml` (já tem o plugin JaCoCo desde a 1.4 com `prepare-agent` + `report`).

Localizar o bloco `<plugin>` do JaCoCo (artifactId `jacoco-maven-plugin`) e **adicionar** uma terceira `<execution>` com id `check`, após a execução `report` existente. Não modificar `prepare-agent` nem `report`.

Estrutura sugerida da nova execução:

```xml
<execution>
    <id>check</id>
    <phase>verify</phase>
    <goals>
        <goal>check</goal>
    </goals>
    <configuration>
        <excludes>
            <!-- Main do Spring Boot, sem logica testavel. -->
            <exclude>**/FinancasApplication.class</exclude>
        </excludes>
        <rules>
            <!-- Threshold global: 75% de cobertura agregada. -->
            <rule>
                <element>BUNDLE</element>
                <limits>
                    <limit>
                        <counter>INSTRUCTION</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.75</minimum>
                    </limit>
                </limits>
            </rule>

            <!-- Pacote infrastructure: 60% (existem classes hoje). -->
            <rule>
                <element>PACKAGE</element>
                <includes>
                    <include>**.infrastructure.*</include>
                </includes>
                <limits>
                    <limit>
                        <counter>INSTRUCTION</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.60</minimum>
                    </limit>
                </limits>
            </rule>

            <!--
                Thresholds adiados (pacotes vazios hoje):
                - domain        — 90% — ativar quando primeira classe de dominio entrar (Camada 2)
                - application   — 80% — ativar quando primeiro use case entrar (Camada 2)
                - interfaces    — 70% — ativar quando primeiro Controller de bounded context entrar (Camada 2)

                Manter as regras abaixo COMENTADAS ate ter classes nesses pacotes.
                JaCoCo `check` em pacote sem classes tem comportamento inconsistente
                entre versoes — incluir agora geraria falso positivo ou falso negativo.

                <rule>
                    <element>PACKAGE</element>
                    <includes><include>**.domain.*</include></includes>
                    <limits><limit><counter>INSTRUCTION</counter><value>COVEREDRATIO</value><minimum>0.90</minimum></limit></limits>
                </rule>
                <rule>
                    <element>PACKAGE</element>
                    <includes><include>**.application.*</include></includes>
                    <limits><limit><counter>INSTRUCTION</counter><value>COVEREDRATIO</value><minimum>0.80</minimum></limit></limits>
                </rule>
                <rule>
                    <element>PACKAGE</element>
                    <includes><include>**.interfaces.*</include></includes>
                    <limits><limit><counter>INSTRUCTION</counter><value>COVEREDRATIO</value><minimum>0.70</minimum></limit></limits>
                </rule>
            -->
        </rules>
    </configuration>
</execution>
```

**Pontos críticos da configuração:**

- `<phase>verify</phase>` — `check` precisa rodar depois de `report`. Phase `verify` é o ponto correto. Não usar `test` (rodaria antes do report).
- `<element>BUNDLE</element>` — agregado total do projeto.
- `<element>PACKAGE</element>` — granularidade por pacote. Padrão `**.infrastructure.*` casa com `com.laboratorio.financas.shared.infrastructure.security`, `com.laboratorio.financas.shared.infrastructure.web` etc. Validar que pega o que se espera (ver Tarefa 3).
- `<counter>INSTRUCTION</counter>` — granularidade de bytecode (não LINE). Mais estável entre versões da JVM/compilador. Se mudar pra LINE, thresholds precisam ser recalibrados.
- `<value>COVEREDRATIO</value>` + `<minimum>0.75</minimum>` — 0.75 = 75%. Valor decimal entre 0 e 1.
- Exclusão `**/FinancasApplication.class` em `<excludes>` da configuração de `check` (não dos goals `prepare-agent`/`report`).

**Pesquisar antes de chutar sintaxe.** A documentação oficial do plugin (jacoco.org/jacoco/trunk/doc/check-mojo.html) é a referência. Não confiar em memória pra estrutura XML — JaCoCo é particularmente picuinhas com elemento errado dentro do `<rule>`.

### Tarefa 2 — Rodar `mvnw verify` e confirmar que passa

```powershell
cd C:\projetos\financas-lab
.\mvnw clean verify
```

Esperado:

- `Tests run: 5` (mantém o que existe — esta etapa não adiciona teste novo)
- BUILD SUCCESS
- Saída do JaCoCo Check loga algo como "All coverage checks have been met" ou similar
- Relatório em `target/site/jacoco/index.html` gerado normalmente

Se BUILD FAILURE acontecer com mensagem de threshold não atingido, **parar e reportar** ao operador antes de tentar consertar. Pode ser:
- Configuração XML errada (caminho mais provável)
- Pacote `infrastructure` não atingindo 60% genuinamente (improvável dado que `HealthcheckControllerTest` já exercita o controller; mas `SecurityConfig` pode não estar sendo coberto suficientemente pelo teste atual)
- Threshold global 75% não sendo atingido (improvável; só 4 classes Java no main)

### Tarefa 3 — Validar destrutivamente que `check` realmente quebra build

Esta validação **não vai pro commit** — é teste local pra confirmar que o gate funciona.

Passo a passo:

1. Editar **localmente** o `pom.xml` mudando `<minimum>0.75</minimum>` da regra BUNDLE pra `<minimum>0.99</minimum>` (cobertura 99%, impossível de bater com o código atual).
2. Rodar `.\mvnw clean verify`. Esperado: **BUILD FAILURE** com mensagem indicando que a regra BUNDLE não foi atingida (algo tipo "Rule violated for bundle financas: instructions covered ratio is X.XX, but expected minimum is 0.99").
3. Reverter o `pom.xml`: `git checkout pom.xml`. Confirmar via `git diff pom.xml` que voltou ao estado limpo.
4. Rodar `.\mvnw clean verify` de novo. Esperado: **BUILD SUCCESS**.
5. Documentar essa validação no PR body (texto sugerido na seção "PR" abaixo).

**Não commitar o `<minimum>0.99</minimum>` em hipótese alguma.** É instrumento de teste local, não estado do projeto.

### Tarefa 4 — Atualizar `decisoes.md`

Três adições, em ordem:

**4a.** Em **"Configuração crítica do `pom.xml`"**, atualizar o bullet do JaCoCo (que hoje diz "regras de cobertura por camada entram na Etapa 2.4"). Substituir por:

```markdown
- JaCoCo plugin com 3 execuções: `prepare-agent` + `report` + `check`. Regras ativas hoje: BUNDLE 75% (global), PACKAGE `**.infrastructure.*` 60%. Regras de `domain`/`application`/`interfaces` (90%/80%/70%) ficam **comentadas** no `pom.xml` aguardando primeira classe nesses pacotes (Camada 2). Exclusão única: `FinancasApplication.class`.
```

**4b.** Em **"Convenções de código"** → **"Cobertura mínima por camada (JaCoCo)"**, adicionar nota explicativa **abaixo da tabela existente** (não modificar a tabela):

```markdown
**Status atual de aplicação dos thresholds (Etapa 2.4):**

- ✅ **Ativos:** BUNDLE 75% (global), `infrastructure` 60%
- ⏸️ **Aguardando classes (ativados na Camada 2):** `domain` 90%, `application` 80%, `interfaces` 70`%

Regras inativas estão comentadas no `pom.xml` e devem ser descomentadas no PR que introduzir a primeira classe do pacote correspondente. Esse é débito técnico consciente — registrado, com data de resolução conhecida (Camada 2).
```

**4c.** Adicionar linha no **"Histórico de mudanças"** no fim do arquivo, no topo da lista:

```markdown
- **2026-05-08** — Etapa 2.4 concluída: JaCoCo `check` ativado com thresholds BUNDLE 75% e `infrastructure` 60%. Thresholds de `domain`/`application`/`interfaces` ficam comentados aguardando primeira classe (Camada 2). Validação destrutiva confirmou que `mvnw verify` falha quando cobertura cai abaixo do threshold.
```

### Tarefa 5 — Atualizar `progresso.md`

**5a.** Atualizar campo "Última atualização" no topo: `2026-05-08 (Etapa 2.4)`.

**5b.** Marcar como `[x]` na seção da Camada 1 o critério mais próximo de "JaCoCo configurado". O critério atual está como:

```
- [x] JaCoCo configurado (sem thresholds — apenas prepare-agent + report; thresholds por camada entram na Etapa 2.4)
```

Substituir por:

```
- [x] JaCoCo configurado com thresholds (BUNDLE 75%, infrastructure 60%; domain/application/interfaces aguardam Camada 2)
```

**5c.** Adicionar nova seção **"Lições da Etapa 2.4"** logo antes de **"Lições da Etapa 2.3"** (mantendo ordem decrescente). Conteúdo: candidatos a hook e lições de ambiente que **realmente forem observados durante a execução**. Padrão pra preencher quando nada digno emergir:

```markdown
## Lições da Etapa 2.4

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum novo nesta etapa.)

### Licoes de ambiente

(Nenhuma nova nesta etapa.)
```

**Regra dura:** só registrar lições **realmente observadas**. Se a configuração XML do JaCoCo der trabalho não-trivial, se a sintaxe `<element>PACKAGE</element>` + `<includes>` não funcionar como esperado, se a validação destrutiva revelar que o caminho `**.infrastructure.*` não casa, etc. — registrar honestamente. Não inventar.

**5d.** Adicionar entrada no **"Histórico de mudanças deste documento"** no topo da lista:

```markdown
- **2026-05-08** — Etapa 2.4 concluída: JaCoCo `check` com thresholds aplicados (BUNDLE 75%, infrastructure 60%), thresholds dos pacotes vazios comentados como TODO Camada 2, validação destrutiva confirmando gate. Mergeado via PR #XX.
```

(O `#XX` é placeholder. Será substituído pelo número real do PR num commit adicional **depois** que o PR for aberto, conforme passo final desta etapa — ver "Pós-criação do PR" abaixo.)

### Tarefa 6 — Versionar este próprio prompt

Confirmar que `docs/prompt-etapa-2-4.md` está em disco como untracked, e incluir no commit de docs.

## Restrições e freios

1. **Não tocar em arquivos fora do escopo.** Arquivos permitidos:
   - `pom.xml`
   - `docs/decisoes.md`
   - `docs/progresso.md`
   - `docs/prompt-etapa-2-4.md` (este arquivo)

2. **"Necessidade técnica direta" não é exceção válida à Restrição 1.** Se ao executar você identificar que algo precisa mudar fora dessa lista (Java code, application.yml, ci.yml, AbstractIntegrationTest, qualquer outro arquivo), **pare e reporte** ao operador antes de tocar.

3. **Não escrever código de aplicação nesta etapa.** Não criar classes pra atingir threshold. Não criar testes novos pra "completar cobertura". Os testes que existem hoje (5 ao todo) precisam atingir os thresholds **com o que já existe**. Se não atingirem, é problema da configuração, não do código.

4. **Não excluir classes de coverage além de `FinancasApplication.class`.** Tentação comum: excluir `*Config.class`, `*Response.class` (records), `*Exception.class`. Recusar — exclusão amplia silenciosamente. Manter lista enxuta. Se threshold não bater por causa de uma classe específica, parar e reportar pra discutir, não excluir.

5. **Não ativar os thresholds comentados** (`domain`/`application`/`interfaces`). Eles estão comentados por decisão consciente — Camada 2 reativa.

6. **Não tocar em `ci.yml`.** O CI atual roda `mvnw verify`, então a regra `check` (em phase `verify`) já é executada automaticamente. Nenhuma alteração no workflow é necessária.

7. **Não antecipar Etapa 2.5 (Checkstyle + SpotBugs).** Se a configuração do JaCoCo der pegadinha, resolver dentro do JaCoCo — não introduzir Checkstyle/SpotBugs como atalho.

8. **Validar conteúdo bruto antes de commitar.** Para arquivos com acentos: `Get-Content <path> -Encoding UTF8`. Para `pom.xml`: confirmar que não introduziu BOM nem mudou indentação do resto do arquivo (alguns editores re-formatam XML inteiro ao salvar). `git diff pom.xml` deve mostrar apenas a adição da execução `check`.

9. **Pesquisar antes de chutar sintaxe JaCoCo.** A documentação oficial é `jacoco.org/jacoco/trunk/doc/check-mojo.html`. Estrutura de `<rule>`, `<element>`, `<counter>`, `<value>`, `<limits>` é específica e errar um nome de tag silenciosamente faz a regra ser ignorada (build "passa" sem aplicar a regra real).

## Estrutura de commits

Branch: `feat/jacoco-thresholds`

Commits atômicos, em ordem:

**Commit 1** — `feat: adiciona JaCoCo check com thresholds BUNDLE 75% e infrastructure 60%`
- `pom.xml`

**Commit 2** — `docs: registra etapa 2.4 (JaCoCo thresholds) em decisoes e progresso`
- `docs/decisoes.md`
- `docs/progresso.md`
- `docs/prompt-etapa-2-4.md`

## Validação antes de abrir PR

```powershell
git status                                                          # working tree limpo
git log --oneline -3                                                # 2 commits novos visiveis
.\mvnw clean verify                                                 # BUILD SUCCESS, Tests run: 5, JaCoCo check passou
git diff main pom.xml | Select-String -Pattern "check|infrastructure|BUNDLE"   # diff faz sentido
Get-Content docs/progresso.md -Encoding UTF8 | Select-String "Etapa 2.4"
```

## PR

Título: `feat: etapa 2.4 — JaCoCo check com thresholds por camada`

Body sugerido (ajustar conforme execução real, especialmente a seção "Validação destrutiva" com os números reais observados):

```markdown
## Summary

Implementa a Etapa 2.4 do roadmap: JaCoCo `check` ativado com thresholds reais. A partir deste merge, `mvnw verify` (e o CI por consequência) falha se cobertura cair abaixo dos limites.

### Mudanças

- `pom.xml`: terceira `<execution>` no plugin JaCoCo (id `check`, phase `verify`) com:
  - Regra BUNDLE: 75% global (INSTRUCTION coveredratio)
  - Regra PACKAGE `**.infrastructure.*`: 60%
  - Regras `domain`/`application`/`interfaces` **comentadas** com TODO referenciando Camada 2 (pacotes vazios hoje)
  - Exclusão única: `**/FinancasApplication.class`
- `decisoes.md`: atualiza bullet do JaCoCo na configuração crítica do `pom.xml`, adiciona nota de status na seção de cobertura por camada, registra histórico.
- `progresso.md`: atualiza critério, registra lições.

### Validação local

- `mvnw clean verify` local: BUILD SUCCESS, Tests run: 5
- JaCoCo Check loga "All coverage checks have been met" (ou equivalente)
- Relatório gerado em `target/site/jacoco/index.html`

### Validação destrutiva

Confirmação de que o gate funciona, **não commitada**:

1. Mudei localmente `<minimum>0.75</minimum>` para `<minimum>0.99</minimum>` na regra BUNDLE
2. `mvnw clean verify` retornou **BUILD FAILURE** com mensagem indicando regra violada
3. `git checkout pom.xml` e nova execução de `mvnw clean verify` voltou a BUILD SUCCESS

### Decisões de escopo

- **Thresholds dos pacotes vazios são débito técnico consciente.** Comentados no `pom.xml` com TODO. Reativados na Camada 2 quando primeira classe do pacote correspondente entrar.
- **`SecurityConfig` não foi excluído** — incluí-lo nos thresholds incentiva testar o filtro de Security, que é exatamente onde mais bug aparece.
- **Lista de exclusões enxuta** — só `FinancasApplication.class`. Não excluir `*Config`, records, etc.

### Próximo passo

Etapa 2.5 (Checkstyle + SpotBugs) — fora do escopo deste PR.
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

- `main` local sincronizada com `origin/main`, incluindo squash commit da 2.4
- `git status` limpo
- `mvnw clean verify` passa local com `Tests run: 5` e JaCoCo check verde
- `docs/progresso.md` reflete 2.4 concluída, número real do PR no histórico
- Branch `feat/jacoco-thresholds` deletada local e remotamente

Reportar ao operador o estado final com `git log --oneline -3` e `git status`. Parar.

## O que NÃO fazer ao terminar

- Não criar prompt da 2.5
- Não tocar em `pom.xml` pra adicionar Checkstyle/SpotBugs, ajustar Surefire, mexer em dependência
- Não criar testes novos "preventivos"
- Não criar `application-dev.yml`, `application-prod.yml`, ou qualquer outro arquivo
- Não sugerir "próximo passo" espontaneamente. Fim de etapa = parada explícita.
