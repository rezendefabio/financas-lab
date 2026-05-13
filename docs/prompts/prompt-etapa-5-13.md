# Prompt -- Sub-etapa 5.13: test-writer estendido para frontend

## Contexto

Estende o agente `test-writer` para detectar arquivos `frontend/` e gerar testes
Vitest + Testing Library no lugar de JUnit. A skill `/write-test` nao muda --
ja aceita qualquer path e delega para o agente. A extensao e transparente para
o executor: o mesmo comando `/write-test <path>` funciona para Java e frontend.

Apos esta sub-etapa, a convencao implicita do executor (CLAUDE.md) passa a cobrir
tambem componentes, hooks e services criados em `frontend/`.

Camada 4. Um arquivo editado (test-writer.md), um editado (CLAUDE.md), documentacao.

---

## O que implementar

### Editar `.claude/agents/test-writer.md`

Leia o arquivo completo antes de editar. A logica de deteccao de frontend
deve ser inserida **no inicio** do fluxo do agente, antes da logica Java.

**Nova secao a inserir: "Deteccao de frontend"**

```markdown
## Deteccao de frontend

Se o path do argumento comecar com `frontend/` ou contiver `/app/`, `/components/`,
`/hooks/`, `/services/`, `/lib/` em contexto de arquivo `.ts` ou `.tsx`,
o agente opera em **modo frontend** (Vitest + Testing Library).

Caso contrario, opera no modo padrao (JUnit 5 -- descrito abaixo).
```

**Nova secao: "Modo frontend -- categorias e padroes"**

Inserir apos a secao de deteccao:

```markdown
## Modo frontend

### Identificar categoria pelo path

| Categoria | Path pattern | Arquivo de teste gerado |
|-----------|-------------|-------------------------|
| Componente | `src/app/**/*.tsx`, `src/components/**/*.tsx` | mesmo diretorio, `<Nome>.test.tsx` |
| Hook | `src/hooks/**/*.ts` | mesmo diretorio, `<nome>.test.ts` |
| Service/utility | `src/services/**/*.ts`, `src/lib/**/*.ts` | mesmo diretorio, `<nome>.test.ts` |

Se o arquivo de teste ja existir: reportar "arquivo ja existe: <path>" e parar.
Nunca sobrescrever teste existente.

### Padroes por categoria

**Componente:**
- `render()` + `screen` para queries semanticas (por role, label, text)
- `userEvent` para interacoes (type, click, select)
- `waitFor()` para operacoes assincronas
- `vi.mock()` para services, hooks de contexto e `next/navigation`
- Mocks declarados antes dos imports do modulo alvo (Vitest hoist)
- `beforeEach(() => { vi.clearAllMocks() })`

Estrutura minima de imports:
```typescript
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'
```

**Hook:**
- `renderHook()` de `@testing-library/react` quando o hook depende de contexto React
- `act()` para disparar efeitos
- Para hooks que dependem de services: `vi.mock()` os services
- Para hooks de estado simples: instanciar direto com `renderHook`

Estrutura minima de imports:
```typescript
import { renderHook, act } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
```

**Service/utility:**
- `vi.mock('./api-client', () => ({ apiFetch: vi.fn() }))` para servicos que usam apiFetch
- `vi.spyOn(module, 'funcao')` para espionar funcoes de modulos (ex: authModule.setToken)
- `vi.stubGlobal('fetch', vi.fn())` para mockar fetch global (nao usar em services -- usar apiFetch)
- `afterEach(() => { vi.restoreAllMocks() })` para limpeza
- `vi.unstubAllGlobals()` apos cada teste que usa stubGlobal

Estrutura minima de imports:
```typescript
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
```

### Leitura obrigatoria antes de gerar

Antes de gerar o teste, leia:
1. O arquivo alvo completo (entender assinatura, dependencias, efeitos)
2. O arquivo de setup `frontend/src/test/setup.ts` (matchers disponíveis)
3. Um teste existente de mesma categoria como referencia de estilo
   (ex: para componente, ler `src/app/(auth)/login/page.test.tsx`)

### Validacao

Apos escrever o arquivo de teste, rodar:
```powershell
Push-Location frontend
npm run test:run
$exit = $LASTEXITCODE
Pop-Location
```

Se exit != 0: reportar o output de erro literalmente. Nao corrigir automaticamente.
O operador decide se o erro e no teste gerado ou no codigo alvo.

### Exemplos de cenarios cobertos

Para um **componente** novo `src/components/ContaCard.tsx`:
- renderiza com props basicas (smoke test)
- chama callback ao clicar em acao
- exibe estado de loading quando prop isLoading=true

Para um **hook** novo `src/hooks/useContas.ts`:
- retorna lista vazia inicialmente
- carrega contas apos montagem
- expoe funcao de refetch

Para um **service** novo `src/services/contas.service.ts`:
- listar() chama apiFetch com path correto
- criar() chama apiFetch com metodo POST e body correto
- erro de API propaga como ApiError
```

**Atualizar secao de validacao existente (Java):**

