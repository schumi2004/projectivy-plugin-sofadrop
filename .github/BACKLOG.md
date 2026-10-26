# Backlog: mogelijke upstream-PR's

Alleen voor deze fork (staat in `.github/`, gaat nooit mee in een PR). Elke PR wordt een eigen branch
vanaf `main` met alleen de commits van die ene wijziging. De commits tussen haakjes staan op
`feature/test` (gebouwd op de oude 1.0.x) en moeten opnieuw op de huidige upstream worden toegepast.

Status t.o.v. upstream 1.1.0: **ontbreekt** = niet in upstream, **controleren** = upstream heeft iets
vergelijkbaars of de code is veranderd, **waarschijnlijk al gedaan** = lijkt upstream al te hebben.

## Kleine fixes

- [ ] **Footer: bron even groot als de streamingregel.** Bij "Streaming en bron van de lijst" tekent
  `drawSource(primary = false)` in `WallpaperRenderer.kt` de bron kleiner en grijzer (40 i.p.v. 50,
  grijs 165 i.p.v. 205). Upstream doet dit bewust ("dimmer"), dus in de PR uitleggen waarom. — *ontbreekt*
- [ ] **Sleutels niet in de Projectivy-backup.** `Settings.export()`/`import()` (via de exported
  service, voor elke app bereikbaar) lezen/schrijven `tmdb_token`, `trakt_client_id`,
  `trakt_client_secret` en `mdblist_api_key`. (d4fcbdb) — *ontbreekt, upstream exporteert ze nog*
- [ ] **Minimaal jaar op jaar van eerste uitzending** bij series (nu de laatste uitzending, waardoor een
  lopende serie uit 1963 door "vanaf 2000" komt). Alleen relevant samen met het minimaal-jaarfilter.
  (13627ae) — *ontbreekt*

## Selectie van titels

- [ ] **Dubbelen binnen de marge-pool.** Titels die een eerdere lijst al nam, tellen mee in dezelfde
  pool als de filters, zodat de lijst ze kan compenseren; filters gaan vóór DUPLICATE; geen extra
  TMDB-details zonder filter; een mislukte titel telt niet als genomen. (5708cbe, ccbd3fe) —
  *controleren, upstream heeft `takeIncluded` nog*
- [ ] **Korte lijst verder inlezen** (tot 200 titels) als er na filters/dubbelen te weinig over is.
  (f14dbf9) — *ontbreekt*
- [ ] **Aanvullen uit andere lijsten** tot titels per lijst × lijsten, om de beurt. (562924e, alleen de
  worker; de workflow-wijziging niet) — *ontbreekt*
- [ ] **Unit-tests voor de selectie** (`SelectionTest`, `TmdbCatalog.select/fetchCount/grow`).
  (d4fcbdb, f14dbf9) — *ontbreekt*

## Filters

- [ ] **Minimale rating.** (3f136e8) — *controleren: upstream gebruikt `minRating` wel, maar mogelijk
  alleen voor de weergave van ratings*
- [ ] **Minimaal jaar van uitgave** (vast jaar naast de rollende maximale leeftijd). (a3bb11e) — *ontbreekt*
- [ ] **Blocklist per titel**, vanaf de pagina van een lijst. (19aaa33) — *ontbreekt*
- [ ] **Blokkeren vanuit Wallpapers bekijken (▼)**, met label GEBLOKKEERD. (59ad44c) — *ontbreekt*
- [ ] **Lijst met geblokkeerde titels** (namen bewaard, per titel deblokkeren). (13627ae) — *ontbreekt*
- [ ] **Genrenamen in het filterscherm gelijk aan de wallpapers** (Talk → Talkshow, News → Nieuws).
  (d2beef2) — *waarschijnlijk al gedaan: upstream `Genres.kt` noemt ze "like the wallpapers name them";
  News → Nieuws controleren*

## Gedrag

- [ ] **Wijzigingen tellen als "nog niet toegepast"** voor minimaal jaar, rating, genres en blocklist
  (signature), zodat de set bij het verlaten van de instellingen opnieuw gemaakt wordt. (59ad44c) —
  *alleen nodig voor filters die upstream niet in de signature heeft*
- [ ] **Nieuwe set na een wijziging tijdens het genereren** (anders wacht hij op de volgende geplande
  update). (13627ae) — *controleren*

## Weergave en vertaling

- [ ] **Samenvatting 4 tot 6 regels**, zelfde lettergrootte. (ca475ab) — *controleren, upstream heeft de
  layout veranderd*
- [ ] **Nederlandse vertaling van de instellingen** (`values-nl/strings.xml`, incl. Projectivy-menunamen
  Weergave › Achtergrond › Launcher achtergrond). (53ec931, b91b03c) — *ontbreekt; upstream heeft veel
  nieuwe teksten, dus opnieuw vertalen*

## Niet voor upstream (fork-only)

- CI-workflow, vaste debug-sleutel, versienummers, releases voor de updater: `.github/workflows/build-dev.yml`.
- Trakt redirect URI: opgelost door ook de upstream-URI in de eigen Trakt-app te registreren.
