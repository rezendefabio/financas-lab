# Prompt -- Sub-etapa 5.22: Fix tipo Transacao e NaN na listagem

## Contexto

Bug critico: valores exibidos como `R$ NaN` na pagina `/transacoes`.

Causa raiz: `TransacaoResponse.java` retorna `valor` como `BigDecimal` plano
e `moeda` como `String` plana na raiz do JSON. O tipo TypeScript `Transacao`
declara `valor: ValorMonetario` (objeto `{ valor: number, moeda: string }`),
causando `transacao.valor.valor` == `undefined` == NaN no `formatBRL`.

Leia antes de comecar:
- `frontend/src/features/transacoes/types/transacao.ts` (tipo a corrigir)
- `frontend/src/app/(dashboard)/transacoes/page.tsx` (componente a corrigir)
- `src/main/java/.../transacao/interfaces/dto/TransacaoResponse.java` (fonte da verdade)

---

## Mudanca 1 -- Corrigir tipo `Transacao`

Em `frontend/src/features/transacoes/types/transacao.ts`, substituir o conteudo por:

```typescript
export interface Transacao {
  id: string
  tipo: 'RECEITA' | 'DESPESA' | 'TRANSFERENCIA'
  valor: number
  moeda: string
  data: string
  descricao: string
  contaId: string
  contaDestinoId: string | null
  categoriaId: string | null
  criadoEm: string
  atualizadoEm: string
}
```

Remover o import de `ValorMonetario` (nao e mais usado).

---

## Mudanca 2 -- Corrigir componente na listagem

Em `frontend/src/app/(dashboard)/transacoes/page.tsx`, linha que exibe o valor:

```tsx
// ANTES (causa NaN)
{formatBRL(transacao.valor.valor)}

// DEPOIS
{formatBRL(transacao.valor)}
```

---

## Mudanca 3 -- Verificar e corrigir testes

Leia os testes existentes de transacoes. Se algum mock usa `valor: { valor: X, moeda: Y }`,
corrigir para `valor: X, moeda: 'BRL'`. Rodar `.\scripts\check-front.ps1` para confirmar.

---

## Fluxo de execucao

```
1. git checkout -b fix/etapa-5-22-valor-nan-transacao

2. Ler os arquivos listados acima

3. Aplicar Mudancas 1, 2, 3

4. .\scripts\check-front.ps1 -- verde antes de continuar

5. commit: fix(transacoes): corrige tipo Transacao para espelhar TransacaoResponse Java

6. Atualizar docs/progresso.md (registra sub-etapa 5.22)

7. commit: docs(progresso): registra sub-etapa 5.22
   (inclui docs/progresso.md + docs/prompts/prompt-etapa-5-22.md)

8. /ship -> PR; corrigir apontamentos autonomamente
```

## Estrutura de commits

```
fix(transacoes): corrige tipo Transacao para espelhar TransacaoResponse Java
docs(progresso): registra sub-etapa 5.22
```

## Restricoes

- NAO alterar `TransacaoResponse.java` -- o backend esta correto.
- NAO alterar o servico de transacoes -- o `criar` usa campos planos (correto).
- Apenas corrigir o tipo TypeScript e o acesso ao campo no componente.
