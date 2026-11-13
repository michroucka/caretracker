# Poznámky z testování – koordinátorka (reálná organizace)

Testováno na starší verzi, přibližně duben–květen 2026. Stav jednotlivých bodů se ověří proti aktuálnímu kódu.

## 1. Rozdělení středisek na obce

Středisko je potřeba rozdělit na jednotlivé obce (podle města).
Příklad: středisko Kralovicko zahrnuje obce Kralovice, Hadačka, Výrov, Kozojedy, Bohy, Kozlany atd.

V obcích jsou domy s pečovatelskou službou (DPS), které patří pod středisko. Pro Kralovicko jsou to DPS v Kralovicích, Kozlanech a Bohách.
Účel zatím není přesně známý, obce budou nejspíš potřeba na víc věcí.

**Návrh (2026-11-13):**
- Klient může být i z obce, která pod středisko oficiálně nepatří (dojezdová vzdálenost, péče po domluvě). Pevný seznam obcí proto nestačí.
- U klienta výběr druhu ubytování: DPS, nájemní byt, obecní byt, rodinný dům, vlastní byt, u příbuzných.
- DPS (tabulka pod střediskem) mají předvyplněnou adresu, kterou u klienta nejde změnit. U ostatních druhů se adresa píše ručně.
- Město jako autocomplete z tabulky obcí střediska (spravuje admin), povoluje i vlastní hodnotu (obec mimo středisko).
- Inspirace: formulář klienta ve stávající aplikaci organizace (sekce Ubytování: druh ubytování, okres, výtah, trvalá adresa v zařízení, klíče, kód zámku, patro/pokoj, dojezdová vzdálenost v km, předchozí adresa).

## 2. Hodnoty odvozené z čísla střediska

Každé středisko má číslo (už je přidané). Z něj se odvozují další hodnoty, např. osobní číslo klienta. Odvozování je ještě potřeba upravit.

**Návrh (2026-11-13):** osobní číslo ve tvaru `XXYYYY` bez oddělovačů: `XX` je číslo střediska na 2 číslice, `YYYY` pořadové číslo ve středisku. Používá se ve variabilním symbolu, který je `RRMM` + osobní číslo na 6 číslic (max. 10 číslic). Číslo má být vidět už ve formuláři před uložením.

## 3. Klient

1. Možnost vkládat sociální šetření (nejspíš nahrání dokumentu).
2. Denní záznamy (jsou u IP). Nejspíš stačí u IP, jinde nejsou potřeba.
3. Vstup do bytu: záznam o tom, jak se do bytu dostat (zvonek, klíče, zavolat atd.). Pokud klíče, možnost nahrát souhlas s jejich poskytnutím.
4. Smlouva (nejspíš nahrání dokumentu).
5. Pokud je klient nesvéprávný, musí být uvedeno jméno opatrovníka a kontakt na něj.
6. Osobní číslo se generuje, ale při vytváření klienta není vidět, objeví se až po kliknutí na „Vytvořit“. Pro uživatele je to matoucí. (Souvisí s bodem 2.)
7. Koordinátor by měl mít možnost dělat IP všem klientům střediska.

## 4. Individuální plán (IP)

1. Možnost tisku ve vhodném formátu (A4).
2. U klienta v IP chybí telefon.
3. Velké přepracování IP (budoucí funkce, zatím bez přesného zadání).
   - Původně jen několik textových polí kvůli jednoduchosti a rozsahu BP. Teď je sekce nedostatečná a působí nedodělaně.
   - CPOS Touškov používá ioplan (aplikace jen na individuální plány). Cíl: aby CareTracker dokázal ioplan nahradit.
   - Bude muset být interaktivnější. Podrobnosti doladit později podle představ koordinátorky.

## 5. Úkony podle vyhlášky

Úkony se teď zadávají ručně pro každou organizaci. Jsou ale pevně dané vyhláškou č. 505/2006 Sb., kterou se provádějí některá ustanovení zákona č. 108/2006 Sb., o sociálních službách (§ 40; verze č. 14, znění od 1. 7. 2024, novela 195/2024 Sb.).
Otázka: brát úkony přímo z vyhlášky, nebo aspoň umožnit jejich import jedním kliknutím?

