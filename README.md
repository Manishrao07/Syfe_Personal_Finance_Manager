# Personal Finance Manager

A Spring Boot 3 REST API for tracking income/expense transactions, categorizing
them, setting savings goals, and generating monthly/yearly reports. Built for the
Personal Finance Manager assignment.

## Tech Stack

| Component      | Choice                                             |
|-----------------|-----------------------------------------------------|
| Language        | Java 21 (LTS, satisfies the Java 17+ requirement)   |
| Framework       | Spring Boot 3.3.4                                    |
| Security        | spring-boot-starter-security, session-based auth with cookies |
| Persistence     | Spring Data JPA + H2 (file-based)                    |
| Validation      | Bean Validation (jakarta.validation)                 |
| Testing         | JUnit 5, Mockito, MockMvc, JaCoCo                    |
| Build           | Maven (wrapper included, `./mvnw`)                   |
| Deployment      | Docker image on Render                               |

## Architecture

```
Controller  →  Service  →  Repository  →  H2 (file-based)
    │             │
    │             └─ business rules, data-isolation checks
    └─ HTTP binding, validation trigger, status codes only
```

- **Layered**: controllers do no business logic and never touch repositories
  directly; services own all rules including per-user data-isolation checks.
- **DTOs everywhere**: `dto/request` and `dto/response` packages are fully separate
  from `entity/` — no entity is ever serialized directly.
- **Global exception handling**: `exception/GlobalExceptionHandler` (`@RestControllerAdvice`)
  is the single place that turns any thrown exception into the uniform
  `{ "status", "message", "timestamp" }` error body, guaranteeing every predictable
  failure returns a 4xx rather than a 5xx.
- **Session auth**: `SecurityConfig` wires cookie-based `HttpSession` auth (no JWT).
  A custom `AuthenticationEntryPoint`/`AccessDeniedHandler` return JSON (not Spring's
  default HTML/redirect) for 401/403 raised at the security-filter level.
- **Config**: `application.yml` externalizes `local` / `render` / `test` profiles.

## Setup

### Prerequisites
- JDK 21 (the project also compiles under newer JDKs, but Lombok's annotation
  processor was observed to fail under a very new/EA `javac`; JDK 21 LTS is the
  tested, recommended toolchain)
- Maven is not required to be pre-installed — use the bundled wrapper (`./mvnw`)

### Clone and build
```bash
git clone <your-repo-url>
cd personal-finance-manager
./mvnw clean package
```

### Run locally
```bash
./mvnw spring-boot:run
# or, after packaging:
java -jar target/personal-finance-manager.jar
```
The API is available at `http://localhost:8080/api`. The `local` profile is active
by default and persists to a file-based H2 database at `./data/financedb` — data
survives restarts. An H2 console is available at `http://localhost:8080/h2-console`
(JDBC URL `jdbc:h2:file:./data/financedb`, user `sa`, empty password).

### Run tests and check coverage
```bash
./mvnw test
open target/site/jacoco/index.html   # HTML coverage report
```
Current line coverage: **94%** (JaCoCo `check` is bound to the `test` phase and
will **fail the build** if coverage drops below the required 80%).

Test suite: 10 classes / 55 tests — unit tests (Mockito) for every service method
including edge cases (future dates, negative amounts, duplicate categories,
cross-user access, referenced-category deletion, goal date validation), plus
MockMvc/`@SpringBootTest` integration tests covering the full
register → login → create → read → update → delete flow for every resource.

## Assumptions (spec was ambiguous)

These are the explicit calls made where the assignment left more than one
defensible reading:

1. **`date` in a transaction update is ignored, not rejected.** `TransactionUpdateRequest`
   has no `date` field at all, so a `date` sent in a PUT body is silently dropped by
   Jackson rather than causing a 400. Chosen because "either ignore or reject" was
   explicitly offered as a free choice.
2. **Deleting a default category → `400 Bad Request`**, while deleting *another
   user's* custom category → `403 Forbidden`. The two are different failure modes
   (a domain rule vs. a data-isolation violation), so they get different codes even
   though both are phrased as "cannot delete" in the spec.
3. **A category referenced by any transaction, including soft-deleted ones, cannot
   be deleted** (`409 Conflict`). Soft-deleted transactions are still real
   historical records, so the reference is still real.
4. **Default categories are global**, seeded once at startup with no owner, and
   visible to every user. Custom categories are scoped to their creating user;
   uniqueness is enforced per-user, not globally, so two different users can each
   have a custom category named `"Rent"`.
5. **Cross-user data isolation status codes differ by resource, matching each
   resource's own documented status-code list exactly:**
   - **Goals** (`/api/goals/{id}`) documents `403` — hitting another user's goal
     returns `403 Forbidden`.
   - **Transactions** (`/api/transactions/{id}`) documents only `400/401/404` — no
     `403` is listed — so hitting or modifying another user's transaction returns
     `404 Not Found`, which also avoids confirming the id exists at all.
   - **Categories** (`DELETE /api/categories/{name}`) documents `403` — deleting
     another user's custom category by name returns `403 Forbidden`.
