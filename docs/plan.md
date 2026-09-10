# NibbleMath — Implementation Plan

Follows the Android dev playbook (Projects repo, `docs/android-dev-playbook.md`).
Stage 0 is done except the Play app ID (blocked on developer-account
verification — everything else in this plan is unblocked).

Conventions: trunk-based, one PR per unit, `main` always releasable.
Definition of done per PR: compiles, lint clean, unit tests pass, manual smoke
on the emulator.

---

## Phase 0 — Scaffolding (playbook Stage 1) · ~1 day — done 2026-09-08

| # | Unit | Contents | Done when |
|---|------|----------|-----------|
| 0.1 | Project scaffold | Gradle (Kotlin DSL), Kotlin + Jetpack Compose (Material3), single activity + Compose Navigation skeleton, theme, `minSdk 26` / `targetSdk 37`, versioning from git tag (`versionCode = M*10000+m*100+p`), `gradle.properties` (`-Xmx2g`, no JDK pin — AGP 9.4.0 works on the system JDK), CI workflow (PR: build + lint + unit tests) | `./gradlew build` green in CI; debug APK installs and runs on the Pixel AVD |
| 0.2 | Keystore | `keytool -genkeypair` → `keystore/upload.jks` (RSA 2048, 10000 days, alias `upload`); backed up twice offline; never committed | `keytool -list` succeeds on a backup copy |

## Phase 1 — Domain core · ~3 days — done 2026-09-08

Pure Kotlin module, zero Android dependencies, 100% unit-tested. This is the
heart of the app — get it right before any UI.

| # | Unit | Contents | Done when |
|---|------|----------|-----------|
| 1.1 | Units & conversion | Unit model (g, kg, ml, L, tsp, tbsp, cup, oz, each), conversion table, metric-default formatting, **pack-size parsing** ("1 L", "500 g", "12 dozen", "1 kg bag") | unit tests incl. pack-size edge cases |
| 1.2 | Costing engine | ingredient cost = amount ÷ pack size × pack price; recipe batch cost = Σ; per-item = batch ÷ yield; handles missing price (excluded + flagged) | unit tests: proration, per-item, missing-price, zero-yield |
| 1.3 | Scaling engine | scale by factor or target servings; practical rounding (whole eggs, ½-tsp increments, 5 g for small amounts); exposes exact + rounded values | unit tests: scale up/down, rounding rules |

## Phase 2 — Data layer · ~3 days · done 2026-09-08

| # | Unit | Contents | Done when |
|---|------|----------|-----------|
| 2.1 | Room schema + repositories | Entities: books, recipes, steps, ingredients (canonical + aliases), products (brand, pack size, price), recipe-ingredient choices, pantry entries, price cache (store, product, price, fetchedAt); DAOs + repositories; bundled seed ingredient catalog (JSON) | in-memory repository tests; migration test harness in place |
| 2.2 | JSON export/import | versioned full backup/restore (books, recipes, pantry, choices, catalog edits); import validates schema version | round-trip test: export → mutate → import |

## Phase 3 — Recipe book UI (offline, no network) · ~1–2 weeks

| # | Unit | Contents | Done when |
|---|------|----------|-----------|
| 3.1 | Books + recipe list (home) | books CRUD, recipe cards with cost at a glance (batch + per-item), empty states, tablet two-pane | smoke: create book → see card |
| 3.2 | Recipe editor | card fields (name, book, yield + item label, notes), ingredient rows (amount + unit + ingredient picker), steps list; ingredient picker: search + pantry quick-select + manual entry | smoke: create a full recipe by hand |
| 3.3 | Cook view | cost summary (batch + per-item), scale control (servings stepper), ingredient rows with product chip + cost contribution, steps; tap ingredient → product picker (stub: manual price only) | smoke: scale 24→12 cookies, costs halve |
| 3.4 | Pantry | entries CRUD (ingredient + product + price), "use" → adds to recipe, search | smoke: pantry entry appears in ingredient picker |

- 3.1 done 2026-09-09
- 3.2 done 2026-09-09
- 3.3 done 2026-09-09
- 3.4 done 2026-09-09

## Phase 4 — Real prices · ~1 week

| # | Unit | Contents | Done when |
|---|------|----------|-----------|
| 4.1 | Product picker + manual price | attach product (brand, pack size, price) to a recipe ingredient or pantry entry; manual override always available; choice memory per recipe | smoke: pick product → cost updates → reopen recipe, choice remembered |
| 4.2 | Store adapters | `PriceSource` interface + Woolworths, Coles, ALDI adapters (OkHttp, public product pages, rate-limited, user-initiated only); price cache with ~7-day TTL; offline fallback = last known price; per-store health flag | adapter unit tests against fixture HTML/JSON; manual smoke: live search returns results |
| 4.3 | Price lookup UI | search box, store filter, results with pack size + $/unit, pick → attach to ingredient/pantry; manual entry fallback row | smoke: search "milk" → pick → cost updates |

