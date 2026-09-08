# NibbleMath — Spec Sheet

| | |
|---|---|
| **Status** | draft |
| **Date** | 2026-09-08 |
| **Author** | Andre |
| **Repo** | `/mnt/storage/Git/NibbleMath` (own git history, own GitHub repo) |

---

## 1. Purpose

Make it easy for home cooks to see the true cost of what they cook — per batch and
per item — while also being a great recipe book. Cost clarity drives smarter
shopping and cooking decisions.

## 2. Core features (MVP)

1. **Recipe books** — create multiple books; each holds recipe cards.
2. **Recipe cards** — name, book, yield (e.g. "24 cookies"), ingredients (amount + unit), steps, notes.
3. **Costing** — cost a recipe from your ingredient choices (which milk, which brand); package price prorated by amount used; shown per batch and per item (e.g. per cookie).
4. **Ingredient choice memory** — each ingredient in a recipe remembers the chosen product (brand/variant) + price; reused next time.
5. **Price lookup** — look up ingredient prices online from local AU stores (Woolworths, Coles, ALDI); pick which one you use; manual price override always available.
6. **Pantry** — quick-select list of go-to ingredients (product + price); tap to add to a recipe.
7. **OCR ingestion** — photograph a recipe (book, print, screen) → on-device OCR → editable draft recipe card.
8. **Units** — metric/grams default; translate between metric, US customary and Australian (g/kg, ml/L, cups, tsp, tbsp, oz).
9. **Scaling** — scale up/down by servings or factor; amounts, costs and per-item figures recompute; rounds to practical amounts (whole eggs, teaspoon increments).
10. **Backup** — export/import all data as JSON (reinstall-safe; no cloud).

## 3. Screens / UI

Phone and tablet; simple and cook-friendly: big touch targets, high-contrast text,
one-thumb reach. Tablet gets two-pane layouts (books + recipes; recipe + detail).

### 3.1 Books (home)

Purpose: pick a book; see recipes with cost at a glance.

```
+----------------------------------+
| NibbleMath                   +   |
+----------------------------------+
| Bakes | Weeknights | Preserves   |
+----------------------------------+
| Chocolate Chip Cookies           |
|   $2.10/batch · $0.09/cookie    |
| Sourdough Loaf                   |
|   $4.85/batch · $4.85/loaf      |
+----------------------------------+
```

### 3.2 Recipe (cook view)

Purpose: read and cook; see cost; scale; tap an ingredient to change its choice.

```
+----------------------------------+
| <- Chocolate Chip Cookies        |
| $2.10/batch · $0.09/cookie      |
| servings: [-] 24 [+]            |
+----------------------------------+
| 200g whole milk        [DairyLO*]|
|   $0.64              $0.11      |
| 120g butter (salted)  [Toll*]   |
|   $0.88              $0.19      |
| 2 eggs               [free-range]|
+----------------------------------+
| 1. Cream butter + sugar ...     |
| 2. Fold in flour ...            |
+----------------------------------+
```

### 3.3 Recipe editor

Purpose: create/edit a recipe card; add ingredients from pantry or price lookup.

```
+----------------------------------+
| Name: [Chocolate Chip Cookies ]  |
| Book: [Bakes v]  Yield: [24] [cookies v]
+----------------------------------+
| + ingredient                     |
| 200g [whole milk v] [DairyLO*]  x|
| 120g [butter (salted) v] [Toll*] x|
+----------------------------------+
| Steps: [1. ...] [2. ...] [+]    |
+----------------------------------+
|              [Save]              |
+----------------------------------+
```

### 3.4 Pantry

Purpose: quick-select go-to ingredients (product + price) for fast recipe building.

```
+----------------------------------+
| Pantry                        +  |
+----------------------------------+
| whole milk        DairyLO 1L    |
|   $3.20/L                   use |
| butter (salted)   Toll 500g     |
|   $3.90/500g                use |
| free-range eggs   12-dozen      |
|   $6.40/doz                 use |
+----------------------------------+
```

### 3.5 Price lookup

Purpose: search stores for a product; pick one; manual override.

```
+----------------------------------+
| [milk                       ] [Go]|
| store: [Woolworths v]            |
+----------------------------------+
| Woolworths  DairyLO Whole 1L     |
|   $3.20 · $3.20/L         [pick] |
| Coles       Anchor Whole 3L      |
|   $7.50 · $2.50/L         [pick] |
| ALDI        Aldi Whole 2L        |
|   $4.10 · $2.05/L         [pick] |
+----------------------------------+
| manual: [price] [pack size] [set]|
+----------------------------------+
```

### 3.6 OCR ingestion

Purpose: photograph a recipe → on-device OCR → editable draft card.

```
+----------------------------------+
| [camera]  [gallery]              |
+----------------------------------+
| (photo)                          |
| OCR: "200g whole milk, 120g     |
| butter, 2 eggs ..."              |
+----------------------------------+
| draft: 200g whole milk     [edit]|
|        120g butter         [edit]|
|        2 eggs              [edit]|
+----------------------------------+
| [discard]       [save as recipe] |
+----------------------------------+
```

## 4. Data

All local; no accounts, no cloud. Internet is used only for price lookup.

| Data | Store (file / embedded DB / server) | Notes |
|---|---|---|
| Books, recipes, steps, notes | Room (embedded SQLite) | survives updates |
| Ingredient catalog (canonical names, aliases, unit conversions) | Room + bundled seed JSON | grows via pantry/lookup |
| Ingredient choices (recipe → product + price) | Room | per-recipe memory |
| Pantry entries (product + price) | Room | |
| Price cache (store, product, price, fetchedAt) | Room | ~7-day TTL; offline falls back to last known price |
| OCR source images | app files dir | deleted after import |
| Settings (units, default book, store prefs) | DataStore | |

Survives an update: everything. Survives a reinstall: only via JSON export/import
(feature 10).

## 5. Stack

| Layer | Choice | Why |
|---|---|---|
| Shell / framework | Android — Kotlin, Jetpack Compose (Material3), single activity, responsive phone/tablet | per playbook; simple, fast, native |
| Language | Kotlin | |
| Data | Room + DataStore | embedded, offline-first |
| OCR | ML Kit Text Recognition (on-device) | offline, no API cost |
| Price lookup | OkHttp + per-store adapters (Woolworths, Coles, ALDI) | the only network feature |
| Packaging / distribution | Gradle → APK (UAT) / AAB (Play); fastlane internal track | per playbook |

Package `com.andre.nibblemath` (immutable once published); minSdk 26; targetSdk
37 (bump annually per Play deadline).

## 6. Out of scope

- Health/nutrition stats (v2 — on-device food-composition DB)
- Shopping list generation
- Accounts, cloud sync, server backup
- Recipe import from websites/URLs (OCR covers paper + screens)
- Pantry inventory (quantities on hand, expiry)
- Non-AU stores/regions
- Sharing recipes with other people

## Stage 0 exit criteria

- [x] Spec agreed
- [x] Repo exists (local + GitHub)
- [ ] Play Console app ID reserved — blocked: developer account registered,
      verification pending (owner action; reserve `com.andre.nibblemath` once
      verified, StreamScout's `com.andre.streamscout` is pending the same way)
