# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

CareTracker is a full-stack care coordination system for managing elderly/disabled care services. It tracks clients, caregivers, performed care tasks, billing, and individual care plans. The primary language in the UI and codebase comments is **Czech**.

## Code Style

- Don't add comments unless specifically asked to or the code is difficult to understand.
- Comments, function names, variables are all in english, even when prompts are in different language, code is always english, except for text that gets printed to the user of the application (i.e. errors that get shown in toasts)
- Always use existing patterns, try to avoid duplicit code.
- Prefer simple solutions.

## Collaboration Style

- The user is learning and wants to write the implementation code themselves. Default to reviewing, explaining, and pointing out bugs/issues rather than jumping straight to editing files.
- When asked to "look at" or check code, give a review (what's wrong, why, what pattern to follow) instead of fixing it directly.
- Only edit files when explicitly asked to make the change, or for small mechanical/infrastructure tasks unrelated to what the user is actively learning (e.g. config, routing wiring, one-off renames) where they haven't indicated they want to do it themselves.
- If unsure whether the user wants a review or a fix, ask, or default to reviewing first.

## Development Commands

### Docker (primary workflow)

```bash
make up              # Start all services (backend, frontend, db, pgadmin)
make down            # Stop all services
make restart-backend # Restart a single service
make build           # Rebuild images without cache
make rebuild s=backend  # Rebuild and restart specific service (use after Dockerfile changes)
make clean           # Wipe volumes + full rebuild (resets database)
make logs service=backend  # Tail logs for a service
make compile         # Recompile backend inside container (DevTools auto-restarts)
```

Services:

- Frontend: http://localhost:3000 (Vite dev server proxied)
- Backend: http://localhost:8080
- PostgreSQL: localhost:5432
- PgAdmin: http://localhost:5050

### Backend hot-reload workflow

The backend uses Spring Boot DevTools. The recommended cycle for backend changes:

1. Edit Java source files
2. Run `make compile` — DevTools detects the recompiled classes and restarts automatically

### Tests

Decisions and conventions: `docs/testing/CONVENTIONS.md`.

```bash
cd backend && ./mvnw test     # unit tests (*Test)
cd backend && ./mvnw verify   # + integration tests (*IT, PostgreSQL via Testcontainers, needs Docker) + JaCoCo report
cd frontend && npm run test:run   # Vitest once (npm test = watch mode)
cd frontend && npm run coverage
make e2e-up && make e2e      # Playwright E2E against the demo build on :3040 (fresh seed DB), make e2e-down afterwards
```

- Backend integration tests extend `support/AbstractIntegrationTest` (shared Spring context, DB emptied before each test, `loggedInAs(...)`, data via `TestData`). Don't add `@MockitoBean` in subclasses, it creates a new context.
- Authorization tests use `support/AccessScenario` (two organizations, one user per `Actor` named by relation to a client) and run each endpoint for every actor: allowed set → 2xx, others → 403. A known security gap gets skipped with `assumeFalse(...)` / `@Disabled` and a comment why.
- Frontend: `renderWithProviders` from `src/test/render.jsx`, API mocked with MSW (`src/test/server.js`), sample users in `src/test/users.js`. Unhandled requests fail the test.
- E2E (`e2e/`): import `test` and `expect` from `./support/test.js` (fails a test on any Content Security Policy violation), not from `@playwright/test`. Tests start signed in via `test.use({ storageState: storageStateOf("<demo account>") })`, data comes from the demo seed, tests that create records clean up after themselves. Don't log out with a shared storage state, log in inside the test instead.
- CI (`.github/workflows/ci.yml`) runs on every PR; demo deploy runs only after CI passes on `main`.

Gotchas:

- The map can't be tested in E2E: the demo seed has no client coordinates and geocoding runs with a fake key. Manually: `update client set latitude=49.7475, longitude=13.3776 where id=24` in the E2E database.
- On the live demo Cloudflare injects an inline JavaScript Detections script that `script-src 'self'` blocks. It doesn't break the app; if it gets in the way, turn it off in the Cloudflare dashboard (Security -> Bots), don't allow it in the CSP.
- The dev backend container writes to `backend/target` as root, so Maven on the host fails afterwards: `sudo chown -R $USER backend/target`, or run tests in a separate `git worktree`.
- HSTS is `max-age=31536000` with `includeSubDomains`: a browser that visited the demo refuses plain HTTP to `demo.caretracker.cz` and its subdomains for a year.

### Verification

- Never claim lint, tests or build passed unless the command ran in this session and succeeded; quote its final summary line.
- Before opening a PR: `npm run lint`, `npm run coverage` and `npm run build` in `frontend/`, `./mvnw verify` in `backend/` (E2E too when the demo build, nginx or a user flow changed).
- Tests must not depend on the real current date or time: pin the clock (`vi.useFakeTimers({ toFake: ['Date'] })` on the frontend, `Clock` / `TestClock` on the backend).
- CI runs on Node 22; don't rely on behaviour of newer Node versions.
- `main` is protected (ruleset: PR only, required checks Backend, Frontend, E2E, CodeQL). Every change goes through a PR from an up-to-date `main`; if asked to push to `main` directly, say it is not possible and open a PR.

## Architecture

### Tech Stack

- **Frontend**: React 19, JavaScript (JSX; TypeScript only in `app/` and `contexts/`), Vite, TailwindCSS v4, shadcn/ui (Radix, components in `components/ui/`), sonner toasts, React Router 7, Tabler icons
- **Backend**: Spring Boot 4.1 (Java 21), Spring Security (session-based), Spring Data JPA, Flyway migrations
- **Database**: PostgreSQL 15
- **PDF generation**: Flying Saucer
- **Email**: Spring Mail + Thymeleaf templates

### Frontend Structure (`frontend/src/`)

```
api/          # Fetch wrappers (getJSON, postJSON, putJSON, deleteJSON)
app/          # Root layout, router config
pages/        # One file per route/page
components/   # Reusable UI components, shadcn/ui primitives in components/ui/
hooks/        # Custom data-fetching and filter hooks
contexts/     # AuthContext (global user state), ThemeContext (dark/light)
lib/          # Pure helpers (dates, pagination, pictures, cn in utils.js)
utils/        # Formatters, sorting, error toasts
constants/    # Roles (ROLES, hasRole) and other constants
styles/       # Global CSS and design tokens (index.css)
```

Key hooks:

- `useListFilters.js` - search text, active/inactive status and page of a list page, kept in the URL
- `usePagedList.js` - client-side pagination of a filtered list, page kept in the URL
- `useSearchParamsUpdater.js` - updates some URL search params and keeps the rest

### Backend Structure (`backend/src/main/java/cz/zcu/kiv/caretracker/`)

```
controller/      # REST controllers (one per domain)
service/         # Business logic
entity/          # JPA entities
repository/      # Spring Data JPA repos
dto/             # Request/response DTOs
mapper/          # Entity ↔ DTO mapping
specification/   # JPA Criteria API specifications (dynamic filtering)
security/        # MyUserDetails, auth handlers
config/          # SecurityConfig, WebConfig
enums/           # Domain enums (roles, unit types, ...)
exception/       # Custom exception classes
```

Database schema is versioned via Flyway in `src/main/resources/db/migration/`.

### Authentication

- Session-based (Spring Security), no JWT
- 7-day remember-me cookie
- Frontend checks `/api/auth-status` on load; dispatches `auth:unauthorized` custom event on 401 responses to trigger auto-logout
- `MyUserDetails` carries `userId`, `organizationId`, and `active` flag alongside standard Spring Security fields

### Role Hierarchy

```
SUPERADMIN > ADMIN > COORDINATOR > CAREGIVER
CLIENT (separate, read-only access to own data)
```

Role-based filtering is enforced both in backend `@PreAuthorize` annotations and in frontend `ProtectedRoute` + `hasRole` (`constants/roles.js`).

### API Conventions

- All endpoints under `/api/`
- Vite proxy forwards `/api/*` → `http://backend:8080` in dev
- Login/logout use `application/x-www-form-urlencoded` (Spring Security form login); all other endpoints use JSON
- Error responses contain a `.message` string field
- Filter params for list endpoints: `organizationId`, `departmentIds`, `caregiverIds`, `clientId`, `month`, `year`, `status`

### Key Domain Entities

| Entity           | Purpose                                                                     |
| ---------------- | --------------------------------------------------------------------------- |
| `Organization`   | Top-level tenant                                                            |
| `Department`     | Regional office within an org                                               |
| `Employee`       | Staff (CAREGIVER / COORDINATOR / MANAGER roles)                             |
| `Client`         | Care recipient; has assigned caregiver, benefit level, termination tracking |
| `Task`           | Billable service catalog entry (HOUR / OCCURRENCE / KG / KM unit types)     |
| `PerformedTask`  | Record of a service provided; many-to-many with caregivers                  |
| `IndividualPlan` | Versioned care plan per client; contains `DailyRecord` entries              |
