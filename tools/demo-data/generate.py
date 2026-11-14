#!/usr/bin/env python3
"""Generates the Czech demo data of CareTracker.

Writes backend/src/main/resources/demo-data/data.sql (loaded by DemoDataSeeder in the demo and dev profiles)
and tools/demo-data/portraits.json (FLUX prompts and seeds for the portraits that the seeder imports from
backend/src/main/resources/demo-data/portraits/{clients,employees}/<key>.jpg).

The random seed is fixed, so running it again gives the same output. Dates the application compares with today
(care, contracts, plans, records) are written relative to CURRENT_DATE, so the demo stays current after a reset.

Usage: python3 tools/demo-data/generate.py
"""

import json
import random
import unicodedata
from datetime import date
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DATA_SQL = ROOT / "backend/src/main/resources/demo-data/data.sql"
PORTRAITS_JSON = ROOT / "tools/demo-data/portraits.json"

# Birth dates are absolute, ages are counted to this day.
REFERENCE_DATE = date(2026, 11, 14)
# Same bcrypt hash of "heslo" as the original demo accounts.
PASSWORD_HASH = "$2a$12$ergHTn/GtOAI68bbRoBTW.DZEIfkN7ZbXuRhvbOD/WVsHRGIva0ES"

rng = random.Random(20261114)


class Raw(str):
    """SQL expression written as is."""


def days(offset):
    """Date relative to today: negative in the past, positive in the future."""
    if offset == 0:
        return Raw("CURRENT_DATE")
    return Raw(f"CURRENT_DATE {'-' if offset < 0 else '+'} {abs(offset)}")


def sql(value):
    if value is None:
        return "NULL"
    if isinstance(value, Raw):
        return str(value)
    if isinstance(value, bool):
        return "true" if value else "false"
    if isinstance(value, (int, float)):
        return str(value)
    if isinstance(value, date):
        return f"'{value.isoformat()}'"
    return "'" + str(value).replace("'", "''") + "'"


def insert(table, rows):
    if not rows:
        return ""
    columns = list(rows[0].keys())
    values = ",\n".join("    (" + ", ".join(sql(row[column]) for column in columns) + ")" for row in rows)
    return f"INSERT INTO {table} ({', '.join(columns)}) VALUES\n{values};\n\n"


def ascii_slug(text):
    normalized = unicodedata.normalize("NFKD", text)
    return "".join(c for c in normalized if not unicodedata.combining(c)).lower().replace(" ", "")


def phone():
    first = rng.choice("67")
    digits = f"{first}{rng.randint(0, 99):02d}{rng.randint(0, 999):03d}{rng.randint(0, 999):03d}"
    if first == "6":
        digits = "60" + digits[2:]
    return f"+420 {digits[0:3]} {digits[3:6]} {digits[6:9]}"


def pick(weighted):
    items, weights = zip(*weighted)
    return rng.choices(items, weights=weights, k=1)[0]


def g(gender, female, male):
    return female if gender == "FEMALE" else male


# --- Names -------------------------------------------------------------------------------------------------------

SURNAMES = [
    ("Novák", "Nováková"), ("Svoboda", "Svobodová"), ("Novotný", "Novotná"), ("Dvořák", "Dvořáková"),
    ("Černý", "Černá"), ("Procházka", "Procházková"), ("Kučera", "Kučerová"), ("Veselý", "Veselá"),
    ("Horák", "Horáková"), ("Němec", "Němcová"), ("Pokorný", "Pokorná"), ("Marek", "Marková"),
    ("Pospíšil", "Pospíšilová"), ("Hájek", "Hájková"), ("Jelínek", "Jelínková"), ("Král", "Králová"),
    ("Růžička", "Růžičková"), ("Beneš", "Benešová"), ("Fiala", "Fialová"), ("Sedláček", "Sedláčková"),
    ("Doležal", "Doležalová"), ("Zeman", "Zemanová"), ("Kolář", "Kolářová"), ("Navrátil", "Navrátilová"),
    ("Čermák", "Čermáková"), ("Vaněk", "Vaňková"), ("Urban", "Urbanová"), ("Blažek", "Blažková"),
    ("Kříž", "Křížová"), ("Kovář", "Kovářová"), ("Bartoš", "Bartošová"), ("Vlček", "Vlčková"),
    ("Polák", "Poláková"), ("Kopecký", "Kopecká"), ("Musil", "Musilová"), ("Šimek", "Šimková"),
    ("Konečný", "Konečná"), ("Malý", "Malá"), ("Holub", "Holubová"), ("Štěpánek", "Štěpánková"),
    ("Kadlec", "Kadlecová"), ("Hrubý", "Hrubá"), ("Soukup", "Soukupová"), ("Bureš", "Burešová"),
    ("Toman", "Tomanová"), ("Říha", "Říhová"), ("Mach", "Machová"), ("Kratochvíl", "Kratochvílová"),
    ("Sýkora", "Sýkorová"), ("Šťastný", "Šťastná"), ("Vávra", "Vávrová"), ("Matějka", "Matějková"),
    ("Šebek", "Šebková"), ("Hořejší", "Hořejší"), ("Kubát", "Kubátová"), ("Mareš", "Marešová"),
    ("Brabec", "Brabcová"), ("Hanzlík", "Hanzlíková"), ("Kraus", "Krausová"), ("Wagner", "Wagnerová"),
    ("Šindelář", "Šindelářová"), ("Petrák", "Petráková"), ("Krejčí", "Krejčí"), ("Dušek", "Dušková"),
    ("Hrdlička", "Hrdličková"), ("Lukeš", "Lukešová"), ("Bláha", "Bláhová"), ("Hudec", "Hudcová"),
    ("Moravec", "Moravcová"), ("Šulc", "Šulcová"), ("Tůma", "Tůmová"), ("Vacek", "Vacková"),
    ("Janda", "Jandová"), ("Slabý", "Slabá"), ("Vrba", "Vrbová"), ("Pešek", "Pešková"),
    ("Rataj", "Ratajová"), ("Zíka", "Zíková"), ("Řezáč", "Řezáčová"), ("Kočí", "Kočí"),
    ("Vachta", "Vachtová"), ("Kubíček", "Kubíčková"), ("Hošek", "Hošková"), ("Kunc", "Kuncová"),
]

SENIOR_FEMALE = [
    "Marie", "Jaroslava", "Božena", "Anna", "Jiřina", "Věra", "Zdeňka", "Ludmila", "Milada", "Vlasta",
    "Libuše", "Helena", "Hana", "Eva", "Jana", "Alena", "Miroslava", "Jarmila", "Květoslava", "Drahomíra",
    "Blažena", "Růžena", "Emilie", "Olga", "Dagmar", "Irena", "Vladimíra", "Stanislava", "Marta", "Milena",
    "Jindřiška", "Naděžda", "Bohumila", "Zdenka", "Františka",
]
SENIOR_MALE = [
    "Josef", "Václav", "Jiří", "Jan", "Karel", "Jaroslav", "František", "Miroslav", "Zdeněk", "Vladimír",
    "Antonín", "Stanislav", "Milan", "Ladislav", "Bohumil", "Oldřich", "Vlastimil", "Bohuslav", "Jindřich",
    "Pavel", "Rudolf", "Emil", "Alois", "Vojtěch", "Otakar", "Jaromír", "Miloslav",
]
STAFF_FEMALE = [
    "Jana", "Petra", "Lenka", "Martina", "Kateřina", "Lucie", "Monika", "Michaela", "Veronika", "Hana",
    "Ivana", "Markéta", "Tereza", "Zuzana", "Andrea", "Pavla", "Eva", "Radka", "Simona", "Šárka",
    "Romana", "Gabriela", "Blanka", "Iveta", "Dana", "Renata", "Alena", "Jitka", "Klára", "Barbora",
]
STAFF_MALE = ["Tomáš", "Martin", "Petr", "Jakub", "Lukáš", "Michal"]
RELATIVE_FEMALE = ["Jana", "Petra", "Ivana", "Hana", "Lenka", "Dana", "Eva", "Martina", "Alena", "Romana"]
RELATIVE_MALE = ["Petr", "Pavel", "Jiří", "Martin", "Tomáš", "Jan", "Miroslav", "Roman", "David", "Michal"]

