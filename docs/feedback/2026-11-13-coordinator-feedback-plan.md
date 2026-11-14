# Připomínky koordinátorky – implementační plán

> **Pro agenty:** povinný sub-skill superpowers:executing-plans (nebo subagent-driven-development). Kroky jsou checkboxy (`- [ ]`).

**Cíl:** zapracovat rozhodnutí ze sekce „Rozhodnutí“ v `docs/feedback/koordinatorka-2026-jaro.md` (kromě přepracování IP, bod 4.3).

**Architektura:** rozšíření stávajících vrstev (entity → Flyway migrace → DTO/mapper → service s kontrolou přístupu → controller → React stránky). Nové entity: `Municipality`, `SupportedHousing` (DPS), `ClientDocument`, `StatutoryTask`. Osobní číslo klienta se mění z `Long` na text `XXYYYY`.

**Tech stack:** Spring Boot 3 / Java 21, Flyway, PostgreSQL 15, React 19 + HeroUI, Vitest + MSW, Testcontainers.

**Spec:** `docs/feedback/koordinatorka-2026-jaro.md` (sekce „Rozhodnutí“ a návrhy u bodů 1, 2 a 5).

**Vlastník úkolu:** `[Claude]` infrastruktura a mechanické části, `[Michal]` doménové a UI části (domluveno 2026-11-13). Úkoly `[Michal]` Claude jen reviewuje.

**Větev:** `feat/coordinator-feedback` z `origin/main`, jeden PR na konci.

## Globální omezení

- Kód, názvy a komentáře anglicky; texty pro uživatele (chyby v toastu, popisky) česky.
- Žádný em dash nikde.
- Chybové odpovědi mají pole `.message`.
- Nové migrace číslovat od `V30`, schéma musí sedět na entity (`ddl-auto=validate`).
- Postgres enumy přes `CREATE TYPE ... AS ENUM` + `@JdbcType(PostgreSQLEnumJdbcType.class)`, jako `termination_reason` (`V5`).
- Integrační testy dědí `AbstractIntegrationTest`, data přes `TestData`, oprávnění přes `AccessScenario`. Žádné `@MockitoBean` v podtřídách.
- Frontend testy: `renderWithProviders`, MSW, pevný čas (`vi.useFakeTimers({ toFake: ['Date'] })`).
- Upload max. 5 MB (stávající `spring.servlet.multipart.max-file-size`).
- Kód zámku (`lockCode`) se nesmí objevit v `ClientSummaryDTO`, `ClientShortDTO`, exportech ani tiskových sestavách.

## Na co dát pozor při review

1. **Migrace na reálných datech:** přečíslování (Task 4) musí projít i na produkční databázi. Před nasazením spustit kontrolní dotaz z Task 4 Step 1, aby žádná organizace neměla víc než 99 středisek a žádné středisko víc než 9 999 klientů.
2. **Přesun klienta do jiného střediska:** osobní číslo se nezmění, a další volné číslo ve středisku proto musí počítat podle prefixu čísla, ne podle `department_id` (test v Task 4).
3. **Klient v DPS a změna adresy DPS:** adresa klienta se kopíruje při uložení klienta. Po změně adresy DPS se adresy jeho klientů musí přepsat také (test v Task 6).
4. **Nesvéprávný klient bez opatrovníka při úpravě starého záznamu:** existující klienti s `legally_competent = false` nemají opatrovníka; uložení formuláře je má zastavit s jasnou hláškou, ne s 500 (test v Task 8).
5. **Import úkonů dvakrát po sobě:** druhý import nesmí vytvořit duplicitní úkony (test v Task 12).

---

### Task 1 [Claude]: Koordinátor smí upravovat IP klientů svého střediska (3.7)

**Files:**
- Modify: `backend/src/main/java/cz/zcu/kiv/caretracker/service/ClientService.java` (`validateIndividualPlanUpdateAccess`, ~ř. 440)
- Modify: `frontend/src/pages/IndividualPlan.jsx` (`canEdit`, ~ř. 220)
- Modify: `docs/testing/PLAN.md:108`
- Test: `backend/src/test/java/cz/zcu/kiv/caretracker/service/IndividualPlanIT.java`, `frontend/src/pages/IndividualPlan.test.jsx`

