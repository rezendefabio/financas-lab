# Prompt -- Sub-etapa 5.26: Catalogo de mapeamento tipo-backend → componente-frontend

## Contexto

Bug estrutural recorrente na fabrica: agentes executores mapeiam campos do backend para
componentes frontend errados por nao terem um guia de referencia. Exemplos observados:
- `BigDecimal` monetario → `<input type="text">` livre (deveria ser `type="number" step="0.01"`)
- `boolean` → campo de texto (deveria ser `<Switch>` ou radio Sim/Nao)
- FK `UUID` → campo de texto (deveria ser `<Select>` carregado da API)

Esta sub-etapa cria um catalogo formal `docs/field-type-catalog.md`, atualiza `CLAUDE.md`
para referenciar o catalogo, e adiciona regra B7 no `front-reviewer` para bloquear violacoes.

---

## Arquivos a ler antes de comecar

- `CLAUDE.md` (secao `## Frontend` -- onde inserir referencia ao catalogo)
- `.claude/agents/front-reviewer.md` (estrutura atual das regras B1-B6)
- `frontend/src/shared/lib/formatters.ts` (formatadores disponiveis -- citar no catalogo)

---

## Mudanca 1 -- Criar `docs/field-type-catalog.md`

```markdown
# Catalogo de Mapeamento Tipo-Backend → Componente-Frontend

Referencia obrigatoria para todo executor que implementar formulario ou listagem frontend.
Ler antes de gerar qualquer campo. Violacao de B7 bloqueia merge.

## Regras de mapeamento

### Valores monetarios

| Situacao | Input | Exibicao |
|---|---|---|
| `BigDecimal` com significado monetario (valor, limite, saldo) | `<Input type="number" step="0.01" min="0">` com prefixo "R$" | `formatBRL(value)` |
| Moeda (`String valorLimiteMoeda`, `String moeda`) | Campo oculto com valor padrao `"BRL"` -- nao expor ao usuario | `moeda` ao lado do valor |

### Datas e timestamps

| Tipo Java | Input | Exibicao |
|---|---|---|
| `LocalDate` (data simples) | `<Input type="date">` | `formatDate(value)` |
| `LocalDate` representando mes/ano (ex: orcamento.mesAno) | `<Input type="month">` -- retorna `YYYY-MM`; concatenar `-01` ao enviar | `formatDate(value)` ou `MM/YYYY` |
| `Instant` / `LocalDateTime` (timestamp) | Nunca editavel -- apenas exibicao | `formatDate(value)` |

### Booleanos

| Tipo Java | Input | Exibicao |
|---|---|---|
| `boolean` | `<Switch>` (shadcn) ou radio group Sim/Nao | Badge colorido ou icone |

### Referencias (FKs)

| Situacao | Input | Exibicao |
|---|---|---|
| `UUID categoriaId` | `<Select>` carregado de `GET /api/categorias` | Nome da categoria |
| `UUID contaId` | `<Select>` carregado de `GET /api/contas` | Nome da conta |
| Qualquer outro UUID que seja FK | `<Select>` carregado do endpoint correspondente | Nome da entidade |

### Enums

| Situacao | Input | Exibicao |
|---|---|---|
| Enum Java com valores fixos | `<Select>` com mapa de labels (ex: `formatTipoConta`) | Label do mapa, nao o valor raw |

### Strings com restricoes

| Anotacao Java | Input |
|---|---|
| `@Size(max=N)` | `<Input maxLength={N}>` |
| `@Email` | `<Input type="email">` |
| `@NotBlank` | Sem placeholder vazio; incluir `min(1)` no Zod |

### Valores calculados / read-only

| Situacao | Componente |
|---|---|
| `percentualUtilizado` (BigDecimal 0-100+) | Barra de progresso (`<Progress>`) + texto `XX%` |
| `status` (enum como String: ABAIXO/ATENCAO/ATINGIDO/EXCEDIDO) | Badge com cor semantica |
| `criadoEm`, `atualizadoEm` (Instant) | Texto read-only, nunca editavel |
| Saldo derivado, total calculado | Texto read-only com `formatBRL()` |

### Objetos aninhados (ValorMonetario)

Quando o backend retorna `{ valor: number, moeda: string }` aninhado (ex: `valorLimite.valor`,
`totalGasto.valor`), o tipo TypeScript DEVE refletir a estrutura aninhada -- nao achatar.
Acesso correto: `formatBRL(orcamento.valorLimite.valor)`.

## Formatadores disponíveis (frontend/src/shared/lib/formatters.ts)

- `formatBRL(value: number)` -- formata numero como moeda BRL
- `formatDate(dataIso: string)` -- formata data ISO como DD/MM/YYYY
- `formatTipoConta(tipo: string)` -- label para TipoConta
- `formatTipoCategoria(tipo: string)` -- label para TipoCategoria (RECEITA/DESPESA)
- `formatTipoTransacao(tipo: string)` -- label para TipoTransacao
```

---

## Mudanca 2 -- Referenciar o catalogo em `CLAUDE.md`

Na secao `## Frontend`, apos a linha de validacao Zod (B6), adicionar:

```
- Mapeamento de tipo-backend para componente: antes de implementar qualquer campo,
  consultar `docs/field-type-catalog.md`. Violacao e bloqueador B7.
```

---

## Mudanca 3 -- Adicionar B7 em `.claude/agents/front-reviewer.md`

Na tabela de bloqueadores (apos B6), adicionar:

```
| B7 | Campo com tipo semantico errado | `BigDecimal` monetario sem `type="number" step="0.01"`; `boolean` como campo de texto; FK `UUID` como input livre; `Instant` como campo editavel. Consultar `docs/field-type-catalog.md`. |
```

---

## Fluxo de execucao

```
1. git checkout -b docs/etapa-5-26-catalogo-field-type

2. Ler CLAUDE.md, .claude/agents/front-reviewer.md, frontend/src/shared/lib/formatters.ts

3. Criar docs/field-type-catalog.md (Mudanca 1)

4. Editar CLAUDE.md adicionando referencia ao catalogo (Mudanca 2)

5. Editar .claude/agents/front-reviewer.md adicionando B7 (Mudanca 3)

6. Confirmar: Test-Path docs/field-type-catalog.md -- True
   Confirmar: grep "B7" .claude/agents/front-reviewer.md -- deve retornar linha

7. commit: docs(claude): cria catalogo de mapeamento tipo-backend para componente-frontend

8. Atualizar docs/progresso.md (registra sub-etapa 5.26)

9. commit: docs(progresso): registra sub-etapa 5.26
   (inclui docs/progresso.md + docs/prompts/prompt-etapa-5-26.md)

10. /ship -> PR; corrigir apontamentos autonomamente
```

## Estrutura de commits

```
docs(claude): cria catalogo de mapeamento tipo-backend para componente-frontend
docs(progresso): registra sub-etapa 5.26
```

## Restricoes

- NAO alterar nenhum arquivo de codigo frontend ou backend.
- Apenas docs/field-type-catalog.md (novo), CLAUDE.md, .claude/agents/front-reviewer.md
  e docs/progresso.md sao modificados.
- check.ps1 NAO e necessario (sem alteracao de codigo).
- O scope do commit para .claude/agents/ e `claude` (sem ponto -- convencao do projeto).