- 4.1 done 2026-09-09
- 4.2 done 2026-09-09 — `PriceSource`, Woolworths/Coles/ALDI adapters, OkHttp fetcher, rate limiter, lenient JSON/HTML parser, 7-day TTL cache, offline fallback, per-store health, DI wiring, and fixture-based unit tests are green. Woolworths live search works via `POST /apis/ui/Search/products` after a homepage cookie warm-up; Coles live search works via `GET /api/bff/products/search` using a runtime-extracted BFF subscription key and default `storeId=840`; ALDI live search works via `GET https://asl.api.aldi.com.au/commerce/v3/product-search` because the original `api.aldi.com.au` gateway is bot-gated.
- 4.3 done 2026-09-09 — `ProductPickerDialog` has a store search box, All/Woolworths/Coles/ALDI filter, results with price, pack size, and $/unit, pick-to-product attach, and manual entry fallback; `PriceLookupClient.searchStores`, `CostSummary.unitPriceLabel`, and a fake-store `ProductPickerDialog` Compose UI test are green; live smoke searched `"milk"`, picked Woolworths, Coles, and ALDI results, and attached them to the pantry entry; full quality gate is green.

## Phase 5 — OCR ingestion · ~3–5 days

| # | Unit | Contents | Done when |
|---|------|----------|-----------|
| 5.1 | Camera + gallery capture | permission handling (grant + deny paths), image stored in app files dir | smoke: capture photo |
| 5.2 | ML Kit OCR + draft parsing | on-device Text Recognition; extract amount + unit + ingredient lines; produce **editable draft** recipe card (human-in-the-loop, never auto-saved); source image deleted after import | parse unit tests on fixture images; smoke: photo of a printed recipe → usable draft |

- 5.1 done 2026-09-09 — `OcrScreen` is reachable from the Books top bar, requests `CAMERA` at runtime, captures via `TakePicturePreview`, imports gallery images via `OpenDocument`, stores images under `files/ocr/`, previews the selected image, and deletes the stored image on Remove; `OcrScreenTest` and the full quality gate are green, and emulator smoke verified the permission prompt, camera capture, stored file, preview, and Remove cleanup.
- 5.2 done 2026-09-09 — ML Kit Text Recognition runs on-device from `OcrScreen`; `OcrTextParser` extracts recipe name, yield, and amount/unit/ingredient lines; `OcrDraftMapper` produces an editable, never-auto-saved draft with human-in-the-loop ingredient resolution via `IngredientPickerDialog`; Save creates/updates the recipe and deletes the source image, Discard deletes the source image and resets the OCR screen; `OcrTextParserTest`, `OcrScreenTest`, and the full quality gate are green, and emulator smoke verified gallery import of a fixture recipe, ingredient resolution, Save navigation to the recipe editor, source-image cleanup, and Discard cleanup.

## Stage 3 — Testing (playbook) · ~3–5 days

- Unit: Phases 1–2 + adapter parsing (JVM, `./gradlew test`)
- Instrumented: key flows — create book → add recipe → cost → scale; pantry;
  price lookup; OCR draft (Compose test rules, `connectedDebugAndroidTest`)
- Matrix: latest-API AVD + `minSdk` AVD + tablet AVD
- Performance: cold start, memory; Baseline Profiles if slow
- Security pass: gitleaks, network security config (no cleartext), permissions
  justified (CAMERA, INTERNET only)

Stage 3 progress (2026-09-10):
- Instrumented key-flow suite is green: `MigrationTest`, `BooksFlowTest`,
  `OcrDraftFlowTest`, `PantryFlowTest`, and `CookFlowTest` (10 tests) pass on
  `pixel` and `pixel_api26` across 3 consecutive full
  `connectedDebugAndroidTest` runs.
- Fixed a Compose state-update race in `PantryScreen`: pantry loading now
  returns to `Dispatchers.Main` before writing `entries`/`loaded`, preventing
  the API 26 empty-state flake.
- `AppTestBase` clears all tables before each test so in-memory Room state does
  not leak between instrumented tests.
- Matrix: latest-API and `minSdk` AVDs are covered; tablet AVD is still
  unavailable in this environment.
- Security pass completed manually because `gitleaks` is unavailable: no
  hardcoded secrets, no cleartext traffic, and permissions remain limited to
  `INTERNET` plus optional `CAMERA`.
