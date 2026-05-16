# Catalogo de Mapeamento Tipo-Backend → Componente-Frontend

Referencia obrigatoria para todo executor que implementar formulario ou listagem frontend.
Ler antes de gerar qualquer campo. Violacao de B7 bloqueia merge.

## Regras de mapeamento

### Valores monetarios

| Situacao | Input | Exibicao |
|---|---|---|
| `BigDecimal` com significado monetario (valor, limite, saldo) | `MoneyInput` (de `@/shared/components/MoneyInput`) | `formatBRL(value)` |
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
| Enum Java com valores fixos | `<Select>` com mapa de labels e render function em `Select.Value` (ver secao abaixo) | Label do mapa, nao o valor raw |

### Select: exibicao do valor selecionado (obrigatorio)

`@base-ui/react` v1.4.1 -- `Select.Value` nao espelha automaticamente o texto do item selecionado
quando o popup esta fechado. **Obrigatorio** usar render function como `children`.

**Para opcoes estaticas (array de constantes):**

```tsx
<SelectValue placeholder="Selecione">
  {(v: string | null) => OPTIONS.find(o => o.value === v)?.label ?? 'Selecione'}
</SelectValue>
```

**Para opcoes dinamicas (dados da API):**

```tsx
<SelectValue placeholder="Selecione">
  {(v: string | null) => {
    if (!v) return 'Selecione'
    return (items ?? []).find(i => i.id === v)?.nome ?? 'Selecione'
  }}
</SelectValue>
```

**Nunca usar `<SelectValue />` ou `<SelectValue placeholder="..." />` sem render function.**
O resultado seria exibir o `value` bruto (UUID, enum string, booleano) no trigger.

Violacao deste padrao e bloqueador B7 (campo implementado sem consultar o catalogo).

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

## Componentes wrapper (shared/components/)

### MoneyInput

- Quando usar: qualquer `BigDecimal` com significado monetario em formulario
- Importacao: `import { MoneyInput } from '@/shared/components/MoneyInput'`
- Props: `value: number`, `onChange: (value: number) => void`, `disabled?`, `className?`, `id?`
- Integracao react-hook-form: passar `value={field.value}` e `onChange={field.onChange}`, NAO usar spread `{...field}`
- Zod schema: `z.coerce.number().min(0)` para valores >= 0, `.positive()` para obrigatorio positivo
- Nao usar: `<Input type="number" step="0.01">` para valores monetarios

### StatusBadge

- Quando usar: exibicao de qualquer enum/status com cor semantica
- Importacao: `import { StatusBadge, ORCAMENTO_STATUS_CONFIG, META_STATUS_CONFIG } from '@/shared/components/StatusBadge'`
- Props: `status: string`, `config: Record<string, StatusConfig>`, `fallbackLabel?: string`
- Configs pre-prontos: ORCAMENTO_STATUS_CONFIG, META_STATUS_CONFIG, CONTA_ATIVA_CONFIG

### StatCard

- Quando usar: metricas/KPIs no dashboard ou em paginas de detalhe
- Importacao: `import { StatCard } from '@/shared/components/StatCard'`
- Props: `titulo`, `valor` (string ja formatada), `variacao?`, `descricao?`, `icone?`
- Valor ja deve estar formatado antes de passar: usar `formatBRL(valor)` ou `String(count)`
