---
name: feature-front
description: Cria scaffold de feature frontend completo (13 arquivos = 8 producao + 5 testes) a partir dos DTOs Java do bounded context. Inclui types, service, hooks TanStack, componente Form compartilhado, listagem com DataTable+ActionsPanel, pagina de criacao e pagina de edicao (ambas thin wrappers do Form). Argumento: nome do bounded context em snake_case.
disable-model-invocation: true
argument-hint: [nome-do-bounded-context]
---

Voce deve criar o scaffold de feature frontend para o bounded context `$ARGUMENTS`
no projeto financas-lab, lendo os DTOs Java como fonte de verdade. Execute todos os
passos em ordem. Pare e reporte ao operador se qualquer pre-condicao falhar.

## Definicoes

Defina internamente antes de qualquer acao:

- `ARG` = `$ARGUMENTS` (ex: `categoria`, `transacao`, `payee`)
- `PASCAL` = PascalCase de `ARG`: capitalize a primeira letra de cada segmento
  separado por underscore e concatene. Exemplos: `categoria` -> `Categoria`,
  `meta_financeira` -> `MetaFinanceira`, `payee` -> `Payee`.
- `CAMEL` = camelCase de `ARG`: igual a `PASCAL` mas com a primeira letra minuscula.
  Exemplos: `categoria` -> `categoria`, `meta_financeira` -> `metaFinanceira`.
- `PLURAL` = plural da URL base. Por padrao `ARG + 's'` (ex: `categorias`,
  `transacaos`). Se o Controller tiver `@RequestMapping("/api/...")` com valor
  explicito, usar esse valor (ver Passo 1).

## Passo 0 -- Validacoes (ADR-011)

**Validacao 1 -- formato:**
Verifique se `ARG` casa com `^[a-z][a-z0-9_]*$`. Se nao casar: escreva
"ERRO: argumento invalido. Use letras minusculas, digitos e underscore
(ex: /feature-front conta)." e termine.

**Validacao 2 -- DTOs Java existem:**
Use Glob para verificar que ao menos um destes arquivos existe:

```
src/main/java/com/laboratorio/financas/ARG/interfaces/dto/PASCALResponse.java
src/main/java/com/laboratorio/financas/ARG/interfaces/dto/CriarPASCALRequest.java
src/main/java/com/laboratorio/financas/ARG/interfaces/*Controller.java
```

Se nenhum existir: escreva "ERRO: DTOs nao encontrados. Execute /feature ARG e
implemente interfaces antes de /feature-front." e termine.

**Validacao 3 -- feature nao existe:**
Use Glob para verificar que `frontend/src/features/ARG/` NAO existe.
Se existir: escreva "AVISO: frontend/src/features/ARG/ ja existe. Abortando para
evitar sobrescrita." e termine.

## Passo 1 -- Ler DTOs Java

Leia os arquivos abaixo (apenas os que existirem) e infira a estrutura tipada.

### PASCALResponse.java

`src/main/java/com/laboratorio/financas/ARG/interfaces/dto/PASCALResponse.java`

Para cada campo do record/classe: derive o nome camelCase e o tipo TypeScript:

| Tipo Java | Tipo TypeScript |
|-----------|-----------------|
| `UUID` | `string` |
| `String` | `string` |
| `BigDecimal` | `number` |
| `LocalDate`, `Instant`, `LocalDateTime` | `string` (ISO) |
| `boolean`, `Boolean` | `boolean` |
| `Integer`, `Long`, `int`, `long` | `number` |
| `Money` / objeto aninhado `ValorMonetario` | `ValorMonetario` (interface aninhada) |
| Enum | union type com os valores do enum Java, ou `string` se nao for possivel inferir |
| `List<X>` | `X[]` |

Para inferir os valores de um enum: se o campo for de tipo enum, localize o
arquivo do enum no pacote do bounded context (`.../domain/` ou `.../interfaces/dto/`)
e leia as constantes. Se nao encontrar, use `string`.

### CriarPASCALRequest.java

`src/main/java/com/laboratorio/financas/ARG/interfaces/dto/CriarPASCALRequest.java`

Para cada campo: derive o nome, o tipo TypeScript (tabela acima) e a regra Zod:

| Anotacao Java | Regra Zod |
|---------------|-----------|
| `@NotNull` / `@NotBlank` | campo obrigatorio (sem `.optional()`) |
| `@Size(max=N)` | `.max(N)` |
| `@Size(min=M, max=N)` | `.min(M).max(N)` |
| `@Min(N)` | `.min(N)` |
| `UUID` com `@NotNull` | `z.string().uuid()` |
| `BigDecimal` monetario | `z.number().positive()` (NUNCA `z.coerce.number()` -- quebra o type check; ver nota no PASCALForm) |
| `LocalDate` | `z.string().min(1)` |
| `String` com `@NotBlank` | `z.string().min(1)` |
| `String` sem `@NotBlank` | `z.string()` |
| `boolean` / `Boolean` | `z.boolean()` |