Certificar que a secao de validacao Java continua intacta e que o novo modo
frontend nao interfere. A validacao Java usa `./mvnw test`; o frontend usa
`cd frontend && npm run test:run`.

---

### Editar `CLAUDE.md` -- secao `## Frontend`

Leia o arquivo antes de editar. Adicionar ao final da secao `## Frontend`:

```markdown
- Testes: ao criar componente, hook ou service em `frontend/src/`, invocar
  `/write-test <path>` para gerar teste Vitest + Testing Library colocado.
```

Respeitar linha em branco antes e depois do item (hook markdown-blank-lines ativo).

---

### Atualizar `docs/hooks-pendentes.md`

Na secao de agentes/skills, adicionar nota de extensao do test-writer:
- **test-writer (extensao frontend)** -- Sub-etapa 5.13
- Comportamento: detecta path `frontend/` e gera Vitest + Testing Library
  no lugar de JUnit. Categorias: componente, hook, service/utility.
- Validacao: `npm run test:run` em `frontend/`.

Leia o arquivo antes de editar.

---

## Validacao destrutiva (smoke test)

**Cenario 1 -- service existente (arquivo de teste JA existe, deve recusar):**

Invocar o test-writer (via leitura do agente) com:
```
frontend/src/services/auth.service.ts
```
Esperado: agente reporta "arquivo ja existe: src/services/auth.service.test.ts" e para.

**Cenario 2 -- componente hipotetico (criar temp, gerar, validar, deletar):**

```powershell
# Criar componente minimo para o agente ter algo real para ler
$conteudo = @"
interface Props { nome: string }
export function NomeDisplay({ nome }: Props) {
  return <span data-testid="nome">{nome}</span>
}
"@
Set-Content -Path "frontend/src/components/NomeDisplay.tsx" -Value $conteudo -Encoding UTF8
```

Invocar test-writer com `frontend/src/components/NomeDisplay.tsx`.
Verificar que o agente gera `frontend/src/components/NomeDisplay.test.tsx`.
Rodar `npm run test:run` em `frontend/` -- deve passar.

Limpar apos:
```powershell
Remove-Item frontend/src/components/NomeDisplay.tsx
Remove-Item frontend/src/components/NomeDisplay.test.tsx
```

Documentar no body do PR: output gerado no Cenario 2 (conteudo do teste) e
resultado do `npm run test:run`.

---

## Fluxo de execucao

```
1. git checkout -b feat/etapa-5-13-test-writer-frontend

2. Ler antes de implementar:
   - .claude/agents/test-writer.md              (agente atual a estender)
   - frontend/src/app/(auth)/login/page.test.tsx (referencia de componente)
   - frontend/src/services/auth.service.test.ts  (referencia de service)
   - frontend/src/test/setup.ts                  (setup vitest)
   - CLAUDE.md secao ## Frontend                 (onde adicionar convencao)
   - docs/hooks-pendentes.md                    (formato de documentacao)

3. Editar .claude/agents/test-writer.md (adicionar secoes de frontend)

4. Editar CLAUDE.md (adicionar convencao de /write-test para frontend)

5. Editar docs/hooks-pendentes.md

6. Executar Cenario 1 (recusa de arquivo existente)

7. Executar Cenario 2 (componente temporario -> gerar -> validar -> limpar)

8. commit: feat(claude): estende test-writer para componentes React e hooks frontend

9. Atualizar docs/progresso.md (registra 5.13)

10. commit: docs(progresso): registra sub-etapa 5.13
    (inclui docs/progresso.md + docs/prompts/prompt-etapa-5-13.md)

11. /ship -> PR com reviews; corrigir apontamentos autonomamente
```

---

## Estrutura de commits (5.13)

```
feat(claude): estende test-writer para componentes React e hooks frontend
docs(progresso): registra sub-etapa 5.13
```

---

## Restricoes

- NAO modificar a logica Java do test-writer -- extensao e aditiva, nao substitutiva.
- NAO criar novo agente ou nova skill -- a extensao e no agente existente.
- NAO sobrescrever testes existentes -- reportar e parar se arquivo ja existe.
- A validacao frontend usa `npm run test:run` (nao `./mvnw test`).
- Push-Location/Pop-Location para mudar de diretorio (padrao 5.11).
- Arquivos de teste gerados ficam colocados (mesmo diretorio do arquivo alvo).
- Se `npm run test:run` falhar no Cenario 2: reportar output completo ao operador
  (nao corrigir automaticamente -- pode ser falha no componente temporario).

---

## Estado esperado ao terminar

- PR aberto com 2 commits acima de main.
- `.claude/agents/test-writer.md` com secoes de deteccao e modo frontend.
- `CLAUDE.md` com convencao de `/write-test` para frontend.
- Cenario 1 validado: recusa correta de arquivo existente.
- Cenario 2 validado: teste gerado + `npm run test:run` passando.
- docs/progresso.md com 5.13 registrada.

---

## O que NAO fazer ao terminar

- NAO fazer merge sem instrucao do operador.
- NAO rodar /ship mais de uma vez.
- NAO deixar arquivos temporarios do Cenario 2 no working tree.