**Interfaces:** `user.departmentId` z `/api/auth-status` (už existuje, `AuthController.java:69`).

- [ ] **Step 1:** test `coordinatorOfClientsDepartmentCanUpdatePlan` (PUT `/api/clients/{id}/individual-plan` jako koordinátor stejného střediska, klient s jiným pečovatelem → 2xx) a `coordinatorOfOtherDepartmentCannotUpdatePlan` (→ 403). Stejné dva testy pro POST denního záznamu.
- [ ] **Step 2:** `./mvnw verify -Dit.test=IndividualPlanIT` → první test FAIL (403).
- [ ] **Step 3:** v `validateIndividualPlanUpdateAccess` větev `COORDINATOR`: projde, když `client.getDepartment().getId()` == oddělení zaměstnance; `CAREGIVER` beze změny.
- [ ] **Step 4:** Vitest: koordinátor stejného střediska vidí „Nová verze“, koordinátor jiného ne. `canEdit = client.active && (isAdmin || user.employeeId === client.caregiver?.id || (user.role === COORDINATOR && user.departmentId === client.department?.id))`.
- [ ] **Step 5:** `./mvnw verify -Dit.test=IndividualPlanIT` a `npm run test:run -- IndividualPlan` → PASS. Upravit `PLAN.md:108` na „[x] změněno: koordinátor upravuje IP všech klientů střediska“.
- [ ] **Step 6:** commit `feat: coordinator edits individual plans of all clients in the department`

### Task 2 [Michal]: Telefon a tisk IP (4.1, 4.2)

**Files:** Modify `frontend/src/pages/IndividualPlan.jsx` (hlavička ř. 51-58, akce u „Nová verze“), styly do `frontend/src/index.css` (nebo kde jsou globální styly); Test `frontend/src/pages/IndividualPlan.test.jsx`

- [ ] **Step 1:** test: tlačítko „Tisk“ volá `window.print` (`vi.spyOn(window, "print").mockImplementation(() => {})`). Telefon nanejvýš jako jeden `expect` v existujícím testu, samostatný test nemá cenu.
- [ ] **Step 2:** `npm run test:run -- IndividualPlan` → FAIL.
- [ ] **Step 3:** implementace. Tiskové styly `@media print` + `@page { size: A4; margin: 15mm; }`: skrýt navigaci, sidebar, tlačítka a denní záznamy (třída např. `print:hidden` v Tailwind v4), obsah na plnou šířku, bez tmavého režimu.
- [ ] **Step 4:** testy PASS; ručně ověřit náhled tisku v Chrome (A4, jedna stránka hlavičky bez ořezu).
- [ ] **Step 5:** commit `feat: individual plan shows phone and prints on A4`

### Task 3 [Claude]: Číslo střediska povinné, 1–99, unikátní v organizaci (2)

**Files:**
- Create: `backend/src/main/resources/db/migration/V30__department_number_required.sql`
- Modify: `Department.java` (`nullable = false`), `DepartmentService.java` (validace), `TestData.department(...)` (přidělí další volné číslo v organizaci)
- Modify: `frontend/src/components/departments/departmentForm.js` (validace)
- Test: `service/DepartmentLifecycleIT.java`, `departmentForm.test.js`

**Produces:** `department.department_number int NOT NULL`, unikátní index `(organization_id, department_number)`.

- [ ] **Step 1:** testy: vytvoření bez čísla → 400 „Číslo střediska je povinné“; 0 a 100 → 400 „Číslo střediska musí být mezi 1 a 99“; duplicita v organizaci → 400 „Středisko s tímto číslem už v organizaci existuje“; stejné číslo v jiné organizaci → 2xx.
- [ ] **Step 2:** `./mvnw verify -Dit.test=DepartmentLifecycleIT` → FAIL.
- [ ] **Step 3:** migrace: střediskům s `NULL` přidělit čísla po organizacích (`max(existující) + row_number() over (partition by organization_id order by id)`), pak `SET NOT NULL`, `CHECK (department_number BETWEEN 1 AND 99)`, unikátní index. Validace v service; porušení indexu přeložit v `GlobalExceptionHandler` stejně jako u osobního čísla.
- [ ] **Step 4:** frontend validace se stejnými texty, test `departmentForm.test.js`.
- [ ] **Step 5:** `./mvnw verify` celý (TestData se mění všude) a `npm run test:run` → PASS.
- [ ] **Step 6:** commit `feat: department number is required and unique within the organization`

