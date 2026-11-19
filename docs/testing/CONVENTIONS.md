# Testy a CI – rozhodnutí a konvence

Jak se v projektu testuje. Příkazy a pomocné třídy jsou v `.claude/CLAUDE.md` (sekce Tests).

---

## 1. Rozhodnutí

| Téma | Volba | Proč |
| --- | --- | --- |
| Databáze v testech | **Testcontainers + PostgreSQL 15** (stejná verze jako produkce) | Testy jdou přes skutečné Flyway migrace a skutečné SQL. H2 by schovala rozdíly (dotazy pro nástěnku, `bytea`, datumové funkce). |
| Unit vs. integrační testy na backendu | Unit: `*Test` (Surefire, `mvn test`). Integrační se Springem a DB: `*IT` (Failsafe, `mvn verify`). | Rychlé testy běží zvlášť a hned, pomalé jen ve `verify`. |
| Pokrytí backendu | JaCoCo, report v CI | Prahy pokrytí se jen zvedají. |
| Frontend | Vitest + Testing Library + `user-event` + `jest-dom`, API přes **MSW** | Testuje se chování z pohledu uživatele, ne implementace. MSW mockuje síť, ne `api.js`, takže se testuje i ten. |
| E2E | Playwright, pár kritických toků | Proti skutečnému stacku (docker compose) s demo seedem. Jen to, co jinými testy nejde ověřit. |
| CI | GitHub Actions na `ubuntu-latest` (má Docker pro Testcontainers) | Deploy dál na self-hosted runneru, ale až po zeleném CI. |
| Nové funkce | Od teď test spolu s kódem (ideálně TDD) | Jinak dluh poroste znovu. |

---

## 2. Konvence

- Struktura testů zrcadlí `main`: `service/ClientService` → `service/ClientServiceTest`.
- Název testu popisuje chování, ne metodu: `caregiverCannotReadClientFromOtherDepartment`, ne `testGetClientById2`. Na frontendu `it("hides create button for caregiver")`.
- Každý test má tři části: příprava → akce → ověření (Arrange / Act / Assert). Oddělit prázdným řádkem, žádné komentáře `// given`.
- Jeden test = jeden důvod selhání. Víc `assert` je v pořádku, pokud ověřují tutéž věc.
- Testovací data přes `TestData` (`testData.department(org)`, `testData.employee(dept, CAREGIVER)`), ne kopírovaný setup v každém testu. Metody vyplní povinná pole unikátními hodnotami, test mění jen to, na čem mu záleží.
- Mockovat jen hranice systému (e-mail, geokódování, QR API, čas). Vlastní repozitáře a služby v integračních testech nemockovat.
- Čas: kde logika závisí na „dnes“ (nástěnka, tento měsíc), injektovat `Clock` místo `LocalDate.now()`.
- Testy jsou nezávislé na pořadí. Integrační testy po sobě uklízí (`DatabaseCleaner` před každým testem vyprázdní všechny tabulky; migrace demo data neobsahují, ta nahrává `DemoDataSeeder` jen v profilech `demo` a `dev`).
- Na frontendu dotazy podle priority Testing Library: `getByRole` > `getByLabelText` > `getByText` > `getByTestId` (poslední možnost).

## 3. Co čím testovat

| Co | Typ testu | Nástroj |
| --- | --- | --- |
| Výpočty v entitách, formátování, čisté funkce | unit | JUnit / Vitest |
| Oprávnění podle role a organizace/střediska | integrační přes HTTP | `@SpringBootTest` + MockMvc + Testcontainers |
| Byznys pravidla služeb (životní cyklus, plány, tokeny) | integrační | `@SpringBootTest` + Testcontainers |
| JPA specifikace a vlastní dotazy | integrační, jen JPA vrstva | `@DataJpaTest` + Testcontainers |
| Validace vstupů, formát chyb, CSRF, login | web vrstva | `@WebMvcTest` nebo `@SpringBootTest` + MockMvc |
| Hooky, komponenty, stránky | integrační na frontendu | Vitest + Testing Library + MSW |
| Kritické toky přes celou aplikaci | E2E | Playwright |

---

## 4. Odkazy

- Spring Boot – Testing: https://docs.spring.io/spring-boot/reference/testing/index.html (sekce „Testing Spring Boot Applications“ a „Testcontainers“)
- Spring Security – Testing: https://docs.spring.io/spring-security/reference/servlet/test/index.html (sekce „Testing Method Security“ a „MockMvc Support“)
- Testcontainers – PostgreSQL modul: https://java.testcontainers.org/modules/databases/postgres/
- JUnit 5 – Parameterized Tests: https://junit.org/junit5/docs/current/user-guide/#writing-tests-parameterized-tests
- AssertJ: https://assertj.github.io/doc/
- Vitest: https://vitest.dev/guide/
- Testing Library – priorita dotazů: https://testing-library.com/docs/queries/about#priority
- user-event: https://testing-library.com/docs/user-event/intro
- MSW: https://mswjs.io/docs/
- Playwright: https://playwright.dev/docs/intro
- GitHub Actions – `workflow_run`: https://docs.github.com/en/actions/writing-workflows/choosing-when-your-workflow-runs/events-that-trigger-workflows#workflow_run
