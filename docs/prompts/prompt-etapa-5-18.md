# Prompt -- Sub-etapa 5.18: Categorias frontend (listagem e criacao)

## Contexto

Implementar as paginas de Categorias no frontend. O bounded context `categoria`
ja existe no backend e o `categoriasService` ja esta implementado em
`src/features/categorias/services/categorias.service.ts`. Esta sub-etapa
adiciona apenas as paginas Next.js que consomem esse servico.

Camada 4. Apenas frontend. Sem mudancas no backend.

---

## Escopo

Duas paginas:

1. `/categorias` -- listagem de categorias em cards
2. `/categorias/novo` -- formulario de criacao

Nao ha pagina de detalhe nesta sub-etapa (servico nao expoe `buscarPorId`).

---

## Referencia para o padrao de codigo

Antes de comecar, leia estes arquivos como referencia do padrao estabelecido:

- `frontend/src/app/(dashboard)/contas/page.tsx` (padrao de listagem)
- `frontend/src/app/(dashboard)/contas/novo/page.tsx` (padrao de formulario)
- `frontend/src/app/(dashboard)/layout.tsx` (sidebar ja tem item Categorias)
- `frontend/src/features/categorias/services/categorias.service.ts` (servico existente)
- `frontend/src/features/categorias/types/categoria.ts` (tipos existentes)
- `frontend/src/shared/lib/formatters.ts` (adicionar funcao aqui)

---

## Mudanca 1 -- Adicionar `formatTipoCategoria` em `src/shared/lib/formatters.ts`

Leia o arquivo antes de editar. Adicionar ao final:

```typescript
export function formatTipoCategoria(tipo: string): string {
  const labels: Record<string, string> = {
    RECEITA: 'Receita',
    DESPESA: 'Despesa',
  }
  return labels[tipo] ?? tipo
}
```

Atualizar `frontend/src/shared/lib/formatters.test.ts` adicionando os casos:

```typescript
it('formatTipoCategoria retorna label em portugues', () => {
  expect(formatTipoCategoria('RECEITA')).toBe('Receita')
  expect(formatTipoCategoria('DESPESA')).toBe('Despesa')
  expect(formatTipoCategoria('DESCONHECIDO')).toBe('DESCONHECIDO')
})
```

---

## Mudanca 2 -- Pagina de listagem (`src/app/(dashboard)/categorias/page.tsx`)

```tsx
'use client'
import { useQuery } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { categoriasService } from '@/features/categorias/services/categorias.service'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/components/ui/card'
import { Badge } from '@/shared/components/ui/badge'
import { Button } from '@/shared/components/ui/button'
import { Skeleton } from '@/shared/components/ui/skeleton'
import { cn } from '@/shared/lib/utils'
import { formatTipoCategoria } from '@/shared/lib/formatters'
import type { Categoria } from '@/features/categorias/types/categoria'

function CategoriaCard({ categoria, onClick }: { categoria: Categoria; onClick: () => void }) {
  const isReceita = categoria.tipo === 'RECEITA'
  return (
    <Card
      className={cn(
        'cursor-pointer transition-colors hover:bg-muted/50 border-l-4',
        isReceita ? 'border-l-primary' : 'border-l-destructive'
      )}
      onClick={onClick}
    >
      <CardHeader className="pb-2">
        <div className="flex items-center justify-between">
          <CardTitle className="text-base">{categoria.nome}</CardTitle>
          <Badge variant={isReceita ? 'default' : 'destructive'}>
            {formatTipoCategoria(categoria.tipo)}
          </Badge>
        </div>
      </CardHeader>
      {categoria.categoriaPaiId && (
        <CardContent>
          <p className="text-xs text-muted-foreground">Subcategoria</p>
        </CardContent>
      )}
    </Card>
  )
}

export default function CategoriasPage() {
  const router = useRouter()
  const { data, isLoading, isError } = useQuery({
    queryKey: ['categorias'],
    queryFn: categoriasService.listar,
  })

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold tracking-tight">Categorias</h1>
        <Button onClick={() => router.push('/categorias/novo')}>Nova Categoria</Button>
      </div>

      {isLoading && (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {[1, 2, 3].map((i) => (
            <Card key={i}>
              <CardHeader>
                <Skeleton className="h-5 w-32" />
              </CardHeader>
            </Card>
          ))}
        </div>
      )}

      {isError && (
        <p className="text-sm text-destructive">Erro ao carregar categorias.</p>
      )}

      {data && data.length === 0 && (
        <div className="flex flex-col items-center gap-4 py-12 text-center">
          <p className="text-muted-foreground">Nenhuma categoria cadastrada.</p>
          <Button onClick={() => router.push('/categorias/novo')}>Criar primeira categoria</Button>
        </div>
      )}

      {data && data.length > 0 && (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {data.map((categoria) => (
            <CategoriaCard
              key={categoria.id}
              categoria={categoria}
              onClick={() => {}}
            />
          ))}
        </div>
      )}
    </div>
  )
}
```

---

## Mudanca 3 -- Formulario de criacao (`src/app/(dashboard)/categorias/novo/page.tsx`)

Schema Zod espelhando `CriarCategoriaRequest.java`:

| Campo Java | Anotacao | Zod |
|-----------|----------|-----|
| `nome` | `@NotBlank @Size(max=100)` | `z.string().min(1, 'Nome obrigatorio').max(100)` |
| `tipo` | `@NotNull TipoCategoria` | `z.enum(['RECEITA', 'DESPESA'])` |
| `categoriaPaiId` | nullable UUID | `z.string().uuid().optional()` |

