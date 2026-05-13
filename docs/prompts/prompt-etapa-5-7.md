# Prompt -- Sub-etapa 5.7: Bounded context `usuario` + autenticacao JWT

## Contexto

Autenticacao completa: cadastro/login com email+senha, JWT stateless. Padrao novo:
Spring Security + JwtAuthenticationFilter + BCrypt. Alem do bounded context `usuario`,
esta sub-etapa ATUALIZA o SecurityConfig existente e todos os 9 *ControllerTest.java
que passarao a exigir token JWT nos requests.

jjwt 0.12.7 ja esta no pom.xml. Spring Security ja esta no pom.xml. spring-security-test
tambem. SecurityConfig existe em `shared/infrastructure/security/SecurityConfig.java`
(atualmente permite tudo -- sera substituido).

Sub-etapa 5.7 (Camada 4).

---

## Domain: `usuario/domain/`

### `Usuario.java`

**Campos:** id (UUID), email (String), senhaHash (String), ativo (boolean), criadoEm (Instant)

**Construtor de criacao** (id gerado, criadoEm = Instant.now(), ativo = true):
```java
public Usuario(String email, String senhaHash) {
    Objects.requireNonNull(email, "email nao pode ser nulo");
    Objects.requireNonNull(senhaHash, "senhaHash nao pode ser nulo");
    if (email.isBlank()) throw new IllegalArgumentException("email nao pode ser vazio");
    this.id = UUID.randomUUID();
    this.email = email.toLowerCase().trim();
    this.senhaHash = senhaHash;
    this.ativo = true;
    this.criadoEm = Instant.now();
}
```

**Construtor de reconstrucao** (todos os campos explícitos -- mesmo padrao de Transacao).

Getters para todos os campos. Sem setters (imutavel apos criacao).

### `UsuarioRepository.java`

```java
Usuario salvar(Usuario usuario);
Optional<Usuario> buscarPorEmail(String email);
boolean existePorEmail(String email);
```

### `EmailJaExisteException.java`

`RuntimeException` com mensagem `"Email ja cadastrado: " + email`.

### `CredenciaisInvalidasException.java`

`RuntimeException` com mensagem `"Credenciais invalidas"`.
**Nao revelar se o email existe ou nao** -- mesma mensagem para os dois casos.

---

## Infrastructure: `usuario/infrastructure/`

### `usuario/infrastructure/persistence/UsuarioEntity.java`

```
@Entity @Table(name = "usuario")
id UUID @Id
email VARCHAR(255) UNIQUE NOT NULL
senha_hash VARCHAR(255) NOT NULL
ativo BOOLEAN NOT NULL
criado_em TIMESTAMPTZ NOT NULL
```

### `usuario/infrastructure/persistence/UsuarioJpaRepository.java`

```java
extends JpaRepository<UsuarioEntity, UUID>
Optional<UsuarioEntity> findByEmail(String email);
boolean existsByEmail(String email);
```

### `usuario/infrastructure/persistence/UsuarioMapper.java`

MapStruct. Seguir padrao de OrcamentoMapper. `senhaHash` → `senhaHash` (campo direto,
sem @Mapping especial necessario se nomes coincidirem).

### `usuario/infrastructure/persistence/UsuarioRepositoryImpl.java`

Implementa UsuarioRepository. Injeta JpaRepository + Mapper.

### `usuario/infrastructure/security/JwtService.java`

Usa jjwt 0.12.7 (API moderna). Ler propriedades via @Value:

```java
@Component
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-seconds:86400}")
    private long expirationSeconds;

    public String gerarToken(String email) {
        return Jwts.builder()
            .subject(email)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expirationSeconds * 1000L))
            .signWith(getSigningKey(), Jwts.SIG.HS256)
            .compact();
    }

    public String extrairEmail(String token) {
        return getClaims(token).getSubject();
    }

    public boolean tokenValido(String token) {
        try { getClaims(token); return true; }
        catch (JwtException e) { return false; }
    }

    public long getExpirationSeconds() { return expirationSeconds; }

    private Claims getClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }
}
```

### `usuario/infrastructure/security/JwtAuthenticationFilter.java`

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // Injeta JwtService

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = authHeader.substring(7);
        if (jwtService.tokenValido(token) &&
            SecurityContextHolder.getContext().getAuthentication() == null) {
            String email = jwtService.extrairEmail(token);
            UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(email, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        filterChain.doFilter(request, response);
    }
}
```

---

## Application: `usuario/application/`

### `RegistrarUsuarioUseCase.java`

Injeta: UsuarioRepository, PasswordEncoder

```java
public record Comando(String email, String senha) {}

@Transactional
public Usuario executar(Comando comando) {
    if (usuarioRepository.existePorEmail(comando.email())) {
        throw new EmailJaExisteException(comando.email());
    }
    String hash = passwordEncoder.encode(comando.senha());
    Usuario usuario = new Usuario(comando.email(), hash);
    return usuarioRepository.salvar(usuario);
}
```

### `LoginUseCase.java`

Injeta: UsuarioRepository, PasswordEncoder, JwtService

```java
public record Resultado(String token, String tipo, long expiresIn) {}