- Performance baseline recorded on 2026-09-10 with the debug APK on the two
  available emulators (`emulator-5554`: API 36 / Android 16, 1080x2400;
  `emulator-5556`: API 26 / Android 8.0, 1080x1920). After one discarded
  first-launch warm-up, five cold starts produced `am start -W` TotalTime
  results of 659, 672, 694, 666, and 665 ms on API 36 (median 666 ms) and 511,
  503, 502, 525, and 491 ms on API 26 (median 503 ms). Three idle memory
  samples after launch were 86.2, 86.5, and 86.6 MB TOTAL PSS on API 36 and
  50.1, 50.0, and 50.2 MB TOTAL on API 26. These are debug-build emulator
  numbers; no Baseline Profile is added yet, but revisit if release cold start
  or memory regresses noticeably.
- Signed release verification on 2026-09-10: a replacement upload keystore was
  generated because the original password was unrecoverable; no release had
  been published with the old key. `apksigner verify --print-certs` succeeded
  for SHA-256
  `4016a646ab6637a1fe153f6be8bbc066baf99e062c9f44ad82d5bab640674db8`. The
  signed APK installed and launched on both emulators. Five release cold starts
  after the first launch were 303, 298, 312, 322, and 332 ms on API 36 (median
  312 ms) and 369, 354, 370, 381, and 365 ms on API 26 (median 369 ms). Three
  idle release memory samples were 28.5, 26.9, and 26.9 MB TOTAL PSS on API 36
  and 23.5, 23.4, and 23.6 MB TOTAL on API 26.

## Stage 4 — UAT (playbook) · ~2–5 days

Signed release APK → real phone via scrcpy → playbook §6.2 checklist →
sign-off recorded → tag `vX.Y.0`.

Status 2026-09-10: real-device UAT on the Pixel 8 Pro is complete — all 12
playbook §6.2 checklist items pass (see `docs/uat-2026-09-10.md`), including a
valid 30-minute foreground screen-on battery/data soak (−2% battery, zero app
data usage, no crashes). The only remaining item is a packaging decision:
sign off the current `0.0.0-dev` build as the UAT baseline, or rebuild with a
proper RC `versionName`/`versionCode` (e.g. `0.1.0-rc.1` / `10100`) before the
final sign-off and `vX.Y.0` tag.

## Stage 5 — Play

Prep now (no verification needed):

- Store listing: title (≤30), short (≤80), full (≤4000) description
- Icon 512×512 + feature graphic 1024×500; screenshots from UAT
- Privacy policy URL (host in Projects repo or GitHub Pages)
- Data safety form: no data collected; network use = ingredient price lookup

Remaining (account verified 2026-09-08):

1. App ID reserved — done: `com.loopworks.nibblemath` created (`com.andre.streamscout` still pending)
2. CI: tag → signed AAB → fastlane internal track (service account secrets)
3. Promote internal → closed → production (staged 10 → 50 → 100)

---

## Dependencies

```
0.1 ─┬─ 1.1 ─ 1.2 ─ 1.3 ─┬─ 2.1 ─ 2.2 ─┬─ 3.1 ─ 3.2 ─ 3.3 ─ 3.4
     │                    │             └──────────────────────┤
     └─ 0.2 (independent)└─────────────────────────────────────┤
                                                               ▼
                                              4.1 ─ 4.2 ─ 4.3   5.1 ─ 5.2
```

- Phase 1 is the critical path — pure logic, no device needed, start first.
- Phase 5 (OCR) is independent of Phase 4 — can run in parallel once 2.1 lands.
- 3.3 stubs the product picker (manual price) so UI work never waits on 4.x.

## Risks

| Risk | Mitigation |
|---|---|
| Store sites change / anti-bot break adapters | adapters behind `PriceSource` interface; manual override always works; cache TTL keeps last known prices; per-store health flag degrades gracefully |
| Scraping ToS / rate limiting | public product pages only, user-initiated, rate-limited, identifiable UA |
| OCR parsing quality on arbitrary layouts | human-in-the-loop draft, every field editable, never auto-saved |
| Pack-size normalization (1 L vs 1000 ml, "12 dozen") | unit system owns pack parsing; ambiguous packs force manual entry |
| Costing trust (wrong price → wrong "true cost") | every cost shows its source (store + date or manual); stale cache flagged |

## Rough timeline

| Phase | Effort |
|---|---|
| 0 Scaffolding | 1 day |
| 1 Domain core | 3 days |
| 2 Data layer | 3 days |
| 3 Recipe book UI | 1–2 weeks |
| 4 Real prices | 1 week |
| 5 OCR | 3–5 days (parallel with 4) |
| Stages 3–4 (test + UAT) | 1 week |

≈ 5–6 weeks to a signed-off release candidate, assuming Play verification
lands before Stage 5.
