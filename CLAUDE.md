# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build
./mvnw clean install

# Run application
./mvnw spring-boot:run

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=ClienteRepositoryTest
./mvnw test -Dtest=ClienteMapperTest
./mvnw test -Dtest=ClienteServiceTest
./mvnw test -Dtest=GlobalExceptionHandlerTest

# Run a single test method
./mvnw test -Dtest=ClienteRepositoryTest#deveRetornarTrueQuandoEmailJaExiste
```

## Architecture

Spring Boot 4.0.5 + Java 17 REST API for managing customers (`Cliente`), with Spring Security protecting endpoints.

### Cliente domain

**Layer flow:** `ClienteController` → `ClienteService` → `ClienteMapper` → `ClienteRepository` → PostgreSQL

- **Model:** `Cliente` entity mapped to table `clientes` with unique constraints on `email` and `cpf`.
- **DTO (input):** `ClienteDto` — Java record with Bean Validation. Fields: `genero` (`@NotBlank`), `nomeCompleto` (`@NotBlank`, `@Size(3-100)`), `email` (`@NotBlank`, `@Email`), `cpf` (`@NotBlank`, `@Pattern` 11 raw digits e.g. `00000000000`), `observacoes` (optional, `@Size(max=250)`).
- **DTO (update):** `ClienteUpdateDto` — same fields and validations as `ClienteDto`, used exclusively for update operations.
- **DTO (output):** `ClienteResponseDto` — mirrors `Cliente` with no validation. Fields: `id`, `genero`, `nomeCompleto`, `email`, `cpf`, `observacoes`.
- **Repository:** `ClienteRepository` extends `JpaRepository<Cliente, Long>` with custom existence checks by `email` and `cpf`.
- **Mapper:** `ClienteMapper` is a `@Component` with three methods:
  - `toEntity(ClienteDto)` → `Cliente`
  - `toResponseDto(Cliente)` → `ClienteResponseDto`
  - `updateEntityFromDto(ClienteUpdateDto, Cliente)` → updates entity fields in place (void)
- **Service:** `ClienteService` contains the full business logic:
  - `cadastrarNovoCliente(ClienteDto)` — checks duplicate email/cpf, saves and returns `ClienteResponseDto`
  - `listarTodosClientes()` — returns all clients as `List<ClienteResponseDto>`
  - `buscarClientePeloId(Long id)` — returns `ClienteResponseDto` or throws `RegraNegocioException` if not found
  - `atualizarCliente(Long id, ClienteUpdateDto)` — finds, updates via mapper, saves and returns `ClienteResponseDto`
  - `excluirCliente(Long id)` — checks existence, deletes or throws `RegraNegocioException`
- **Controller:** `ClienteController` is a `@RestController` mapped to `/api/clientes` with `@CrossOrigin(origins = "*")`:
  - `POST /api/clientes` — requires `ROLE_ADMIN` — creates a client, returns HTTP 201 + `ClienteResponseDto`
  - `GET /api/clientes` — returns all clients as `List<ClienteResponseDto>`
  - `GET /api/clientes/{id}` — returns a single client or HTTP 400 if not found
  - `PUT /api/clientes/{id}` — updates a client, returns HTTP 200 + `ClienteResponseDto`
  - `DELETE /api/clientes/{id}` — deletes a client, returns HTTP 204
- **Exception:** `RegraNegocioException` extends `RuntimeException` for business rule violations. `GlobalExceptionHandler` (`@RestControllerAdvice`) handles `RegraNegocioException` (HTTP 400 + message) and `MethodArgumentNotValidException` (HTTP 400 + `{ "campo": "mensagem" }` JSON).

### Security domain

**Layer flow (login):** `AuthenticationController` → `AuthenticationManager` → `AuthorizationService` → `UserRepository` → PostgreSQL

- **Model:** `User` entity mapped to table `users`, implements `UserDetails`. Fields: `id`, `login` (unique), `password` (BCrypt hash), `role` (`UserRole` enum).
- **Enum:** `UserRole` — `ADMIN` (receives `ROLE_ADMIN` + `ROLE_USER`) and `USER` (receives `ROLE_USER`). Hierarchy is manual because Spring Security does not assume role hierarchy by default.
- **Repository:** `UserRepository` extends `JpaRepository<User, Long>` with `findByLogin(String login)` returning `UserDetails`.
- **Service:** `AuthorizationService` implements `UserDetailsService` — `loadUserByUsername` loads the user from the database for Spring Security to validate credentials.
- **Configuration:** `SecurityConfigurations` in `infra/security/` declares:
  - `SecurityFilterChain` — CSRF disabled, stateless session, authorization rules
  - `AuthenticationManager` bean — required for injection in the controller
  - `PasswordEncoder` bean — `BCryptPasswordEncoder` shared instance
- **DTOs:** `AuthenticationDto` (login input), `RegisterDto` (register input with `UserRole`)
- **Controller:** `AuthenticationController` mapped to `/auth`:
  - `POST /auth/login` — public — authenticates credentials, returns HTTP 200 + `"Login realizado com sucesso!"`
  - `POST /auth/register` — public — checks for duplicate login, hashes password, saves user, returns HTTP 200 + `"Usuário cadastrado com sucesso!"` or HTTP 400 + `"Login já cadastrado."`

### Authorization rules

| Endpoint | Acesso |
|---|---|
| `POST /auth/login` | público |
| `POST /auth/register` | público |
| `POST /api/clientes` | `ROLE_ADMIN` |
| demais endpoints | qualquer usuário autenticado |

## Database

Requires PostgreSQL running locally (start with `docker-compose up -d`):

- URL: `jdbc:postgresql://localhost:5432/clientes_db_001`
- User/password: `postgres` / `postgres`
- DDL: `hibernate.ddl-auto=validate` — Hibernate only validates schema, Flyway manages all DDL

**Flyway migrations:**

| Arquivo | O que cria |
|---|---|
| `V1__create_table_clientes.sql` | tabela `clientes` |
| `V2__create_table_users.sql` | tabela `users` |

Tests use H2 in-memory via `@DataJpaTest` — no external DB needed for tests.

## Tests

| Classe de teste              | Tipo            | O que cobre                                         |
|------------------------------|-----------------|-----------------------------------------------------|
| `ClienteRepositoryTest`      | `@DataJpaTest`  | `existsByEmail`, `existsByCpf` com H2 in-memory    |
| `ClienteMapperTest`          | Unit (plain)    | `toEntity`, `toResponseDto`, `updateEntityFromDto`  |
| `ClienteServiceTest`         | Unit (Mockito)  | todos os métodos de `ClienteService` com mocks      |
| `GlobalExceptionHandlerTest` | Unit (plain)    | `handleRegraNegocio` e `handleValidacao`            |

## Notes

- `ClienteDto`, `ClienteUpdateDto`, `ClienteResponseDto` estão completos.
- `GlobalExceptionHandler` está completo — trata `RegraNegocioException` e erros de validação de DTOs.
- `ClienteService` e `ClienteMapper` estão completos com testes unitários cobrindo todos os cenários.
- `ClienteController` está completo — expõe os endpoints REST em `/api/clientes`.
- Spring Security está implementado com autenticação via banco de dados. JWT ainda não implementado — o `/login` retorna mensagem simples por enquanto.
- Próximo passo de segurança: implementar JWT (`TokenService`, `SecurityFilter`).