Atencao (bloqueador B6): cada anotacao Java deve ter contrapartida no schema Zod.
Divergencia entre Zod e Java e bloqueador.

### Controller

`src/main/java/com/laboratorio/financas/ARG/interfaces/*Controller.java`

Leia o valor de `@RequestMapping("/api/...")` -- esse e o `PLURAL` real (ex:
`@RequestMapping("/api/categorias")` -> URL base `/api/categorias`).

Para cada metodo publico mapeado, derive o metodo do service correspondente:

| Mapeamento Java | Metodo do service |
|-----------------|-------------------|
| `@GetMapping` sem path | `listar: () => apiFetch<PASCAL[]>('/api/PLURAL')` |
| `@GetMapping("/{id}")` | `buscar: (id: string) => apiFetch<PASCAL>(`/api/PLURAL/${id}`)` |
| `@PostMapping` sem path | `criar: (payload: CriarPASCALPayload) => apiFetch<PASCAL>('/api/PLURAL', { method: 'POST', ... })` |
| `@PutMapping("/{id}")` | `atualizar: (id, payload) => apiFetch<PASCAL>(..., { method: 'PUT', ... })` |
| `@PatchMapping("/{id}")` | `atualizar: (id, payload) => apiFetch<PASCAL>(..., { method: 'PATCH', ... })` |
| `@DeleteMapping("/{id}")` | `remover: (id: string) => apiFetch<void>(..., { method: 'DELETE' })` |
| Sub-recurso (ex: `@GetMapping("/{id}/progresso")`) | metodo especifico nomeado pelo sub-recurso (ex: `progresso: (id) => ...`) |

Use apenas os metodos que existem no Controller. Nao invente metodos.

### Catalogo de campos

Leia tambem `docs/field-type-catalog.md` para verificar o mapeamento de campos
especiais (ValorMonetario, LocalDate como mes/ano, etc.). Use-o para anotar nos
`// TODO` qual componente o executor deve usar em cada campo.

## Passo 2 -- Gerar os 13 arquivos (8 producao + 5 testes)

Use a ferramenta Write para cada arquivo. O Write cria os diretorios pai
automaticamente. Substitua `ARG`, `PASCAL`, `CAMEL` e `PLURAL` pelos valores
definidos. Os campos das interfaces e do schema Zod devem ser os campos reais
inferidos no Passo 1, nao comentarios genericos.

**ANTI-ALUCINACAO (regra dura -- bugs reais observados no smoke):** so importar
de modulos que EXISTEM nos templates abaixo ou em `@/shared/`. NAO inventar
hooks/utils/imports. Em particular:
- SCREEN_CODE e CONST, NUNCA hook (`useScreenCode` NAO existe).
- Util de CSV: `import { exportToCsv } from '@/shared/lib/export-csv'`, assinatura
  EXATA `exportToCsv(filename: string, rows: object[], columns: {key,label}[])`.
  Conferir a assinatura real antes de chamar -- NAO adivinhar aridade/ordem.
- Nao adicionar funcionalidade alem do template (ex: nao inventar export CSV se
  o template da listagem nao tem). Manter o escopo dos 13 arquivos.
Se precisar de um util que nao esta no template, confirmar o caminho e a
assinatura reais antes de usar -- import/assinatura inventados quebram o
`next build` e custam re-rodadas caras.

### Arquivo 1: frontend/src/features/ARG/types/ARG.ts

```typescript
// Interfaces geradas a partir dos DTOs Java de PASCAL

export interface ValorMonetario {
  valor: number
  moeda: string
}

// Interface principal -- campos inferidos de PASCALResponse.java
export interface PASCAL {
  id: string
  // ... campos inferidos
}

// Payload de criacao -- campos inferidos de CriarPASCALRequest.java
export interface CriarPASCALPayload {
  // ... campos inferidos
}

// Se houver enums, gerar o union type correspondente, ex:
// export type PASCALStatus = 'VALOR_A' | 'VALOR_B'
```

Regras:
- Omitir `ValorMonetario` se o dominio nao tiver nenhum campo monetario.
- Para cada enum detectado no Passo 1, gerar o respectivo `export type`.

### Arquivo 2: frontend/src/features/ARG/services/ARG-service.ts

```typescript
import { apiFetch } from '@/services/api-client'
import type { PASCAL, CriarPASCALPayload } from '../types/ARG'

export const CAMELService = {
  listar: () => apiFetch<PASCAL[]>('/api/PLURAL'),
  buscar: (id: string) => apiFetch<PASCAL>(`/api/PLURAL/${id}`),
  criar: (payload: CriarPASCALPayload) =>
    apiFetch<PASCAL>('/api/PLURAL', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  // ... demais metodos inferidos do Controller
}
```

