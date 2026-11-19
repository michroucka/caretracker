# CareTracker – vizuální identita a design systém

Co platí pro brand, design tokeny a nové stránky. Historie přechodu z HeroUI na shadcn/ui je v git historii tohoto souboru (dříve `PLAN.md`).

- **Brand board:** [`brand-board.html`](brand-board.html) (otevři v prohlížeči). Online verze: https://claude.ai/artifact/JbAvNfiupW1hqn5U7EUoQg
- **Assety:** [`assets/`](assets/) – SVG loga, favicon, ikony aplikace, PNG

---

## 1. Rozhodnutí

| Téma | Rozhodnutí | Proč |
| --- | --- | --- |
| Název | Zůstává **CareTracker** | Funkční a popisný. Symbol na názvu nezávisí, takže jméno lze kdykoliv změnit bez nového loga. |
| Symbol | **Buňka** – mřížka 2×2, kde jedna buňka je kruh | Tři čtverce jsou evidence (úkony, plány, výkazy), kruh je klient. Spojuje „care“ i „tracker“, funguje v 16 px a v jedné barvě. |
| Wordmark | `caretracker` malými písmeny, Bricolage Grotesque 600, převedeno na křivky | Přátelské, ne dětské. V textu UI se dál píše „CareTracker“. |
| Barvy | **Švestka + meruňka** místo modrozelené | Odklon od zdravotnického klišé, kterým mluví celá kategorie. |
| Písma | **Bricolage Grotesque** (nadpisy) + **Figtree** (text, UI) | Obě SIL OFL, plná čeština. Nahrazují Nunito. |
| UI knihovna | **shadcn/ui** na Tailwind v4 (dřív HeroUI v2) | Kód komponent je náš, styluje se přímo Tailwindem, menší bundle. HeroUI v2 se těžko přizpůsoboval. |
| Ikony | **Tabler** (`@tabler/icons-react`) místo Lucide | Vybráno 2026-11-08 po srovnání v katalogu. Pozor: tloušťka čáry je prop `stroke`, ne `strokeWidth`. Plné varianty (`…Filled`) nejsou u všech ikon, aktivní stav proto dělat barvou, ne výplní. |
| Animace | `motion` (už je v závislostech) | Jedna knihovna pro přechody stránek, layout animace a gesta. |
| Detaily záznamů | **Sheet zprava** (na mobilu přes celou obrazovku), stav v URL. Klient a individuální plán zůstávají jako stránky. | Seznam zůstává vidět, detail jde sdílet odkazem, tlačítko zpět ho zavře. Modal zakrývá kontext a vede k vnořeným modalům. Viz sekce 2.6. |
| Úprava záznamu | **Přímo v sheetu** – přepnutí zobrazení na formulář | Žádný další modal nad detailem, uživatel neztratí kontext. |

---

## 2. Brand – specifikace

### 2.1 Logo

**Konstrukce** (plátno 256 × 256):

- buňka 96, mezera 16, okraj 24, zaoblení rohů 28
- kruh vpravo nahoře, střed (184, 72), poloměr **50** (o 2 víc než polovina buňky – optická korekce, kruh vedle čtverce působí menší)
- **malý řez** pro 32 px a méně (`symbol-small-color.svg`, favicon, ikony): buňka 100, mezera 24, okraj 16, zaoblení 30, kruh r 52

**Barevnost:**

| Varianta | Čtverce | Kruh | Wordmark | Kdy |
| --- | --- | --- | --- | --- |
| Primární (`*-color.svg`) | Švestka | Meruňka | Inkoust | Světlé podklady, výchozí |
| Inverzní (`*-reversed.svg`) | Papír | Meruňka | Papír | Tmavý režim, tmavé podklady |
| Jedna barva (`*-plum.svg`) | Švestka | Švestka | Švestka | Jednobarevný tisk, razítka |
| Černá / bílá (`*-black.svg`, `*-white.svg`) | – | – | – | Fax, gravírování, bílá na švestkové |