6. **Soft delete** for transactions (a `deleted` flag, never a hard delete), so
   history is preserved for auditing even though deleted transactions are excluded
   from the default transaction list, savings-goal progress, and reports.
7. **Goal progress is never persisted** — `currentProgress`, `progressPercentage`,
   and `remainingAmount` are recomputed from live transaction data on every
   `GET`/`POST`/`PUT`, per the spec's explicit requirement.
8. **Report months/years with no activity in a category simply omit that category**
   from the `totalIncome`/`totalExpenses` maps (matching the spec's example
   payloads, which only show categories with non-zero values).
9. **CSRF is disabled.** This is a stateless-client JSON API with no browser form
   submissions; the assignment's automated test script also has no mechanism to
   fetch/replay a CSRF token.
10. Only fields present (non-null) in an update request body are applied — sending
    a field as JSON `null` is treated the same as omitting it, not as "clear this
    field." To clear a text field like `description`, send an empty string `""`.
11. Defensively, a `month` outside `1–12` on the monthly report endpoint returns
    `400 Bad Request` even though the spec's status table for that endpoint only
    lists `200/401` — returning a 5xx for bad input is explicitly disallowed, so a
    4xx was added rather than letting `LocalDate.of()` throw.

## API Documentation

Base path: `/api`. All endpoints except `POST /api/auth/register` and
`POST /api/auth/login` require a valid session cookie (returned via `Set-Cookie`
on login) — send it back with every subsequent request (e.g. `curl -b cookies.txt`).

### Auth
| Method | Path | Status codes | Body |
|---|---|---|---|
| POST | `/api/auth/register` | 201, 400, 409 | `{username, password, fullName, phoneNumber}` |
| POST | `/api/auth/login` | 200, 401 | `{username, password}` → sets session cookie |
| POST | `/api/auth/logout` | 200, 401 | — |

### Transactions
| Method | Path | Status codes | Notes |
|---|---|---|---|
| POST | `/api/transactions` | 201, 400, 401 | `{amount, date, category, description?}` |
| GET | `/api/transactions` | 200, 401 | `?startDate&endDate&categoryId&type` |
| PUT | `/api/transactions/{id}` | 200, 400, 401, 404 | any field but `date`; another user's id → 404 |
| DELETE | `/api/transactions/{id}` | 200, 401, 404 | soft delete |

### Categories
| Method | Path | Status codes | Notes |
|---|---|---|---|
| GET | `/api/categories` | 200, 401 | defaults + your custom categories |
| POST | `/api/categories` | 201, 400, 401, 409 | `{name, type}` |
| DELETE | `/api/categories/{name}` | 200, 400, 401, 403, 404 | 400 = default category, 403 = another user's, 409 (via create-side check, referenced categories return 409 on delete) |

### Savings Goals
| Method | Path | Status codes | Notes |
|---|---|---|---|
| POST | `/api/goals` | 201, 400, 401 | `{goalName, targetAmount, targetDate, startDate?}` |
| GET | `/api/goals` | 200, 401 | |
| GET | `/api/goals/{id}` | 200, 400, 401, 403, 404 | |
| PUT | `/api/goals/{id}` | 200, 400, 401, 403, 404 | `{targetAmount?, targetDate?}` |
| DELETE | `/api/goals/{id}` | 200, 401, 403, 404 | |

### Reports
| Method | Path | Status codes |
|---|---|---|
| GET | `/api/reports/monthly/{year}/{month}` | 200, 401, (400 for out-of-range month) |
| GET | `/api/reports/yearly/{year}` | 200, 401 |

Full request/response JSON shapes match the assignment spec exactly — see the
assignment document for field-by-field examples (register, login, create
transaction, get transactions, create/get/update goal, monthly/yearly report).

## Deployment (Render)

1. Push this repository to GitHub.
2. In the Render dashboard: **New → Web Service** → connect the repo.
3. Environment: **Docker** (Render detects the `Dockerfile` automatically).
4. Set environment variables:
   - `SPRING_PROFILES_ACTIVE=render` (already the Dockerfile default; only needed
     if you override the image's entrypoint)
   - Render sets `PORT` automatically; `application.yml` already binds to `${PORT:8080}`.
5. Deploy. Render's free tier gives an ephemeral filesystem — **the H2 file-based
   database at `/opt/render/project/src/data/financedb` persists across simple
   restarts of the same instance but is wiped on every new deploy/redeploy**
   (no attached persistent disk on the free tier). To make data survive redeploys,
   attach a Render Disk mounted at `/opt/render/project/src/data` and this app
   will use it automatically — no code change needed, only the disk configuration
   in the Render dashboard.
6. Once live, verify with:
   ```bash
   bash financial_manager_tests.sh https://<your-app>.onrender.com/api
   ```

### Before running the grader script
- Confirm `GET https://<your-app>.onrender.com/api/categories` returns 401 (not
  a network error) — proves the app booted and the DB/seed job ran.
- Register one throwaway user and confirm registration → login → an authenticated
  call round-trips correctly (cookies persist) before trusting the full suite.
- Render free-tier instances sleep after inactivity; the first request after a
  cold start can take 30–60s — the assignment's own note about "a couple of
  minutes on first run" accounts for this.
