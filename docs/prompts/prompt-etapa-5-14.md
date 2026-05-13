# Prompt -- Sub-etapa 5.14: CRUD de Contas no frontend

## Contexto

Primeira feature de dominio no frontend. O backend ja tem todos os endpoints
necessarios (`/api/contas`). O service `contasService` ja existe com todos
os metodos. A sidebar ja aponta para `/contas`. Falta apenas criar as paginas.

Camada 4. Tres paginas novas, tipos verificados/atualizados, testes Vitest para
cada pagina.

---

## Estado atual (leia antes de implementar)

### Endpoints disponiveis (backend)

- `POST /api/contas` → 201 + ContaResponse
- `GET /api/contas` → List<ContaResponse> (query param `ativa` opcional)
- `GET /api/contas/{id}` → ContaResponse
- `DELETE /api/contas/{id}` → 204
- `GET /api/contas/{id}/saldo` → SaldoResponse
- `GET /api/contas/saldo-total` → SaldoTotalResponse

### Service existente (`frontend/src/services/contas.service.ts`)

Ja implementado com: `listar`, `criar`, `buscarPorId`, `calcularSaldo`,
`saldoTotal`, `desativar`. Leia o arquivo antes de implementar -- nao recriar.

### Tipos existentes

Leia `frontend/src/types/` para verificar o que ja existe. Se `Conta`,
`SaldoResponse`, ou `SaldoTotalResponse` nao estiverem definidos, adicioná-los
em `frontend/src/types/conta.ts`:

```typescript
export type TipoConta = 'CORRENTE' | 'POUPANCA' | 'DINHEIRO' | 'CARTAO_CREDITO'

export interface Conta {
  id: string
  nome: string
  tipo: TipoConta
  saldoInicialValor: number
  saldoInicialMoeda: string
  ativa: boolean
  criadoEm: string
  atualizadoEm: string
}

export interface SaldoResponse {
  valor: number
  moeda: string
}

export interface SaldoTotalResponse {
  valor: number
  moeda: string
  totalContas: number
}
```

Leia `src/main/java/com/laboratorio/financas/conta/domain/TipoConta.java` para
confirmar os valores do enum antes de declarar os tipos.

---

## Paginas a criar

### Pagina 1: `frontend/src/app/(dashboard)/contas/page.tsx` -- Listagem

**Comportamento:**
- `useQuery({ queryKey: ['contas'], queryFn: contasService.listar })`
- Exibe lista de contas em cards ou tabela com: nome, tipo, saldo inicial formatado em BRL, badge ativa/inativa
- Botao "Nova Conta" no topo direito → `router.push('/contas/novo')`
- Clicar em uma conta → `router.push('/contas/${conta.id}')`
- Estado loading: skeleton (3 linhas ou cards)
- Estado erro: mensagem "Erro ao carregar contas."
- Estado vazio: mensagem "Nenhuma conta cadastrada." + botao "Criar primeira conta"

**Imports obrigatorios:**
```typescript
import { useQuery } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { contasService } from '@/services/contas.service'
```

---

### Pagina 2: `frontend/src/app/(dashboard)/contas/novo/page.tsx` -- Criar conta

**Comportamento:**
- Formulario com React Hook Form + Zod 4
- Campos:
  - `nome`: string, obrigatorio, max 100 chars
  - `tipo`: select com opcoes CORRENTE, POUPANCA, INVESTIMENTO
  - `saldoInicialValor`: number, obrigatorio, >= 0
  - `saldoInicialMoeda`: string, default "BRL", readonly (fixo para MVP)
- Submit → `contasService.criar(data)` → `queryClient.invalidateQueries(['contas'])` → `router.push('/contas')`
- Botao "Cancelar" → `router.push('/contas')`
- Estado de loading no botao submit durante a mutacao
- Exibir erro da API se falhar (ex: "Erro ao criar conta.")

**Schema Zod 4:**
```typescript
import { z } from 'zod'

const schema = z.object({
  nome: z.string().min(1, 'Nome obrigatorio').max(100),
  tipo: z.enum(['CORRENTE', 'POUPANCA', 'DINHEIRO', 'CARTAO_CREDITO']),
  saldoInicialValor: z.number().min(0, 'Valor deve ser >= 0'),
  saldoInicialMoeda: z.string(),
})
```

**Imports obrigatorios:**
```typescript
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { contasService } from '@/services/contas.service'
```

---

### Pagina 3: `frontend/src/app/(dashboard)/contas/[id]/page.tsx` -- Detalhe

**Comportamento:**
- Extrair `id` de `useParams()`
- `useQuery({ queryKey: ['conta', id], queryFn: () => contasService.buscarPorId(id) })`
- `useQuery({ queryKey: ['conta-saldo', id], queryFn: () => contasService.calcularSaldo(id) })`
- Exibe: nome, tipo, saldo atual (da query saldo), saldo inicial, status (ativa/inativa)
- Botao "Desativar conta" visivel apenas se `conta.ativa === true`
  - `useMutation`: `contasService.desativar(id)` → `queryClient.invalidateQueries(['contas'])` → `router.push('/contas')`
  - Pedir confirmacao antes de desativar (alert simples ou botao duplo "Confirmar")