used_full_names = set()


def unique_name(first_names, gender):
    while True:
        first = rng.choice(first_names)
        male, female = rng.choice(SURNAMES)
        last = female if gender == "FEMALE" else male
        if (first, last) not in used_full_names:
            used_full_names.add((first, last))
            return first, last, (male, female)


def relative(client_surnames):
    """A son keeps the surname, a daughter often has her husband's."""
    if rng.random() < 0.5:
        return "syn", rng.choice(RELATIVE_MALE), client_surnames[0]
    surname = client_surnames[1] if rng.random() < 0.35 else rng.choice(SURNAMES)[1]
    return "dcera", rng.choice(RELATIVE_FEMALE), surname


# --- Places ------------------------------------------------------------------------------------------------------

# (name, postal code, has streets); villages without streets use "<village> <house number>"
DEPARTMENTS = [
    {
        "id": 1, "number": 1, "name": "Kralovicko", "city": "Kralovice", "street": "Plzeňská 154", "postal": "331 41",
        "clients": 110, "caregivers": 14,
        "municipalities": [("Kralovice", "331 41", True, 40), ("Hadačka", "331 41", False, 5), ("Výrov", "331 41", False, 6),
                           ("Kozojedy", "331 41", False, 10), ("Bohy", "331 41", False, 8), ("Kozlany", "331 44", True, 22),
                           ("Mladotice", "331 41", False, 8)],
        "outside": [("Čistá", "270 34", 11), ("Jesenice", "270 33", 14)],
        "housing": [(1, "DPS Kralovice", "Jungmannova 640", "Kralovice", "331 41", 9),
                    (2, "DPS Kozlany", "Školní 330", "Kozlany", "331 44", 6),
                    (3, "DPS Bohy", "Bohy 45", "Bohy", "331 41", 3)],
    },
    {
        "id": 2, "number": 2, "name": "Plasko", "city": "Plasy", "street": "Nádražní 315", "postal": "331 01",
        "clients": 80, "caregivers": 11,
        "municipalities": [("Plasy", "331 01", True, 45), ("Babina", "331 01", False, 7), ("Žebnice", "331 01", False, 9),
                           ("Lomnička", "331 01", False, 8), ("Horní Hradiště", "331 01", False, 11)],
        "outside": [("Kaznějov", "331 51", 8)],
        "housing": [(4, "DPS Plasy", "Na Výsluní 401", "Plasy", "331 01", 10)],
    },
    {
        "id": 3, "number": 3, "name": "Manětínsko", "city": "Manětín", "street": "Manětín 86", "postal": "331 62",
        "clients": 60, "caregivers": 9,
        "municipalities": [("Manětín", "331 62", False, 30), ("Brdo", "331 62", False, 8), ("Hrádek", "331 62", False, 8),
                           ("Lipí", "331 62", False, 7), ("Stvolny", "331 62", False, 7)],
        "outside": [("Nečtiny", "331 63", 9)],
        "housing": [],
    },
    {
        "id": 4, "number": 4, "name": "Třemošensko", "city": "Třemošná", "street": "Plzeňská 260", "postal": "330 11",
        "clients": 90, "caregivers": 14,
        "municipalities": [("Třemošná", "330 11", True, 45), ("Ledce", "330 14", False, 12), ("Zruč-Senec", "330 08", True, 22),
                           ("Dolní Bělá", "330 11", False, 11)],
        "outside": [("Chotíkov", "330 17", 7), ("Kaznějov", "331 51", 10)],
        "housing": [(5, "DPS Třemošná", "Školní 1032", "Třemošná", "330 11", 12)],
    },
]
SECOND_DEPARTMENT = {
    "id": 5, "number": 1, "city": "Rokycany", "street": "Školní 422", "postal": "337 01",
    "municipalities": [("Rokycany", "337 01", True, 6), ("Kamenný Újezd", "337 01", False, 1), ("Mýto", "338 05", True, 1)],
}

TOWNS = ("Kralovice", "Kozlany", "Plasy", "Manětín", "Třemošná", "Rokycany", "Mýto")

STREETS = [
    "Školní", "Nádražní", "Husova", "Plzeňská", "Na Výsluní", "Komenského", "Sokolská", "Zahradní", "Polní",
    "Krátká", "Lipová", "Družstevní", "Palackého", "Masarykova", "Tyršova", "Luční", "Jiráskova", "Nerudova",
    "Smetanova", "Pod Lesem", "K Rybníku", "Havlíčkova", "Za Humny", "Okružní", "Míru",
]

# --- Tasks -------------------------------------------------------------------------------------------------------

# statutory catalog from V35__statutory_task.sql: code -> (short name, unit type, max price)
STATUTORY = {
    "a1": ("Podání jídla a pití", "HOUR", 165), "a2": ("Doplňky stravy", "HOUR", 165),
    "a3": ("Oblékání", "HOUR", 165), "a4": ("Pomůcka na končetinu", "HOUR", 165),
    "a5": ("Kompenzační pomůcka", "HOUR", 165), "a6": ("Sledování stavu", "HOUR", 165),
    "a7": ("Orientace a pohyb v bytě", "HOUR", 165), "a8": ("Přesun a polohování", "HOUR", 165),
    "b1": ("Osobní hygiena", "HOUR", 165), "b2": ("Vlasy a nehty", "HOUR", 165),
    "b3": ("Ošetření drobných poranění", "HOUR", 165), "b4": ("Pomoc na WC", "HOUR", 165),
    "b5": ("Močový katétr", "HOUR", 165), "c1-obed": ("Oběd", "OCCURRENCE", 135),
    "c2": ("Dovoz jídla", "OCCURRENCE", 65), "c3": ("Pomoc s přípravou jídla", "HOUR", 165),
    "c4": ("Příprava a podání jídla", "HOUR", 165), "d1": ("Běžný úklid", "HOUR", 165),
    "d2": ("Velký úklid", "HOUR", 165), "d3": ("Donáška vody", "HOUR", 165), "d4": ("Topení", "HOUR", 165),
    "d5": ("Běžný nákup a pochůzky", "HOUR", 165), "d6": ("Velký nákup", "OCCURRENCE", 180),
    "d7": ("Praní ložního prádla", "KG", 110), "d8": ("Praní osobního prádla", "KG", 110),
    "e1": ("Doprovod dítěte", "HOUR", 165), "e2": ("Doprovod dospělého", "HOUR", 165),
    "f1": ("Pomoc s uplatněním práv", "HOUR", 165), "f2": ("Vyřizování záležitostí", "HOUR", 165),
    "g1": ("Dohled", "HOUR", 165), "h1": ("Podání léků", "HOUR", 165),
    "h2": ("Vyprázdnění sáčku vývodu", "HOUR", 165), "h3": ("Výměna sáčku vývodu", "HOUR", 165),
}
MAIN_TASK_CODES = [code for code, (_, unit, _) in STATUTORY.items() if unit == "HOUR"] + ["c1-obed", "c2", "d6", "d7", "d8"]
OPTIONAL_TASKS = [("Doprava autem", "KM", 12), ("Pedikúra", "OCCURRENCE", 150)]
SECOND_ORG_TASKS = [("b1", 150), ("c1-obed", 120), ("c2", 40), ("d1", 150), ("d5", 150), ("d8", 90)]

