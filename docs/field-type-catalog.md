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