- Botao "Voltar" → `router.push('/contas')`
- Estado loading: skeleton
- Estado erro (conta nao encontrada): mensagem "Conta nao encontrada." + botao voltar

**Imports obrigatorios:**
```typescript
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useParams, useRouter } from 'next/navigation'
import { contasService } from '@/services/contas.service'
```

---

## Testes (invocar /write-test para cada pagina)

Apos criar cada pagina, invocar o agente `test-writer` (modo frontend) para
gerar o teste correspondente. O agente deve ser invocado com o path da pagina.

**Cobertura minima esperada por pagina:**

**contas/page.test.tsx:**
- Renderiza lista de contas quando query retorna dados
- Exibe skeleton durante loading
- Exibe "Nenhuma conta" quando lista vazia
- Clique em "Nova Conta" navega para /contas/novo
- Clique em uma conta navega para /contas/[id]

**contas/novo/page.test.tsx:**
- Renderiza todos os campos do formulario
- Submit com dados validos chama contasService.criar
- Exibe erro de validacao para nome vazio
- Botao Cancelar navega para /contas

**contas/[id]/page.test.tsx:**
- Renderiza detalhes da conta quando query retorna dados
- Exibe saldo atual da conta
- Botao Desativar visivel quando conta ativa
- Botao Desativar ausente quando conta inativa
- Clique em Desativar chama contasService.desativar

Para cada `vi.mock`, mockar:
- `@/services/contas.service` (contasService)
- `next/navigation` (useRouter, useParams)
- `@tanstack/react-query` (nao mockar -- usar QueryClient real com wrapper)

Padrao de wrapper para React Query nos testes:
```typescript
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'

const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  )
}
```

---

## Validacao (check-front.ps1)

Antes do commit final, rodar:
```powershell
.\scripts\check-front.ps1
```

Deve passar lint + todos os testes (15 existentes + novos das 3 paginas) + build.
Se falhar: corrigir antes de commitar.

---

## Fluxo de execucao

```
1. git checkout -b feat/etapa-5-14-contas-frontend

2. Ler antes de implementar:
   - frontend/src/types/              (verificar tipos existentes)
   - frontend/src/services/contas.service.ts
   - frontend/src/app/(dashboard)/layout.tsx
   - frontend/src/app/(dashboard)/page.tsx (referencia de padrao)
   - frontend/src/app/(auth)/login/page.tsx (referencia formulario RHF+Zod)
   - src/main/java/.../conta/domain/TipoConta.java (valores do enum)

3. Verificar/criar tipos em frontend/src/types/conta.ts

4. Criar contas/page.tsx

5. Invocar test-writer para contas/page.tsx -> gerar contas/page.test.tsx

6. Criar contas/novo/page.tsx

7. Invocar test-writer para contas/novo/page.tsx -> gerar contas/novo/page.test.tsx

8. Criar contas/[id]/page.tsx

9. Invocar test-writer para contas/[id]/page.tsx -> gerar contas/[id]/page.test.tsx

10. .\scripts\check-front.ps1 -- deve passar

11. commit: feat(contas): implementa paginas de listagem, criacao e detalhe

12. Atualizar docs/progresso.md (registra 5.14)

13. commit: docs(progresso): registra sub-etapa 5.14
    (inclui docs/progresso.md + docs/prompts/prompt-etapa-5-14.md)

14. /ship -> PR com reviews (pr-reviewer + architect-reviewer + front-reviewer)
```

---

## Estrutura de commits (5.14)

```
feat(contas): implementa paginas de listagem, criacao e detalhe
docs(progresso): registra sub-etapa 5.14
```

---

## Restricoes

- NAO modificar `contasService` -- ja esta completo.
- NAO usar `fetch` diretamente -- usar apenas via `contasService` (regra B1 do front-reviewer).
- NAO usar `asChild` em componentes shadcn -- usar `render` prop (regra B2).
- Saldo inicial moeda fixo em "BRL" para MVP -- sem seletor de moeda.
- Confirmacao de desativacao: alert simples ou estado local de confirmacao -- sem modal complexo.
- `saldoInicialMoeda` no form: campo readonly visivel ou hidden com valor "BRL".
- Se `check-front.ps1` falhar: investigar e corrigir antes de commitar.
- Testes de paginas com React Query: usar QueryClient real com wrapper (nao mockar react-query).

---

## Estado esperado ao terminar

- PR aberto com 2 commits acima de main.
- 3 paginas criadas: `/contas`, `/contas/novo`, `/contas/[id]`
- Testes para cada pagina gerados via test-writer
- `.\scripts\check-front.ps1` verde (lint + testes + build)
- front-reviewer nao reporta bloqueadores B1-B5
- docs/progresso.md com 5.14 registrada

---

## O que NAO fazer ao terminar

- NAO fazer merge sem instrucao do operador.
- NAO testar visualmente (executor nao tem acesso ao browser) -- validacao e via
  check-front.ps1. Operador testa no browser apos merge.
- NAO rodar /ship mais de uma vez.
- NAO implementar edicao de conta -- nao ha endpoint PUT no backend.
