# Estudo de Spring Security

## Visão Geral

O Spring Security é o módulo responsável por autenticação e autorização na aplicação. Neste projeto ele foi configurado para proteger os endpoints da API usando o modelo **stateless** — sem sessão no servidor, cada requisição precisa provar quem é por conta própria (futuramente via token JWT).

### Fluxo de autenticação (login)

```
POST /auth/login
      │
      ▼
AuthenticationController
      │  cria UsernamePasswordAuthenticationToken
      ▼
AuthenticationManager
      │  delega para
      ▼
AuthorizationService (UserDetailsService)
      │  chama
      ▼
UserRepository.findByLogin()
      │  retorna UserDetails (User)
      ▼
Spring Security compara a senha informada com o hash BCrypt salvo
      │
      ├── inválido → lança BadCredentialsException → HTTP 401
      └── válido
            │
            ▼
        TokenService.generateToken()
            │  assina JWT com HMAC256
            └── HTTP 200 + { "token": "eyJ..." }
```

### Fluxo de requisição autenticada (JWT)

```
GET/PUT/DELETE /api/**
      │  header: Authorization: Bearer eyJ...
      ▼
SecurityFilter (OncePerRequestFilter)
      │  extrai token do header (remove "Bearer ")
      ▼
TokenService.validateToken()
      │  verifica assinatura HMAC256
      │
      ├── inválido/ausente → segue sem autenticar → Spring bloqueia → HTTP 403
      └── válido
            │  retorna login (subject do token)
            ▼
        UserRepository.findByLogin()
            │
            ▼
        SecurityContextHolder.setAuthentication()
            │
            ▼
        FilterChain continua → Controller processa → HTTP 200
```

### Fluxo de cadastro (register)

```
POST /auth/register
      │
      ▼
AuthenticationController
      │  verifica se login já existe
      ├── existe   → HTTP 400 "Login já cadastrado."
      └── não existe
            │  encripta senha com BCrypt
            │  salva User no banco
            └── HTTP 200 "Usuário cadastrado com sucesso!"
```

---

## Componentes implementados

| Classe / Arquivo | Pacote | Responsabilidade |
|---|---|---|
| `User` | `model` | Entidade JPA + contrato `UserDetails` |
| `UserRole` | `model` | Enum com os papéis do sistema |
| `UserRepository` | `repository` | Acesso ao banco para a tabela `users` |
| `AuthorizationService` | `service` | Carrega usuário pelo login para o Spring Security |
| `SecurityConfigurations` | `infra/security` | Regras de acesso, beans de segurança |
| `AuthenticationDto` | `dto` | Input do login |
| `RegisterDto` | `dto` | Input do cadastro |
| `AuthenticationController` | `controller` | Endpoints `/auth/login` e `/auth/register` |
| `TokenService` | `service` | Gera e valida tokens JWT |
| `SecurityFilter` | `infra/security` | Intercepta requisições, valida JWT e autentica o usuário no contexto |
| `LoginResponseDto` | `dto` | Output do login contendo o token JWT |
| `V2__create_table_users.sql` | `db/migration` | Migration Flyway que cria a tabela `users` |

---

## Passo 1 — Dependências e Flyway

No `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

**O que acontece ao adicionar a dependência sem configuração:**
- Todos os endpoints ficam bloqueados automaticamente (retorna HTTP 401)
- O Spring gera um usuário padrão `user` com senha aleatória impressa no console
- Isso é temporário — nos próximos passos vamos substituir por autenticação real com banco de dados

**Sobre o `ddl-auto`:** com o Flyway configurado, o valor foi trocado de `update` para `validate`. O Hibernate não altera mais o banco — ele só verifica se as tabelas batem com as entidades. Quem gerencia o schema agora é o Flyway.

> **Lembrete Flyway:** antes de criar novas tabelas, adicionar o script em `src/main/resources/db/migration/` seguindo o padrão `V{número}__{descrição}.sql`. A tabela `users` foi criada via `V2__create_table_users.sql`:
>
> ```sql
> CREATE TABLE IF NOT EXISTS users (
>     id       BIGSERIAL    PRIMARY KEY,
>     login    VARCHAR(100) NOT NULL UNIQUE,
>     password VARCHAR(255) NOT NULL,
>     role     VARCHAR(50)  NOT NULL
> );
> ```
>
> A coluna `password` armazenará o **hash** da senha, nunca a senha em texto puro. O BCrypt gera hashes de ~60 caracteres, por isso `VARCHAR(255)`.

---

## Passo 2 — Enum UserRole

Criado o enum `UserRole` em `model/`:

```java
public enum UserRole {
    ADMIN("admin"),
    USER("user");