@Transactional(readOnly = true)
public Resultado executar(String email, String senha) {
    Usuario usuario = usuarioRepository.buscarPorEmail(email)
        .orElseThrow(CredenciaisInvalidasException::new);
    if (!passwordEncoder.matches(senha, usuario.getSenhaHash())) {
        throw new CredenciaisInvalidasException();
    }
    String token = jwtService.gerarToken(email);
    return new Resultado(token, "Bearer", jwtService.getExpirationSeconds());
}
```

---

## SecurityConfig -- SUBSTITUIR o arquivo existente

`shared/infrastructure/security/SecurityConfig.java` -- leia o arquivo atual antes
de substituir. O novo conteudo:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtFilter)
            throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/auth/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) ->
                    res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Nao autorizado"))
            )
            .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

---

## Interface: `usuario/interfaces/`

### `AuthController.java`

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @PostMapping("/registrar")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse registrar(@RequestBody @Valid RegistrarRequest request) {
        // chama RegistrarUsuarioUseCase
        // trata EmailJaExisteException → retornar 409 Conflict
    }

    @PostMapping("/login")
    public TokenResponse login(@RequestBody @Valid LoginRequest request) {
        // chama LoginUseCase
        // trata CredenciaisInvalidasException → retornar 401
    }
}
```

Para tratar as excecoes no controller, verificar se ja existe `GlobalExceptionHandler`
em `shared/infrastructure/web/`. Se sim: adicionar handlers la. Se nao: usar
try-catch no AuthController ou criar `@ControllerAdvice`.

### DTOs

**`RegistrarRequest`:** `@NotBlank @Email String email`, `@NotBlank @Size(min=8) String senha`

**`LoginRequest`:** `@NotBlank @Email String email`, `@NotBlank String senha`

**`TokenResponse`:** `String token`, `String tipo`, `long expiresIn`

**`UsuarioResponse`:** `UUID id`, `String email`, `Instant criadoEm`
-- com metodo `fromDomain(Usuario u)` estatico

---

## Migration SQL (V8)

```sql
CREATE TABLE usuario (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    senha_hash VARCHAR(255) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_usuario_email UNIQUE (email)
);
```

---

## application.properties -- adicionar

```properties
app.jwt.secret=dGhpcy1pcy1hLXZlcnktbG9uZy1zZWNyZXQta2V5LWZvci1obWFjLXNoYTI1Ni1hbHdheXM=
app.jwt.expiration-seconds=86400
```

O secret e Base64 de uma string de 64 bytes -- suficiente para HS256.

---

## AbstractAuthenticatedIntegrationTest (NOVO -- base para E2E com auth)

Criar em `src/test/java/com/laboratorio/financas/shared/`:

```java
public abstract class AbstractAuthenticatedIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    protected String token;

    @BeforeEach
    void autenticar() throws Exception {
        String body = """
                {"email":"executor@test.com","senha":"senha12345678"}
                """;
        // Registrar (pode ja existir -- ignorar erro)
        mockMvc.perform(post("/api/auth/registrar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // Login
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk())
                .andReturn();

        token = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(result.getResponse().getContentAsString())
                .get("token").asText();
    }

    protected MockHttpServletRequestBuilder comAuth(MockHttpServletRequestBuilder request) {
        return request.header("Authorization", "Bearer " + token);
    }
}
```

---

## Atualizacao dos 9 ControllerTest existentes

**IMPORTANTE:** Leia cada arquivo antes de editar para ver como MockMvc e injetado
e como os requests sao construidos.

### Arquivos a atualizar (8 testes de negocio):

1. `conta/interfaces/ContaControllerTest.java`
2. `categoria/interfaces/CategoriaControllerTest.java`
3. `transacao/interfaces/TransacaoControllerTest.java`
4. `orcamento/interfaces/OrcamentoControllerTest.java`
5. `meta/interfaces/MetaControllerTest.java`
6. `lancamentorecorrente/interfaces/LancamentoRecorrenteControllerTest.java`
7. `relatorio/interfaces/RelatorioControllerTest.java`
8. `importacao/interfaces/ImportacaoControllerTest.java`

**Mudancas em cada arquivo:**
1. Trocar `extends AbstractIntegrationTest` por `extends AbstractAuthenticatedIntegrationTest`
2. Remover `@Autowired MockMvc mockMvc` se presente (ja vem da classe mae)
3. Em cada `mockMvc.perform(...)`, envolver o RequestBuilder com `comAuth(...)`:
   ```java
   // Antes:
   mockMvc.perform(post("/api/contas").contentType(...).content(...))
   // Depois:
   mockMvc.perform(comAuth(post("/api/contas").contentType(...).content(...)))
   ```

### Arquivo que NAO muda:

`shared/infrastructure/web/HealthcheckControllerTest.java` -- endpoint publico,
nao precisa de auth. Continua `extends AbstractIntegrationTest`.

---

## Testes novos (por convencao implicita CLAUDE.md)

