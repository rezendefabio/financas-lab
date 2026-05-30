---
name: feature-front
description: Cria scaffold de feature frontend (types, service, pages) a partir dos DTOs Java do bounded context. Gera 6 arquivos stub prontos para o executor preencher. Argumento: nome do bounded context em snake_case.
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
| `BigDecimal` monetario | `z.coerce.number().positive()` |
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

## Passo 2 -- Gerar os 6 arquivos

Use a ferramenta Write para cada arquivo. O Write cria os diretorios pai
automaticamente. Substitua `ARG`, `PASCAL`, `CAMEL` e `PLURAL` pelos valores
definidos. Os campos das interfaces e do schema Zod devem ser os campos reais
inferidos no Passo 1, nao comentarios genericos.

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

### Arquivo 3: frontend/src/features/ARG/index.ts

```typescript
export type { PASCAL, CriarPASCALPayload } from './types/ARG'
export { CAMELService } from './services/ARG-service'
```

Exportar tambem os enums e interfaces auxiliares (ex: `PASCALStatus`,
`ValorMonetario`) realmente presentes em `types/ARG.ts`.

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
                      onClick={() => router.push(`/ARGs/${row.id}`)}
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

### Arquivo 5: frontend/src/app/(dashboard)/ARG/novo/page.tsx

Quando criar e editar compartilham os mesmos campos (regra do projeto), extrair
um componente `components/PASCALForm.tsx` e reusa-lo nas duas paginas -- ver
`docs/crud-patterns.md` secao 7.4/7.5. O stub abaixo mantem o form na propria
pagina `novo`; o executor extrai o componente compartilhado ao implementar a
pagina de edicao.

```typescript
'use client'
import { useForm, type Resolver } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { useState } from 'react'
import { ArrowLeft } from 'lucide-react'
import { CAMELService } from '@/features/ARG/services/ARG-service'
import { useDraftForm } from '@/shared/hooks/useDraftForm'
import { Card, CardContent } from '@/shared/components/ui/card'
import {
  Form, FormControl, FormField, FormItem, FormLabel, FormMessage,
} from '@/shared/components/ui/form'
import { FormGrid } from '@/shared/components/FormGrid'
import { FormCol } from '@/shared/components/FormCol'
import { Input } from '@/shared/components/ui/input'
import { Button } from '@/shared/components/ui/button'

// Schema Zod inferido de CriarPASCALRequest.java (espelhamento Java <-> Zod, B6)
const schema = z.object({
  // Campos com regras inferidas das anotacoes Java
  // Exemplo: nome: z.string().min(1, 'Obrigatorio'),
})

type FormValues = z.infer<typeof schema>

export default function NovoPASCALPage() {
  const router = useRouter()
  const queryClient = useQueryClient()
  const [apiError, setApiError] = useState<string | null>(null)

  const form = useForm<FormValues>({
    resolver: zodResolver(schema) as Resolver<FormValues>,
    defaultValues: {
      // valores padrao para cada campo do schema
    },
  })
  // Rascunho persistente entre trocas de aba (CLAUDE.md): clearDraft no sucesso e no Cancelar.
  const { clearDraft } = useDraftForm(form)

  const mutation = useMutation({
    mutationFn: (values: FormValues) => CAMELService.criar(values as any),
    onSuccess: async () => {
      clearDraft()
      await queryClient.invalidateQueries({ queryKey: ['ARGs'] })
      router.push('/ARGs')
    },
    onError: () => {
      setApiError('Erro ao criar ARG.')
    },
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
            <Form {...form}>
              <form
                onSubmit={form.handleSubmit((v) => { setApiError(null); mutation.mutate(v) })}
                className="space-y-4"
              >
                <FormGrid>
                  {/* TODO: um FormCol + campo por campo do schema. Componente por
                      tipo conforme docs/field-type-catalog.md:
                      - Money -> <MoneyInput> (FormCol span 7)
                      - FK (UUID) -> <LookupField> (queryKey com sufixo, span 6)
                      - enum -> Controller + <Select> com SelectValue render fn
                      - LocalDate -> <Input type="date"> (span 5)
                      - texto longo -> <Input> (span 12) */}
                </FormGrid>

                {apiError && <p className="text-sm text-destructive">{apiError}</p>}

                <div className="flex gap-3 pt-2">
                  <Button type="submit" disabled={mutation.isPending}>
                    {mutation.isPending ? 'Salvando...' : 'Salvar'}
                  </Button>
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => { clearDraft(); router.push('/ARGs') }}
                  >
                    Cancelar
                  </Button>
                </div>
              </form>
            </Form>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
```

Regras:
- O objeto passado a `z.object({ ... })` deve conter os campos reais com as regras
  Zod inferidas de `CriarPASCALRequest.java` (espelhamento Java <-> Zod, B6).
- `defaultValues` deve conter um valor padrao para cada campo do schema
  (`''` para string, `0` para number, `false` para boolean).
- Manter `mutation.mutate(values as any)` no stub -- o `as any` deve ser removido
  pelo executor quando o payload correto for implementado.
- Os `// TODO` dentro de `<FormGrid>` sao para o executor preencher cada `FormCol`
  + campo conforme `docs/field-type-catalog.md` (componente por tipo) e o layout
  de spans da secao 7.4 de `crud-patterns.md`.

### Arquivo 6: frontend/src/app/(dashboard)/ARG/[id]/page.tsx

