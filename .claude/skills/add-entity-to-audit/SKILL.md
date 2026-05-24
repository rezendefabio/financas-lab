---
name: add-entity-to-audit
description: Instrumenta um controller Spring com audit log (AuditPublisher + AuditEvent) para todos os endpoints de mutacao (@PostMapping, @PutMapping, @PatchMapping, @DeleteMapping). Recebe o path do controller como argumento. Exemplo: /add-entity-to-audit src/main/java/com/laboratorio/financas/anexo/interfaces/AnexoController.java
disable-model-invocation: true
argument-hint: [path-do-controller]
---

Voce deve instrumentar o controller informado em `$ARGUMENTS` com o padrao de
audit log do projeto. Template de referencia: `ContaController` e
`CentroCustoController`.

## Passo 0 -- Validacao do argumento

Se `$ARGUMENTS` estiver vazio: escreva a mensagem abaixo e termine.

```
ERRO: /add-entity-to-audit requer o path do controller.

Uso:
  /add-entity-to-audit src/main/java/com/laboratorio/financas/<dominio>/interfaces/<Nome>Controller.java
```

Se o path nao terminar em `Controller.java`: reportar erro e encerrar.

Se o arquivo nao existir (use Read tool; se falhar): reportar erro e encerrar.

## Passo 1 -- Inspecao do controller

Leia o arquivo completo com a Read tool.

Extraia:

- `package` declarado.
- Nome da classe (ex: `AnexoController`).
- Nome base = classe sem o sufixo `Controller` (ex: `Anexo`).
- ENTITY_TYPE = nome base em **kebab-case** lowercase.
  - `Anexo` -> `"anexo"`.
  - `CentroCusto` -> `"centro-custo"`.
  - `LancamentoRecorrente` -> `"lancamento-recorrente"`.
  - Regra: insere `-` antes de cada letra maiuscula (exceto a primeira) e lowercase tudo.

Detecte ja-instrumentado:

- Existe import `AuditPublisher` **E** campo `auditPublisher`?
- Se sim, vai para Passo 2.

Identifique os endpoints de mutacao:

- `@PostMapping` -> CREATE.
- `@PutMapping` ou `@PatchMapping` -> UPDATE.
- `@DeleteMapping` -> DELETE.

Para cada endpoint UPDATE/DELETE, voce precisa de uma forma de buscar o estado
anterior (before-state). Verifique se existe um UseCase `Buscar<Nome>PorIdUseCase`
no diretorio `<dominio>/application/`. Caso contrario, **injete o repositorio
de dominio diretamente** (ex: `AnexoRepository`) e use `buscarPorId(id)`.

## Passo 2 -- Caso ja instrumentado

Se ja instrumentado, escreva:

```
Controller <nome> ja esta instrumentado com audit log. Nada a fazer.
```

E encerre.

## Passo 3 -- Aplicar instrumentacao

Use Edit/Write para aplicar:

### 3a. Imports

Adicionar (ordem alfabetica, respeitar grupos existentes):

```java
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laboratorio.financas.auditlog.domain.AuditAction;
import com.laboratorio.financas.auditlog.domain.AuditEvent;
import com.laboratorio.financas.auditlog.infrastructure.AuditPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestHeader;
```

Adicione tambem o import do repositorio de dominio se for usa-lo para
recuperar before-state.

### 3b. Constantes apos abertura da classe

```java
private static final Logger LOG = LoggerFactory.getLogger(<Nome>Controller.class);
private static final String ENTITY_TYPE = "<entity-type-kebab>";
```

### 3c. Novos campos + parametros do construtor

Adicione `final AuditPublisher auditPublisher` e `final ObjectMapper objectMapper`.
Se for usar o repositorio para before-state, adicione `final <Nome>Repository repository`.
Atualize o construtor para receber e atribuir.

### 3d. Endpoints de mutacao

Para cada endpoint:

- Adicione `@RequestHeader(value = "X-Screen-Code", required = false) String screenCode` como parametro.

**POST (CREATE):**

```java
auditPublisher.publish(new AuditEvent(
        ENTITY_TYPE, criado.getId(), AuditAction.CREATE,
        userEmail(), screenCode, null, toJson(response)));
```

**PUT/PATCH (UPDATE):**

```java
<Entidade> antes = <busca>(id);  // buscarPorIdUseCase.executar(id) ou repository.buscarPorId(id).orElseThrow(...)
String before = toJson(<Response>.fromDomain(antes));
// ... executa atualizacao ...
auditPublisher.publish(new AuditEvent(
        ENTITY_TYPE, id, AuditAction.UPDATE,
        userEmail(), screenCode, before, toJson(response)));
```

**DELETE:**

```java
<Entidade> antes = <busca>(id);
String before = toJson(<Response>.fromDomain(antes));
// ... executa delete ...
auditPublisher.publish(new AuditEvent(
        ENTITY_TYPE, id, AuditAction.DELETE,
        userEmail(), screenCode, before, null));
```

**Importante para uploads (MultipartFile):**

Em endpoint POST que recebe `MultipartFile`, NUNCA serialize o `MultipartFile`.
Serialize apenas a response DTO (metadados: nome, tamanho, tipoConteudo, id, ...).
A response DTO ja contem so metadados se seguir o padrao do projeto.

### 3e. Helpers privados

Adicione ao final da classe:

```java
private String userEmail() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    return (auth != null) ? auth.getName() : null;
}

private String toJson(Object obj) {
    if (obj == null) {
        return null;
    }
    try {
        return objectMapper.writeValueAsString(obj);
    } catch (JsonProcessingException ex) {
        LOG.warn("Falha ao serializar payload de audit log para {}", ENTITY_TYPE, ex);
        return null;
    }
}
```

## Passo 4 -- SpotBugs

Leia `config/spotbugs/spotbugs-excludes.xml`. Se NAO existir um `<Match>` com
`<Class name="~.*Controller" />` e `<Bug pattern="EI_EXPOSE_REP2" />` (ou
`EI_EXPOSE_REP,EI_EXPOSE_REP2`), adicione:

```xml
<Match>
    <Class name="~.*Controller" />
    <Bug pattern="EI_EXPOSE_REP2" />
</Match>
```

Se ja existe, nao alterar.

## Passo 5 -- Compilar

Execute via Bash:

```
powershell -NoProfile -Command ".\mvnw compile -q"
```

Se a compilacao falhar, reportar o erro e parar (operador decide).

**Esta skill nao executa `mvnw verify`**. Testes E2E devem ser revisados pelo
executor manualmente, pois assinaturas de endpoint mudaram (novo parametro
`screenCode`) -- mesmo sendo `required = false`, testes existentes continuam
funcionando.

## Passo 6 -- Relatorio

Escreva:

```
add-entity-to-audit aplicado em <NomeController>

ENTITY_TYPE: "<kebab>"
Endpoints instrumentados:
  - <metodo> <path> (<acao>)
  ...

Imports adicionados:
  - <lista>

SpotBugs:
  - <"exclude generico .*Controller ja existia" | "adicionado exclude .*Controller">

Compilacao: OK | FALHOU
```