Gerar apenas os metodos que existem no Controller. Nao inventar metodos.

### Arquivo 2b: frontend/src/features/ARG/hooks/use-ARG.ts

Hooks TanStack Query/Mutation que encapsulam o service. Eliminam o boilerplate
de `useQuery`/`useMutation` nas paginas.

```typescript
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { CAMELService } from '../services/ARG-service'
import type { CriarPASCALPayload } from '../types/ARG'

export function usePASCALs() {
  return useQuery({
    queryKey: ['ARGs'],
    queryFn: () => CAMELService.listar(),
  })
}

export function usePASCAL(id: string | undefined) {
  return useQuery({
    queryKey: ['ARG', id],
    queryFn: () => CAMELService.buscar(id as string),
    enabled: !!id,
  })
}

export function useCriarPASCAL() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: CriarPASCALPayload) => CAMELService.criar(payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['ARGs'] }),
  })
}

// TODO: adicionar useAtualizarPASCAL e useRemoverPASCAL conforme metodos do service.
```

### Arquivo 3: frontend/src/features/ARG/index.ts

```typescript
export type { PASCAL, CriarPASCALPayload } from './types/ARG'
export { CAMELService } from './services/ARG-service'
export { usePASCALs, usePASCAL, useCriarPASCAL } from './hooks/use-ARG'
export { PASCALForm, defaultPASCALFormValues, type PASCALFormValues } from './components/PASCALForm'
```

Exportar tambem os enums e interfaces auxiliares (ex: `PASCALStatus`,
`ValorMonetario`) realmente presentes em `types/ARG.ts`, e hooks/mutations
adicionais (useAtualizar*, useRemover*) conforme metodos disponiveis no service.

### Arquivo 4: frontend/src/app/(dashboard)/ARG/page.tsx

Padrao client-side (cadastro). Para listagem server-side paginada (backend
retorna `Page<>`), trocar `useQuery` por `useListPage` + `FilterBar` -- ver
`docs/crud-patterns.md` secao 7.3.

```typescript
'use client'
import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { Pencil, Trash2 } from 'lucide-react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { CAMELService } from '@/features/ARG/services/ARG-service'
import { Card, CardContent } from '@/shared/components/ui/card'
import { Button } from '@/shared/components/ui/button'
import { DataTable, type ColumnDef } from '@/shared/components/DataTable'
import { ActionsPanel } from '@/shared/components/ActionsPanel'
import type { PASCAL } from '@/features/ARG/types/ARG'

// SCREEN_CODE e uma CONST simples (string literal), NUNCA um hook.
// NAO inventar `useScreenCode()` nem importar de `@/shared/hooks/useScreenCode`
// (esse hook NAO existe). O valor e passado direto como prop screenCode={SCREEN_CODE}.
// SCREEN_CODE no formato MOD-ENT-001 -- o prompt da task informa o codigo correto.
const SCREEN_CODE = 'MOD-ENT-001' // TODO: substituir pelo codigo real (screens.registry)

// TODO: colunas reais do dominio. Formatadores em docs/field-type-catalog.md:
// formatBRL p/ Money (render: (v) => formatBRL(v.valor)), formatDate p/ LocalDate,
// <StatusBadge> p/ enum com cor.
const columns: ColumnDef<PASCAL>[] = [
  // { key: 'nome', label: 'Nome', sortable: true },
]

export default function PASCALsPage() {
  const router = useRouter()
  const queryClient = useQueryClient()
  const [selecionada, setSelecionada] = useState<PASCAL | null>(null)
  const [confirmDeleteId, setConfirmDeleteId] = useState<string | null>(null)

  const { data: itens, isLoading, isError } = useQuery({
    queryKey: ['ARGs'],
    queryFn: CAMELService.listar,
  })

  // Se o Controller nao expoe DELETE, remover este bloco e a coluna de Excluir.
  const deleteMutation = useMutation({
    mutationFn: (id: string) => CAMELService.remover(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['ARGs'] })
      setConfirmDeleteId(null)
    },
  })

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between gap-3">
        <h1 className="text-2xl font-semibold tracking-tight">PASCALs</h1>
        <div className="flex items-center gap-2">
          <ActionsPanel
            entityType="ARG"
            entityId={selecionada?.id ?? null}
            screenCode={SCREEN_CODE}
          />
          <Button onClick={() => router.push('/ARGs/novo')}>+ Novo</Button>
        </div>
      </div>

      {isError && <p className="text-sm text-destructive">Erro ao carregar ARGs.</p>}

      {!isError && (
        <Card>
          <CardContent className="p-0">
            <DataTable
              data={itens ?? []}
              columns={columns}
              keyField="id"
              isLoading={isLoading}
              emptyMessage="Nenhum ARG cadastrado."
              onRowClick={setSelecionada}
              rowActions={(row) =>
                confirmDeleteId === row.id ? (
                  <span className="inline-flex gap-2">
                    <Button
                      size="sm"
                      variant="destructive"
                      onClick={() => deleteMutation.mutate(row.id)}
                      disabled={deleteMutation.isPending}
                    >
                      Confirmar
                    </Button>
                    <Button size="sm" variant="outline" onClick={() => setConfirmDeleteId(null)}>
                      Cancelar
                    </Button>
                  </span>
                ) : (
                  <div className="flex items-center gap-1">
                    <Button
                      variant="ghost"
                      size="icon-sm"
                      aria-label="Editar"
                      onClick={() => router.push(`/ARGs/${row.id}/editar`)}
                    >
                      <Pencil className="h-4 w-4" />
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon-sm"
                      aria-label="Excluir"
                      onClick={() => setConfirmDeleteId(row.id)}
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </div>
                )
              }
            />
          </CardContent>
        </Card>
      )}
    </div>
  )
}
```

