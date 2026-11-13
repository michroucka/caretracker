# Úkony pečovatelské služby podle vyhlášky – návrh k potvrzení

**Stav:** návrh pro Task 12, do migrace se zapíše až po potvrzení.

**Zdroj:** vyhláška č. 505/2006 Sb., § 6 (pečovatelská služba), https://www.zakonyprolidi.cz/cs/2006-505.
Web ukazuje **verzi 17, účinnou od 1. 7. 2026** (nejspíš novela č. 100/2026 Sb., ta podle dalších zdrojů zavedla skupinu h). Koordinátorka uváděla verzi 14 (novela 195/2024 Sb.), ta už neplatí.

**Pozor:** názvy úkonů níže jsou podle struktury § 6, ale nejsou ověřené slovo od slova (stránka vrátila jen shrnutí). Před zápisem do migrace je potřeba zkontrolovat přesné znění.

## Maximální úhrady (§ 6 odst. 2)

| Způsob                                 | Maximum                                             | Platí pro                      |
| -------------------------------------- | --------------------------------------------------- | ------------------------------ |
| za hodinu skutečně spotřebovaného času | 165 Kč (do 80 h měsíčně), 145 Kč (nad 80 h měsíčně) | a, b, c3–c4, d1–d5, e, f, g, h |
| celodenní strava (min. 3 hlavní jídla) | 290 Kč za den                                       | c1                             |
| oběd                                   | 135 Kč                                              | c1                             |
| dovoz nebo donáška jídla               | 65 Kč za úkon                                       | c2                             |
| velký nákup                            | 180 Kč za úkon                                      | d6                             |
| praní a žehlení prádla                 | 110 Kč za kg                                        | d7, d8                         |

## Navržené položky katalogu

`unitType`: HOUR = za hodinu, OCCURRENCE = za úkon, KG = za kg. KM ve vyhlášce není (fakultativní činnost).

| Kód | Krátký název | Název | Jednotka | Max. Kč |
| --- | --- | --- | --- | --- |
| a1 | Podání jídla a pití | Pomoc a podpora při podávání jídla a pití | HOUR | 165 |
| a2 | Doplňky stravy | Pomoc s podáním doplňků stravy nebo potravin pro zvláštní lékařské účely ústy | HOUR | 165 |
| a3 | Oblékání | Pomoc při oblékání a svlékání, obouvání a zouvání, včetně speciálních pomůcek | HOUR | 165 |
| a4 | Pomůcka na končetinu | Pomoc s použitím neinvazivního zdravotnického prostředku podporujícího končetinu | HOUR | 165 |
| a5 | Kompenzační pomůcka | Pomoc s použitím kompenzační pomůcky pro běžné činnosti | HOUR | 165 |
| a6 | Sledování stavu | Základní sledování neinvazivně pozorovatelných tělesných projevů a funkcí | HOUR | 165 |
| a7 | Orientace a pohyb v bytě | Pomoc při prostorové orientaci a samostatném pohybu ve vnitřním prostoru | HOUR | 165 |
| a8 | Přesun a polohování | Pomoc při přesunu na lůžko nebo vozík, vstávání, uléhání, sedání a změně poloh | HOUR | 165 |
| b1 | Osobní hygiena | Pomoc při úkonech osobní hygieny a péči o pokožku bez porušení kůže | HOUR | 165 |
| b2 | Vlasy a nehty | Pomoc při základní péči o vlasy a nehty | HOUR | 165 |
| b3 | Ošetření drobných poranění | Ošetření drobných nekomplikovaných a nechronických poranění kůže | HOUR | 165 |
| b4 | Pomoc na WC | Pomoc při použití WC a související hygieně | HOUR | 165 |
| b5 | Močový katétr | Pomoc s vyprázdněním nebo výměnou sběrného sáčku močového katétru | HOUR | 165 |
| c1-den | Celodenní strava | Zajištění celodenní stravy (min. 3 hlavní jídla) | OCCURRENCE | 290 |
| c1-obed | Oběd | Zajištění oběda | OCCURRENCE | 135 |
| c2 | Dovoz jídla | Dovoz nebo donáška jídla | OCCURRENCE | 65 |
| c3 | Pomoc s přípravou jídla | Pomoc při přípravě jídla a pití | HOUR | 165 |
| c4 | Příprava a podání jídla | Příprava a podání jídla a pití | HOUR | 165 |
| d1 | Běžný úklid | Běžný úklid a údržba domácnosti | HOUR | 165 |
| d2 | Velký úklid | Pomoc při zajištění velkého úklidu (sezónní, po malování) | HOUR | 165 |
| d3 | Donáška vody | Donáška vody | HOUR | 165 |
| d4 | Topení | Topení v kamnech včetně donášky a přípravy topiva, údržba topných zařízení | HOUR | 165 |
| d5 | Běžný nákup a pochůzky | Běžné nákupy a pochůzky | HOUR | 165 |
| d6 | Velký nákup | Velký nákup (týdenní nákup, ošacení, vybavení domácnosti) | OCCURRENCE | 180 |
| d7 | Praní ložního prádla | Praní a žehlení ložního prádla, jeho drobné opravy | KG | 110 |
| d8 | Praní osobního prádla | Praní a žehlení osobního prádla, jeho drobné opravy | KG | 110 |
| e1 | Doprovod dítěte | Doprovázení dětí do školy, školského zařízení, k lékaři a zpět | HOUR | 165 |
| e2 | Doprovod dospělého | Doprovázení dospělých do školy, zaměstnání, k lékaři, na úřady a instituce a zpět | HOUR | 165 |
| f1 | Pomoc s uplatněním práv | Pomoc při komunikaci vedoucí k uplatňování práv a oprávněných zájmů | HOUR | 165 |
| f2 | Vyřizování záležitostí | Pomoc při vyřizování běžných záležitostí | HOUR | 165 |
| g1 | Dohled | Dohled nad osobou, která by mohla ohrozit sebe nebo jiné | HOUR | 165 |
| h1 | Podání léků | Pomoc s podáním humánního léčivého přípravku bez porušení kožní bariéry | HOUR | 165 |
| h2 | Vyprázdnění sáčku vývodu | Pomoc s vyprázdněním sběrného sáčku vývodu močového měchýře nebo tlustého střeva | HOUR | 165 |
| h3 | Výměna sáčku vývodu | Pomoc s výměnou sběrného sáčku vývodu tlustého střeva | HOUR | 165 |

