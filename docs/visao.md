# Visão — SaaS de Finanças Pessoais (Projeto-Laboratório)

> Documento curto e estável. Define **o que** estamos construindo e **por quê**, sem entrar em como.
> Atualizado quando a visão muda — não a cada decisão técnica.

---

## Propósito do projeto

Este projeto tem **dois propósitos simultâneos**, e isso precisa ficar explícito porque eles podem conflitar em decisões de escopo:

1. **Propósito primário — Validar a fábrica AI-native.** Servir como laboratório controlado para construir, testar e refinar o modelo de desenvolvimento descrito no blueprint. O sucesso do projeto é medido pelo amadurecimento da fábrica, não pelo produto em si.
2. **Propósito secundário — Produzir um SaaS de finanças pessoais minimamente útil.** Um app que o próprio desenvolvedor usaria para controlar finanças, com qualidade técnica que sirva de portfólio.

**Quando os propósitos conflitam, o primário ganha.** Exemplo concreto: se uma feature de produto seria valiosa para usuários reais mas não exercita nada novo da fábrica, ela é cortada. Se uma feature menos atraente para usuários exercita um aspecto importante da fábrica (ex: importação batch que justifica refactor automatizado), ela entra.

---

## Por que finanças pessoais

- **Domínio conhecido pelo desenvolvedor.** Não há curva de aprendizado de regras de negócio competindo com curva de aprendizado da fábrica.
- **Referências fartas e maduras.** Mobills, Organizze, YNAB, Mint resolvem o "o que" — restando focar no "como".
- **Espaço real para IA.** Categorização automática, parsing de extratos, insights, agente de planejamento são extensões naturais que validam a tese central da fábrica.
- **Web + mobile força decisão arquitetural cedo.** Tipo de "dor produtiva" que um laboratório precisa.
- **Dados sensíveis em escala pessoal.** Força disciplina de segurança desde o início, sem o peso de compliance enterprise.

---

## Quem é o usuário

**Usuário único no MVP: o próprio desenvolvedor.** Single-tenant. Sem multi-usuário, sem compartilhamento, sem convites. Isso elimina decisões prematuras sobre auth complexa, RBAC, isolamento de dados — que entrariam apenas se o laboratório validar e o produto for evoluído.

Personas que **não** são alvo do MVP (mas podem ser depois):
- Família compartilhando finanças
- Casais com contas conjuntas
- Pequenos empreendedores misturando PF/PJ
- Usuários que querem múltiplas moedas

---

## Escopo do MVP

### O que entra

**Núcleo transacional:**
- Cadastro/login (email + senha, sem OAuth)
- Contas: corrente, poupança, dinheiro, cartão de crédito (CRUD)
- Categorias hierárquicas em 1 nível (pai/filho), com seed inicial
- Transações: receita, despesa, transferência entre contas (CRUD)
- Saldo por conta + saldo total (calculado, não armazenado)

**Visualização e análise:**
- Relatório mensal: gastos por categoria
- Relatório mensal: evolução do saldo
- Filtros básicos (período, conta, categoria)

**Importação:**
- CSV em formato fixo (1 layout documentado, sem detecção automática)

**Plataforma:**
- Web responsivo + PWA com offline básico (cache de leitura, sync ao reconectar)

### O que fica fora

Explicitamente fora do MVP, ainda que sejam features óbvias de finanças pessoais:

- OFX e Open Finance (qualquer integração bancária real)
- Investimentos (carteiras, posições, renda variável, renda fixa)
- Metas financeiras
- Recorrências (transações que se repetem automaticamente)
- Compartilhamento entre usuários
- Multi-moeda
- Parcelamento de cartão com fatura agregada
- Categorização automática por IA
- Notificações push
- App mobile nativo (RN ou Flutter)
- Dashboard customizável
- Exportação para PDF/Excel
- Conciliação bancária

Cada item desta lista é uma decisão consciente, não um esquecimento. **Reabertura desta lista exige justificativa explícita** — não acrescentar features porque "seria fácil".

---

## Critérios de sucesso

### Para a fábrica (propósito primário)

A fábrica será considerada validada se, ao final do MVP:

1. Existe uma esteira reprodutível de Camadas 1-4 documentada e funcional
2. Pelo menos 60% das features finais foram implementadas em fluxo Tier 2 (gate humano no PR, não no commit)
3. Pelo menos 3 routines/loops Tier 1 estão rodando com valor demonstrável **(atingido na 5.31/5.32)**
4. CI é confiável o suficiente para que green build = código mergeable sem segunda revisão manual
5. Tempo entre "spec de feature pronta" e "PR aberto" caiu pelo menos 50% comparado ao baseline manual — não medido formalmente; estimativa empírica: ~80% de redução para features Tier 2 (spec a PR em <30min vs ~3h manual)
6. Houve pelo menos uma situação onde o desenvolvedor fechou o laptop com agentes trabalhando e voltou para revisar resultado — sem ansiedade