### Arquivo 4b: frontend/src/features/ARG/components/PASCALForm.tsx

Componente de formulario COMPARTILHADO entre criar e editar (secao 7.4 de
crud-patterns). Encapsula `useForm` + `zodResolver` + `useDraftForm` + layout
`FormGrid`. As paginas `novo` e `[id]/editar` sao thin wrappers que so injetam
defaultValues e a mutation.

```typescript
'use client'
import { useForm, type Resolver } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useDraftForm } from '@/shared/hooks/useDraftForm'
import {
  Form, FormControl, FormField, FormItem, FormLabel, FormMessage,
} from '@/shared/components/ui/form'
import { FormGrid } from '@/shared/components/FormGrid'
import { FormCol } from '@/shared/components/FormCol'
import { Input } from '@/shared/components/ui/input'
import { Button } from '@/shared/components/ui/button'

// Schema Zod inferido de CriarPASCALRequest.java (espelhamento Java <-> Zod, B6)
export const PASCALFormSchema = z.object({
  // Campos com regras inferidas das anotacoes Java
  // Exemplo: nome: z.string().min(1, 'Obrigatorio'),
})

export type PASCALFormValues = z.infer<typeof PASCALFormSchema>

/** Valores iniciais padrao do form (string -> '', number -> 0, boolean -> false). */
export function defaultPASCALFormValues(): PASCALFormValues {
  return {
    // valores padrao para cada campo do schema
  } as PASCALFormValues
}

interface PASCALFormProps {
  defaultValues: PASCALFormValues
  onSubmit: (values: PASCALFormValues) => void
  isSubmitting: boolean
  apiError: string | null
  onClearApiError: () => void
  submitLabel: string
  onCancel: () => void
}

export function PASCALForm({
  defaultValues, onSubmit, isSubmitting, apiError,
  onClearApiError, submitLabel, onCancel,
}: PASCALFormProps) {
  const form = useForm<PASCALFormValues>({
    // Defesa PRIMARIA: campos Money no schema usam `z.number().positive()`,
    // NUNCA `z.coerce.number()`. MoneyInput ja entrega number; coerce tem input
    // `unknown` que faz o zodResolver inferir Resolver<{valor: unknown}>,
    // incompativel com useForm<{valor: number}> -> `next build` falha no type check.
    // Com z.number() os tipos batem e o cast abaixo vira no-op inofensivo.
    // O cast `as Resolver<PASCALFormValues>` fica como rede secundaria (caso
    // algum campo escape para coerce). NAO trocar z.number() por z.coerce.number().
    resolver: zodResolver(PASCALFormSchema) as Resolver<PASCALFormValues>,
    defaultValues,
  })
  const { clearDraft } = useDraftForm(form)

  return (
    <Form {...form}>
      <form
        onSubmit={form.handleSubmit((v) => { clearDraft(); onClearApiError(); onSubmit(v) })}
        className="space-y-4"
      >
        <FormGrid>
          {/* TODO: um FormCol + campo por campo do schema. Componente por tipo
              conforme docs/field-type-catalog.md:
              - Money -> <MoneyInput> (FormCol span 7)
              - FK (UUID) -> <LookupField> (queryKey com sufixo, span 6)
              - enum -> Controller + <Select> com SelectValue render fn
              - LocalDate -> <Input type="date"> (span 5)
              - boolean -> <input type="checkbox"> (Switch UI nao existe no projeto)
              - texto curto -> <Input> (span 12) */}
        </FormGrid>

        {apiError && <p className="text-sm text-destructive">{apiError}</p>}

        <div className="flex gap-3 pt-2">
          <Button type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Salvando...' : submitLabel}
          </Button>
          <Button
            type="button"
            variant="outline"
            onClick={() => { clearDraft(); onCancel() }}
          >
            Cancelar
          </Button>
        </div>
      </form>
    </Form>
  )
}
```