**Pravidla:**

- Ochranná zóna kolem loga = 1 buňka symbolu v dané velikosti.
- Minimální šířka horizontálního loga **96 px**. Pod tím jen symbol.
- Symbol pod 32 px vždy v malém řezu.
- Barevný je jen kruh. Žádné jiné barvy, přechody, stíny, obrysy.
- Kruh je vždy vpravo nahoře. Logo neotáčet, nedeformovat, nepřeskládávat.

**Soubory v `assets/`:**

| Soubor | Použití |
| --- | --- |
| `symbol-{color,reversed,plum,black,white}.svg` | Samotný symbol |
| `symbol-small-color.svg` | Symbol pro ≤ 32 px |
| `lockup-horizontal-*.svg` | Navbar/sidebar, hlavička PDF, e-maily, README |
| `lockup-stacked-*.svg` | Přihlašovací obrazovka, čtvercové plochy |
| `favicon.svg` | Favicon, sám se přepne na světlé čtverce v tmavém režimu prohlížeče |
| `favicon.ico` | Fallback favicon (16, 32, 48) |
| `app-icon.svg`, `png/icon-{180,192,512}.png` | PWA manifest, apple-touch-icon |
| `png/icon-{16,32,48}.png` | Rastrové malé ikony |
| `png/lockup-horizontal-{color,reversed}.png` | E-maily (klienti často neumí SVG) |

### 2.2 Barvy

| Název | HEX | Úkol | Kontrast |
| --- | --- | --- | --- |
| Švestka | `#4A2B5E` | Primární: značka, hlavní tlačítka, aktivní navigace, odkazy | bílý text 11,7 : 1 |
| Meruňka | `#E26A35` | Akcent: kruh v logu, focus ring, zvýraznění, stav „čeká“ | inkoustový text 5,2 : 1 · na bílé 3,3 : 1 (jen grafika, **nikdy text na bílé**) |
| Levandule | `#ECE3F2` | Měkké plochy: vybraný řádek, aktivní položka menu, štítky, sekundární tlačítko | švestkový text 9,4 : 1 |
| Inkoust | `#1E1A24` | Text, wordmark | na papíře 15,8 : 1 |
| Papír | `#F7F5F9` | Pozadí aplikace. Karty, tabulky a dialogy jsou bílé. | – |

Pomocné odvozené hodnoty (nejsou to nové brand barvy, jen odstíny):

| Token | Světlý | Tmavý |
| --- | --- | --- |
| Pozadí | `#F7F5F9` | `#17131C` |
| Povrch (karta, dialog) | `#FFFFFF` | `#211B28` |
| Povrch 2 (hover, zebra) | `#F7F3F9` | `#2A2332` |
| Text | `#1E1A24` | `#F1EDF5` |
| Tlumený text | `#645C6E` (5,9 : 1) | `#A79EB2` (7,1 : 1) |
| Linka / border | `#E2DCE8` | `#352D3E` |
| Primární | `#4A2B5E` + bílý text | `#C2A4D6` + inkoustový text (7,8 : 1) |

**Škála švestky** (Tailwind `plum-*`):

| 50 | 100 | 200 | 300 | 400 | 500 | 600 | **700** | 800 | 900 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `#F7F3F9` | `#ECE3F2` | `#DAC7E6` | `#C2A4D6` | `#A679C3` | `#8A4FB0` | `#6E3F8D` | `#4A2B5E` | `#372046` | `#24152E` |

**Škála meruňky** (Tailwind `apricot-*`):

| 50 | 100 | 200 | 300 | 400 | **500** | 600 | 700 | 800 | 900 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `#FCEFE9` | `#F8DBCE` | `#F2BDA6` | `#EDA07E` | `#E78255` | `#E26A35` | `#CD531D` | `#A54318` | `#783111` | `#4C1F0B` |

**Sémantické barvy** (nejsou součást brandu, slouží stavům):