- `usuario/domain/UsuarioTest.java` -- unit, via /migrate
- `usuario/application/RegistrarUsuarioUseCaseTest.java` -- Mockito
- `usuario/application/LoginUseCaseTest.java` -- Mockito
- `usuario/infrastructure/persistence/UsuarioRepositoryImplTest.java` -- Testcontainers
- `usuario/interfaces/AuthControllerTest.java` -- MockMvc E2E
  **NÃO extends AbstractAuthenticatedIntegrationTest** (testa auth em si, endpoints publicos)
  **Extends AbstractIntegrationTest** diretamente

---

## Fluxo de execucao

```
1. git checkout -b feat/etapa-5-7-usuario-auth

2. /feature usuario  → leia a skill, crie stubs manualmente
3. commit: feat(usuario): cria skeleton via /feature

4. Implementa Usuario.java + UsuarioRepository.java + exceptions
5. Implementa UsuarioEntity + JpaRepository + Mapper + RepositoryImpl
6. /migrate usuario  → leia a skill, execute manualmente (gera V8 + UsuarioTest.java)
7. commit: feat(usuario): implementa domain e Entity; adiciona migration V8 via /migrate

8. Implementa JwtService + JwtAuthenticationFilter + UsuarioDetailsService (se necessario)
9. SUBSTITUI SecurityConfig.java (leia o atual primeiro)
10. Implementa RegistrarUsuarioUseCase + LoginUseCase
11. Adiciona app.jwt.* em application.properties
12. Por convencao: RegistrarUsuarioUseCaseTest + LoginUseCaseTest (Mockito)
               + UsuarioRepositoryImplTest (Testcontainers)
13. commit: feat(usuario): implementa JWT, SecurityConfig e use cases

14. Implementa AuthController + 4 DTOs
15. Cria AbstractAuthenticatedIntegrationTest
16. Atualiza 8 *ControllerTest (extends + comAuth)
17. Cria AuthControllerTest (extends AbstractIntegrationTest, nao AbstractAuthenticated)
18. ./mvnw verify  -- BUILD SUCCESS obrigatorio
    (se testes falharem por auth: diagnosticar e corrigir antes de commitar)
19. Atualiza docs/progresso.md (registra 5.7 e padroes: JWT stateless, BCrypt, filter)
20. commit: feat(usuario): implementa AuthController, atualiza testes E2E; registra sub-etapa 5.7
    (inclui docs/progresso.md + docs/prompts/prompt-etapa-5-7.md)

21. /ship → PR com reviews; corrigir apontamentos autonomamente
```

---

## Estrutura de commits (5.7)

```
feat(usuario): cria skeleton via /feature
feat(usuario): implementa domain e Entity; adiciona migration V8 via /migrate
feat(usuario): implementa JWT, SecurityConfig e use cases
feat(usuario): implementa AuthController, atualiza testes E2E; registra sub-etapa 5.7
```

---

## Arquivos de referencia (ler antes de implementar)

- `shared/infrastructure/security/SecurityConfig.java` -- arquivo atual a substituir
- `transacao/domain/Transacao.java` -- padrao de construtor duplo para Usuario
- `conta/infrastructure/persistence/ContaEntity.java` -- padrao de @Entity
- `orcamento/application/CriarOrcamentoUseCase.java` -- padrao de @Transactional
- `src/test/java/.../shared/AbstractIntegrationTest.java` -- base para AbstractAuthenticated
- `conta/interfaces/ContaControllerTest.java` -- padrao atual de MockMvc (ler antes de atualizar)

---

## Restricoes

- NAO modificar pom.xml (dependencias ja existem)
- NAO usar `@WithMockUser` (bypassa o filtro JWT real)
- NAO criar usuarios hardcoded no SecurityConfig (deve vir do DB)
- O secret JWT em application.properties e para dev -- nao usar em producao
- Se hook bloquear commit: ler, corrigir sem --no-verify
- Se ./mvnw verify falhar nos testes de controller por 401: verificar se
  AbstractAuthenticatedIntegrationTest esta correto e se todos os mockMvc.perform
  estao usando comAuth()

---

## Padrao novo a documentar em progresso.md

**JWT stateless + BCrypt (primeira ocorrencia):** autenticacao via filtro
`OncePerRequestFilter` que valida Bearer token a cada request. SecurityConfig
configurado para estateless (sem sessao). BCrypt via PasswordEncoder bean no
SecurityConfig. AbstractAuthenticatedIntegrationTest como base padrao para todos
os testes E2E de endpoints protegidos.

---

## Estado esperado ao terminar

- PR aberto com 4 commits acima de main.
- ./mvnw verify BUILD SUCCESS -- todos os testes existentes + novos passando.
- POST /api/auth/registrar e POST /api/auth/login funcionando (publicos).
- Todos os outros /api/** retornam 401 sem token valido.
- docs/progresso.md com 5.7 registrada.
- docs/prompts/prompt-etapa-5-7.md commitado no ultimo commit.

---

## O que NAO fazer ao terminar

- NAO fazer merge sem instrucao do operador.
- NAO rodar /ship mais de uma vez.
- NAO deixar testes de controller falhando (todos devem passar com auth).