    private final String rule;

    UserRole(String rule) {
        this.rule = rule;
    }

    public String getRule() {
        return rule;
    }
}
```

**Pontos importantes:**
- Usar enum em vez de `String` garante que apenas valores válidos sejam atribuídos — impossível inserir um role inexistente por acidente
- Cada constante recebe uma `String` via construtor, exposta pelo `getRule()` — útil para serialização
- Comparar enums com `==` é correto em Java — enums são singletons por definição

---

## Passo 3 — Entidade User

Criada a classe `User` em `model/`, implementando `UserDetails` do Spring Security:

```java
@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String login;

    private String password;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    public User() {}

    public User(String login, String password, UserRole role) {
        this.login = login;
        this.password = password;
        this.role = role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (this.role == UserRole.ADMIN)
            return List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_USER"));
        else
            return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override public String getUsername() { return login; }
    @Override public String getPassword() { return password; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
```

**Pontos importantes:**
- `implements UserDetails` é obrigatório para o Spring Security reconhecer a classe como um usuário autenticável
- `getAuthorities()` implementa uma **hierarquia manual de roles**: ADMIN recebe `ROLE_ADMIN` + `ROLE_USER`, enquanto USER recebe apenas `ROLE_USER`. O Spring Security não presume hierarquia entre roles por padrão — sem isso, um ADMIN seria bloqueado em endpoints restritos a `ROLE_USER`
- `getUsername()` retorna `login`, pois é o campo usado como identificador único
- Os 4 métodos booleanos retornam `true` — conta sempre ativa. A interface já fornece `default true`, mas sobrescrever torna o comportamento explícito
- `@Enumerated(EnumType.STRING)` persiste o **nome** da constante (`ADMIN`, `USER`) no banco. `EnumType.ORDINAL` (padrão) é perigoso — reordenar as constantes quebraria todos os registros existentes

---

## Passo 4 — UserRepository

Criada a interface `UserRepository` em `repository/`:

```java
public interface UserRepository extends JpaRepository<User, Long> {

    UserDetails findByLogin(String login);
}
```

**Pontos importantes:**
- `findByLogin` é uma **query method** do Spring Data JPA — o SQL é gerado automaticamente pelo nome do método (`SELECT * FROM users WHERE login = ?`)
- O retorno é `UserDetails` (não `User`) porque é exatamente o tipo exigido pelo Spring Security — e `User` implementa `UserDetails`, então é compatível
- Nenhuma anotação `@Query` ou implementação manual é necessária

---

## Passo 5 — AuthorizationService

Criada a classe `AuthorizationService` em `service/`, implementando `UserDetailsService`:

```java
@Service
public class AuthorizationService implements UserDetailsService {

    private final UserRepository userRepository;

    public AuthorizationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        return userRepository.findByLogin(login);
    }
}
```

**Pontos importantes:**
- `UserDetailsService` é a interface central do Spring Security para carregar usuários durante a autenticação — o framework a chama automaticamente ao processar um login
- O único método obrigatório é `loadUserByUsername`, que recebe o identificador do usuário e deve retornar um `UserDetails` ou lançar `UsernameNotFoundException`
- Ao registrar essa classe como `@Service`, o Spring a detecta e a usa automaticamente como provedor de usuários, substituindo o usuário padrão gerado no Passo 1
- **Injeção via construtor** é preferida ao `@Autowired` em campo: o campo pode ser `final`, as dependências ficam explícitas e a classe é facilmente testável com mocks

---

## Passo 6 — SecurityConfigurations

Criada a classe `SecurityConfigurations` no pacote `infra/security/`:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfigurations {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) {
        return httpSecurity
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/clientes").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

**Pontos importantes:**
- O pacote `infra.security` separa configurações de segurança das classes de negócio — convenção comum em projetos Spring profissionais
- `@EnableWebSecurity` habilita o módulo de segurança web do Spring — sem ela, as configurações do `SecurityFilterChain` seriam ignoradas
- **CSRF desabilitado**: APIs REST stateless não usam sessão de browser, portanto não são vulneráveis a CSRF. Manter ativo quebraria requisições via Postman/fetch sem tokens adicionais
- **`STATELESS`**: o servidor não cria nem armazena sessão HTTP. Cada requisição se autentica por conta própria. Fundamental para escalabilidade horizontal
- **Regras de autorização** (a ordem importa — o Spring aplica a primeira regra que bater):
  - `POST /auth/login` e `POST /auth/register` → `permitAll()` (endpoints públicos)
  - `POST /api/clientes` → apenas `ROLE_ADMIN`
  - qualquer outra rota → qualquer usuário autenticado
- **`@Bean AuthenticationManager`**: expõe o `AuthenticationManager` gerenciado pelo Spring para injeção no controller. Sem esse bean declarado, o Spring lança `NoSuchBeanDefinitionException` ao iniciar
- **`@Bean PasswordEncoder`**: declara o `BCryptPasswordEncoder` como bean único compartilhado. Evita instanciar `new BCryptPasswordEncoder()` a cada requisição

---

## Passo 7 — DTOs de autenticação

### AuthenticationDto

```java
public record AuthenticationDto(
        @NotBlank String login,
        @NotBlank String password
) {
}
```

- Representa o corpo de `POST /auth/login`
- `@NotBlank` rejeita campos nulos ou vazios — o `GlobalExceptionHandler` retorna HTTP 400 automaticamente

### RegisterDto

```java
public record RegisterDto(
        @NotBlank String login,
        @NotBlank String password,
        @NotNull UserRole role
) {
}
```

- Representa o corpo de `POST /auth/register`
- `role` usa o tipo `UserRole` — o Jackson rejeita valores desconhecidos na deserialização, impossibilitando roles inválidos
- `@NotNull` em vez de `@NotBlank` porque `UserRole` é enum, não `String`

---

## Passo 8 — AuthenticationController

Criada a classe `AuthenticationController` em `controller/`:

```java
@RestController
@RequestMapping("/auth")
public class AuthenticationController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthenticationController(AuthenticationManager authenticationManager,
                                    UserRepository userRepository,
                                    PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody @Valid AuthenticationDto data) {
        var usernamePassword = new UsernamePasswordAuthenticationToken(data.login(), data.password());
        this.authenticationManager.authenticate(usernamePassword);

        return ResponseEntity.ok("Login realizado com sucesso!");
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody @Valid RegisterDto data) {
        if (this.userRepository.findByLogin(data.login()) != null)
            return ResponseEntity.badRequest().body("Login já cadastrado.");

        String encryptedPassword = passwordEncoder.encode(data.password());
        User newUser = new User(data.login(), encryptedPassword, data.role());
        userRepository.save(newUser);

        return ResponseEntity.ok("Usuário cadastrado com sucesso!");
    }
}
```

**Pontos importantes:**

**`/login`:**
- `UsernamePasswordAuthenticationToken` empacota as credenciais brutas
- `authenticationManager.authenticate(...)` executa o fluxo completo: chama `loadUserByUsername`, carrega o usuário e compara a senha informada com o hash BCrypt salvo no banco
  - Credenciais inválidas → Spring lança `BadCredentialsException` → HTTP 401 automático
  - Credenciais válidas → HTTP 200 com mensagem
- O retorno atual é uma `String` simples. Quando o JWT for implementado, este endpoint passará a retornar o token

**`/register`:**
- Verifica duplicidade via `findByLogin` antes de qualquer operação — se o login já existe, retorna HTTP 400 imediatamente sem tentar salvar
- A senha nunca é salva em texto puro — `passwordEncoder.encode()` gera o hash BCrypt antes de persistir
- `PasswordEncoder` é injetado como bean (não instanciado com `new`) — a instância é única e compartilhada pelo Spring

---

---

## Passo 9 — Dependência JWT

Adicionado ao `pom.xml`:

```xml
<dependency>
    <groupId>com.auth0</groupId>
    <artifactId>java-jwt</artifactId>
    <version>4.4.0</version>