| Stav | DEFAULT | Měkké pozadí / text | Poznámka |
| --- | --- | --- | --- |
| Success | `#1E8752` | `#E3F0E9` / `#145835` | beze změny |
| Warning | `#E6B325` | `#FCF6E4` / `#6D5512` | beze změny, text na ní inkoustový |
| Danger | `#C9364F` | `#FAEBED` / `#892436` | **posun z `#D64545`** do malinové, aby se nepletla s meruňkou |

### 2.3 Typografie

| Role | Písmo | Řezy | Fallback |
| --- | --- | --- | --- |
| Nadpisy, wordmark, velká čísla | Bricolage Grotesque | 500, 600, 700 | `"Figtree", ui-sans-serif, system-ui, sans-serif` |
| Text, formuláře, tabulky | Figtree | 400, 500, 600, 700 | `ui-sans-serif, system-ui, -apple-system, "Segoe UI", sans-serif` |

Google Fonts odkaz (do `frontend/index.html` místo Nunito):

```html
<link href="https://fonts.googleapis.com/css2?family=Bricolage+Grotesque:opsz,wght@12..96,500..700&family=Figtree:wght@400..700&display=swap" rel="stylesheet">
```

Velikostní škála, poměr 1,25, základ 16 px:

| Token | px | Použití |
| --- | --- | --- |
| `text-xs` | 12,8 | popisky, meta, štítky (uppercase + `tracking-wide`) |
| `text-base` | 16 | text, inputy, tabulky (tabulky můžou 14) |
| `text-xl` | 20 | h4, nadpis karty |
| `text-2xl` | 25 | h3, nadpis stránky na mobilu |
| `text-3xl` | 31 | h2, nadpis stránky |
| `text-4xl` | 39 | h1, velká čísla v přehledu |

Pravidla:

- Nadpisy `font-display font-semibold`, `text-wrap: balance`, `tracking-tight`.
- Všechna čísla v tabulkách a částky `tabular-nums`.
- Formát: `24. 10. 2026`, `1 240,00 Kč`, `0,5 h` (české locale přes `Intl`).

### 2.4 Tvar, prostor, stín

| Token | Hodnota | Použití |
| --- | --- | --- |
| `--radius-sm` | 8px | checkbox, malé štítky |
| `--radius-md` | 10px | tlačítka, inputy, položky menu |
| `--radius-lg` | 14px | karty, tabulky, dropdowny |
| `--radius-xl` | 18px | dialogy, přihlašovací karta |
| pill | 999px | stavové pilulky, filtry |
| `--shadow-sm` | `0 1px 2px rgb(30 26 36 / 0.06)` | karty |
| `--shadow-md` | `0 8px 24px rgb(30 26 36 / 0.10)` | dropdowny, popovery |
| `--shadow-lg` | `0 20px 50px rgb(30 26 36 / 0.18)` | dialogy |

Prostor: Tailwind škála po 4 px. Stránka `px-4 md:px-6 lg:px-8`, mezery mezi sekcemi `gap-6`, uvnitř karty `p-5`.

Stíny jsou jemné a tónované do inkoustu, ne šedé. Karty oddělovat spíš linkou `border` než stínem.

### 2.5 Pohyb

Jediný zdroj pravdy: CSS proměnné v `index.css` (`--duration-*`, `--ease-*`) a stejné hodnoty pro `motion` v `src/lib/motion.js` (`DURATION`, `EASE_OUT`, `EASE_IN`). V kódu **nikdy** `duration-150`, `duration-[250ms]` ani vlastní `cubic-bezier`, jen tokeny.