### Task 4 [Claude]: Osobní číslo `XXYYYY` (2, 3.6)

**Files:**
- Create: `V31__personal_number_by_department.sql`
- Modify: `Client.java` (`String personalNumber`, `length = 6`), `ClientRequestDTO`, `ClientDTO`, `ClientService.saveClient` + `validatePersonalNumber`, `ClientRepository`, `ReceiptFormatting.variableSymbol`, `ReceiptService`, `DepartmentController` + `DepartmentService`
- Test: `ClientPersonalNumberIT`, `ReceiptFormattingTest`, `ReceiptIT`, `ClientRequestErrorsTest`, `GlobalExceptionHandlerTest`

**Produces:**
- `GET /api/departments/{id}/next-personal-number` → `{"personalNumber": "010043"}`; role SUPERADMIN, ADMIN, COORDINATOR; přístup ke středisku přes `validateDepartmentAccess`.
- `String DepartmentService.nextPersonalNumber(Long departmentId)`
- `static String ReceiptFormatting.variableSymbol(String personalNumber, int month, int year)` → `RRMM` + personalNumber.

- [ ] **Step 1:** kontrolní dotaz pro produkci (uložit do popisu PR):
  ```sql
  SELECT organization_id, count(*) FROM department GROUP BY 1 HAVING count(*) > 99;
  SELECT department_id, count(*) FROM client GROUP BY 1 HAVING count(*) > 9999;
  ```
- [ ] **Step 2:** testy:
  - bez osobního čísla ve středisku č. 1 → `"010001"`, druhý klient `"010002"`;
  - ukončený klient s `"010007"` → další je `"010008"` (čísla se nepoužijí znovu);
  - klient `"010005"` přesunutý do střediska č. 2 si číslo nechá, další ve středisku 1 je `"010006"`;
  - `next-personal-number` vrací totéž, co by se přidělilo;
  - ruční `"020099"` → uloží se; `"12345"`, `"1234567"`, `"01a001"` → 400 „Osobní číslo musí mít 6 číslic“;
  - duplicita v organizaci (i proti ukončenému klientovi) → 400 „Osobní číslo je už obsazené“;
  - `variableSymbol("010042", 3, 2026)` → `"2603010042"`.
- [ ] **Step 3:** `./mvnw verify -Dit.test='ClientPersonalNumberIT,ReceiptIT'` → FAIL.
- [ ] **Step 4:** migrace: `ALTER COLUMN personal_number TYPE varchar(6)`, přečíslování `lpad(department_number,2,'0') || lpad(row_number() over (partition by department_id order by id),4,'0')`, `SET NOT NULL`. Stávající index `idx_client_personal_number_organization` zůstává (pokrývá i ukončené).
- [ ] **Step 5:** generování: další číslo = max čtyřčíslí mezi klienty organizace, jejichž číslo začíná prefixem střediska, + 1; nad 9999 → 400 „Ve středisku došla osobní čísla“. Kontrola unikátnosti proti všem klientům organizace (nahradit `existsByPersonalNumberAndOrganizationIdAndActiveTrue`). Číslo se přiděluje před prvním `save`, odpadá dvojí ukládání.
- [ ] **Step 6:** testy PASS, pak celý `./mvnw verify`.
- [ ] **Step 7:** commit `feat: personal number is department number plus a sequence`

### Task 5 [Michal]: Osobní číslo ve formuláři (2, 3.6)

**Files:** `frontend/src/components/clients/clientForm.js`, `ClientFields.jsx`, `pages/ClientCreate.jsx`, `pages/ClientDetail.jsx`; Test `clientForm.test.js`, `ClientCreate.test.jsx`

**Consumes:** `GET /api/departments/{id}/next-personal-number` (Task 4).

- [ ] **Step 1:** testy: po výběru střediska se pole předvyplní `"010043"` (MSW handler); ručně upravené číslo se změnou střediska nepřepíše; validace „Osobní číslo musí mít 6 číslic“; do API jde string, ne `Number`.
- [ ] **Step 2:** FAIL → implementace → PASS (`npm run test:run -- client`).
- [ ] **Step 3:** hint pole: „Předvyplní se podle střediska, lze změnit.“ U editace existujícího klienta se nic nepředvyplňuje.
- [ ] **Step 4:** commit `feat: client form prefills the personal number`