</dependency>
```

**Pontos importantes:**
- A biblioteca `com.auth0:java-jwt` é a mais usada para geração e validação de tokens JWT em projetos Spring Boot — simples, bem mantida e sem dependências pesadas
- A versão deve existir no Maven Central. Versões inexistentes causam falha de build com `Could not resolve dependencies`. A versão estável disponível é `4.4.0`
- Diferente das dependências do `spring-boot-starter-*`, esta **não** é gerenciada pelo BOM do Spring Boot — por isso a `<version>` é obrigatória
- Removida também a dependência `spring-boot-starter-data-jpa-test`, que não existe como artefato oficial do Spring Boot e causaria falha no build

---

## Passo 10 — Propriedade do segredo JWT no `application.yml`

Adicionada a propriedade `api.security.token.secret` ao `application.yml`:

```yaml
api:
  security:
    token:
      secret: ${JWT_SECRET:my-secret-key}
```

**Por que não `api.security.token.secret=...`?**

No formato `.properties`, pontos separam chaves planas:
```properties
api.security.token.secret=my-secret-key
```

No formato `.yml`, cada segmento separado por ponto vira um **nível de indentação** (2 espaços). As duas formas são equivalentes para o Spring — o `@Value("${api.security.token.secret}")` funciona igual nos dois casos.

**Sobre `${JWT_SECRET:my-secret-key}`:**
- O Spring lê primeiro a variável de ambiente `JWT_SECRET`
- Se ela não estiver definida, usa o valor padrão `my-secret-key` (após o `:`)
- Em produção, defina `JWT_SECRET` como variável de ambiente no servidor — nunca suba segredos reais no repositório

---

## Passo 11 — TokenService completo

Classe `TokenService` em `service/` com geração e validação de JWT:

```java
@Service
public class TokenService {