# --- Text pools --------------------------------------------------------------------------------------------------

JOBS_F = ["prodavačka v Jednotě", "účetní v JZD", "učitelka v mateřské škole", "zdravotní sestra v nemocnici v Plzni",
          "dojička v JZD", "švadlena", "úřednice na poště", "kuchařka ve školní jídelně", "dělnice v Škodovce",
          "laborantka", "knihovnice"]
JOBS_M = ["traktorista v JZD", "zámečník ve Škodovce", "řidič autobusu ČSAD", "strojvedoucí", "zedník", "lesník",
          "elektrikář", "učitel na základní škole", "automechanik", "horník", "truhlář"]
CHILDREN = ["dceru a syna", "dvě dcery", "jednoho syna", "tři děti", "dva syny", "jednu dceru"]
LIKES = [
    "Poslech dechovky a lidovek v rádiu", "Káva s mlékem a domácí buchty", "Křížovky a osmisměrky",
    "Zahrádka, v létě pěstuje rajčata", "Seriály a zprávy v televizi", "Procházky ke kapličce a na hřbitov",
    "Návštěvy vnoučat", "Kočka, kterou má doma", "Čtení regionálních novin", "Vzpomínky na houbaření",
    "Fotografie z mládí a rodinná alba", "Teplý čaj s medem",
]
LIKES_F = ["Pletení a háčkování", "Pečení koláčů"]
LIKES_M = ["Fotbal v televizi", "Rybaření a vyprávění o něm", "Hra v karty se sousedem"]
DISLIKES = [
    "Spěch a změny v domluveném čase", "Hlasitou televizi a hluk", "Studené jídlo", "Sprchování večer",
    "Mléčnou rýži", "Telefonáty s neznámými lidmi", "Cigaretový kouř", "Chodit ven za deště",
]
DISLIKES_GENDERED = [("Když jí někdo přerovnává věci ve skříni", "Když mu někdo přerovnává věci ve skříni"),
                     ("Když s ní někdo mluví jako s dítětem", "Když s ním někdo mluví jako s dítětem")]