### Task 6 [Michal]: Ubytování, obce a DPS – backend (1)

**Files:**
- Create: `V32__accommodation.sql`, `entity/Municipality.java`, `entity/SupportedHousing.java`, `enums/AccommodationType.java`, `enums/AccessMethod.java`, repozitáře, DTO, `MunicipalityController` a `SupportedHousingController` (nebo endpointy v `DepartmentController`)
- Modify: `Client.java`, `ClientRequestDTO`, `ClientDTO`, `ClientMapper`, `ClientService.saveClient`
- Test: `controller/AccommodationIT`, `controller/AccommodationAccessIT` (AccessScenario), napsal Claude

**Produces:**
- `AccommodationType { SUPPORTED_HOUSING, RENTED_FLAT, MUNICIPAL_FLAT, FAMILY_HOUSE, OWN_FLAT, WITH_RELATIVES }`
- `AccessMethod { DOORBELL, KEYS, PHONE_CALL, OTHER }`
- `Municipality(id, department, name)`; `SupportedHousing(id, department, name, street, city, postalCode, active)`
- Client: `accommodationType` (nullable kvůli starým záznamům), `supportedHousing` (FK, povinné právě když typ = DPS), `travelDistanceKm Integer`, `hasElevator Boolean`, `floorOrRoom String`, `accessMethod`, `lockCode String(20)`, `accessNote TEXT`
- `GET/POST/DELETE /api/departments/{id}/municipalities`, `GET/POST/PUT /api/departments/{id}/supported-housings` (+ `terminate` jako u ostatních); čtení zaměstnanci organizace, zápis SUPERADMIN, ADMIN a koordinátor daného střediska (`validateDepartmentAccess`).

- [ ] **Step 1:** testy: klient s typem DPS dostane adresu z DPS, i když request posílá jinou; DPS z jiného střediska → 400 „Dům s pečovatelskou službou nepatří do střediska klienta“; typ DPS bez DPS → 400; změna adresy DPS přepíše adresy jeho klientů; `lockCode` není v odpovědi `GET /api/clients` (seznam); přístupová matice endpointů obcí a DPS.
- [ ] **Step 2:** FAIL → implementace → PASS.
- [ ] **Step 3:** commit `feat: client accommodation with municipalities and supported housing`

### Task 7 [Michal]: Ubytování, obce a DPS – frontend (1)

**Files:** `components/departments/DepartmentSheet.jsx` (správa obcí a DPS), `components/clients/ClientFields.jsx` (nová `FieldSection` „Ubytování“ s druhem ubytování, DPS, adresou, výtahem, patrem, přístupem do bytu, km), `clientForm.js`, `pages/ClientDetail.jsx`; testy k nim

- [ ] **Step 1:** testy: výběr DPS vyplní a zamkne ulici, město a PSČ; změna typu z DPS zpět adresu odemkne; město nabízí obce střediska a přijme i vlastní hodnotu; kód zámku se zobrazí jen v detailu klienta.
- [ ] **Step 2:** FAIL → implementace (město přes `Combobox`; pokud neumí vlastní hodnotu, rozšířit ho o prop `allowsCustomValue`) → PASS.
- [ ] **Step 3:** commit `feat: accommodation section in the client form`

### Task 8 [Michal]: Další údaje klienta – backend (3)

**Files:** `V33__client_details.sql`, `Client.java`, `ClientRequestDTO`, `ClientDTO`, `ClientMapper`, `ClientService.validateRequiredFields`; Test `ClientCreateValidationIT`, `ClientLifecycleIT`

**Produces (Client):** `titleBefore String(20)`, `titleAfter String(20)`, `phone2 String(16)`, `guardianName String`, `guardianContact TEXT`, `careFrom LocalDate`, `careTo LocalDate`, `contractFrom LocalDate`, `contractTo LocalDate`, `contractIndefinite Boolean` (default true), `noCharge Boolean` (default false, NOT NULL), `intakeAssessmentDate LocalDate`.

