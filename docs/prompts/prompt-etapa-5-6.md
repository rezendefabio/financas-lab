# Prompt -- Sub-etapa 5.6: Bounded context `importacao`

## Contexto

Importacao de transacoes via CSV em formato fixo. Padrao novo: file upload
(multipart/form-data) + parsing + batch de escrita. Sem entidade propria, sem
migration -- mesmo padrao de BC pure-query da 5.5, mas com escrita cross-BC
(cria Transacoes via TransacaoRepository).

Nao ha biblioteca CSV no pom.xml. Usar `BufferedReader` + `InputStreamReader` -- nao
adicionar dependencias sem autorizacao do operador.

Sub-etapa 5.6 (Camada 4).

---

## Layout CSV (fixo, documentado)

Header obrigatorio (primeira linha):
```
tipo,valor,moeda,data,descricao,contaId,contaDestinoId,categoriaId
```

Regras:
- Separador: virgula `,`
- Sem aspas / sem quoting -- virgulas nao sao permitidas em nenhum campo
- Campos opcionais vazios: campo vazio (ex: `,,`)
- Encoding: UTF-8
- `tipo`: RECEITA, DESPESA ou TRANSFERENCIA
- `valor`: decimal com ponto (ex: `1500.00`)
- `moeda`: ISO 3 letras (ex: `BRL`)
- `data`: formato `yyyy-MM-dd`
- `descricao`: max 200 chars, sem virgula
- `contaId`: UUID obrigatorio
- `contaDestinoId`: UUID, obrigatorio apenas para TRANSFERENCIA, vazio para os demais
- `categoriaId`: UUID opcional, vazio se ausente

Exemplos de linhas validas:
```
DESPESA,150.00,BRL,2026-05-01,Supermercado,11111111-0000-0000-0000-000000000001,,22222222-0000-0000-0000-000000000002
RECEITA,3000.00,BRL,2026-05-01,Salario,11111111-0000-0000-0000-000000000001,,
TRANSFERENCIA,500.00,BRL,2026-05-01,Reserva mensal,11111111-0000-0000-0000-000000000001,33333333-0000-0000-0000-000000000003,
```

---

## Application layer

### `ImportarTransacoesCsvUseCase`

Injeta: `TransacaoRepository`

**Estrategia em duas fases (parsing separado de persistencia):**

**Fase 1 -- Parsing (sem DB):**
1. Ler o conteudo como `BufferedReader` via `new InputStreamReader(new ByteArrayInputStream(conteudo), StandardCharsets.UTF_8)`
2. Ler primeira linha: validar que e exatamente `tipo,valor,moeda,data,descricao,contaId,contaDestinoId,categoriaId`
   - Se diferente: lancar `IllegalArgumentException("Cabecalho CSV invalido. Formato esperado: tipo,valor,moeda,data,descricao,contaId,contaDestinoId,categoriaId")`
3. Para cada linha seguinte (numeracao comeca em 2):
   - Ignorar linhas vazias (trim + isBlank)
   - Tentar parsear: split por `,`, trim em cada campo, validar quantidade de colunas (exatamente 8)
   - Montar objeto interno `LinhaValida` com os dados parseados, ou `LinhaInvalida` com numero da linha e motivo
   - Erros de parsing (formato de data invalido, UUID invalido, valor nao numerico, tipo desconhecido): capturar como `LinhaInvalida`

**Fase 2 -- Persistencia (@Transactional):**
1. Para cada `LinhaValida`, construir `Transacao` usando o construtor de nova transacao:
   ```java
   new Transacao(tipo, new Money(valor, Currency.getInstance(moeda)), data, descricao, contaId, contaDestinoId, categoriaId)
   ```
   Leia `Transacao.java` antes de implementar para confirmar assinatura.
2. Salvar via `transacaoRepository.salvar(transacao)`
3. Erros de persistencia (ex: violacao de constraint) capturar por linha e adicionar aos erros
4. Continuar processando as demais linhas mesmo com falhas individuais

**Metodo publico:**
```java
public Resultado importar(byte[] conteudoCsv)
```

Separar as duas fases internamente; o metodo publico chama fase 1 (sem @Transactional)
e depois chama metodo privado `@Transactional` para fase 2.

**Resultado (inner records):**
```java
record Resultado(
    int totalLinhas,    // linhas de dados (excluindo header e linhas vazias)
    int importadas,
    int falhas,
    List<ErroImportacao> erros
)

record ErroImportacao(int linha, String motivo)
```

Records internos (`LinhaValida`, `LinhaInvalida`) sao privados ao use case -- nao
expor no contrato publico.

---

## Interface layer

### `ImportacaoController.java`

```java
@RestController
@RequestMapping("/api/importacoes")
public class ImportacaoController {

    @PostMapping(value = "/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ImportacaoResponse importarCsv(
            @RequestParam("arquivo") MultipartFile arquivo) {
        byte[] conteudo = arquivo.getBytes();  // pode lancar IOException -- deixar propagar
        ImportarTransacoesCsvUseCase.Resultado resultado = useCase.importar(conteudo);
        return ImportacaoResponse.fromResultado(resultado);
    }
}
```