STRENGTHS = [
    ("Je trpělivá a vstřícná", "Je trpělivý a vstřícný"), ("Ráda pomáhá s vařením", "Rád opravuje drobnosti v domě"),
    ("Najíst se zvládá sama", "Najíst se zvládá sám"), ("Má smysl pro humor", "Má smysl pro humor"),
    ("Pravidelně cvičí na lůžku", "Pravidelně cvičí na lůžku"),
    ("Dobře spolupracuje s pečovatelkami", "Dobře spolupracuje s pečovatelkami"),
    ("Má dobrou paměť na rodinné příběhy", "Má dobrou paměť na rodinné příběhy"),
]
ASPIRATIONS = [
    ("Zůstat co nejdéle doma", "Zůstat co nejdéle doma"),
    ("Udržet si samostatnost při hygieně", "Udržet si samostatnost při hygieně"),
    ("Chodit sama na krátké procházky", "Chodit sám na krátké procházky"),
    ("Být v kontaktu s rodinou a sousedy", "Být v kontaktu s rodinou a sousedy"),
    ("V létě jet na chalupu k dceři", "V létě jet na chalupu k synovi"),
    ("Zlepšit chůzi po operaci kyčle", "Zlepšit chůzi po operaci kyčle"),
    ("Brát léky pravidelně a vyznat se v nich", "Brát léky pravidelně a vyznat se v nich"),
]
ADDITIONAL_INFO = [
    "Má doma psa, při návštěvě ho zavřít v kuchyni.", "Špatně slyší, mluvit pomalu a zřetelně.",
    "Náhradní klíče má sousedka z přízemí.", "Nekouří, v bytě se nesmí kouřit.", None, None,
]
HYGIENE = [
    ("Sprchování 2× týdně s dopomocí, jinak hygienu u umyvadla zvládá sama.",
     "Sprchování 2× týdně s dopomocí, jinak hygienu u umyvadla zvládá sám."),
    ("Celková hygiena na lůžku, intimní hygiena při každé výměně inkontinenčních pomůcek.",
     "Celková hygiena na lůžku, intimní hygiena při každé výměně inkontinenčních pomůcek."),
    ("Hygienu zvládá sama, ve sprše potřebuje dohled kvůli riziku pádu.",
     "Hygienu zvládá sám, ve sprše potřebuje dohled kvůli riziku pádu."),
]
SELF_CARE = [
    ("Obléká se sama, pomoc s ponožkami a obuví.", "Obléká se sám, pomoc s ponožkami a obuví."),
    ("Po CMP potřebuje pomoc s oblékáním horní poloviny těla.", "Po CMP potřebuje pomoc s oblékáním horní poloviny těla."),
    ("Zvládá, je potřeba připomínat výměnu oblečení.", "Zvládá, je potřeba připomínat výměnu oblečení."),
]
MOBILITY = [
    "Po bytě chodí s chodítkem, venku jezdí na vozíku.", "Chodí o holi, schody zvládá s oporou zábradlí.",
    "Imobilní, polohování po 2 hodinách, přesun na vozík s pomocí.", "Chodí bez pomůcek, pomaleji, hrozí pád.",
]
DIET = [
    "Diabetická dieta, obědy dovážené pečovatelskou službou.", "Šetřící strava, málo pije, připomínat pitný režim.",
    ("Bez omezení, má ráda sladké.", "Bez omezení, má rád sladké."), "Mletá strava kvůli potížím s polykáním.",
]
HOME_CARE = [
    "Běžný úklid 1× týdně, praní prádla zajišťuje pečovatelská služba.",
    "Domácnost zvládá s pomocí dcery, služba zajišťuje velký nákup.",
    "Úklid kuchyně a koupelny při každé návštěvě, velký úklid 2× ročně.",
]
SOCIAL_CONTACT = [
    "Dcera dojíždí o víkendech, syn telefonuje denně.", ("Žije sama, sousedka se zastavuje každý den.", "Žije sám, soused se zastavuje každý den."),
    ("Chodí do klubu seniorů, ráda si povídá.", "Chodí do klubu seniorů, rád si povídá."), "Rodina bydlí v Plzni, jezdí jednou za čtrnáct dní.",
]
ACTIVITIES = [
    "Křížovky, rádio, televize.", "V létě práce na zahrádce.", "Knihy z místní knihovny, pečovatelka je vyměňuje.",
    "Pletení pro vnoučata.", "Procházka kolem domu, když je hezky.",
]
HEALTH = [
    "Diabetes 2. typu, vysoký tlak. Léky v dávkovači připravuje rodina.", "Stav po CMP, slabost levé strany.",
    "Artróza kolen, špatně slyší.", "Lehká demence, sledovat orientaci a pitný režim.",
    "Stav po operaci kyčle, cvičí podle rehabilitační sestry.", "Srdeční selhání, otoky dolních končetin, sledovat váhu.",
]
RIGHTS = [
    ("S penězi hospodaří sama, na úřadech pomáhá syn.", "S penězi hospodaří sám, na úřadech pomáhá syn."),
    "Potřebuje pomoc při vyřizování příspěvku na péči a dokladů.", "Své záležitosti vyřizuje samostatně.",
]
DAILY_RECORDS = [
    ("Ranní hygiena proběhla bez obtíží, paní {L} spolupracovala a byla v dobré náladě.",
     "Ranní hygiena proběhla bez obtíží, pan {L} spolupracoval a byl v dobré náladě."),
    ("Klientka si stěžovala na bolest kolene, doporučeno kontaktovat praktického lékaře. Informována rodina.",
     "Klient si stěžoval na bolest kolene, doporučeno kontaktovat praktického lékaře. Informována rodina."),
    ("Nákupní seznam připraven s klientkou, nákup doručen, vyúčtování předáno.",
     "Nákupní seznam připraven s klientem, nákup doručen, vyúčtování předáno."),
    ("Paní {L} dnes snědla celý oběd, vypila asi litr a půl tekutin.",
     "Pan {L} dnes snědl celý oběd, vypil asi litr a půl tekutin."),
    ("Klientka byla unavená, v noci špatně spala. Hygiena provedena na lůžku.",
     "Klient byl unavený, v noci špatně spal. Hygiena provedena na lůžku."),
    "Procházka kolem domu asi 20 minut s chodítkem, bez pádu.",
    "Léky podány podle dávkovače, ranní dávka byla užita správně.",
    ("Úklid kuchyně a koupelny, vyneseny odpadky. Paní {L} pomáhala s utíráním nádobí.",
     "Úklid kuchyně a koupelny, vyneseny odpadky. Pan {L} pomáhal s utíráním nádobí."),
    ("Klientka si přála jít na hřbitov, doprovod proběhl v klidu.", "Klient si přál jít na hřbitov, doprovod proběhl v klidu."),
    ("Prohlížení fotografií z mládí, klientka vzpomínala na práci a na rodinu.",
     "Prohlížení fotografií z mládí, klient vzpomínal na práci a na rodinu."),
    "Kontrola kůže bez otlaků, nohy namazány krémem.",
    ("Paní {L} odmítla sprchu, hygiena provedena u umyvadla.", "Pan {L} odmítl sprchu, hygiena provedena u umyvadla."),
    "Prádlo z prádelny přineseno a uloženo do skříně.",
    ("Klientka měla návštěvu vnučky, náladu měla velmi dobrou.", "Klient měl návštěvu vnuka, náladu měl velmi dobrou."),
    "Mírně zvýšený tlak 150/90, informována koordinátorka a rodina.",
    ("Společně uvařená polévka, klientka krájela zeleninu u stolu.", "Společně uvařená polévka, klient krájel zeleninu u stolu."),
    "Vyřízena objednávka léků v lékárně, recepty vyzvednuty u lékaře.",
    ("Klientka dnes působila zmateně, nevěděla, jaký je den. Sledujeme.",
     "Klient dnes působil zmateně, nevěděl, jaký je den. Sledujeme."),
    ("Křížovky a poslech rádia, klientka byla spokojená.", "Křížovky a poslech rádia, klient byl spokojený."),
    "Výměna ložního prádla, vyvětráno, upraveno lůžko.",
]
CLIENT_NOTES = [
    "Špatně slyší, mluvit nahlas a zřetelně.", "Diabetik, sladkosti nenakupovat.", "Alergie na penicilin.",
    "Při návštěvě nejdřív zazvonit dvakrát, otevírá pomalu.", "V bytě je pes, je hodný.",
    "Preferuje pečovatelku, ne pečovatele.", "Obědy jen ve všední dny, o víkendu vaří syn.",
]
ACCESS_NOTES = {
    "DOORBELL": ["Zvonek u branky, jméno na schránce.", "Zvonit dlouze, klientka špatně slyší."],
    "KEYS": ["Klíče v bezpečnostní schránce u vchodu.", "Klíče převzaty na základě souhlasu, uloženy na středisku."],
    "PHONE_CALL": ["Před příchodem zavolat, otevře dcera.", "Zavolat z ulice, klient sejde dolů otevřít."],
    "OTHER": ["Vchod ze dvora, branka bývá zavřená na háček.", "Klíč pod rohožkou u sousedky, domluveno s rodinou."],
}
DOCTORS = [
    ("MUDr. Jana Horáková", "Kralovice", 1), ("MUDr. Petr Vojta", "Kralovice", 1), ("MUDr. Eva Škodová", "Kozlany", 1),
    ("MUDr. Martin Kubeš", "Plasy", 2), ("MUDr. Lenka Fialová", "Plasy", 2), ("MUDr. Tomáš Rybák", "Manětín", 3),
    ("MUDr. Hana Benešová", "Třemošná", 4), ("MUDr. Karel Petrák", "Třemošná", 4), ("MUDr. Alena Krausová", "Rokycany", 5),
]
TERMINATIONS = [("EXITUS", 12), ("RESIDENTIAL_SERVICE", 8), ("CONTRACT_TERMINATION", 4), ("MUTUAL_AGREEMENT", 3), ("OTHER", 3)]
TERMINATION_NOTES = {
    "RESIDENTIAL_SERVICE": "Přechod do domova pro seniory.",
    "CONTRACT_TERMINATION": "Smlouvu vypověděla rodina, péči zajistí sama.",
    "MUTUAL_AGREEMENT": "Ukončeno dohodou, služba už není potřeba.",
    "OTHER": "Přestěhování k rodině mimo region.",
}

TASK_COMBOS = [
    (["Oběd", "Dovoz jídla"], 32),
    (["Oběd", "Dovoz jídla", "Běžný nákup a pochůzky"], 12),
    (["Osobní hygiena", "Oblékání"], 14),
    (["Osobní hygiena", "Oblékání", "Běžný úklid"], 12),
    (["Běžný úklid", "Běžný nákup a pochůzky", "Praní osobního prádla"], 12),
    (["Oběd", "Dovoz jídla", "Osobní hygiena", "Podání léků"], 8),
    (["Běžný nákup a pochůzky", "Velký nákup", "Doprovod dospělého", "Doprava autem"], 6),
    (["Osobní hygiena", "Pomoc na WC", "Příprava a podání jídla", "Sledování stavu"], 4),
]
INTENSIVE_COMBO = ["Osobní hygiena", "Oblékání", "Přesun a polohování", "Podání jídla a pití", "Podání léků", "Dohled", "Pomoc na WC"]

# --- Portraits ---------------------------------------------------------------------------------------------------

HAIR_SENIOR_F = ["short curly white hair", "short grey hair with a soft perm", "silver hair pinned in a low bun",
                 "short straight grey hair", "thin white hair combed back", "chin-length light grey hair"]
HAIR_SENIOR_M = ["bald with a white fringe of hair", "short grey hair combed to the side", "thinning white hair",
                 "receding grey hair and a grey moustache", "short white hair and a neatly trimmed white beard",
                 "grey crew cut"]
CLOTHES_SENIOR_F = ["a beige knitted cardigan", "a floral blouse", "a lilac wool sweater", "a navy cardigan over a white blouse",
                    "a green knitted vest over a patterned blouse", "a dark red cardigan"]
