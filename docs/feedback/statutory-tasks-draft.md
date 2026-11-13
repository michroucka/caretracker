# Úkony pečovatelské služby podle vyhlášky – návrh k potvrzení

**Stav:** návrh pro Task 12, do migrace se zapíše až po potvrzení.

**Zdroj:** vyhláška č. 505/2006 Sb., § 6 (pečovatelská služba), https://www.zakonyprolidi.cz/cs/2006-505.
Web ukazuje **verzi 17, účinnou od 1. 7. 2026** (nejspíš novela č. 100/2026 Sb., ta podle dalších zdrojů zavedla skupinu h). Koordinátorka uváděla verzi 14 (novela 195/2024 Sb.), ta už neplatí.

**Pozor:** názvy úkonů níže jsou podle struktury § 6, ale nejsou ověřené slovo od slova (stránka vrátila jen shrnutí). Před zápisem do migrace je potřeba zkontrolovat přesné znění.

## Maximální úhrady (§ 6 odst. 2)

| Způsob | Maximum | Platí pro |
| --- | --- | --- |
| za hodinu skutečně spotřebovaného času | 165 Kč (do 80 h měsíčně), 145 Kč (nad 80 h měsíčně) | a, b, c3–c4, d1–d5, e, f, g, h |
| celodenní strava (min. 3 hlavní jídla) | 290 Kč za den | c1 |
| oběd | 135 Kč | c1 |
| dovoz nebo donáška jídla | 65 Kč za úkon | c2 |
| velký nákup | 180 Kč za úkon | d6 |
| praní a žehlení prádla | 110 Kč za kg | d7, d8 |

## Navržené položky katalogu

`unitType`: HOUR = za hodinu, OCCURRENCE = za úkon, KG = za kg. KM ve vyhlášce není (fakultativní činnost).

| Kód | Název | Jednotka | Max. Kč |
| --- | --- | --- | --- |
| a1 | Pomoc a podpora při podávání jídla a pití | HOUR | 165 |
| a2 | Pomoc s podáním doplňků stravy nebo potravin pro zvláštní lékařské účely ústy | HOUR | 165 |
| a3 | Pomoc při oblékání a svlékání, obouvání a zouvání, včetně speciálních pomůcek | HOUR | 165 |
| a4 | Pomoc s použitím neinvazivního zdravotnického prostředku podporujícího končetinu | HOUR | 165 |
| a5 | Pomoc s použitím kompenzační pomůcky pro běžné činnosti | HOUR | 165 |
| a6 | Základní sledování neinvazivně pozorovatelných tělesných projevů a funkcí | HOUR | 165 |
| a7 | Pomoc při prostorové orientaci a samostatném pohybu ve vnitřním prostoru | HOUR | 165 |
| a8 | Pomoc při přesunu na lůžko nebo vozík, vstávání, uléhání, sedání a změně poloh | HOUR | 165 |
| b1 | Pomoc při úkonech osobní hygieny a péči o pokožku bez porušení kůže | HOUR | 165 |
| b2 | Pomoc při základní péči o vlasy a nehty | HOUR | 165 |
| b3 | Ošetření drobných nekomplikovaných a nechronických poranění kůže | HOUR | 165 |
| b4 | Pomoc při použití WC a související hygieně | HOUR | 165 |
| b5 | Pomoc s vyprázdněním nebo výměnou sběrného sáčku močového katétru | HOUR | 165 |
| c1-den | Zajištění celodenní stravy (min. 3 hlavní jídla) | OCCURRENCE | 290 |
| c1-obed | Zajištění oběda | OCCURRENCE | 135 |
| c2 | Dovoz nebo donáška jídla | OCCURRENCE | 65 |
| c3 | Pomoc při přípravě jídla a pití | HOUR | 165 |
| c4 | Příprava a podání jídla a pití | HOUR | 165 |
| d1 | Běžný úklid a údržba domácnosti | HOUR | 165 |
| d2 | Pomoc při zajištění velkého úklidu (sezónní, po malování) | HOUR | 165 |
| d3 | Donáška vody | HOUR | 165 |
| d4 | Topení v kamnech včetně donášky a přípravy topiva, údržba topných zařízení | HOUR | 165 |
| d5 | Běžné nákupy a pochůzky | HOUR | 165 |
| d6 | Velký nákup (týdenní nákup, ošacení, vybavení domácnosti) | OCCURRENCE | 180 |
| d7 | Praní a žehlení ložního prádla, jeho drobné opravy | KG | 110 |
| d8 | Praní a žehlení osobního prádla, jeho drobné opravy | KG | 110 |
| e1 | Doprovázení dětí do školy, školského zařízení, k lékaři a zpět | HOUR | 165 |
| e2 | Doprovázení dospělých do školy, zaměstnání, k lékaři, na úřady a instituce a zpět | HOUR | 165 |
| f1 | Pomoc při komunikaci vedoucí k uplatňování práv a oprávněných zájmů | HOUR | 165 |
| f2 | Pomoc při vyřizování běžných záležitostí | HOUR | 165 |
| g1 | Dohled nad osobou, která by mohla ohrozit sebe nebo jiné | HOUR | 165 |
| h1 | Pomoc s podáním humánního léčivého přípravku bez porušení kožní bariéry | HOUR | 165 |
| h2 | Pomoc s vyprázdněním sběrného sáčku vývodu močového měchýře nebo tlustého střeva | HOUR | 165 |
| h3 | Pomoc s výměnou sběrného sáčku vývodu tlustého střeva | HOUR | 165 |

Celkem 34 položek.

## K rozhodnutí

1. **Dvě hodinové sazby (165 / 145 Kč):** maximum závisí na počtu hodin klienta za měsíc, katalog umí jedno maximum na položku. Návrh: kontrolovat proti 165 Kč a 145 Kč zatím neřešit (týká se jen klientů nad 80 h měsíčně).
2. **Jedna položka za hodinu, nebo 31 samostatných?** Organizace často účtují hodinové úkony jako jednu položku („péče za hodinu“). Import všech 31 hodinových úkonů může katalog zahltit. Alternativa: importovat jen skupiny (a, b, …), nebo nechat admina vybrat, co importovat.
3. **c1 rozdělené na celodenní stravu a oběd**, protože mají jiné maximum.
4. **Přesné znění názvů** ověřit proti vyhlášce, hlavně skupiny a, b a g.