| Token | Hodnota | Tailwind / motion | Použití |
| --- | --- | --- | --- |
| `--duration-fast` | 150ms | `duration-fast` / `DURATION.fast` | focus ring a rámeček inputu, barva labelu, stisk, odchod drobností (odkreslení checkboxu) |
| `--duration-base` | 200ms | `duration-base` / `DURATION.base` | hover barvy tlačítek, dropdown, select, popover, tooltip, toast, chybová hláška, zavření dialogu a sheetu, kreslení checkboxu |
| `--duration-slow` | 300ms | `duration-slow` / `DURATION.slow` | otevření dialogu a sheetu, přechod stránky, zatřesení pole |
| `--ease-out` | `cubic-bezier(0.2, 0, 0, 1)` | `ease-out` / `EASE_OUT` | výchozí pro všechno: vstupy, hover, změny stavu |
| `--ease-in` | `cubic-bezier(0.4, 0, 1, 1)` | `ease-in` / `EASE_IN` | jen odchody, které mají „odjet“ (odkreslení fajfky, mizení prvku) |

Overlaye (dialog, sheet): `data-[state=open]:duration-slow data-[state=closed]:duration-base` – odchod je vždy kratší než vstup.

Pravidla:

- Animuje se jen `transform` a `opacity` (rychlé, bez přepočtu layoutu).
- Dialog: fade + scale z 0,96. Sheet/drawer: slide z okraje. Dropdown: fade + posun 4 px.
- Seznamy a tabulky při filtrování: `motion` `layout` + `AnimatePresence`, jen pro rozumný počet řádků (stovky ne).
- Přechod mezi stránkami: krátký fade + posun 8 px, žádné dlouhé efekty.
- Vždy respektovat `prefers-reduced-motion` (`useReducedMotion()` z motion, `motion-reduce:` v Tailwindu).
- Skeletony místo spinnerů tam, kde je známý tvar obsahu (tabulky, karty).

**Co čím animovat:**

| Vrstva | Nástroj | Na co |
| --- | --- | --- |
| Mikrointerakce | Tailwind `transition-*` + tokeny délek | hover, focus, stisk tlačítka (`active:scale-[0.98]`), změny barev |
| Otevření / zavření overlayů | `tw-animate-css` přes Radix `data-[state=open/closed]` (shadcn ho přidává) | dialog, sheet, dropdown, popover, tooltip |
| Odchody, layout, výška `auto`, gesta | `motion` (`AnimatePresence`, `layout`, `layoutId`, `drag`) | mazání řádku, přeskládání filtrované tabulky, posuvný indikátor aktivní položky v sidebaru, rozbalení sekce, stažení sheetu na mobilu, počítadla čísel na přehledu |
| Přechod mezi stránkami | View Transitions API (`viewTransition` na `<Link>` / `navigate` v React Router 7) | krátké prolnutí stránek bez vlastního kódu |

Cíl je, aby aplikace působila „živě“ díky konzistenci (všude stejné délky a křivky), ne díky efektům.

### 2.6 Vzory zobrazení

| Vzor | Kdy | Kde |
| --- | --- | --- |
| **Sheet zprava** (na mobilu přes celou obrazovku nebo zespodu) | Prohlížení a úprava záznamu ze seznamu | Detail provedeného úkonu, úkonu z katalogu, střediska, organizace, zaměstnance. Zápis nového úkonu (nejčastější akce, často z mobilu). |
| **Stránka** | Bohatá entita s podsekcemi | Klient, individuální plán, měsíční výkaz |
| **Dialog** | Krátká soustředěná akce | Vytvoření střediska / úkonu / organizace, generování výkazu, filtry na mobilu |
| **Potvrzovací dialog (`alert-dialog`)** | Potvrzení nevratné akce | Smazání, ukončení, deaktivace |

Pravidla pro sheet s detailem:

- Stav v URL přes search param (`/performed-tasks?detail=123`). Otevření = `setSearchParams`, zavření = odebrání parametru, takže tlačítko zpět sheet zavře a odkaz jde sdílet.
- Otevřený řádek je v tabulce zvýrazněný levandulí.
- Klik na jiný řádek sheet nezavírá, jen prolne obsah (`AnimatePresence` s `key` podle id).
- Úprava přímo v sheetu: tlačítko „Upravit“ přepne zobrazení na formulář, „Uložit“ / „Zrušit“ ho vrátí zpět. Žádný další modal.
- Smazání z detailu otevře `alert-dialog` nad sheetem (jediný povolený vnořený overlay).