```tsx
'use client'
import { useForm, Controller, type Resolver } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { useState } from 'react'
import { ArrowLeft } from 'lucide-react'
import { categoriasService } from '@/features/categorias/services/categorias.service'
import { Card, CardContent } from '@/shared/components/ui/card'
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/shared/components/ui/form'
import { Input } from '@/shared/components/ui/input'
import { Button } from '@/shared/components/ui/button'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/shared/components/ui/select'

const schema = z.object({
  nome: z.string().min(1, 'Nome obrigatorio').max(100),
  tipo: z.enum(['RECEITA', 'DESPESA']),
  categoriaPaiId: z.string().uuid().optional(),
})

type FormValues = z.infer<typeof schema>

const TIPOS = [
  { value: 'RECEITA', label: 'Receita' },
  { value: 'DESPESA', label: 'Despesa' },
] as const

export default function NovaCategoriaPage() {
  const router = useRouter()
  const queryClient = useQueryClient()
  const [apiError, setApiError] = useState<string | null>(null)

  const { data: categorias } = useQuery({
    queryKey: ['categorias'],
    queryFn: categoriasService.listar,
  })

  const form = useForm<FormValues>({
    resolver: zodResolver(schema) as Resolver<FormValues>,
    defaultValues: {
      nome: '',
      tipo: 'DESPESA',
      categoriaPaiId: undefined,
    },
  })

  const mutation = useMutation({
    mutationFn: (values: FormValues) =>
      categoriasService.criar({
        nome: values.nome,
        tipo: values.tipo,
        ...(values.categoriaPaiId ? { categoriaPaiId: values.categoriaPaiId } : {}),
      }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['categorias'] })
      router.push('/categorias')
    },
    onError: () => {
      setApiError('Erro ao criar categoria.')
    },
  })

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" aria-label="Voltar" onClick={() => router.back()}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <h1 className="text-2xl font-semibold tracking-tight">Nova Categoria</h1>
      </div>

      <div className="max-w-xl">
        <Card>
          <CardContent className="pt-6 space-y-4">
            <Form {...form}>
              <form onSubmit={form.handleSubmit((v) => { setApiError(null); mutation.mutate(v) })} className="space-y-4">
                <FormField
                  control={form.control}
                  name="nome"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Nome</FormLabel>
                      <FormControl>
                        <Input className="w-full" placeholder="Ex: Alimentacao" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormItem>
                  <FormLabel>Tipo</FormLabel>
                  <Controller
                    control={form.control}
                    name="tipo"
                    render={({ field }) => (
                      <Select value={field.value} onValueChange={field.onChange}>
                        <SelectTrigger className="w-full">
                          <SelectValue placeholder="Selecione o tipo" />
                        </SelectTrigger>
                        <SelectContent>
                          {TIPOS.map((t) => (
                            <SelectItem key={t.value} value={t.value}>
                              {t.label}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    )}
                  />
                  {form.formState.errors.tipo && (
                    <p className="text-sm text-destructive">
                      {form.formState.errors.tipo.message}
                    </p>
                  )}
                </FormItem>

                {categorias && categorias.length > 0 && (
                  <FormItem>
                    <FormLabel>Categoria pai (opcional)</FormLabel>
                    <Controller
                      control={form.control}
                      name="categoriaPaiId"
                      render={({ field }) => (
                        <Select value={field.value ?? ''} onValueChange={(v) => field.onChange(v || undefined)}>
                          <SelectTrigger className="w-full">
                            <SelectValue placeholder="Nenhuma (categoria raiz)" />
                          </SelectTrigger>
                          <SelectContent>
                            {categorias.map((c) => (
                              <SelectItem key={c.id} value={c.id}>
                                {c.nome}
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      )}
                    />
                  </FormItem>
                )}

                {apiError && (
                  <p className="text-sm text-destructive">{apiError}</p>
                )}

                <div className="flex gap-3 pt-2">
                  <Button type="submit" disabled={mutation.isPending}>
                    {mutation.isPending ? 'Salvando...' : 'Salvar'}
                  </Button>
                  <Button type="button" variant="outline" onClick={() => router.push('/categorias')}>
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

---

## Validacao

```powershell
.\scripts\check-front.ps1
```

Lint + testes + build devem passar.

---

## Fluxo de execucao

```
1. git checkout -b feat/etapa-5-18-categorias-frontend

2. Ler arquivos de referencia listados acima

3. Adicionar formatTipoCategoria em formatters.ts + atualizar formatters.test.ts

4. Criar frontend/src/app/(dashboard)/categorias/page.tsx

5. Criar frontend/src/app/(dashboard)/categorias/novo/page.tsx

6. .\scripts\check-front.ps1 -- verde antes de continuar

7. commit: feat(categorias): implementa paginas de listagem e criacao

8. Atualizar docs/progresso.md (registra sub-etapa 5.18)

9. commit: docs(progresso): registra sub-etapa 5.18
   (inclui docs/progresso.md + docs/prompts/prompt-etapa-5-18.md)

10. /ship -> PR; corrigir apontamentos autonomamente
```

---

## Estrutura de commits (5.18)

```
feat(categorias): implementa paginas de listagem e criacao
docs(progresso): registra sub-etapa 5.18
```

---

## Restricoes

- NAO alterar o servico de categorias (ja implementado).
- NAO adicionar pagina de detalhe (fora do escopo).
- NAO adicionar dependencias novas.
- Schema Zod deve espelhar exatamente `CriarCategoriaRequest.java` (regra B6).
- Usar `render` prop em vez de `asChild` (base-nova nao tem @radix-ui).

---

## Estado esperado ao terminar

- PR aberto com 2 commits acima de main.
- `/categorias` lista cards com badge RECEITA (emerald) / DESPESA (red).
- `/categorias/novo` formulario com Nome, Tipo select, Categoria pai select opcional.
- `.\scripts\check-front.ps1` verde.