A pagina de detalhe depende do metodo `buscar`. Antes de gerar este arquivo,
verifique se o Controller tem um `@GetMapping("/{id}")` (ou seja, se o service
gerado no Arquivo 2 contem `buscar`):

- **Se `buscar` existe:** gere o arquivo com o template abaixo.
- **Se `buscar` NAO existe:** o domino nao expoe busca por id. Gere uma versao
  reduzida da pagina que nao chama `buscar` -- apenas o cabecalho com botao Voltar,
  um `Card` com um `// TODO` indicando que o domino nao tem endpoint de detalhe,
  e o botao Voltar para `/ARGs`. Nao importe `useQuery`. Reporte no relatorio
  final que a pagina de detalhe foi gerada em modo reduzido (sem `buscar`).

Template (quando `buscar` existe):

```typescript
'use client'
import { useQuery } from '@tanstack/react-query'
import { useRouter, useParams } from 'next/navigation'
import { ArrowLeft } from 'lucide-react'
import { CAMELService } from '@/features/ARG/services/ARG-service'
import { Button } from '@/shared/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/components/ui/card'
import { Skeleton } from '@/shared/components/ui/skeleton'

export default function PASCALDetalhePage() {
  const router = useRouter()
  const params = useParams()
  const id = params.id as string

  const { data: item, isLoading, isError } = useQuery({
    queryKey: ['ARG', id],
    queryFn: () => CAMELService.buscar(id),
  })

  if (isLoading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-48 w-full" />
      </div>
    )
  }

  if (isError || !item) {
    return <p className="text-sm text-destructive">Erro ao carregar ARG.</p>
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" aria-label="Voltar" onClick={() => router.push('/ARGs')}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <h1 className="text-2xl font-semibold tracking-tight">Detalhe do PASCAL</h1>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Dados</CardTitle>
        </CardHeader>
        <CardContent>
          {/* TODO: grid com campos do item */}
          <p className="text-sm text-muted-foreground">ID: {item.id}</p>
        </CardContent>
      </Card>

      <div className="flex gap-3">
        <Button variant="outline" onClick={() => router.push('/ARGs')}>
          Voltar
        </Button>
        {/* TODO: acoes especificas do dominio (remover, cancelar, etc.) */}
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

## Passo 3 -- Verificacao pos-geracao

Verifique que os 9 arquivos existem (6 producao + 3 testes):

```bash
ls frontend/src/features/ARG/types/ARG.ts
ls frontend/src/features/ARG/services/ARG-service.ts
ls frontend/src/features/ARG/services/ARG-service.test.ts
ls frontend/src/features/ARG/index.ts
ls "frontend/src/app/(dashboard)/ARG/page.tsx"
ls "frontend/src/app/(dashboard)/ARG/page.test.tsx"
ls "frontend/src/app/(dashboard)/ARG/novo/page.tsx"
ls "frontend/src/app/(dashboard)/ARG/novo/page.test.tsx"
ls "frontend/src/app/(dashboard)/ARG/[id]/page.tsx"
```

Se algum arquivo estiver ausente: reporte qual falta e nao emita o relatorio de
sucesso.

Verifique que o TypeScript compila sem erros nos arquivos gerados:

```bash
cd frontend && npx tsc --noEmit --project tsconfig.json
```

Se houver erros de TypeScript NOS ARQUIVOS GERADOS: corrija antes de prosseguir.
Erros em outros arquivos pre-existentes: ignorar -- nao e responsabilidade desta skill.

## Passo 4 -- Relatorio final

Produza o seguinte relatorio (substituindo `ARG` pelo valor real):

```
/feature-front ARG concluido.

Arquivos gerados (9 = 6 producao + 3 testes):

Producao:
  frontend/src/features/ARG/types/ARG.ts
  frontend/src/features/ARG/services/ARG-service.ts
  frontend/src/features/ARG/index.ts
  frontend/src/app/(dashboard)/ARG/page.tsx
  frontend/src/app/(dashboard)/ARG/novo/page.tsx
  frontend/src/app/(dashboard)/ARG/[id]/page.tsx

Testes (baseline -- expandir com casos especificos do dominio):
  frontend/src/features/ARG/services/ARG-service.test.ts
  frontend/src/app/(dashboard)/ARG/page.test.tsx
  frontend/src/app/(dashboard)/ARG/novo/page.test.tsx

Tipos inferidos:  <lista de interfaces e enums gerados>
Metodos service: <lista de metodos do service>
Schema Zod:      <lista de campos com regras>

Proximos passos:
  1. Preencher `columns` (ColumnDef) e SCREEN_CODE em page.tsx; formatters/StatusBadge
     conforme docs/field-type-catalog.md
  2. Preencher os campos dentro de <FormGrid> em novo/page.tsx (componente por tipo:
     MoneyInput / LookupField / Select / Input) -- crud-patterns secao 7.4
  3. Se houver edicao: extrair components/PASCALForm.tsx compartilhado e criar
     [id]/editar/page.tsx + page.test.tsx (crud-patterns secao 7.5)
  4. Preencher detalhe em [id]/page.tsx
  5. EXPANDIR os 3 testes gerados com casos especificos do dominio (NAO recriar):
     - service.test.ts: testes para buscar/atualizar/remover conforme metodos disponiveis
     - page.test.tsx: testes de linhas exibindo campos, exclusao com confirmacao
     - novo/page.test.tsx: testes de submit valido + validacao de campo
     NAO invocar /write-test para os 3 baseline -- ja estao gerados.
  6. npm run build (verificar sem erros)
```