CLOTHES_SENIOR_M = ["a checked flannel shirt", "a grey knitted vest over a light shirt", "a brown cardigan",
                    "a blue sweater over a collared shirt", "a dark jacket over a striped shirt", "a plain green fleece"]
HAIR_STAFF_F = ["shoulder-length brown hair tied in a ponytail", "short dark blonde hair", "long dark hair in a braid",
                "shoulder-length auburn hair", "short light brown bob", "curly brown hair tied back"]
HAIR_STAFF_M = ["short brown hair", "short dark hair and light stubble", "buzz cut", "short blond hair"]
CLOTHES_CAREGIVER = ["a light blue care worker uniform tunic", "a white care uniform with a light blue collar",
                     "a navy care worker polo shirt", "a mint green care uniform tunic"]
CLOTHES_OFFICE = ["a white blouse", "a navy blouse with a name badge", "a light grey blouse and a dark cardigan"]
CLOTHES_OFFICE_M = ["a light blue shirt", "a navy polo shirt with a name badge"]
GLASSES = ["wearing glasses and ", "wearing thin metal-framed glasses and ", "wearing large tortoiseshell glasses and ", ""]
EXPRESSIONS_SENIOR = ["gentle smile", "warm smile", "calm neutral expression", "slight smile", "kind smile with crinkled eyes"]
EXPRESSIONS_STAFF = ["friendly smile", "warm smile", "confident slight smile", "relaxed smile"]


def portrait_prompt(seed, age, gender, staff_clothes=None):
    choose = random.Random(seed)
    female = gender == "FEMALE"
    if staff_clothes is None:
        hair = choose.choice(HAIR_SENIOR_F if female else HAIR_SENIOR_M)
        clothes = choose.choice(CLOTHES_SENIOR_F if female else CLOTHES_SENIOR_M)
        glasses = choose.choice(GLASSES[:3] + ["", ""]) if age > 75 else choose.choice(GLASSES)
        expression = choose.choice(EXPRESSIONS_SENIOR)
        skin = "natural skin texture with wrinkles"
    else:
        hair = choose.choice(HAIR_STAFF_F if female else HAIR_STAFF_M)
        clothes = choose.choice(staff_clothes)
        glasses = choose.choice(["", "", "wearing glasses and "])
        expression = choose.choice(EXPRESSIONS_STAFF)
        skin = "natural skin texture"
    article = "an" if str(age)[0] == "8" or age in (11, 18) else "a"
    person = "woman" if female else "man"
    return (f"Head and shoulders portrait photograph of {article} {age}-year-old Czech {person}, {hair}, "
            f"{glasses or 'wearing '}{clothes}, {expression}, looking at the camera, plain light grey background, "
            f"soft even daylight, {skin}, realistic photo, sharp focus")


# --- Generation --------------------------------------------------------------------------------------------------

def age_on_reference(birth):
    return REFERENCE_DATE.year - birth.year - ((REFERENCE_DATE.month, REFERENCE_DATE.day) < (birth.month, birth.day))


def random_birth(min_age, max_age):
    age = rng.randint(min_age, max_age)
    year = REFERENCE_DATE.year - age - 1
    return date(year, rng.randint(1, 12), rng.randint(1, 28)) if rng.random() < 0.5 else date(year + 1, rng.randint(1, 10), rng.randint(1, 28))


def address(municipality, has_streets):
    if has_streets:
        return f"{rng.choice(STREETS)} {rng.randint(1, 980)}"
    return f"{municipality} {rng.randint(1, 260)}"


def build_employees():
    employees = []
    next_id = 1

    def add(first_names, gender, role, department, age_range):
        nonlocal next_id
        first, last, _ = unique_name(first_names, gender)
        employee = {"id": next_id, "first_name": first, "last_name": last, "gender": gender, "role": role,
                    "department_id": department, "organization_id": 1 if department <= 4 else 2,
                    "age": rng.randint(*age_range)}
        employees.append(employee)
        next_id += 1
        return employee

    # fixed ids for the demo accounts: 1 admin (head and coordinator of 02), 2 coordinator of 01, 3 caregiver of 01
    add(STAFF_FEMALE, "FEMALE", "MANAGER", 2, (45, 56))
    add(STAFF_FEMALE, "FEMALE", "COORDINATOR", 1, (34, 50))
    add(STAFF_FEMALE, "FEMALE", "CAREGIVER", 1, (28, 52))
    add(STAFF_FEMALE, "FEMALE", "COORDINATOR", 3, (32, 55))
    add(STAFF_FEMALE, "FEMALE", "COORDINATOR", 4, (30, 48))
    for department in DEPARTMENTS:
        already = 1 if department["id"] == 1 else 0
        for _ in range(department["caregivers"] - already):
            gender = "MALE" if rng.random() < 0.1 else "FEMALE"
            add(STAFF_FEMALE if gender == "FEMALE" else STAFF_MALE, gender, "CAREGIVER", department["id"], (23, 61))
    second = {
        "manager": add(STAFF_FEMALE, "FEMALE", "MANAGER", 5, (40, 58)),
        "coordinator": add(STAFF_MALE, "MALE", "COORDINATOR", 5, (30, 50)),
        "caregivers": [add(STAFF_FEMALE, "FEMALE", "CAREGIVER", 5, (25, 55)) for _ in range(2)],
    }
    return employees, second


def build_clients(employees):
    clients = []
    caregivers_by_department = {}
    for employee in employees:
        if employee["role"] == "CAREGIVER":
            caregivers_by_department.setdefault(employee["department_id"], []).append(employee["id"])

    housing_slots = []
    for department in DEPARTMENTS:
        for housing_id, _, _, _, _, count in department["housing"]:
            housing_slots += [housing_id] * count
    housing_by_id = {h[0]: h for d in DEPARTMENTS for h in d["housing"]}

    for department in DEPARTMENTS:
        housing_here = [h[0] for h in department["housing"]]
        department_slots = [slot for slot in housing_slots if slot in housing_here]
        department_clients = []
        for index in range(department["clients"]):
            client = new_client(department["id"], department["municipalities"], department["outside"])
            if index < len(department_slots):
                housing = housing_by_id[department_slots[index]]
                client.update(accommodation_type="SUPPORTED_HOUSING", supported_housing_id=housing[0], street=housing[2],
                              city=housing[3], postal_code=housing[4], has_elevator=True,
                              floor_or_room=f"byt č. {rng.randint(1, 30)}", travel_distance_km=None)
            department_clients.append(client)
        # personal numbers grow with time: the longest served client has the lowest number
        department_clients.sort(key=lambda c: c["care_days_ago"], reverse=True)
        caregivers = caregivers_by_department[department["id"]]
        for sequence, client in enumerate(department_clients, start=1):
            client["personal_number"] = f"{department['number']:02d}{sequence:04d}"
            client["caregiver_id"] = caregivers[(sequence * 7 + rng.randint(0, 2)) % len(caregivers)]
        clients += department_clients

    for client_id, client in enumerate(clients, start=1):
        client["id"] = client_id
        client["organization_id"] = 1
    return clients


