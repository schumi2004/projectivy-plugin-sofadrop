# Backlog: mogelijke upstream-PR's

Alleen voor deze fork (staat in `.github/`, gaat nooit mee in een PR). Elke PR wordt een eigen branch
vanaf `main` met één commit voor die ene wijziging, en wordt voor het testen in `dev` gemerged. De
commits tussen haakjes staan op `feature/test` (gebouwd op de oude 1.0.x) en moeten opnieuw op de
huidige upstream worden toegepast.

Status t.o.v. upstream 1.1.0: **ontbreekt** = niet in upstream, **controleren** = upstream heeft iets
vergelijkbaars of de code is veranderd, **waarschijnlijk al gedaan** = lijkt upstream al te hebben.

## Klaar als PR-branch (in `dev` om te testen)

- [x] **Sofascreen, de wallpapers als screensaver** — `pr/screensaver`. Eigen inhoud (stijl,
  samenvatting, cast, voettekst, tekst linksonder), klok (Roboto Bold, datum, lijntje, 24/12 uur),
  weer via Open-Meteo, zelfherstel van ontbrekende screensaverkopieën.
- [x] **Leeftijdsfilter: 10 jaar en vanaf jaar** — `pr/age-filter`. Jaar in dezelfde instelling als
  de maanden (≥ 1900 = jaar), dus ook per lijst; series vanaf de eerste uitzending. (a3bb11e, 13627ae)
- [x] **Sleutels niet in de Projectivy-backup** — `pr/backup-without-secrets`. (d4fcbdb)
- [x] **Nederlandse vertaling** — `pr/dutch-translation`. Screensaver en jaarfilter hebben hun
  Nederlands in eigen bestanden (`values-nl/strings_screensaver.xml`, `strings_age.xml`); samenvoegen
  in `strings.xml` zodra upstream de vertaling heeft.
- [x] **Footer: bron even groot als de streamingregel** — `pr/footer-fix`. Nog niet zeker of die
  wordt aangeboden. Komt hij upstream vóór de screensaver, dan geeft `WallpaperRenderer.kt` een klein
  conflict (zie de merge in `dev`).
- [x] **Updater** — `pr/update-check`.

- [x] **Selectie: dubbelen en aanvullen tot de gekozen aantallen** — `pr/selection`. Dubbelen en
  verborgen bekeken titels binnen de pool van de lijst; een titel pas "genomen" als hij gemaakt is;
  korte lijst verder inlezen (tot 200); aanvullen uit de andere lijsten; `SelectionTest`.
  (5708cbe, ccbd3fe, f14dbf9, 562924e, d4fcbdb)
- [x] **Minimale rating** — `pr/min-rating`. Standaardfilter voor automatische lijsten en per lijst;
  titels met minder dan 20 stemmen blijven staan. (3f136e8)
- [x] **Blocklist** — `pr/blocklist`. Per titel vanaf de pagina van een lijst, ▼ in Wallpapers
  bekijken met label GEBLOKKEERD, lijst met geblokkeerde titels (deblokkeren, alles wissen), in de
  signature. (19aaa33, 59ad44c, 13627ae)

De drie filter-PR's gaan uit van `main` en raken dezelfde regels (filtercheck in `TmdbCatalog`,
`Exclusion`, ID's in de filterschermen, eind van `strings.xml`): na het mergen van één krijgen de
andere kleine conflicten (opgelost in `dev`).

## Geparkeerd

- [ ] **Nieuwe set na een wijziging tijdens het genereren** (anders wacht hij op de volgende geplande
  update). (13627ae) — *controleren*
- [ ] **Samenvatting 4 tot 6 regels**, zelfde lettergrootte. (ca475ab) — *controleren*
- [ ] **Genrenamen in het filterscherm gelijk aan de wallpapers** (News → Nieuws). (d2beef2) —
  *waarschijnlijk al gedaan*
- [ ] **Lettertype kiezen** voor heel Sofadrop (wallpapers en screensaverklok), een paar meegeleverde
  OFL-fonts. Overnemen van Projectivy kan niet: de plug-in-API geeft geen thema of lettertype door.

## Afgevallen

- "Tot jaar" (een periode, zoals de jaren 80–90): geen toegevoegde waarde.
- Klok van de screensaver af en toe verplaatsen: hij verschuift elke minuut al een paar pixels.

## Niet voor upstream (fork-only)

- CI-workflow, vaste debug-sleutel, versienummers, releases voor de updater: `.github/workflows/build-dev.yml`.
- Trakt redirect URI: opgelost door ook de upstream-URI in de eigen Trakt-app te registreren.