Regras:
- O objeto passado a `z.object({ ... })` deve conter os campos reais com as regras
  Zod inferidas de `CriarPASCALRequest.java` (espelhamento Java <-> Zod, B6).
- `defaultPASCALFormValues()` deve retornar valor padrao para cada campo do
  schema (`''` string, `0` number, `false` boolean).
- Os `// TODO` dentro de `<FormGrid>` sao para o executor preencher cada `FormCol`
  + campo conforme `docs/field-type-catalog.md` (componente por tipo) e o layout
  de spans da secao 7.4 de `crud-patterns.md`.

### Arquivo 5: frontend/src/app/(dashboard)/ARG/novo/page.tsx

Thin wrapper do `PASCALForm`. So orquestra a mutation de criar.

```typescript
'use client'
import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { ArrowLeft } from 'lucide-react'
import { CAMELService } from '@/features/ARG/services/ARG-service'
import {
  PASCALForm,
  defaultPASCALFormValues,
  type PASCALFormValues,
} from '@/features/ARG/components/PASCALForm'
import { Card, CardContent } from '@/shared/components/ui/card'
import { Button } from '@/shared/components/ui/button'

export default function NovoPASCALPage() {
  const router = useRouter()
  const queryClient = useQueryClient()
  const [apiError, setApiError] = useState<string | null>(null)

  const mutation = useMutation({
    mutationFn: (values: PASCALFormValues) => CAMELService.criar(values as any),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['ARGs'] })
      router.push('/ARGs')
    },
    onError: () => setApiError('Erro ao criar PASCAL.'),
  })

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" aria-label="Voltar" onClick={() => router.back()}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <h1 className="text-2xl font-semibold tracking-tight">Novo PASCAL</h1>
      </div>

      <div className="max-w-xl">
        <Card>
          <CardContent className="pt-6 space-y-4">
            <PASCALForm
              defaultValues={defaultPASCALFormValues()}
              onSubmit={(v) => mutation.mutate(v)}
              isSubmitting={mutation.isPending}
              apiError={apiError}
              onClearApiError={() => setApiError(null)}
              submitLabel="Salvar"
              onCancel={() => router.push('/ARGs')}
            />
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
```

### Arquivo 6: frontend/src/app/(dashboard)/ARG/[id]/editar/page.tsx

Thin wrapper do `PASCALForm` para edicao. Carrega dados via `usePASCAL(id)`
(hook do Arquivo 2b) e dispara mutation de atualizacao.

**Se o Controller NAO tem `@GetMapping("/{id})` (`buscar` ausente no service):**
reporte no relatorio final que a pagina de edicao foi pulada -- nao ha como
carregar os dados existentes -- e nao gere este arquivo. Continue com os
demais. A rota `/<plural>/<id>/editar` ficara inexistente; ajustar a listagem
no Arquivo 4 para nao expor botao Editar.

**IMPORTANTE -- regra `react-hooks/set-state-in-effect`:** NAO usar
`useState` + `useEffect` + `setState` para espelhar dados de `usePASCAL`.
Isso causa cascading renders (e o lint do projeto rejeita). Em vez disso:
fazer early-return enquanto `data` nao chegou e DERIVAR `initialValues`
diretamente apos o return. O `PASCALForm` so monta quando os dados ja
existem, e o `useForm({ defaultValues })` dentro dele recebe os valores
corretos na primeira renderizacao.

**IMPORTANTE -- usar `useParams()` (client hook), NAO `use(params)`:**
no Next.js 16 a prop `params` virou `Promise` em SERVER components. MAS
esta pagina e CLIENT (`'use client'`), e o hook `useParams()` de
`next/navigation` continua sincrono e funciona normalmente em todas as
versoes do Next.js. Usar `use(params)` aqui exige `vi.mock('react')` no
teste pra desempacotar a Promise sob jsdom -- complexidade desnecessaria.
**Padrao canonico para CLIENT pages: `useParams()`.** O pattern
`use(params)` so e necessario em SERVER components (sem `'use client'`).

