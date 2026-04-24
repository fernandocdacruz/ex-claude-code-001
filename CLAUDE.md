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

Spring Boot 4.0.5 + Java 17 REST API for managing customers (`Cliente`).

**Layer flow:** `ClienteController` → `ClienteService` → `ClienteMapper` → `ClienteRepository` → PostgreSQL

- **Model:** `Cliente` entity mapped to table `clientes` with unique constraints on `email` and `cpf`.
- **DTO (input):** `ClienteDto` is a Java record with Bean Validation annotations. Fields: `genero` (`@NotBlank`), `nomeCompleto` (`@NotBlank`, `@Size(3-100)`), `email` (`@NotBlank`, `@Email`), `cpf` (`@NotBlank`, `@Pattern` 11 raw digits e.g. `00000000000`), `observacoes` (optional, `@Size(max=250)`).
- **DTO (update):** `ClienteUpdateDto` is a Java record with the same fields and validations as `ClienteDto`. Used as input for update operations.
- **DTO (output):** `ClienteResponseDto` is a Java record with no validation. Fields: `id`, `genero`, `nomeCompleto`, `email`, `cpf`, `observacoes`.
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
  - `POST /api/clientes` — creates a client, returns HTTP 201 + `ClienteResponseDto`
  - `GET /api/clientes` — returns all clients as `List<ClienteResponseDto>`
  - `GET /api/clientes/{id}` — returns a single client or HTTP 400 if not found
  - `PUT /api/clientes/{id}` — updates a client, returns HTTP 200 + `ClienteResponseDto`
  - `DELETE /api/clientes/{id}` — deletes a client, returns HTTP 204
- **Exception:** `RegraNegocioException` extends `RuntimeException` for business rule violations. `GlobalExceptionHandler` (`@RestControllerAdvice`) handles `RegraNegocioException` (HTTP 400 + message) and `MethodArgumentNotValidException` (HTTP 400 + `{ "campo": "mensagem" }` JSON).

## Database

Requires PostgreSQL running locally:

- URL: `jdbc:postgresql://localhost:5432/clientes_db_001`
- User/password: `postgres` / `postgres`
- DDL: `hibernate.ddl-auto=update` (schema is auto-managed by Hibernate)

Tests use H2 in-memory via `@DataJpaTest` — no external DB needed for tests.

## Tests

| Classe de teste           | Tipo         | O que cobre                                          |
|---------------------------|--------------|------------------------------------------------------|
| `ClienteRepositoryTest`   | `@DataJpaTest` | `existsByEmail`, `existsByCpf` com H2 in-memory    |
| `ClienteMapperTest`       | Unit (plain) | `toEntity`, `toResponseDto`, `updateEntityFromDto`  |
| `ClienteServiceTest`      | Unit (Mockito) | todos os métodos de `ClienteService` com mocks     |
| `GlobalExceptionHandlerTest` | Unit (plain) | `handleRegraNegocio` e `handleValidacao`         |

## Notes

- `application.yml` has a typo on line 1 (`confispring:` instead of `spring:`). This causes the app to fail on startup — it must be fixed before running.
- `ClienteDto` está completo — todos os campos de `Cliente` estão mapeados com validações. CPF aceita 11 dígitos sem pontuação.
- `ClienteUpdateDto` está completo — mesmos campos e validações do `ClienteDto`, usado exclusivamente para atualizações.
- `ClienteResponseDto` está completo — espelho de `Cliente` sem validações, usado como resposta da API.
- `GlobalExceptionHandler` está completo — trata `RegraNegocioException` e erros de validação de DTOs.
- `ClienteService` está completo com testes unitários cobrindo todos os cenários (happy path + exceções).
- `ClienteMapper` está completo com testes unitários cobrindo todos os métodos e casos de borda.
- `ClienteController` está completo — expõe os endpoints REST em `/api/clientes` (POST, GET, GET/{id}, PUT/{id}, DELETE/{id}).