def new_client(department_id, municipalities, outside):
    gender = "FEMALE" if rng.random() < 0.7 else "MALE"
    first, last, surnames = unique_name(SENIOR_FEMALE if gender == "FEMALE" else SENIOR_MALE, gender)
    birth = random_birth(65, 98)
    if rng.random() < 0.05:
        city, postal, travel = rng.choice(outside)
        street = f"{city} {rng.randint(1, 300)}"
    else:
        city, postal, has_streets, _ = rng.choices(municipalities, weights=[m[3] for m in municipalities], k=1)[0]
        street = address(city, has_streets)
        travel = None
    town = city in TOWNS or city == "Zruč-Senec"
    accommodation = pick([("FAMILY_HOUSE", 30 if town else 70), ("OWN_FLAT", 25 if town else 5), ("RENTED_FLAT", 15 if town else 3),
                          ("MUNICIPAL_FLAT", 12 if town else 3), ("WITH_RELATIVES", 12)])
    flat = accommodation in ("OWN_FLAT", "RENTED_FLAT", "MUNICIPAL_FLAT")
    care_days_ago = rng.randint(20, 9 * 365)
    access = pick([("DOORBELL", 35), ("KEYS", 25), ("PHONE_CALL", 15), ("OTHER", 5), (None, 20)])
    relation, relative_first, relative_last = relative(surnames)
    client = {
        "first_name": first, "last_name": last, "gender": gender, "surnames": surnames, "date_of_birth": birth,
        "street": street, "city": city, "postal_code": postal, "travel_distance_km": travel,
        "department_id": department_id, "accommodation_type": accommodation, "supported_housing_id": None,
        "has_elevator": (rng.random() < 0.4) if flat else None,
        "floor_or_room": f"{rng.randint(1, 4 if town else 1)}. patro" if flat else ("přízemí" if rng.random() < 0.3 else None),
        "access_method": access, "lock_code": f"{rng.randint(1000, 9999)}" if access == "KEYS" and rng.random() < 0.6 else None,
        "access_note": rng.choice(ACCESS_NOTES[access]) if access and rng.random() < 0.7 else None,
        "care_days_ago": care_days_ago,
        "benefits": pick([("NONE", 8), ("ONE", 22), ("TWO", 30), ("THREE", 25), ("FOUR", 15)]),
        "phone": phone() if rng.random() < 0.8 else None, "phone2": phone() if rng.random() < 0.2 else None,
        "email": None, "title_before": None, "title_after": None,
        "relatives_contact": f"{relation} {relative_first} {relative_last}, {phone()}" if rng.random() < 0.85 else None,
        "general_practitioner": None, "notes": rng.choice(CLIENT_NOTES) if rng.random() < 0.25 else None,
        "legally_competent": True, "guardian_name": None, "guardian_contact": None,
        "no_charge": False, "active": True, "termination_date": None, "termination_reason": None,
        "relative": (relation, relative_first, relative_last),
    }
    client["relatives_contact"] = client["relatives_contact"][0].upper() + client["relatives_contact"][1:] if client["relatives_contact"] else None
    if rng.random() < 0.15:
        client["email"] = f"{ascii_slug(first)}.{ascii_slug(last)}{rng.randint(1, 99)}@example.com"
    doctor = rng.choice([d for d in DOCTORS if d[2] == department_id])
    client["general_practitioner"] = f"{doctor[0]}, {doctor[1]}, {phone()}"
    return client


def apply_client_flags(clients):
    """Spreads the rarer situations over clients; the first two clients of 01 stay ordinary for the demo accounts."""
    candidates = [c for c in clients if c["id"] > 2]
    rng.shuffle(candidates)
    cursor = 0

    def take(count, condition=lambda c: True):
        nonlocal cursor
        taken = []
        while len(taken) < count:
            client = candidates[cursor % len(candidates)]
            cursor += 1
            if condition(client) and not client.get("flagged"):
                client["flagged"] = True
                taken.append(client)
        return taken

    for reason, count in TERMINATIONS:
        for client in take(count, lambda c: c["care_days_ago"] > 120):
            ended_days_ago = rng.randint(5, min(client["care_days_ago"] - 30, 700))
            client.update(active=False, termination_reason=reason, termination_days_ago=ended_days_ago)
            if reason in TERMINATION_NOTES:
                client["notes"] = TERMINATION_NOTES[reason]

    for client in take(20):
        relation, first, last = client["relative"]
        if rng.random() < 0.6:
            client["guardian_name"] = f"{first} {last} ({relation})"
            client["guardian_contact"] = f"{phone()}, {ascii_slug(first)}.{ascii_slug(last)}@example.com"
        else:
            municipality = client["city"]
            is_town = municipality in TOWNS
            client["guardian_name"] = f"{'Město' if is_town else 'Obec'} {municipality} (veřejný opatrovník)"
            client["guardian_contact"] = f"{'Městský' if is_town else 'Obecní'} úřad {municipality}, {phone()}"
        client["legally_competent"] = False

    for client in take(6, lambda c: c["active"]):
        client["no_charge"] = True
        client["notes"] = "Služba bez úhrady na základě rozhodnutí obce."

    titles = [("Ing.", None), ("Mgr.", None), ("MUDr.", None), ("PhDr.", None), ("RNDr.", None), ("Ing.", "CSc."), ("Ing.", None)]
    for client, (before, after) in zip(take(len(titles)), titles):
        client["title_before"] = before
        client["title_after"] = after

    for client in take(6, lambda c: c["active"] and c["id"] % 20 != 0 and c["benefits"] in ("THREE", "FOUR")):
        client["benefits"] = "FOUR"
        client["intensive"] = True


def assign_tasks(clients, task_ids):
    links = []
    for client in clients:
        if client.get("intensive"):
            names = INTENSIVE_COMBO
        else:
            names = list(pick(TASK_COMBOS))
            if rng.random() < 0.1:
                names.append("Pedikúra")
            if client["supported_housing_id"] and "Dovoz jídla" in names:
                names.remove("Dovoz jídla")
        client["tasks"] = names
        links += [{"client_id": client["id"], "task_id": task_ids[name]} for name in names]
    return links


def client_row(client):
    care_from = -client["care_days_ago"]
    termination = -client["termination_days_ago"] if not client["active"] else None
    definite = rng.random() < 0.15
    return {
        "id": client["id"], "first_name": client["first_name"], "last_name": client["last_name"], "gender": client["gender"],
        "personal_number": client["personal_number"], "date_of_birth": client["date_of_birth"],
        "termination_date": days(termination) if termination is not None else None,
        "termination_reason": client["termination_reason"], "email": client["email"], "phone": client["phone"],
        "street": client["street"], "city": client["city"], "postal_code": client["postal_code"],
        "legally_competent": client["legally_competent"], "benefits": client["benefits"],
        "relatives_contact": client["relatives_contact"], "general_practitioner": client["general_practitioner"],
        "notes": client["notes"], "created": days(care_from), "active": client["active"],
        "accommodation_type": client["accommodation_type"], "supported_housing_id": client["supported_housing_id"],
        "travel_distance_km": client["travel_distance_km"], "has_elevator": client["has_elevator"],
        "floor_or_room": client["floor_or_room"], "access_method": client["access_method"], "lock_code": client["lock_code"],
        "access_note": client["access_note"], "title_before": client["title_before"], "title_after": client["title_after"],
        "phone2": client["phone2"], "guardian_name": client["guardian_name"], "guardian_contact": client["guardian_contact"],
        "care_from": days(care_from), "care_to": days(termination) if termination is not None else None,
        "contract_from": days(care_from), "contract_to": days(termination if termination is not None else rng.randint(60, 300)) if definite or termination is not None else None,
        "contract_indefinite": not definite and termination is None,
        "no_charge": client["no_charge"], "intake_assessment_date": days(care_from - rng.randint(3, 14)),
        "department_id": client["department_id"], "organization_id": client["organization_id"], "caregiver_id": client["caregiver_id"],
    }


