# České demo data – implementační plán

**Cíl:** nahradit náhodná anglická data realistickými českými demo daty pro malou až střední pečovatelskou službu, mimo Flyway migrace, s AI portréty u části lidí.

**Větev:** `feat/czech-demo-data` z `main` (56b6928).

## Rozhodnutí (odsouhlaseno 2026-11-14)

- **Data mimo migrace:** falešná data z `V2__insert_dummy_data.sql`, `V18__recent_dummy_data.sql`, `V19__anning_performed_tasks.sql` a `V20__anning_individual_plan.sql` úplně odstranit (soubory zůstanou kvůli historii verzí, obsah nahradí komentář, že demo data žijí v seedu). Produkce zatím nikde neběží, demo se při nasazení čistí (`flyway.clean`). `V22` (převod dummy hodin) se stane no-op, nemazat. Ověřit, že ostatní migrace (V3, V4, V23, V24, V29, V30, V31) projdou nad prázdnými tabulkami.
- **Seed jen pro demo a vývoj:** nový seed se načte při startu aplikace v profilech `demo` a `dev`, jen když v DB není žádná organizace. Pořadí: data (SQL), portréty (Java přes `PictureService`, aby se vytvořily obě velikosti), pak stávající generátor aktivity (`demo-activity/seed.sql`). Noční reset dema (`DemoConfig.resetDemoData`) po `clean` + `migrate` spustí totéž. Lokální `docker-compose.yml` dostane `SPRING_PROFILES_ACTIVE=dev`. Profil `test` seed nikdy nespouští.
- **Generátor:** Python skript s pevným seedem v repu (`tools/demo-data/generate.py`), výstupy commitnuté: `backend/src/main/resources/demo-data/data.sql`, `tools/demo-data/portraits.json` (prompty a seedy pro FLUX), portréty `backend/src/main/resources/demo-data/portraits/{clients|employees}/<osobní číslo nebo id>.jpg` (512×512, JPEG q85).
- **Demo účty** zůstávají: `superadmin`, `admin`, `coordinator`, `caregiver`, `client`, heslo `heslo` (stejný hash jako dnes). `coordinator` je koordinátorka střediska 01, `caregiver` pečovatelka z 01, `client` klient z 01 s IP.
- Do patičky přihlašovací stránky v demo buildu (nebo všude, pokud demo build nejde rozlišit jednoduše) text: „Demo obsahuje smyšlené osoby, fotografie jsou vygenerované AI.“

## Rozsah dat

**Hlavní organizace:** „Pečovatelská služba Severní Plzeňsko, z. ú.“ (smyšlená), sídlo Kralovice, smyšlený účet a IČO.

| Středisko (číslo) | Sídlo | Obce (municipality) | DPS (supported_housing) | Klienti |
| --- | --- | --- | --- | --- |
| 01 Kralovicko | Kralovice | Kralovice, Hadačka, Výrov, Kozojedy, Bohy, Kožlany, Mladotice | DPS Kralovice, DPS Kožlany, DPS Bohy | ~110 |
| 02 Plasko | Plasy | Plasy, Babina, Žebnice, Lomnička, Horní Hradiště | DPS Plasy | ~80 |
| 03 Manětínsko | Manětín | Manětín, Brdo, Hrádek, Lipí, Stvolny | – | ~60 |
| 04 Třemošensko | Třemošná | Třemošná, Ledce, Zruč-Senec, Dolní Bělá | DPS Třemošná | ~90 |

Celkem 340 klientů v hlavní organizaci.

- **Zaměstnanci:** 1 vedoucí (MANAGER, účet `admin`), 4 koordinátorky (jedna na středisko; koordinátorka střediska 02 je zároveň vedoucí = role MANAGER a účet ADMIN a je nastavená jako `department.coordinator_id`), asi 48 pečovatelek a pečovatelů (14/11/9/14). Ostatní zaměstnanci mají účty (aktivní, heslo `heslo`) jen u několika, zbytek bez účtu nebo s nevyřízenou aktivací – jak to dovoluje model.
- **Klienti:** věk 65–98, jména podle generace (Marie, Jaroslava, Božena, Josef, Václav, Jiří…), ženská příjmení ve správném tvaru, rodné číslo nevyplňovat. Osobní číslo `XXYYYY` podle střediska a pořadí. Adresy: běžné názvy ulic (Školní, Nádražní, Husova, Plzeňská, Na Výsluní…) v obcích střediska, smyšlená čísla, správná PSČ obcí. ~5 % klientů z obce mimo seznam střediska s dojezdovou vzdáleností v km.
  - Ubytování: ~40 v DPS (adresa domu), ostatní rodinný dům, vlastní byt, nájemní byt, obecní byt, u příbuzných. Výtah, patro, přístup do bytu (zvonek/klíče/zavolat, u klíčů smyšlený kód zámku) u části.
  - ~20 nesvéprávných s opatrovníkem (jméno a kontakt), ~6 se službou bez úhrad, ~30 ukončených s různými důvody a daty, tituly u pár klientů (Ing., Mgr., MUDr., PhDr., RNDr.), druhý telefon u části, praktický lékař, kontakt na příbuzné, smlouva od/do (určitá i neurčitá), péče od, vstupní šetření.
  - Kontakty: e-maily `@example.com`, telefony ve formátu `+420 6xx xxx xxx` / `+420 7xx xxx xxx`.
- **Úkony:** importované položky vyhlášky (z `statutory_task`, krátké názvy, cena = maximum, odkaz `statutory_task_id`) – všechny hodinové + oběd, dovoz, velký nákup, praní; plus fakultativní „Doprava autem“ (KM, 12 Kč) a „Pedikúra“ (úkon, 150 Kč). Klientům přiřadit typické kombinace.
- **Individuální plány:** ~60 % aktivních klientů, 1–3 verze, texty sekcí česky a věrohodně (oblíbené činnosti, co nemá rád, zdraví, cíle), denní záznamy za poslední 2–3 měsíce.
- **Provedené úkony:** generátor aktivity (`demo-activity/seed.sql`) doplní 12 měsíců; upravit mapování názvů úkonů na nové krátké názvy. Pár klientů s péčí nad 80 hodin měsíčně (snížená sazba).
- **Dokumenty:** u ~15 klientů 1–2 malé vygenerované PDF (smlouva, vstupní šetření) – volitelné, jen pokud je jednoduché je vyrobit bez externích závislostí.
- **Druhá organizace:** „Domácí péče Rokycansko, o. p. s.“ – 1 středisko (01 Rokycany), 1 admin, 1 koordinátor, 2 pečovatelky, 8 klientů, pár úkonů. Účty bez demo přezdívek.
- **Portréty:** ~100 klientů (~30 %) a ~30 zaměstnanců (~60 %); prompt podle věku, pohlaví, vlasů, brýlí, oblečení; seed z osobního čísla (klienti) nebo id (zaměstnanci).

## Testy a ověření

- Backend `./mvnw verify` (test profil seed nespouští; `ApplicationContextIT` a migrační testy musí projít nad prázdnými migracemi).
- Nový IT pro seeder: v profilu `dev` na prázdné DB vytvoří organizace, 340+ klientů, portréty mají obě velikosti, druhé spuštění nic nezdvojí.
- E2E: přepsat konstanty (např. `CLIENT_WITH_PLAN`) a očekávané texty na nová data; `make e2e-up && make e2e`.
- Frontend `npm run test:run`, `npm run lint`, `npm run build`.