**Rozhodnuto:**
- Šablona úkonů z vyhlášky (tabulka plněná migrací) a import do organizace jedním kliknutím (cena předvyplněná maximem, dál upravitelná). Vlastní (fakultativní) úkony jdou přidávat dál.
- `Task` dostane nullable odkaz na položku vyhlášky: kontrola maximální ceny a přehled úkonů dotčených novelou.
- Existující (ručně zadaný) úkon jde v editaci přiřadit k položce vyhlášky, aby se kontrola a pozdější výkazy týkaly i jich.
- Importovaný úkon dostane krátký hovorový název, který jde dál přejmenovat. Oficiální název z vyhlášky je vždy vidět v detailu úkonu (a menším textem v seznamu).
- Zeptat se koordinátorky: má být na účtence krátký, nebo oficiální název?
- Výkazy po zákonných kategoriích zatím ne. Díky odkazu půjdou přidat později bez změny dat.
- Před implementací ověřit přesné aktuální znění přílohy vyhlášky.

## Stav proti kódu (2026-11-13)

| Bod | Stav | Poznámka |
| --- | --- | --- |
| 1 Obce a DPS | platí | `Department` má jen adresu, obce ani DPS neexistují. `Client.city` je volný text. |
| 2 Číslo střediska | platí | `departmentNumber` se jen ukládá a zobrazuje, nic se z něj neodvozuje. |
| 3.1 Sociální šetření | platí | Nahrávat jde jen fotku (`PictureService`), dokumenty ne. |
| 3.2 Denní záznamy | hotovo | Jsou u IP, nic neměnit. |
| 3.3 Vstup do bytu | platí | Pole ani dokument neexistují. |
| 3.4 Smlouva | platí | Stejné jako 3.1. |
| 3.5 Opatrovník | částečně | Je jen příznak `legallyCompetent`, jméno ani kontakt opatrovníka chybí. |
| 3.6 Osobní číslo při vytváření | platí | Číslo = ID klienta (+1 do volného) až po uložení (`ClientService.saveClient`). |
| 3.7 Koordinátor a IP | platí | `validateIndividualPlanUpdateAccess` a `canEdit` v `IndividualPlan.jsx` pustí koordinátora jen k vlastním klientům. |
| 4.1 Tisk IP | platí | Tisk ani tiskové styly nejsou. |
| 4.2 Telefon v IP | platí | Hlavička IP (`IndividualPlan.jsx:51-58`) telefon nezobrazuje. |
| 4.3 Přepracování IP | budoucí | Bez zadání. |
| 5 Úkony z vyhlášky | rozhodnuto | Neimplementováno. |

## Rozhodnutí (2026-11-13)

Vše jde do jednoho PR (kromě přepracování IP, bod 4.3).

- **1 Ubytování:** druh ubytování (DPS, nájemní byt, obecní byt, rodinný dům, vlastní byt, u příbuzných). DPS jako tabulka pod střediskem, adresa u klienta v DPS nejde změnit. Obce střediska spravuje admin, město je autocomplete s vlastní hodnotou. Dojezdová vzdálenost v km.
- **2 Osobní číslo:** `XXYYYY` (číslo střediska 01–99 + pořadové číslo 4 číslice), uložené jako text. Číslo střediska povinné, 1–99, unikátní v organizaci. Čísla se nepoužívají znovu (ani po ukončených klientech). Předvyplněné ve formuláři po výběru střediska, upravitelné, unikátnost hlídá backend i unikátní index. Při přesunu klienta do jiného střediska se nemění. Stávající klienti se migrací přečíslují.
- **3 Klient:** titul před/za, telefon 2, opatrovník (jméno a kontakt, povinné u nesvéprávného), přístup do bytu (způsob, kód zámku, poznámka), výtah, patro/pokoj, smlouva (péče od/do, smlouva od/do, na dobu (ne)určitou), služba bez úhrad, vstupní šetření (datum), dokumenty klienta (PDF/sken s typem: smlouva, šetření, souhlas s klíči, jiné). Kód zámku nedávat do seznamů, exportů ani tisků.
- **3.6:** vyřešeno předvyplněním osobního čísla (bod 2).
- **3.7:** koordinátor smí upravovat IP všech klientů svého střediska (backend, frontend, testy, upravit `docs/testing/PLAN.md:108`).
- **4.1 Tisk IP:** teď tiskové styly A4 a tlačítko Tisk. PDF až po přepracování IP.
- **4.2:** telefon v hlavičce IP.
- **5:** viz rozhodnutí u bodu 5.
- **Dokumenty klienta:** vidí a spravuje jen admin a koordinátor střediska, pečovatel ne (ani čtení). Ověřit s koordinátorkou.
- **Zeptat se koordinátorky:** rodinný stav, místo narození, číslo rozhodnutí, způsob platby, číslo plátce, zvýhodnění, typ klienta, obecní úřad, štítky, trvalá a předchozí adresa.
- **Nepřidávat:** rodné číslo, datum úmrtí.