### Para o produto (propósito secundário)

O produto será considerado funcional se:

1. Desenvolvedor usa o app para controlar finanças pessoais reais por pelo menos 30 dias consecutivos
2. Importação de CSV funciona com extrato real do banco do desenvolvedor
3. Relatórios refletem realidade financeira sem ajuste manual
4. App passa em audit básico de segurança (senhas hashadas, HTTPS, sem dados sensíveis em log, JWT com expiração)

---

## Não-objetivos explícitos

- **Não é um SaaS comercial.** Não há cliente pagante esperado. Decisões de produto não devem ser justificadas com "outros usuários vão querer X".
- **Não é um portfólio para impressionar.** Decisões técnicas não devem ser justificadas com "fica bonito no LinkedIn". Stack on-distribution > stack vistosa.
- **Não é um exercício de over-engineering.** Cada padrão arquitetural aplicado precisa justificar seu custo. Arquitetura limpa entra porque permite evolução para DDD tático on-demand. DDD tático completo desde o início **não entra** — fica como porta aberta para quando justificar.
- **Não é um produto pronto para escala.** Decisões de infraestrutura assumem 1 usuário, não 10.000. Refactor para escala é um projeto futuro se a validação da fábrica permitir.

---

## Horizonte temporal

- **MVP:** 6-8 semanas a partir da Camada 1 começar (não da Camada 0).
- **Camada 0 (este momento):** 1-2 dias para consolidar documentos fundadores. Não mais.
- **Reavaliação:** ao final do MVP, decidir se vale evoluir o produto ou se a fábrica está madura o suficiente para migrar para projeto de cliente.

---

## Validações externas — Boris Cherny (criador do Claude Code, maio/2026)

> Registro de alinhamentos e insights colhidos de entrevista pública de Boris Cherny
> (criador do Claude Code) na Sequoia em maio/2026. Não muda a visão — a confirma.

### O que este projeto já faz que Boris valida como certo

- **Loops/routines como padrão central:** Boris opera com "dezenas de loops rodando a
  qualquer momento" — babysit-prs (CI + conflitos), watch-ci, daily-summary. Camada B
  da fábrica valida essa direção.
- **Execução paralela massiva (/batch):** Boris descreve 150 PRs num único dia via
  agentes paralelos. `/batch` implementa exatamente esse padrão.
- **"Process lead" > "technology lead":** Boris afirma que a vantagem real da Anthropic
  não é tecnologia (modelo disponível para todos) mas mudança organizacional — estrutura,
  processo, forma de trabalhar. Este projeto é exatamente esse experimento.
- **Todo SQL escrito por modelos:** migration-writer já faz isso.
- **Agentes que se auto-agendam:** ScheduleWakeup + `/loop` = o que Boris chama de
  "routines server-side que sobrevivem ao fechamento do laptop".

### O que Boris sugere que ainda não fizemos

- **Feedback clustering loop:** Boris tem uma routine que agrega feedback do Twitter
  a cada 30 minutos e clusters para ele. Equivalente: loop que agrega comentários de
  PR, sugestões de review e anotações de progresso em resumo semanal da fábrica.
- **Métricas de fábrica capturadas automaticamente:** Boris monitora PRs/dia como
  métrica. Nossa fábrica não captura sistematicamente: tempo spec→PR, taxa de aprovação
  na primeira revisão, bloqueadores por executor. Candidato a Tier 1 routine.
- **Comunicação entre agentes via Slack/MCP:** Boris descreve Claudes de membros
  diferentes do time comunicando sobre Slack para resolver incertezas entre si. Exige
  MCP Slack — fora do escopo atual, mas direção futura.

### Insights sobre o futuro do modelo (para o blueprint)

- **"A harness fica menos importante conforme o modelo melhora."** Safety mechanisms
  como prompt injection, static verification, human-in-the-loop vão diminuir de
  importância. O que não muda: arquitetura de feedback, loops e paralelismo.
- **"Coding will be democratized like literacy."** O accountant escreverá o software
  contábil melhor que o engenheiro, porque conhece o domínio. Implicação para o
  blueprint: o artefato mais valioso é o modelo operacional, não o código.
- **Stack on-distribution importa (hoje) menos que antes.** Em 2024 escolher TypeScript/React
  era crucial para performance do modelo. Em 2026 com Opus 4.7, o modelo escreve qualquer
  stack. A escolha de Java/Spring/Next.js continua válida por outras razões (robustez, DDD).

---

## Glossário de termos do projeto

Para uso consistente nos documentos seguintes:

- **Fábrica** — o conjunto de práticas, ferramentas, agentes e infraestrutura descrito no blueprint AI-native
- **Laboratório** — este projeto específico, usado para construir e validar a fábrica
- **MVP** — escopo definido neste documento; não confundir com "primeira versão entregável"
- **Tier 1/2/3** — níveis de autonomia descritos no blueprint
- **Camada 0-6** — as camadas da fábrica; Camada 0 = Discovery (fase atual)