**Rozložení detailu (schválený vzor: `src/components/catalog/TaskDetailSheetExample.jsx`, v katalogu „Sheet – detail úkonu“):**

- Šířka `sm:max-w-md`, na mobilu celá šířka. Hlavička stojí, tělo se posouvá.
- Hlavička: štítek s typem záznamu + tlumená meta (datum, středisko), velký nadpis `font-display`.
- Akce **v patičce** (od 2026-11-08, původně pod nadpisem): vlevo menu „⋯“ (smazání, deaktivace…), vpravo „Upravit“. Při úpravě je na stejném místě „Zrušit / Uložit změny“, takže akce jsou vždy dole a na mobilu pod palcem. Řeší `RecordSheet` přes `onEdit` a `moreActions`.
- Nahoře souhrn: 2 tlumené dlaždice s klíčovými čísly (množství, cena) v `tabular-nums`.
- Pod tím sekce s malými uppercase popisky oddělené `Separator`. Lidé jako iniciály + jméno + detail.
- Úprava na místě: obsah se prolne na formulář z `FormField`, patička „Zrušit / Uložit změny“ jen v režimu úpravy.

---

## 3. Design tokeny v kódu

Jediný zdroj pravdy jsou CSS proměnné v `frontend/src/styles/index.css`. Názvy sémantických tokenů odpovídají konvenci shadcn (`--background`, `--primary`, …), takže komponenty ze shadcn fungují bez úprav. Brand barvy mají vlastní škály.

> Pozor na rozdíl v názvosloví: v **shadcn** je `secondary` měkké sekundární tlačítko (u nás levandule) a `accent` je hover pozadí. Meruňka má vlastní token `--highlight`.

Tokeny (zkráceně):

