# NibbleMath — Implementation Plan

Follows the Android dev playbook (Projects repo, `docs/android-dev-playbook.md`).
Stage 0 is done except the Play app ID (blocked on developer-account
verification — everything else in this plan is unblocked).

Conventions: trunk-based, one PR per unit, `main` always releasable.
Definition of done per PR: compiles, lint clean, unit tests pass, manual smoke
on the emulator.

---

## Phase 0 — Scaffolding (playbook Stage 1) · ~1 day

| # | Unit | Contents | Done when |
|---|------|----------|-----------|
| 0.1 | Project scaffold | Gradle (Kotlin DSL), Kotlin + Jetpack Compose (Material3), single activity + Compose Navigation skeleton, theme, `minSdk 26` / `targetSdk 36`, versioning from git tag (`versionCode = M*10000+m*100+p`), `gradle.properties` (JDK 17 pin, `-Xmx2g`), CI workflow (PR: build + lint + unit tests) | `./gradlew build` green in CI; debug APK installs and runs on the Pixel AVD |
| 0.2 | Keystore | `keytool -genkeypair` → `keystore/upload.jks` (RSA 2044, 10000 days, alias `upload`); backed up twice offline; never committed | `keytool -list` succeeds on a backup copy |

## Phase 1 — Domain core · ~3 days

Pure Kotlin module, zero Android dependencies, 100% unit-tested. This is the
heart of the app — get it right before any UI.

| # | Unit | Contents | Done when |
|---|------|----------|-----------|
| 1.1 | Units & conversion | Unit model (g, kg, ml, L, tsp, tbsp, cup, oz, each), conversion table, metric-default formatting, **pack-size parsing** ("1 L", "500 g", "12 dozen", "1 kg bag") | unit tests incl. pack-size edge cases |
| 1.2 | Costing engine | ingredient cost = amount ÷ pack size × pack price; recipe batch cost = Σ; per-item = batch ÷ yield; handles missing price (excluded + flagged) | unit tests: proration, per-item, missing-price, zero-yield |
| 1.3 | Scaling engine | scale by factor or target servings; practical rounding (whole eggs, ½-tsp increments, 5 g for small amounts); exposes exact + rounded values | unit tests: scale up/down, rounding rules |

## Phase 2 — Data layer · ~3 days

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

## Phase 4 — Real prices · ~1 week

| # | Unit | Contents | Done when |
|---|------|----------|-----------|
| 4.1 | Product picker + manual price | attach product (brand, pack size, price) to a recipe ingredient or pantry entry; manual override always available; choice memory per recipe | smoke: pick product → cost updates → reopen recipe, choice remembered |
| 4.2 | Store adapters | `PriceSource` interface + Woolworths, Coles, ALDI adapters (OkHttp, public product pages, rate-limited, user-initiated only); price cache with ~7-day TTL; offline fallback = last known price; per-store health flag | adapter unit tests against fixture HTML/JSON; manual smoke: live search returns results |
| 4.3 | Price lookup UI | search box, store filter, results with pack size + $/unit, pick → attach to ingredient/pantry; manual entry fallback row | smoke: search "milk" → pick → cost updates |

## Phase 5 — OCR ingestion · ~3–5 days

| # | Unit | Contents | Done when |
|---|------|----------|-----------|
| 5.1 | Camera + gallery capture | permission handling (grant + deny paths), image stored in app files dir | smoke: capture photo |
| 5.2 | ML Kit OCR + draft parsing | on-device Text Recognition; extract amount + unit + ingredient lines; produce **editable draft** recipe card (human-in-the-loop, never auto-saved); source image deleted after import | parse unit tests on fixture images; smoke: photo of a printed recipe → usable draft |

## Stage 3 — Testing (playbook) · ~3–5 days

- Unit: Phases 1–2 + adapter parsing (JVM, `./gradlew test`)
- Instrumented: key flows — create book → add recipe → cost → scale; pantry;
  price lookup; OCR draft (Compose test rules, `connectedDebugAndroidTest`)
- Matrix: latest-API AVD + `minSdk` AVD + tablet AVD
- Performance: cold start, memory; Baseline Profiles if slow
- Security pass: gitleaks, network security config (no cleartext), permissions
  justified (CAMERA, INTERNET only)

## Stage 4 — UAT (playbook) · ~2–5 days

Signed release APK → real phone via scrcpy → playbook §6.2 checklist →
sign-off recorded → tag `vX.Y.0`.

## Stage 5 — Play (blocked on account verification)

Prep now (no verification needed):

- Store listing: title (≤30), short (≤80), full (≤4000) description
- Icon 512×512 + feature graphic 1024×500; screenshots from UAT
- Privacy policy URL (host in Projects repo or GitHub Pages)
- Data safety form: no data collected; network use = ingredient price lookup

After verification:

1. Reserve `com.andre.nibblemath` (and `com.andre.streamscout`)
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