- [ ] **Step 1:** testy: nesvéprávný bez jména nebo kontaktu opatrovníka → 400 „U nesvéprávného klienta vyplňte jméno a kontakt opatrovníka“ (create i update starého záznamu); `contractIndefinite = false` bez `contractTo` → 400 „U smlouvy na dobu určitou vyplňte datum konce“; `contractTo` před `contractFrom` → 400 „Konec smlouvy nesmí být před jejím začátkem“; totéž pro `careTo`/`careFrom`.
- [ ] **Step 2:** FAIL → implementace → PASS.
- [ ] **Step 3:** commit `feat: client titles, second phone, guardian, contract and intake dates`

### Task 9 [Michal]: Další údaje klienta – frontend (3)

**Files:** `ClientFields.jsx` (osobní údaje: tituly, telefon 2; nová sekce „Opatrovník“ viditelná, když není svéprávný; sekce „Smlouva“; datum vstupního šetření), `clientForm.js`, `ClientDetail.jsx`, `IndividualPlan.jsx` (jméno s tituly); testy

- [ ] **Step 1:** testy validace se stejnými texty jako Task 8; opatrovník se zobrazí po odškrtnutí „Svéprávný“; jméno v detailu i v IP s tituly („Mgr. Jana Nová, DiS.“).
- [ ] **Step 2:** FAIL → implementace → PASS.
- [ ] **Step 3:** commit `feat: client form has titles, guardian and contract`

### Task 10 [Michal]: Služba bez úhrad v účtenkách (3)

**Files:** `ReceiptService.java`, šablona účtenky v `templates/`; Test `ReceiptIT`

- [ ] **Step 1:** test: klient s `noCharge = true` → účtenka obsahuje úkony, celkem 0 Kč, `paymentQrClient.fetchPng` se nezavolá, místo QR text „Služba je poskytována bez úhrady“.
- [ ] **Step 2:** FAIL → implementace → PASS.
- [ ] **Step 3:** commit `feat: receipts of clients without charge have no payment`

### Task 11 [Claude]: Dokumenty klienta – backend (3.1, 3.3, 3.4)

**Files:**
- Create: `V34__client_document.sql`, `entity/ClientDocument.java`, `enums/ClientDocumentType.java`, `repository/ClientDocumentRepository.java`, `dto/client/ClientDocumentDTO.java`, `service/ClientDocumentService.java`, endpointy v `ClientController`
- Test: `service/ClientDocumentIT.java`, `controller/ClientDocumentAccessIT.java`

**Produces:**
- `ClientDocumentType { CONTRACT, INTAKE_ASSESSMENT, KEY_CONSENT, OTHER }`
- `ClientDocument(id, client, type, fileName, contentType, size, data bytea, uploadedAt, uploadedBy Employee)`; `ClientDocumentDTO(id, type, fileName, contentType, size, uploadedAt, uploadedByName)` bez dat
- `GET /api/clients/{id}/documents` → `List<ClientDocumentDTO>` (nejnovější první)
- `POST /api/clients/{id}/documents` multipart `file`, `type` → `ClientDocumentDTO`
- `GET /api/clients/{id}/documents/{documentId}` → soubor, `Content-Disposition: attachment; filename*=UTF-8''…`
- `DELETE /api/clients/{id}/documents/{documentId}` → 204
- Čtení, nahrání i smazání: SUPERADMIN, ADMIN a COORDINATOR střediska klienta. CAREGIVER a CLIENT nic (403). Otevřít čtení pečovatelům půjde později jednou změnou v `@PreAuthorize` a kontrole přístupu, pokud to koordinátorka bude chtít.

- [ ] **Step 1:** testy: nahrání PDF a stažení vrátí stejné bajty a název s diakritikou; typ souboru mimo `application/pdf`, `image/jpeg`, `image/png` → 400 „Nahrát lze jen PDF, JPEG nebo PNG“ (kontrolovat i podle obsahu, jako `PictureService`); prázdný soubor → 400; seznam neobsahuje data; dokument jiného klienta přes cizí `clientId` → 404; přístupová matice přes `AccessScenario`; ukončený klient: čtení jde, nahrání 400 jako u IP.
- [ ] **Step 2:** FAIL → implementace → PASS, celý `./mvnw verify`.
- [ ] **Step 3:** commit `feat: client documents`

### Task 12 [Claude]: Úkony z vyhlášky – backend (5)