### DTOs

**`ImportacaoResponse.java`:**
```java
public record ImportacaoResponse(
    int totalLinhas,
    int importadas,
    int falhas,
    List<ErroImportacaoResponse> erros
) {
    public record ErroImportacaoResponse(int linha, String motivo) { }

    public static ImportacaoResponse fromResultado(ImportarTransacoesCsvUseCase.Resultado r) {
        List<ErroImportacaoResponse> erros = r.erros().stream()
            .map(e -> new ErroImportacaoResponse(e.linha(), e.motivo()))
            .toList();
        return new ImportacaoResponse(r.totalLinhas(), r.importadas(), r.falhas(), erros);
    }
}
```

---

## Estrutura de arquivos

```
importacao/
  application/
    ImportarTransacoesCsvUseCase.java
  interfaces/
    ImportacaoController.java
    dto/
      ImportacaoResponse.java
```

**Sem domain/** -- nao ha entidade propria.
**Sem infrastructure/** -- usa TransacaoRepository diretamente.
**Sem migration** -- nao ha tabela nova.

---

## Fluxo de execucao

```
1. git checkout -b feat/etapa-5-6-importacao-csv

2. NAO invocar /feature (sem entity stub)
   NAO invocar /migrate (sem tabela)

3. Criar ImportarTransacoesCsvUseCase.java em importacao/application/
   Leia Transacao.java antes de implementar para confirmar assinatura do construtor.

4. commit: feat(importacao): implementa application layer (ImportarTransacoesCsvUseCase)

5. Criar ImportacaoController.java + ImportacaoResponse.java em importacao/interfaces/

6. Por convencao implicita (CLAUDE.md):
   - /write-test ImportarTransacoesCsvUseCase.java (Mockito)
     Cenarios obrigatorios: CSV valido com 3 linhas, linha com tipo invalido,
     linha com data invalida, cabecalho invalido, CSV vazio (so header),
     TRANSFERENCIA sem contaDestinoId.
   - /write-test ImportacaoController.java (MockMvc E2E com MockMultipartFile)
     Usar: MockMvcRequestBuilders.multipart("/api/importacoes/csv")
           .file(new MockMultipartFile("arquivo", "test.csv", "text/csv", conteudoCsv))
   Leia a skill /write-test e execute manualmente seguindo o padrao.

7. ./mvnw verify -- BUILD SUCCESS obrigatorio

8. Atualiza docs/progresso.md (registra 5.6 e padrao novo: file upload + batch cross-BC)

9. commit: feat(importacao): implementa interface layer e testes; registra sub-etapa 5.6
   (inclui docs/progresso.md + docs/prompts/prompt-etapa-5-6.md)

10. /ship → PR aberto com reviews automaticos; corrigir apontamentos autonomamente
```

---

## Estrutura de commits (5.6)

```
feat(importacao): implementa application layer (ImportarTransacoesCsvUseCase)
feat(importacao): implementa interface layer e testes; registra sub-etapa 5.6
```

2 commits (sem skeleton, sem migration).

---

## Arquivos de referencia (ler antes de implementar)

- `transacao/domain/Transacao.java` -- assinatura do construtor de nova transacao
- `transacao/domain/TransacaoRepository.java` -- metodo salvar()
- `transacao/domain/TipoTransacao.java` -- enum para parsear tipo do CSV
- `shared/domain/Money.java` -- construtor Money(BigDecimal, Currency)
- `orcamento/application/CalcularProgressoDoOrcamentoUseCase.java` -- padrao cross-BC

---

## Restricoes

- NAO adicionar dependencias ao pom.xml (nao incluir OpenCSV, Commons CSV, etc.)
- NAO modificar TransacaoRepository ou qualquer BC existente
- NAO criar migration
- NAO invocar /feature ou /migrate
- Virgulas em descricao: nao sao suportadas (layout fixo sem quoting)
- Se hook bloquear commit: ler a mensagem, corrigir sem --no-verify

---

## Padrao novo a documentar em progresso.md

**File upload + batch cross-BC (primeira ocorrencia):** `ImportacaoController` recebe
multipart/form-data; `ImportarTransacoesCsvUseCase` parseia CSV com BufferedReader
(sem biblioteca externa) e persiste Transacoes via TransacaoRepository. Estrategia
duas fases: parsing sem DB + persistencia @Transactional. Erros individuais coletados
sem abortar o batch.

---

## Estado esperado ao terminar

- PR aberto com 2 commits acima de main.
- ./mvnw verify BUILD SUCCESS com todos os testes existentes + novos.
- POST /api/importacoes/csv aceitando multipart/form-data com campo "arquivo".
- docs/progresso.md com 5.6 registrada.
- docs/prompts/prompt-etapa-5-6.md commitado no ultimo commit.

---

## O que NAO fazer ao terminar

- NAO fazer merge sem instrucao do operador.
- NAO rodar /ship mais de uma vez.
- NAO criar domain/ ou infrastructure/.