    @Value("${api.security.token.secret}")
    private String secret;

    public String generateToken(User user) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer("auth-api")
                    .withSubject(user.getLogin())
                    .withExpiresAt(generateExpirationDate())
                    .sign(algorithm);
        } catch (JWTCreationException exception) {
            throw new RuntimeException("Erro while generating token", exception);
        }
    }

    public String validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm)
                    .withIssuer("auth-api")
                    .build()
                    .verify(token)
                    .getSubject();
        } catch (JWTVerificationException exception) {
            return "";
        }
    }

    private Instant generateExpirationDate() {
        return LocalDateTime.now().plusHours(2).toInstant(ZoneOffset.of("-03:00"));
    }
}
```

**Pontos importantes:**

**`generateToken`:**
- `.withIssuer("auth-api")` — identifica quem emitiu o token; validado na verificação para rejeitar tokens de outras origens
- `.withSubject(user.getLogin())` — o `subject` é o payload principal do token: o login do usuário. O `SecurityFilter` lerá esse valor para identificar quem fez a requisição
- `.withExpiresAt(...)` — token expira em 2 horas. Sem expiração, um token roubado seria válido para sempre
- `ZoneOffset.of("-03:00")` — horário de Brasília. Em produção, use `ZoneOffset.UTC` e deixe a aplicação independente de fuso

**`validateToken`:**
- Retorna o `subject` (login) se o token for válido, ou `""` se inválido/expirado
- Retornar string vazia em vez de lançar exceção simplifica o `SecurityFilter` — ele só precisa checar `if (login != null && !login.isEmpty())`

**Armadilha comum — `@Value` do pacote errado:**
Existe `@Value` no Lombok (`lombok.Value`) e no Spring (`org.springframework.beans.factory.annotation.Value`). Se a IDE importar a errada, o campo fica `null` em runtime, causando `NullPointerException` ao gerar o token.

---

## Passo 12 — LoginResponseDto

Criado o record `LoginResponseDto` em `dto/`:

```java
public record LoginResponseDto(String token) {}
```

**Pontos importantes:**
- Encapsula o token em um objeto JSON: `{ "token": "eyJ..." }` — nunca retorne o token como string crua no body, pois o tipo do campo no JSON fica ambíguo
- O record gera automaticamente construtor, getters e `toString()` — nenhum boilerplate necessário
- `TokenResponseDto` foi criado em paralelo com a mesma estrutura, mas o controller usa `LoginResponseDto` por convenção de nomenclatura (resposta do login)

---

## Passo 13 — Atualizar AuthenticationController para retornar JWT

O controller foi atualizado para injetar `TokenService` e retornar o token no login:

```java
@PostMapping("/login")
public ResponseEntity<LoginResponseDto> login(@RequestBody @Valid AuthenticationDto data) {
    var usernamePassword = new UsernamePasswordAuthenticationToken(data.login(), data.password());
    var auth = this.authenticationManager.authenticate(usernamePassword);

    var token = tokenService.generateToken((User) auth.getPrincipal());

    return ResponseEntity.ok(new LoginResponseDto(token));
}
```

**Pontos importantes:**
- O tipo de retorno mudou de `ResponseEntity<String>` para `ResponseEntity<LoginResponseDto>` — o Jackson serializa o record como JSON automaticamente
- `auth.getPrincipal()` retorna o `UserDetails` autenticado; o cast para `User` é seguro porque `AuthorizationService.loadUserByUsername` sempre retorna `User`
- **Armadilha:** concatenar `new LoginResponseDto(token) + "mensagem"` chama `.toString()` do record, resultando em `LoginResponseDto[token=eyJ...]mensagem` — o Postman recebe uma string inválida, não JSON

---

## Passo 14 — SecurityFilter

Criado `SecurityFilter` em `infra/security/`, estendendo `OncePerRequestFilter`:

```java
@Component
public class SecurityFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final UserRepository userRepository;

    public SecurityFilter(TokenService tokenService, UserRepository userRepository) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        var token = this.recoverToken(request);
        if (token != null) {
            var login = tokenService.validateToken(token);
            UserDetails user = userRepository.findByLogin(login);

            var authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response);
    }

    private String recoverToken(HttpServletRequest request) {
        var authHeader = request.getHeader("Authorization");
        if (authHeader == null) return null;
        return authHeader.replace("Bearer ", "");  // espaço após "Bearer" é obrigatório
    }
}
```

**Pontos importantes:**
- `OncePerRequestFilter` garante que o filtro execute **uma única vez por requisição**, mesmo em cadeias de filtros complexas
- `recoverToken` extrai o token do header `Authorization: Bearer eyJ...`. O `.replace("Bearer ", "")` **precisa do espaço** — sem ele o token fica com espaço à esquerda (` eyJ...`), a validação JWT falha e o usuário não é autenticado
- Se o token for inválido ou ausente, o filtro simplesmente não seta o `SecurityContext` e chama `filterChain.doFilter` — o Spring Security bloqueia a requisição na camada de autorização (HTTP 403)
- `SecurityContextHolder.getContext().setAuthentication(authentication)` é a instrução que "loga" o usuário para o escopo da requisição atual. Sem ela, o Spring não sabe quem está fazendo a requisição

---

## Passo 15 — Registrar SecurityFilter no SecurityConfigurations

O `SecurityFilter` foi injetado e registrado antes do filtro padrão do Spring:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfigurations {

    private final SecurityFilter securityFilter;

    public SecurityConfigurations(SecurityFilter securityFilter) {
        this.securityFilter = securityFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) {
        return httpSecurity
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/clientes").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
    // ...
}
```

**Pontos importantes:**
- `.addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)` posiciona o `SecurityFilter` **antes** do filtro de autenticação padrão do Spring, garantindo que o token JWT seja processado antes de qualquer verificação de credenciais de sessão
- Sem esse registro o `SecurityFilter` seria ignorado — o Spring não inclui filtros `@Component` na cadeia de segurança automaticamente; é preciso declará-los explicitamente aqui

---

## Próximos passos

- [ ] **Testes da camada de segurança** — `SecurityFilterTest` e `TokenServiceTest` com JUnit + Mockito para cobrir geração/validação de token e extração do header
- [ ] **Tratamento de erros 401/403 customizado** — criar `AuthEntryPoint` implementando `AuthenticationEntryPoint` para retornar JSON em vez da página de erro padrão do Spring
- [ ] **Refresh token** — implementar endpoint `POST /auth/refresh` que recebe um token expirado (ou um refresh token separado) e devolve um novo JWT sem exigir login novamente
- [ ] **Logout / invalidação de token** — com JWT stateless não há logout nativo; implementar uma blocklist em Redis ou banco para invalidar tokens antes do vencimento