**Files:**
- Create: `V35__statutory_task.sql` (tabulka a data), `entity/StatutoryTask.java`, `repository/StatutoryTaskRepository.java`, `dto/task/StatutoryTaskDTO.java`, `controller/StatutoryTaskController.java`
- Modify: `Task.java` (`@ManyToOne statutoryTask`, nullable), `TaskRequestDTO` (`statutoryTaskId`), `TaskDTO` (`statutoryTaskId`, `statutoryMaxUnitPrice`), `TaskService`, `TaskController`
- Test: `service/TaskCatalogIT.java`, `controller/TaskAccessIT.java`

**Produces:**
- `StatutoryTask(id, code String, name, shortName, unitType UnitType, maxUnitPrice Integer)`; `code` = označení v § 6 odst. 1 (např. `a1`), `name` = oficiální znění, `shortName` = výchozí hovorový název (navrhne Claude v `statutory-tasks-draft.md`, potvrdí Michal)
- Import nastaví `Task.name = shortName`; `Task.name` zůstává volně upravitelný (alias organizace), oficiální název je vždy dostupný přes odkaz
- `TaskDTO` navíc `statutoryTaskCode`, `statutoryTaskName` (oficiální název)
- Tabulka `statutory_hourly_limit(monthly_hours_threshold int, max_unit_price_over_threshold int)` s jedním řádkem `80`, `145` (pro Task 12b)
- Rozhodnuto: hodinové položky mají `maxUnitPrice = 165`; importuje se všech 28 hodinových položek zvlášť (původně uvedených 31 byla chyba v počtu: 34 položek, z toho 4 za úkon a 2 za kg).
- `GET /api/statutory-tasks?organizationId=` (všechny zaměstnanecké role) → `List<StatutoryTaskDTO>` s `importedTaskId` (id úkonu organizace, který na položku odkazuje, jinak `null`)
- `POST /api/tasks/import-statutory?organizationId=` s tělem `{"statutoryTaskIds": [..]}` → `List<TaskDTO>` vytvořených; SUPERADMIN, ADMIN
- `List<Task> TaskService.importStatutoryTasks(Long organizationId, List<Long> statutoryTaskIds)` vytvoří úkony jen pro vybrané položky, na které v organizaci ještě žádný úkon neodkazuje (už importované tiše přeskočí); cena = `maxUnitPrice`.

- [ ] **Step 1:** stáhnout aktuální znění vyhlášky č. 505/2006 Sb. (§ 6 pečovatelská služba, maximální výše úhrady ve znění 195/2024 Sb.) ze zakonyprolidi.cz, sepsat seznam úkonů s jednotkou a maximem a **nechat ho Michalovi potvrdit** před zapsáním do migrace.
- [ ] **Step 2:** testy: import vytvoří jen vybrané položky; `importedTaskId` je vyplněné u importovaných a ručně navázaných; prázdný výběr → 400 „Vyberte alespoň jeden úkon“; import vytvoří položky s cenou = maximum, odkazem a názvem = `shortName`; přejmenovaný úkon dál vrací v `TaskDTO` oficiální `statutoryTaskName`; druhý import nevytvoří nic; ručně vytvořený úkon s odkazem se při importu přeskočí; uložení úkonu s cenou nad maximem → 400 „Cena převyšuje maximální úhradu podle vyhlášky (X Kč)“; úkon bez odkazu se nekontroluje; jednotka úkonu musí odpovídat jednotce položky → 400 „Jednotka úkonu neodpovídá položce vyhlášky“; přístupová matice.
- [ ] **Step 3:** FAIL → implementace → PASS.
- [ ] **Step 4:** commit `feat: tasks can be imported from and linked to the statutory catalog`

### Task 12b [Claude]: Snížená sazba nad 80 hodin měsíčně (5)

Pravidlo § 6 odst. 2: hodiny pečovatelské služby klienta v kalendářním měsíci nad 80 smějí stát nejvýš 145 Kč za hodinu. Před implementací ověřit přesné znění odstavce.

**Files:**
- Modify: `ReceiptFormatting.java` (seskupení řádků účtenky), `ReceiptService.java`, výpočty součtů nástěnky (`DashboardService` / `PerformedTaskRepository`), šablona účtenky jen pokud je potřeba nový popisek řádku
- Test: `ReceiptFormattingTest`, `ReceiptIT`, `DashboardTotalsIT`, `PerformedTaskRepositoryIT`