```css
@import "tailwindcss";
@custom-variant dark (&:is(.dark *));

:root {
  --background: #F7F5F9;
  --foreground: #1E1A24;
  --card: #FFFFFF;
  --card-foreground: #1E1A24;
  --popover: #FFFFFF;
  --popover-foreground: #1E1A24;
  --primary: #4A2B5E;
  --primary-foreground: #FFFFFF;
  --primary-hover: #372046;
  --secondary: #ECE3F2;
  --secondary-foreground: #4A2B5E;
  --secondary-hover: #DAC7E6;
  --muted: #F7F3F9;
  --muted-foreground: #645C6E;
  --accent: #EFE9F3;
  --accent-foreground: #1E1A24;
  --highlight: #E26A35;
  --highlight-foreground: #1E1A24;
  --destructive: #C9364F;
  --destructive-foreground: #FFFFFF;
  --destructive-hover: #AF3249;
  --success: #1E8752;
  --warning: #E6B325;
  --success-soft: #E3F0E9;
  --success-soft-foreground: #145835;
  --warning-soft: #FCF6E4;
  --warning-soft-foreground: #6D5512;
  --danger-soft: #FAEBED;
  --danger-soft-foreground: #892436;
  --pending-soft: #FCEFE9;
  --pending-soft-foreground: #A54318;
  --border: #E2DCE8;
  --input: #E2DCE8;
  --ring: #E26A35;
  --radius: 10px;

  --sidebar: #EDE8F1;
  --sidebar-foreground: #4D4557;
  --sidebar-primary: #4A2B5E;
  --sidebar-primary-foreground: #FFFFFF;
  --sidebar-accent: #E2DAE9;
  --sidebar-accent-foreground: #1E1A24;
  --sidebar-active: #FFFFFF;
  --sidebar-active-foreground: #4A2B5E;
  --sidebar-border: #DDD5E4;
  --sidebar-ring: #E26A35;
}

.dark {
  --background: #17131C;
  --foreground: #F1EDF5;
  --card: #211B28;
  --card-foreground: #F1EDF5;
  --popover: #211B28;
  --popover-foreground: #F1EDF5;
  --primary: #C2A4D6;
  --primary-foreground: #1E1A24;
  --primary-hover: #DAC7E6;
  --secondary: #3B2D4A;
  --secondary-foreground: #F1EDF5;
  --secondary-hover: #4A3A5C;
  --muted: #2A2332;
  --muted-foreground: #A79EB2;
  --accent: #332A3D;
  --accent-foreground: #F1EDF5;
  --highlight: #E78255;
  --highlight-foreground: #1E1A24;
  --destructive: #DE8292;
  --destructive-foreground: #1E1A24;
  --destructive-hover: #E7A5B0;
  --success: #6DB18F;
  --warning: #EFCE71;
  --success-soft: #2D3338;
  --success-soft-foreground: #6DB18F;
  --warning-soft: #423834;
  --warning-soft-foreground: #EFCE71;
  --danger-soft: #3F2B39;
  --danger-soft-foreground: #DE8292;
  --pending-soft: #412B2F;
  --pending-soft-foreground: #E78255;
  --border: #352D3E;
  --input: #352D3E;
  --ring: #E78255;

  --sidebar: #110D15;
  --sidebar-foreground: #C9C0D3;
  --sidebar-primary: #C2A4D6;
  --sidebar-primary-foreground: #1E1A24;
  --sidebar-accent: #211B28;
  --sidebar-accent-foreground: #F1EDF5;
  --sidebar-active: #2A2332;
  --sidebar-active-foreground: #F1EDF5;
  --sidebar-border: #2A2332;
  --sidebar-ring: #E78255;
}

@theme inline {
  --font-sans: "Figtree", ui-sans-serif, system-ui, -apple-system, "Segoe UI", sans-serif;
  --font-display: "Bricolage Grotesque", "Figtree", ui-sans-serif, system-ui, sans-serif;

  --color-background: var(--background);
  --color-foreground: var(--foreground);
  --color-card: var(--card);
  --color-card-foreground: var(--card-foreground);
  --color-popover: var(--popover);
  --color-popover-foreground: var(--popover-foreground);
  --color-primary: var(--primary);
  --color-primary-foreground: var(--primary-foreground);
  --color-primary-hover: var(--primary-hover);
  --color-secondary: var(--secondary);
  --color-secondary-foreground: var(--secondary-foreground);
  --color-secondary-hover: var(--secondary-hover);
  --color-muted: var(--muted);
  --color-muted-foreground: var(--muted-foreground);
  --color-accent: var(--accent);
  --color-accent-foreground: var(--accent-foreground);
  --color-highlight: var(--highlight);
  --color-highlight-foreground: var(--highlight-foreground);
  --color-destructive: var(--destructive);
  --color-destructive-foreground: var(--destructive-foreground);
  --color-destructive-hover: var(--destructive-hover);
  --color-success: var(--success);
  --color-warning: var(--warning);
  --color-success-soft: var(--success-soft);
  --color-success-soft-foreground: var(--success-soft-foreground);
  --color-warning-soft: var(--warning-soft);
  --color-warning-soft-foreground: var(--warning-soft-foreground);
  --color-danger-soft: var(--danger-soft);
  --color-danger-soft-foreground: var(--danger-soft-foreground);
  --color-pending-soft: var(--pending-soft);
  --color-pending-soft-foreground: var(--pending-soft-foreground);
  --color-border: var(--border);
  --color-input: var(--input);
  --color-ring: var(--ring);
  --color-sidebar: var(--sidebar);
  --color-sidebar-foreground: var(--sidebar-foreground);
  --color-sidebar-primary: var(--sidebar-primary);
  --color-sidebar-primary-foreground: var(--sidebar-primary-foreground);
  --color-sidebar-accent: var(--sidebar-accent);
  --color-sidebar-accent-foreground: var(--sidebar-accent-foreground);
  --color-sidebar-active: var(--sidebar-active);
  --color-sidebar-active-foreground: var(--sidebar-active-foreground);
  --color-sidebar-border: var(--sidebar-border);
  --color-sidebar-ring: var(--sidebar-ring);

  --color-plum-50: #F7F3F9;
  --color-plum-100: #ECE3F2;
  --color-plum-200: #DAC7E6;
  --color-plum-300: #C2A4D6;
  --color-plum-400: #A679C3;
  --color-plum-500: #8A4FB0;
  --color-plum-600: #6E3F8D;
  --color-plum-700: #4A2B5E;
  --color-plum-800: #372046;
  --color-plum-900: #24152E;

  --color-apricot-50: #FCEFE9;
  --color-apricot-100: #F8DBCE;
  --color-apricot-200: #F2BDA6;
  --color-apricot-300: #EDA07E;
  --color-apricot-400: #E78255;
  --color-apricot-500: #E26A35;
  --color-apricot-600: #CD531D;
  --color-apricot-700: #A54318;
  --color-apricot-800: #783111;
  --color-apricot-900: #4C1F0B;

  --radius-sm: calc(var(--radius) - 2px);
  --radius-md: var(--radius);
  --radius-lg: calc(var(--radius) + 4px);
  --radius-xl: calc(var(--radius) + 8px);

  --shadow-sm: 0 1px 2px rgb(30 26 36 / 0.06);
  --shadow-md: 0 8px 24px rgb(30 26 36 / 0.10);
  --shadow-lg: 0 20px 50px rgb(30 26 36 / 0.18);

  --ease-out: cubic-bezier(0.2, 0, 0, 1);
  --ease-in: cubic-bezier(0.4, 0, 1, 1);
}

@layer base {
  * { @apply border-border; }
  body { @apply bg-background text-foreground font-sans antialiased; }
  h1, h2, h3, h4 { @apply font-display font-semibold tracking-tight text-balance; }
}
```