Celkem 34 položek. Krátký název (`shortName`, max. 30 znaků, v seznamu jedinečný) je hovorový název pro výběry a účtenky.

## Pravidlo 80 hodin

§ 6 odst. 2 písm. a), převyprávěno podle zakonyprolidi.cz (doslovné znění nemám, stránka vrátila jen parafrázi a krátké citace):

> Maximální výše úhrady za poskytování pečovatelské služby činí a) podle skutečně spotřebovaného času nezbytného k zajištění úkonů uvedených v odstavci 1 písm. a), b), c) bodech 3 a 4, d) bodech 1 až 5 a písm. e) až h) **165 Kč za hodinu, pokud je služba poskytována v rozsahu nejvýše 80 hodin měsíčně, a 145 Kč za hodinu, pokud je poskytována v rozsahu vyšším**; pokud poskytování těchto úkonů, včetně času nezbytného k jejich zajištění, netrvá celou hodinu, výše úhrady se poměrně krátí.

Doslovně ověřený je jen úvod „Maximální výše úhrady za poskytování pečovatelské služby činí“. Obsahově ověřené: částky 165 a 145 Kč, hranice 80 hodin měsíčně, úkony, na které se pravidlo vztahuje, a poměrné krácení neúplné hodiny.

**Neověřeno** (zkontrolovat v plném znění, případně v metodice MPSV):
- přesné slovní znění zbytku věty (výše je rekonstrukce);
- zda se 80 hodin počítá za jednoho klienta a kalendářní měsíc součtem všech hodinových úkonů (předpoklad: ano);
- zda nad 80 hodin platí 145 Kč za všechny hodiny v měsíci, nebo jen za hodiny nad hranicí (formulace „pokud je poskytována v rozsahu vyšším“ spíš znamená za všechny, ale jisté to není).

## Rozhodnuto (Michal)

1. Hodinové položky mají `maxUnitPrice` = 165.
2. Importuje se všech 31 hodinových úkonů jako samostatné položky.
3. Hranice 80 hodin a maximum 145 Kč nad hranicí půjdou do samostatné tabulky `statutory_hourly_limit`.
4. c1 je rozdělené na celodenní stravu a oběd, protože mají jiné maximum.

## Zbývá

- Michal ověří přesné znění názvů úkonů (hlavně skupiny a, b a g) a krátké názvy. Až potom se píše migrace.
- Výklad neověřených bodů pravidla 80 hodin určí, jak se bude kontrolovat maximum 145 Kč.
