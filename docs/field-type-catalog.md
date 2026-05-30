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
| `Instant` / `LocalDateTime` (timestamp) | Nunca editavel -- apenas exibicao | `formatDateTime(value)` -- NAO `formatDate` (causaria Invalid Date) |

### Booleanos

| Tipo Java | Input | Exibicao |
|---|---|---|
| `boolean` | `<input type="checkbox" className="accent-primary">` (nativo) | Badge colorido ou icone |

**Nota:** o componente `Switch` do shadcn NAO esta instalado no projeto. Se for
necessario em algum dia, instalar via `npx shadcn@latest add switch` E atualizar
esta linha. Ate la, usar checkbox nativo -- e o pattern ja aplicado em
todos os formularios existentes (ver `TransacaoForm`, `EmprestimoForm`).

### Referencias (FKs)

| Situacao | Input preferido | Quando usar Select estatico |
|---|---|---|
| `UUID categoriaId`, `UUID payeeId`, `UUID contaId`, qualquer FK | `<LookupField>` (combobox filtravel, `useQuery` interno) | Apenas em telas com poucos itens (<10) e sem necessidade de busca |

**Padrao LookupField (preferido):**
```tsx
<LookupField
  value={field.value ?? null}
  onChange={(v) => field.onChange(v ?? '')}
  queryKey={['categorias', 'lookup']}  // sufixo 'lookup' obrigatorio -- ver B13
  queryFn={() => categoriasService.listar()
    .then(cs => cs.map(c => ({ value: c.id, label: c.nome })))}
  placeholder="Selecione uma categoria"
/>
```

**Convencao critica de queryKey:**
O sufixo `'lookup'` (ou outro discriminador) e OBRIGATORIO quando a pagina de
listagem da mesma entidade usa o queryKey base (ex: `['categorias']`). Sem o
sufixo, o TanStack Query reutiliza o cache da listagem (que contem objetos raw
sem campo `label`), causando `TypeError: Cannot read properties of undefined
(reading 'toLowerCase')` em runtime.

Excecoes (ja tem sufixo natural):
- `['categorias', tipoAtual]` -- o tipo ja discrimina
- `['contas', 'ativas']` -- ja tem segundo elemento distinto

**Se ainda usar Select estatico:** obrigatorio usar `SelectValue` com render
function (ver secao abaixo). Nunca `<SelectValue />` sem children.

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

### Layout de formulario (FormGrid / FormCol)

- **`FormGrid`**: importar de `@/shared/components/FormGrid`. Wrapper `div` com
  `grid grid-cols-12 gap-4`. Usar para agrupar campos que se beneficiam de layout
  lado a lado.
- **`FormCol`**: importar de `@/shared/components/FormCol`. Prop `span` (1-12,
  default 12). Usar dentro de `FormGrid`.

Sugestoes de span por tipo de campo:
| Campo | Span sugerido |
|---|---|
| Descricao / Nome (texto longo) | 12 |
| Valor monetario | 7 |
| Data | 5 |
| Tipo / Status (enum) | 4-6 |
| Conta / Categoria (LookupField) | 6 |
| Booleano (checkbox) | 4 |

Campos simples que nao ganham com grid (formularios de 1-2 campos) podem usar
`space-y-4` diretamente, sem `FormGrid`.

### Objetos aninhados (ValorMonetario)

Quando o backend retorna `{ valor: number, moeda: string }` aninhado (ex: `valorLimite.valor`,
`totalGasto.valor`), o tipo TypeScript DEVE refletir a estrutura aninhada -- nao achatar.
Acesso correto: `formatBRL(orcamento.valorLimite.valor)`.

## Formatadores disponíveis (frontend/src/shared/lib/formatters.ts)

- `formatBRL(value: number)` -- formata numero como moeda BRL
- `formatDate(dataIso: string)` -- formata data ISO como DD/MM/YYYY
- `formatDateTime(instant: string)` -- formata Instant ISO como DD/MM/YYYY HH:mm
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