```typescript
'use client'
import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useParams, useRouter } from 'next/navigation'
import { ArrowLeft } from 'lucide-react'
import { CAMELService } from '@/features/ARG/services/ARG-service'
import { usePASCAL } from '@/features/ARG/hooks/use-ARG'
import {
  PASCALForm,
  defaultPASCALFormValues,
  type PASCALFormValues,
} from '@/features/ARG/components/PASCALForm'
import { Card, CardContent } from '@/shared/components/ui/card'
import { Button } from '@/shared/components/ui/button'
import { Skeleton } from '@/shared/components/ui/skeleton'

export default function EditarPASCALPage() {
  const router = useRouter()
  const params = useParams()
  const id = params.id as string
  const queryClient = useQueryClient()
  const [apiError, setApiError] = useState<string | null>(null)

  const { data, isLoading, isError } = usePASCAL(id)

  const mutation = useMutation({
    mutationFn: (v: PASCALFormValues) => CAMELService.atualizar(id, v as any),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['ARGs'] })
      await queryClient.invalidateQueries({ queryKey: ['ARG', id] })
      router.push('/ARGs')
    },
    onError: () => setApiError('Erro ao atualizar PASCAL.'),
  })

  if (isLoading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-48 w-full" />
      </div>
    )
  }

  if (isError || !data) {
    return <p className="text-sm text-destructive">Erro ao carregar PASCAL.</p>
  }

  // Derivar initialValues SINCRONAMENTE a partir de `data` (que ja existe aqui --
  // o early-return acima garante isso). NUNCA usar useState + useEffect para
  // espelhar dados de useQuery: viola react-hooks/set-state-in-effect.
  // TODO: mapear data (PASCAL) -> PASCALFormValues conforme schema.
  // Ex: const initialValues: PASCALFormValues = {
  //   nome: data.nome,
  //   valor: data.valor.valor,
  //   moeda: data.valor.moeda,
  //   ...
  // }
  const initialValues: PASCALFormValues = defaultPASCALFormValues()

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" aria-label="Voltar" onClick={() => router.push('/ARGs')}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <h1 className="text-2xl font-semibold tracking-tight">Editar PASCAL</h1>
      </div>

      <div className="max-w-xl">
        <Card>
          <CardContent className="pt-6 space-y-4">
            <PASCALForm
              defaultValues={initialValues}
              onSubmit={(v) => mutation.mutate(v)}
              isSubmitting={mutation.isPending}
              apiError={apiError}
              onClearApiError={() => setApiError(null)}
              submitLabel="Salvar alteracoes"
              onCancel={() => router.push('/ARGs')}
            />
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
```

### Arquivo 7: frontend/src/features/ARG/services/ARG-service.test.ts

Testa o contrato do service (path, metodo, payload). Executor expande conforme
campos especificos do dominio.

```typescript
import { describe, it, expect, vi, afterEach } from 'vitest'

vi.mock('@/services/api-client', () => ({ apiFetch: vi.fn() }))

import { apiFetch } from '@/services/api-client'
import { CAMELService } from './ARG-service'
import type { PASCAL } from '../types/ARG'

const mockEntity = {
  id: '00000000-0000-0000-0000-000000000001',
  // TODO: preencher demais campos conforme PASCAL inferido
} as PASCAL

afterEach(() => {
  vi.restoreAllMocks()
})

describe('CAMELService.listar', () => {
  it('chama apiFetch com path correto', async () => {
    vi.mocked(apiFetch).mockResolvedValue([mockEntity])
    const result = await CAMELService.listar()
    expect(apiFetch).toHaveBeenCalledWith('/api/PLURAL')
    expect(result).toEqual([mockEntity])
  })
})

describe('CAMELService.criar', () => {
  it('chama apiFetch com POST e payload serializado', async () => {
    vi.mocked(apiFetch).mockResolvedValue(mockEntity)
    const payload = { /* TODO: campos do CriarPASCALPayload */ } as any
    await CAMELService.criar(payload)
    expect(apiFetch).toHaveBeenCalledWith('/api/PLURAL', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  })
})

// TODO: testes para buscar/atualizar/remover -- gerar conforme metodos que
// existem no service (derivados do Controller no Passo 1).
```

### Arquivo 7b: frontend/src/features/ARG/components/PASCALForm.test.tsx

Testa o componente compartilhado em isolamento (sem react-query, sem router).

```typescript
import React from 'react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { PASCALForm, defaultPASCALFormValues } from './PASCALForm'

beforeEach(() => {
  vi.clearAllMocks()
})

describe('PASCALForm', () => {
  it('renderiza botoes Salvar e Cancelar', () => {
    render(
      <PASCALForm
        defaultValues={defaultPASCALFormValues()}
        onSubmit={vi.fn()}
        isSubmitting={false}
        apiError={null}
        onClearApiError={vi.fn()}
        submitLabel="Salvar"
        onCancel={vi.fn()}
      />,
    )
    expect(screen.getByRole('button', { name: /Salvar/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Cancelar/i })).toBeInTheDocument()
  })

  it('exibe mensagem de apiError quando fornecida', () => {
    render(
      <PASCALForm
        defaultValues={defaultPASCALFormValues()}
        onSubmit={vi.fn()}
        isSubmitting={false}
        apiError="Erro de teste"
        onClearApiError={vi.fn()}
        submitLabel="Salvar"
        onCancel={vi.fn()}
      />,
    )
    expect(screen.getByText('Erro de teste')).toBeInTheDocument()
  })

  // TODO: testes especificos do schema (submit valido chama onSubmit, validacao
  // de campo obrigatorio exibe erro).
})
```