Dark mode se dál přepíná třídou `.dark` na `<html>` (stávající `ThemeContext` a `@custom-variant dark`).

---

## 4. Checklist „stránka hotová“

- [ ] Žádný import z `@heroui/*` ani `lucide-react` (ikony z `@tabler/icons-react`).
- [ ] Barvy jen přes tokeny (`bg-primary`, `text-muted-foreground`, `border-border`…), žádné HEX v JSX.
- [ ] Nadpis stránky `font-display`, čísla `tabular-nums`, české formáty dat a částek.
- [ ] Funguje ve světlém i tmavém režimu.
- [ ] Funguje na 360 px šířky bez horizontálního scrollu stránky.
- [ ] Ovladatelné klávesnicí, focus je vidět (meruňkový ring).
- [ ] Loading stav (skeleton/spinner), prázdný stav, chybový stav s toastem.
- [ ] Animace jen `transform`/`opacity`, respektují `prefers-reduced-motion`.
- [ ] Chování podle rolí beze změny (SUPERADMIN / ADMIN / COORDINATOR / CAREGIVER / CLIENT).
- [ ] `npm run lint` a `npm test` prochází.

---

## 5. Otevřené otázky

| Otázka | Doporučení |
| --- | --- |
| Formuláře: nativní `<form>`, nebo react-hook-form + zod? | react-hook-form + zod: shadcn `form` na ně počítá, validace bude na jednom místě a sdílená mezi formuláři. |
| Ochranná známka / doména | Před veřejným spuštěním ověřit „CareTracker“ v databázích ÚPV a EUIPO. |

---

## 6. Odkazy

- shadcn/ui: https://ui.shadcn.com (Vite: https://ui.shadcn.com/docs/installation/vite, theming: https://ui.shadcn.com/docs/theming)
- Tailwind v4 `@theme`: https://tailwindcss.com/docs/theme
- motion pro React: https://motion.dev/docs/react
- React Aria Components (NumberField, DatePicker): https://react-spectrum.adobe.com/react-aria/components.html
- react-day-picker (české locale): https://daypicker.dev
- Písma: https://fonts.google.com/specimen/Bricolage+Grotesque, https://fonts.google.com/specimen/Figtree (SIL OFL 1.1)

---