def build_plans(clients):
    plans, contents, records = [], [], []
    content_id = record_id = 1
    for client in clients:
        if not client.get("has_plan"):
            continue
        gender = client["gender"]
        plan_id = len(plans) + 1
        versions = 1 if client["care_days_ago"] < 200 else rng.choice([1, 2, 2, 3])
        first_processed = -client["care_days_ago"] + 30
        processed_dates = sorted({min(-3, first_processed + step * rng.randint(170, 190)) for step in range(versions)})
        processed_dates = [d for d in processed_dates if d <= -3] or [-3]
        widowed = rng.random() < 0.6
        life_path = (g(gender, "Narodila se", "Narodil se") + f" v roce {client['date_of_birth'].year}, celý život "
                     + g(gender, "pracovala jako ", "pracoval jako ") + rng.choice(g(gender, JOBS_F, JOBS_M)) + ". "
                     + (f"Má {rng.choice(CHILDREN)}. " if rng.random() < 0.9 else g(gender, "Je bezdětná, nejbližší je neteř. ", "Je bezdětný, nejbližší je synovec. "))
                     + (g(gender, f"Ovdověla před {rng.randint(2, 25)} lety.", f"Ovdověl před {rng.randint(2, 25)} lety.") if widowed
                        else g(gender, "Žije s manželem.", "Žije s manželkou.")))
        base = {
            "likes": ", ".join(rng.sample(LIKES + g(gender, LIKES_F, LIKES_M), 2)),
            "dislikes": rng.choice(DISLIKES + [g(gender, *pair) for pair in DISLIKES_GENDERED]),
            "strengths": g(gender, *rng.choice(STRENGTHS)),
            "aspirations": g(gender, *rng.choice(ASPIRATIONS)),
            "life_path": life_path,
            "additional_info": rng.choice(ADDITIONAL_INFO),
            "hygiene": g(gender, *rng.choice(HYGIENE)),
            "self_care": g(gender, *rng.choice(SELF_CARE)),
            "mobility": rng.choice(MOBILITY),
            "diet": text_variant(rng.choice(DIET), gender),
            "home_care": rng.choice(HOME_CARE),
            "social_contact": text_variant(rng.choice(SOCIAL_CONTACT), gender),
            "activities": rng.choice(ACTIVITIES),
            "health": rng.choice(HEALTH),
            "exercising_rights": "Záležitosti vyřizuje opatrovník." if not client["legally_competent"] else text_variant(rng.choice(RIGHTS), gender),
        }
        plans.append({"id": plan_id, "client_id": client["id"], "organization_id": client["organization_id"]})
        for version, processed in enumerate(processed_dates, start=1):
            row = dict(base)
            if version > 1:
                row["mobility"] = rng.choice(MOBILITY)
                row["health"] = rng.choice(HEALTH)
                row["additional_info"] = "Aktualizace po hodnocení s klientem a rodinou, péče upravena podle aktuálního stavu."
            planned = processed + 182
            if version == len(processed_dates) and rng.random() < 0.08:
                planned = rng.randint(-20, -1)
            contents.append({"id": content_id, "individual_plan_id": plan_id, "version_number": version,
                             "processed_date": days(processed), "planned_update_date": days(planned), **row})
            content_id += 1
        plans[-1]["current_content_id"] = content_id - 1
        window = min(80, max(5, client["care_days_ago"] - 25))
        record_days = sorted(rng.sample(range(1, window), k=min(rng.randint(3, 10), window - 1)), reverse=True)
        for record_day in record_days:
            text = rng.choice(DAILY_RECORDS)
            text = g(gender, *text) if isinstance(text, tuple) else text
            records.append({"id": record_id, "date": days(-record_day), "content": text.replace("{L}", client["last_name"]),
                            "individual_plan_id": plan_id, "created_by_id": client["caregiver_id"]})
            record_id += 1
    return plans, contents, records


def text_variant(text, gender):
    return g(gender, *text) if isinstance(text, tuple) else text


def build_second_organization(second, task_ids):
    clients = []
    caregivers = [e["id"] for e in second["caregivers"]]
    for sequence in range(1, 9):
        client = new_client(5, SECOND_DEPARTMENT["municipalities"], [("Holoubkov", "338 01", 9)])
        client.update(id=340 + sequence, organization_id=2, personal_number=f"01{sequence:04d}",
                      caregiver_id=caregivers[sequence % 2], tasks=[])
        clients.append(client)
    links = []
    names = [STATUTORY[code][0] for code, _ in SECOND_ORG_TASKS]
    for client in clients:
        client["tasks"] = rng.sample(names, 2)
        links += [{"client_id": client["id"], "task_id": task_ids[name]} for name in client["tasks"]]
    return clients, links


def user(id_, username, email, role, employee_id=None, client_id=None, pending=False):
    return {"id": id_, "username": None if pending else username, "email": email,
            "password": None if pending else PASSWORD_HASH, "role": role,
            "activation_token": f"demo-activation-{id_:04d}" if pending else None,
            "token_expiry": Raw("NOW() + INTERVAL '7 days'") if pending else None,
            "active": True, "employee_id": employee_id, "client_id": client_id}


def staff_email(employee):
    return f"{ascii_slug(employee['first_name'])}.{ascii_slug(employee['last_name'])}@example.com"


def build_users(employees, clients, second):
    by_id = {e["id"]: e for e in employees}
    users = [
        user(1, "superadmin", "superadmin@example.com", "SUPERADMIN"),
        user(2, "admin", staff_email(by_id[1]), "ADMIN", employee_id=1),
        user(3, "coordinator", staff_email(by_id[2]), "COORDINATOR", employee_id=2),
        user(4, "caregiver", staff_email(by_id[3]), "CAREGIVER", employee_id=3),
        user(5, "client", clients[0]["email"], "CLIENT", client_id=1),
    ]
    next_id = 6
    named = [by_id[4], by_id[5]] + [e for e in employees if e["role"] == "CAREGIVER" and e["organization_id"] == 1 and e["id"] > 3][::6]
    pending = [e for e in employees if e["role"] == "CAREGIVER" and e["organization_id"] == 1 and e["id"] > 3][3::9]
    for employee in named:
        role = "COORDINATOR" if employee["role"] == "COORDINATOR" else "CAREGIVER"
        users.append(user(next_id, f"{ascii_slug(employee['first_name'])}.{ascii_slug(employee['last_name'])}", staff_email(employee), role, employee_id=employee["id"]))
        next_id += 1
    for employee in pending:
        if employee in named:
            continue
        users.append(user(next_id, None, staff_email(employee), "CAREGIVER", employee_id=employee["id"], pending=True))
        next_id += 1
    for employee, role in [(second["manager"], "ADMIN"), (second["coordinator"], "COORDINATOR"), (second["caregivers"][0], "CAREGIVER")]:
        users.append(user(next_id, f"{ascii_slug(employee['first_name'])}.{ascii_slug(employee['last_name'])}", staff_email(employee), role, employee_id=employee["id"]))
        next_id += 1
    return users