### Arquivo 8: frontend/src/app/(dashboard)/ARG/page.test.tsx

Testa renderizacao da listagem (titulo, botao Novo, estado vazio). Executor
expande com cenarios especificos do dominio.

```typescript
import React from 'react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import PASCALsPage from './page'

vi.mock('@/features/ARG/services/ARG-service', () => ({
  CAMELService: {
    listar: vi.fn(),
    remover: vi.fn(),
  },
}))
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn() }),
}))

import { CAMELService } from '@/features/ARG/services/ARG-service'

function renderWithClient(ui: React.ReactElement) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(<QueryClientProvider client={client}>{ui}</QueryClientProvider>)
}

beforeEach(() => {
  vi.clearAllMocks()
})

describe('PASCALsPage', () => {
  it('renderiza titulo e botao Novo', async () => {
    vi.mocked(CAMELService.listar).mockResolvedValue([])
    renderWithClient(<PASCALsPage />)
    expect(screen.getByRole('heading', { name: /PASCAL/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Novo/i })).toBeInTheDocument()
  })

  it('exibe mensagem de vazio quando a lista chega vazia', async () => {
    vi.mocked(CAMELService.listar).mockResolvedValue([])
    renderWithClient(<PASCALsPage />)
    await waitFor(() => {
      expect(screen.getByText(/Nenhum/i)).toBeInTheDocument()
    })
  })

  // TODO: testes especificos do dominio:
  // - linhas exibem os campos esperados (titulo, data formatada, etc.)
  // - clicar em Excluir abre confirmacao inline
  // - confirmar exclusao chama CAMELService.remover
})
```

### Arquivo 9: frontend/src/app/(dashboard)/ARG/novo/page.test.tsx

Testa renderizacao do form de criacao. Executor expande com submit valido e
validacao de campo conforme schema Zod inferido.

```typescript
import React from 'react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import NovoPASCALPage from './page'

vi.mock('@/features/ARG/services/ARG-service', () => ({
  CAMELService: {
    criar: vi.fn(),
  },
}))
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn(), back: vi.fn() }),
  usePathname: () => '/PLURAL/novo',
}))

function renderWithClient(ui: React.ReactElement) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(<QueryClientProvider client={client}>{ui}</QueryClientProvider>)
}

beforeEach(() => {
  vi.clearAllMocks()
})

describe('NovoPASCALPage', () => {
  it('renderiza titulo e botoes Salvar/Cancelar', () => {
    renderWithClient(<NovoPASCALPage />)
    expect(screen.getByRole('heading', { name: /Novo PASCAL/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Salvar/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Cancelar/i })).toBeInTheDocument()
  })

  // TODO: testes especificos do schema:
  // - submit com payload valido chama CAMELService.criar
  // - campo obrigatorio vazio exibe erro do schema
  // - apos sucesso, navega para /PLURAL
})
```

### Arquivo 9b: frontend/src/app/(dashboard)/ARG/[id]/editar/page.test.tsx

So gerar se o Controller tem `@GetMapping("/{id}")` (Arquivo 6 existe).

**Pattern test-friendly:** mock `useParams()` retornando `{ id: TEST_ID }`. Nao
ha necessidade de `vi.mock('react')` (que seria preciso se a pagina usasse
`use(params)` -- por isso o template do `[id]/editar/page.tsx` mantem
`useParams()`, nao `use(params)`).

```typescript
import React from 'react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import EditarPASCALPage from './page'

vi.mock('@/features/ARG/services/ARG-service', () => ({
  CAMELService: {
    buscar: vi.fn(),
    atualizar: vi.fn(),
  },
}))
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn() }),
  useParams: () => ({ id: '00000000-0000-0000-0000-000000000001' }),
}))

import { CAMELService } from '@/features/ARG/services/ARG-service'

function renderWithClient(ui: React.ReactElement) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(<QueryClientProvider client={client}>{ui}</QueryClientProvider>)
}

beforeEach(() => {
  vi.clearAllMocks()
})

describe('EditarPASCALPage', () => {
  it('exibe skeleton enquanto carrega dados', () => {
    vi.mocked(CAMELService.buscar).mockImplementation(() => new Promise(() => {}))
    const { container } = renderWithClient(<EditarPASCALPage />)
    expect(container.querySelector('.animate-pulse, [data-loading]')).toBeTruthy()
  })

  it('renderiza titulo e form apos carregar dados', async () => {
    vi.mocked(CAMELService.buscar).mockResolvedValue({
      id: '00000000-0000-0000-0000-000000000001',
    } as any)
    renderWithClient(<EditarPASCALPage />)
    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /Editar PASCAL/i })).toBeInTheDocument()
    })
  })

  // TODO: testes especificos (submit chama CAMELService.atualizar, navega apos sucesso).
})
```