**Consumes:** `Task.statutoryTask`, `statutory_hourly_limit` (Task 12).

**Produces:** cena měsíce klienta, ve které se hodinové úkony navázané na vyhlášku nad hranicí účtují cenou `min(uložená unit_price, max_unit_price_over_threshold)`. Nic se neukládá, počítá se při vyúčtování.

- [ ] **Step 1:** testy (`ReceiptFormattingTest`, čistá funkce):
  - 79 h po 165 Kč + záznam 2 h po 165 Kč → 80 h × 165 + 1 h × 145, záznam rozdělený na dva řádky;
  - cena úkonu 140 Kč a 100 h → beze změny, žádné dělení řádků;
  - fakultativní hodinový úkon (bez odkazu na vyhlášku) se do 80 h nepočítá a nesnižuje se;
  - pořadí podle data záznamu, ne podle vložení: dodatečně vložený starší záznam posune hranici;
  - úkony za úkon a za kg se do hodin nepočítají;
  - zaokrouhlení jako `calculatePrice` (po záznamech, `HALF_UP`).
- [ ] **Step 2:** `ReceiptIT` a `DashboardTotalsIT`: součet na nástěnce = součet účtenek i pro klienta nad 80 h.
- [ ] **Step 3:** FAIL → implementace (jedna sdílená funkce pro účtenku i nástěnku; pokud nástěnka sčítá v SQL, převést výpočet nad hranicí do Javy nad načtenými záznamy, nebo ho v SQL replikovat a pokrýt testem shody) → PASS, celý `./mvnw verify`.
- [ ] **Step 4:** commit `feat: hours over the monthly threshold are billed at the reduced statutory rate`

### Task 13 [Michal]: Úkony z vyhlášky a dokumenty – frontend (3, 5)

**Files:** `pages/Tasks.jsx` (tlačítko „Načíst úkony z vyhlášky“), `components/tasks/TaskFields.jsx` + `taskForm.js` (výběr položky vyhlášky, varování nad maximem), `pages/ClientDetail.jsx` + nová `components/clients/ClientDocuments.jsx` (seznam, nahrání s typem, stažení, smazání); testy

**Consumes:** endpointy z Task 11 a 12.

- [ ] **Step 1:** testy: „Načíst úkony z vyhlášky“ otevře dialog se seznamem položek s checkboxy (krátký název, pod ním oficiální, jednotka a maximum), už importované jsou zaškrtnuté a disabled s popiskem „Už v katalogu“, tlačítka „Vybrat vše“ / „Zrušit výběr“ pracují jen s neimportovanými; potvrzení pošle jen vybraná id, ukáže toast „Načteno N úkonů“ a obnoví seznam; když je vše importované, potvrzovací tlačítko je disabled; výběr položky předvyplní jednotku, cenu a název (`shortName`); cena nad maximem ukáže chybu; detail úkonu (`TaskSheet`) navázaného na vyhlášku zobrazuje celý oficiální název a kód (např. „§ 6 a) 1.“), i když je úkon přejmenovaný; seznam úkonů ho ukazuje menším textem pod názvem; dokumenty: seznam, nahrání (FormData), smazání po potvrzení; pečovatel sekci dokumentů nevidí vůbec.
- [ ] **Step 2:** FAIL → implementace → PASS.
- [ ] **Step 3:** commit `feat: statutory task import and client documents in the UI`

### Task 14 [Claude]: Dokončení a PR

- [ ] **Step 1:** `frontend/`: `npm run lint`, `npm run coverage`, `npm run build`; `backend/`: `./mvnw verify`; `make e2e-up && make e2e` (mění se formulář klienta a demo data), `make e2e-down`. Do zprávy citovat poslední řádek výstupu každého příkazu.
- [ ] **Step 2:** doplnit `docs/testing/PLAN.md` o nové testy a změnu u 3.7.
- [ ] **Step 3:** ověřit, že PR #70 je mergnutý a větev je na aktuálním `main` (`git pull --rebase origin main`).
- [ ] **Step 4:** PR s postupem ručního otestování (URL, kliknutí, očekávaný výsledek) pro každý bod a kontrolním dotazem z Task 4 Step 1.