def main():
    employees, second = build_employees()
    clients = build_clients(employees)
    apply_client_flags(clients)

    # demo accounts: client 1 belongs to the "client" account, client 2 is the plan of the E2E test
    for client in clients[:2]:
        client.update(active=True, caregiver_id=3, legally_competent=True, guardian_name=None, guardian_contact=None, no_charge=False)
    clients[0]["email"] = f"{ascii_slug(clients[0]['first_name'])}.{ascii_slug(clients[0]['last_name'])}@example.com"

    task_rows, task_ids = [], {}
    for code in MAIN_TASK_CODES:
        name, unit, price = STATUTORY[code]
        task_ids[name] = len(task_rows) + 1
        task_rows.append({"id": len(task_rows) + 1, "name": name, "unit_price": price, "unit_type": unit, "double_meeting": code == "a8",
                          "active": True, "organization_id": 1, "statutory_task_id": Raw(f"(SELECT id FROM statutory_task WHERE code = '{code}')")})
    for name, unit, price in OPTIONAL_TASKS:
        task_ids[name] = len(task_rows) + 1
        task_rows.append({"id": len(task_rows) + 1, "name": name, "unit_price": price, "unit_type": unit, "double_meeting": False,
                          "active": True, "organization_id": 1, "statutory_task_id": None})
    second_task_ids = {}
    for code, price in SECOND_ORG_TASKS:
        name, unit, _ = STATUTORY[code]
        second_task_ids[name] = len(task_rows) + 1
        task_rows.append({"id": len(task_rows) + 1, "name": name, "unit_price": price, "unit_type": unit, "double_meeting": False,
                          "active": True, "organization_id": 2, "statutory_task_id": Raw(f"(SELECT id FROM statutory_task WHERE code = '{code}')")})

    client_tasks = assign_tasks(clients, task_ids)
    second_clients, second_links = build_second_organization(second, second_task_ids)

    active = [c for c in clients if c["active"]]
    for client in clients[:2]:
        client["has_plan"] = True
    for client in rng.sample(active[2:], k=round(len(active) * 0.6) - 2):
        client["has_plan"] = True
    for client in second_clients[:4]:
        client["has_plan"] = True
    all_clients = clients + second_clients
    plans, contents, records = build_plans(all_clients)

    users = build_users(employees, clients, second)

    out = ["-- Czech demo data, generated by tools/demo-data/generate.py; edit the generator, not this file.\n"
           "-- Loaded by DemoDataSeeder (profiles demo and dev) into an empty database.\n\n"
           "SET client_min_messages = warning;\n\nBEGIN;\n\n"]
    out.append(insert("organization", [
        {"id": 1, "name": "Pečovatelská služba Severní Plzeňsko, z. ú.", "active": True, "manager_id": None,
         "account_prefix": None, "account_number": "2801234567", "bank_code": "2010"},
        {"id": 2, "name": "Domácí péče Rokycansko, o. p. s.", "active": True, "manager_id": None,
         "account_prefix": "19", "account_number": "4501876543", "bank_code": "0800"},
    ]))
    out.append(insert("department", [
        {"id": d["id"], "street": d["street"], "city": d["city"], "postal_code": d["postal"], "coordinator_id": None,
         "organization_id": 1, "department_number": d["number"], "active": True} for d in DEPARTMENTS
    ] + [{"id": 5, "street": SECOND_DEPARTMENT["street"], "city": SECOND_DEPARTMENT["city"], "postal_code": SECOND_DEPARTMENT["postal"],
          "coordinator_id": None, "organization_id": 2, "department_number": 1, "active": True}]))
    out.append(insert("municipality", [
        {"department_id": d["id"], "name": m[0]} for d in DEPARTMENTS + [SECOND_DEPARTMENT] for m in d["municipalities"]
    ]))
    out.append(insert("supported_housing", [
        {"id": h[0], "department_id": d["id"], "name": h[1], "street": h[2], "city": h[3], "postal_code": h[4], "active": True}
        for d in DEPARTMENTS for h in d["housing"]
    ]))
    out.append(insert("employee", [
        {"id": e["id"], "first_name": e["first_name"], "last_name": e["last_name"], "role": e["role"], "active": True,
         "department_id": e["department_id"], "organization_id": e["organization_id"]} for e in employees
    ]))
    out.append("UPDATE organization SET manager_id = 1 WHERE id = 1;\n"
               f"UPDATE organization SET manager_id = {second['manager']['id']} WHERE id = 2;\n"
               "UPDATE department SET coordinator_id = 2 WHERE id = 1;\n"
               "UPDATE department SET coordinator_id = 1 WHERE id = 2;\n"
               "UPDATE department SET coordinator_id = 4 WHERE id = 3;\n"
               "UPDATE department SET coordinator_id = 5 WHERE id = 4;\n"
               f"UPDATE department SET coordinator_id = {second['coordinator']['id']} WHERE id = 5;\n\n")
    out.append(insert("task", task_rows))
    out.append(insert("client", [client_row(c) for c in all_clients]))
    out.append(insert("client_task", client_tasks + second_links))
    out.append(insert("app_user", users))
    out.append(insert("individual_plan", [{"id": p["id"], "client_id": p["client_id"], "organization_id": p["organization_id"]} for p in plans]))
    out.append(insert("individual_plan_content", contents))
    out.append("".join(f"UPDATE individual_plan SET current_content_id = {p['current_content_id']} WHERE id = {p['id']};\n" for p in plans) + "\n")
    out.append(insert("daily_record", records))
    for table in ["organization", "department", "municipality", "supported_housing", "employee", "task", "client",
                  "app_user", "individual_plan", "individual_plan_content", "daily_record"]:
        out.append(f"SELECT setval(pg_get_serial_sequence('{table}', 'id'), (SELECT COALESCE(MAX(id), 1) FROM {table}));\n")
    out.append("\nCOMMIT;\n")

    DATA_SQL.parent.mkdir(parents=True, exist_ok=True)
    DATA_SQL.write_text("".join(out), encoding="utf-8")

    portraits = []
    main_clients = [c for c in clients]
    portrait_clients = clients[:2] + rng.sample(main_clients[2:], k=98)
    for client in sorted(portrait_clients, key=lambda c: c["personal_number"]):
        seed = int(client["personal_number"])
        portraits.append({"id": f"clients/{client['personal_number']}",
                          "prompt": portrait_prompt(seed, age_on_reference(client["date_of_birth"]), client["gender"]),
                          "seed": seed})
    main_staff = [e for e in employees if e["organization_id"] == 1]
    portrait_staff = main_staff[:5] + rng.sample(main_staff[5:], k=25)
    for employee in sorted(portrait_staff, key=lambda e: e["id"]):
        if employee["role"] == "CAREGIVER":
            clothes = CLOTHES_CAREGIVER
        else:
            clothes = CLOTHES_OFFICE if employee["gender"] == "FEMALE" else CLOTHES_OFFICE_M
        portraits.append({"id": f"employees/{employee['id']}",
                          "prompt": portrait_prompt(employee["id"], employee["age"], employee["gender"], clothes),
                          "seed": employee["id"]})
    PORTRAITS_JSON.write_text(json.dumps(portraits, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    print(f"{len(all_clients)} clients ({sum(1 for c in clients if c['active'])} active in the main organization), "
          f"{len(employees)} employees, {len(plans)} plans, {len(records)} daily records, {len(portraits)} portraits")


if __name__ == "__main__":
    main()