## Passo 3 -- Verificacao pos-geracao

Verifique que os 13 arquivos existem (8 producao + 5 testes):

```bash
ls frontend/src/features/ARG/types/ARG.ts
ls frontend/src/features/ARG/services/ARG-service.ts
ls frontend/src/features/ARG/hooks/use-ARG.ts
ls frontend/src/features/ARG/components/PASCALForm.tsx
ls frontend/src/features/ARG/components/PASCALForm.test.tsx
ls frontend/src/features/ARG/services/ARG-service.test.ts
ls frontend/src/features/ARG/index.ts
ls "frontend/src/app/(dashboard)/ARG/page.tsx"
ls "frontend/src/app/(dashboard)/ARG/page.test.tsx"
ls "frontend/src/app/(dashboard)/ARG/novo/page.tsx"
ls "frontend/src/app/(dashboard)/ARG/novo/page.test.tsx"
ls "frontend/src/app/(dashboard)/ARG/[id]/editar/page.tsx"
ls "frontend/src/app/(dashboard)/ARG/[id]/editar/page.test.tsx"
```

Se algum arquivo estiver ausente: reporte qual falta e nao emita o relatorio de
sucesso.

Verifique que o TypeScript compila sem erros nos arquivos gerados. **SEMPRE
filtrar a saida pelo dominio** -- o `tsc` roda o projeto inteiro e tem ~10-15
erros pre-existentes em outras features (contas, orcamentos) que NAO sao seus.
Peneirar esses erros alheios manualmente desperdica minutos (observado: ~30min
extras num smoke). Filtre direto:

```bash
cd frontend && npx tsc --noEmit --project tsconfig.json 2>&1 | grep -iE "features/ARG|app/.dashboard./PLURAL|shell/(screens.registry|icon-map)" || echo "OK: sem erros tsc nos arquivos novos"
```

Se o grep retornar linhas: ha erro TS nos SEUS arquivos -- corrija. Se imprimir
"OK" (grep vazio): compila limpo, prosseguir. NUNCA ler/analisar os erros de
outras features -- nao sao seus.

## Passo 4 -- Relatorio final

Produza o seguinte relatorio (substituindo `ARG` pelo valor real):

```
/feature-front ARG concluido.

Arquivos gerados (13 = 8 producao + 5 testes):

Producao (8):
  frontend/src/features/ARG/types/ARG.ts
  frontend/src/features/ARG/services/ARG-service.ts
  frontend/src/features/ARG/hooks/use-ARG.ts          (TanStack wrappers)
  frontend/src/features/ARG/components/PASCALForm.tsx (form compartilhado)
  frontend/src/features/ARG/index.ts
  frontend/src/app/(dashboard)/ARG/page.tsx           (listagem com DataTable + ActionsPanel)
  frontend/src/app/(dashboard)/ARG/novo/page.tsx      (thin wrapper do PASCALForm)
  frontend/src/app/(dashboard)/ARG/[id]/editar/page.tsx (thin wrapper, carrega via usePASCAL)

Testes (5, baseline -- expandir com casos especificos do dominio):
  frontend/src/features/ARG/services/ARG-service.test.ts
  frontend/src/features/ARG/components/PASCALForm.test.tsx
  frontend/src/app/(dashboard)/ARG/page.test.tsx
  frontend/src/app/(dashboard)/ARG/novo/page.test.tsx
  frontend/src/app/(dashboard)/ARG/[id]/editar/page.test.tsx

Tipos inferidos:  <lista de interfaces e enums gerados>
Metodos service: <lista de metodos do service>
Schema Zod:      <lista de campos com regras>

Proximos passos:
  1. Preencher `columns` (ColumnDef) e SCREEN_CODE em page.tsx; formatters/StatusBadge
     conforme docs/field-type-catalog.md
  2. Preencher campos dentro de <FormGrid> no PASCALForm (componente por tipo:
     MoneyInput / LookupField / Select / Input) -- crud-patterns secao 7.4
  3. Preencher defaultPASCALFormValues() com valores padrao para cada campo do schema
  4. Em [id]/editar/page.tsx, preencher o mapeamento `data (PASCAL) -> PASCALFormValues`
     no useEffect (substituir defaultPASCALFormValues() pelo mapping real)
  5. EXPANDIR os 5 testes gerados com casos especificos do dominio (NAO recriar):
     - service.test.ts: testes para buscar/atualizar/remover conforme metodos disponiveis
     - PASCALForm.test.tsx: submit valido chama onSubmit, validacao de campo
     - page.test.tsx: linhas exibindo campos, exclusao com confirmacao
     - novo/page.test.tsx: submit chama criar
     - editar/page.test.tsx: submit chama atualizar
     NAO invocar /write-test para os 5 baseline -- ja estao gerados.
  6. npm run build (verificar sem erros)
```
