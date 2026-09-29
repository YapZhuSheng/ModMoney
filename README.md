<div align="center">

# 🏁 ModMoney

**Track every rupee of your car build — and get warned before a fitment mistake costs you.**

An offline-only Android app for car-modification budgeting with an automatic compatibility rule engine.

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.23-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-BOM_2024.05-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Material 3](https://img.shields.io/badge/Material_3-Dark_First-FFB74D?style=flat-square&logo=materialdesign&logoColor=white)](https://m3.material.io/)
[![Room](https://img.shields.io/badge/Room-2.6.1-00C853?style=flat-square&logo=sqlite&logoColor=white)](https://developer.android.com/training/data-storage/room)
[![minSdk](https://img.shields.io/badge/minSdk-24-brightgreen?style=flat-square&logo=android&logoColor=white)](https://apilevels.com/)
[![targetSdk](https://img.shields.io/badge/targetSdk-34-brightgreen?style=flat-square&logo=android&logoColor=white)](https://apilevels.com/)
[![Tests](https://img.shields.io/badge/tests-25_passing-success?style=flat-square&logo=junit&logoColor=white)](#tests)
[![Offline](https://img.shields.io/badge/network-none-9E9E9E?style=flat-square&logo=googlechrome&logoColor=white)](#privacy)
[![License](https://img.shields.io/badge/license-MIT-blue?style=flat-square)](LICENSE)

**No `INTERNET` permission. No analytics. No accounts. Your build stays on your phone.**

[⬇️ Download APK](#downloads) · [🛠 Build from source](#build-and-run) · [📖 Full docs](#contents)

</div>

---

## Contents

- [Why this exists](#why-this-exists)
- [Feature tour](#feature-tour)
- [Screenshots](#screenshots)
- [Downloads](#downloads)
- [Build and run](#build-and-run)
- [Project layout](#project-layout)
- [Architecture](#architecture)
- [The compatibility rule engine](#the-compatibility-rule-engine)
- [Money model](#money-model)
- [Sample data](#sample-data)
- [Tests](#tests)
- [Design decisions and assumptions](#design-decisions-and-assumptions)
- [Known limitations](#known-limitations)
- [Privacy](#privacy)
- [License](#license)

---

## Why this exists

Car builds fail for two reasons: the budget quietly doubles, and a part you already paid for doesn't fit. Spreadsheets catch neither. Forum advice catches both, but only if you happen to ask the right question before you click *Buy*.

ModMoney sits between those two worlds. You log every mod with an estimate, mark it bought or installed when it lands, and the app tracks where you are against your cap. The part that makes it more than a list: when you enter structured specs — rim diameter, tire size, offset, ride-height drop — a rule engine reads the **whole build** and tells you what won't work, *live in the editor as you type*, before you've saved a thing.

The headline case from the spec: drop a set of 17" wheels onto a car that left the factory on 16"s, and ModMoney doesn't just say "warning" — it computes the tire aspect ratio that would preserve your stock rolling diameter and names the exact speedometer error you'd see if you didn't.

---

## Feature tour

<table>
<tr>
<td width="50%" valign="top">

### 🚗 Garage
Add cars with a nickname, model, year and budget cap. Each card shows spend against cap with a progress bar, mod count, and either remaining budget or a projected-overspend warning.

</td>
<td width="50%" valign="top">

### 🔧 Mod list
Per car. Name, category, estimated price, actual price, status (planned / bought / installed), priority, and free-text compatibility notes. Sorted by priority → status → newest.

</td>
</tr>
<tr>
<td width="50%" valign="top">

### 📊 Dashboard
Total spent vs. budget cap with a progress bar that draws a faded secondary fill for *projected* spend — a build that's under cap today but headed over is visible at a glance. Plus per-category breakdown with proportional bars, build progress by status, and a roll-up of every non-informational warning.

</td>
<td width="50%" valign="top">

### ⚠️ Compatibility warnings
Nine rules over structured specs (rim, tire, offset, ride-height). Warnings appear **live in the editor as you type**, before you save. Suggestions include the exact tire profile that preserves stock rolling diameter, and speedometer error in km/h.

</td>
</tr>
<tr>
<td width="50%" valign="top">

### 🌙 Dark mode by default
A tuned dark-first Material 3 palette, not the system default. Toggle to light for the session from the garage top bar. Dynamic colour (Material You) is intentionally off so the palette is consistent.

</td>
<td width="50%" valign="top">

### 📴 Truly offline
No `INTERNET` permission declared in the manifest. No analytics, no crash reporting, no accounts. The Room database lives in app-private storage and is included in Android's auto-backup rules so device transfer carries it.

</td>
</tr>
</table>

---

## Screenshots

A visual mockup rendered from the actual seeded state — every number, every warning, every colour pulled from the real Room database via a Robolectric dump. Open it in any browser:

👉 **[`docs/preview.html`](docs/preview.html)** — three-panel preview of the Garage, Dashboard and Mod list as they appear in the app.

The Honda City sample build shown there: **118,500 spent (65.8% of cap), 203,500 projected (23,500 over)** — the projected-overspend banner fires on first launch, the 17" wheel on a 16"-stock car triggers the diameter rule, the correctly sized 205/45 R17 tire reports a good rolling-diameter match, and the −35 mm lowering springs trip the combined-fitment rule.

---

## Downloads

Prebuilt APKs are attached to the [latest release](https://github.com/YapZhuSheng/ModMoney/releases).

| Artifact | Size | Application ID | Notes |
|---|---|---|---|
| `ModMoney-debug.apk` | ~16 MB | `com.modmoney.debug` | Unminified, debuggable, installs alongside release |
| `ModMoney-release.apk` | ~1.1 MB | `com.modmoney` | R8 minified + resource shrunk |

```bash
adb install -r ModMoney-debug.apk
```

> ⚠️ **Signing notice.** Both APKs are signed with the checked-in debug key (`keystore/debug.keystore`, password `android`, alias `androiddebugkey`) so that `assembleRelease` is reproducible out of the box. They are installable and fully functional, but the release build is **not** signed with production credentials. Before publishing anywhere, generate your own keystore and update the `release` signing config in `app/build.gradle.kts`.

---

## Build and run

Requirements: **JDK 17**, **Android SDK** with platform 34 and build-tools 34.0.0. The Gradle wrapper (8.7) is included.

### Android Studio

Open the project root (the directory containing `settings.gradle.kts`). Studio will sync and create `local.properties` pointing at your SDK. Run the `app` configuration. Any Studio version bundling AGP 8.4 or newer works (Jellyfish / 2023.3.1+).

### Command line

```bash
# Point Gradle at your SDK if it isn't already discoverable
echo "sdk.dir=/path/to/Android/sdk" > local.properties

./gradlew :app:assembleDebug        # debug APK
./gradlew :app:assembleRelease      # release APK (minified)
./gradlew :app:testDebugUnitTest    # 25 JVM tests
./gradlew :app:installDebug         # install to a connected device
```

Outputs land in `app/build/outputs/apk/{debug,release}/`.

### Low-memory build machines

`gradle.properties` sets `kotlin.compiler.execution.strategy=in-process` and disables parallel execution. On a machine with ~4 GB of RAM, letting Kotlin fork its own compiler daemon alongside the Gradle daemon causes the OS to kill one of them mid-build. If you're building on a well-resourced workstation you can remove both lines for faster builds.

---

## Project layout

```
ModMoney/
├── settings.gradle.kts
├── build.gradle.kts               # plugin versions
├── gradle.properties              # memory / compiler strategy
├── local.properties.example       # copy to local.properties
├── keystore/debug.keystore        # checked-in debug key
├── gradle/wrapper/                # Gradle 8.7 wrapper
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── java/com/modmoney/
        │   │   ├── ModMoneyApp.kt          # Application, manual DI
        │   │   ├── MainActivity.kt         # single activity, theme state
        │   │   ├── data/
        │   │   │   ├── Enums.kt            # status, priority, category, severity
        │   │   │   ├── Entities.kt         # Car, Mod
        │   │   │   ├── Converters.kt       # enum <-> String
        │   │   │   ├── Daos.kt             # CarDao, ModDao, aggregate SQL
        │   │   │   ├── ModMoneyDatabase.kt # Room DB + seed callback
        │   │   │   ├── SampleData.kt       # Honda City build
        │   │   │   └── GarageRepository.kt
        │   │   ├── rules/
        │   │   │   └── CompatibilityRules.kt   # the rule engine
        │   │   ├── ui/
        │   │   │   ├── GarageViewModel.kt  # state aggregation
        │   │   │   ├── Navigation.kt       # routes
        │   │   │   ├── theme/Theme.kt      # dark-first M3 scheme
        │   │   │   ├── components/Common.kt
        │   │   │   ├── garage/             # GarageScreen, CarEditorScreen
        │   │   │   ├── mods/               # CarHomeScreen, ModEditorScreen
        │   │   │   └── dashboard/DashboardScreen.kt
        │   │   └── util/Money.kt
        │   └── res/                        # strings, theme, adaptive icon
        └── test/java/com/modmoney/
            ├── CompatibilityRulesTest.kt   # 17 rule + money tests
            └── DatabaseSeedTest.kt         # 8 Room tests (Robolectric)
```

---

## Architecture

Deliberately flat, because the app is small:

```
Compose screens
      │  collectAsState
GarageViewModel  ──►  CarDetailState (car + mods + category totals + warnings)
      │
GarageRepository
      │
Room (CarDao, ModDao)  ◄── SQL aggregates for the dashboard
```

- **Single source of truth.** Every screen reads Room `Flow`s. There is no in-memory cache to drift out of sync — editing a mod updates the list, the dashboard and the garage card simultaneously.
- **`CarDetailState`** combines five flows (car, mods, per-category spend, total spent, total projected) into one immutable state object. Compatibility warnings are computed lazily inside it and memoised, so the mod list and dashboard share one evaluation pass.
- **Aggregates run in SQL,** not in Kotlin, so the dashboard doesn't need to load and fold every row. `DatabaseSeedTest` asserts the SQL and the equivalent Kotlin properties produce identical numbers, which guards against the two definitions drifting apart.
- **Manual DI.** `ModMoneyApp` builds the database and repository lazily. Hilt would add annotation processing and build time for one object graph with two nodes.
- **Navigation** is Navigation-Compose with four routes: garage, car editor, car home (dashboard + mods tabs), mod editor.

---

## The compatibility rule engine

`rules/CompatibilityRules.kt` is pure Kotlin with no Android or Room dependency, which is what makes it cheap to test exhaustively.

Structured specs on a mod are all optional. **A rule only fires when the data it needs is present** — a set of floor mats with no specs produces zero warnings, so the feature never nags. Likewise, if a car has no stock reference figures, every rule that compares against stock silently disables.

### The nine rules

| Rule | Fires when | Severity |
|---|---|---|
| `wheel_diameter_up` | Rim larger than the car's stock diameter | Caution (+1"), Critical (+2" or more) |
| `rolling_diameter` | Wheel/tire combo changes overall diameter vs stock | Info within ±3%, Caution to ±5%, Critical beyond |
| `low_profile` | Tire aspect ratio ≤ 45 | Info, Caution at ≤ 35 |
| `offset_change` | Offset differs from assumed stock by ≥ 12 mm | Caution |
| `tire_width_up` | Section width ≥ 15 mm wider than stock | Info, Caution at ≥ 30 mm |
| `lowered` / `raised` | Ride-height delta recorded | Info, Caution past 40 mm drop |
| `lowered_plus_big_wheel` | Lowering **and** an oversized wheel exist in the same build | Caution |
| `wheel_no_tire` | Wheel mod with no tire entry anywhere in the build | Info |
| `brake_clearance` | Brake mod with a 16" or smaller wheel in the build | Info |

### Two things worth calling out

**The headline rule does the arithmetic for you.** Rolling diameter is `rim_mm + 2 × (width × aspect / 100)`. When you add a bigger rim, the engine solves for the aspect ratio that preserves stock rolling diameter and names it. On the sample Honda City (stock 185/55 R16), adding a 17" rim suggests roughly a 45-series tire — and the seeded 205/45 R17 is measured at +1.05%, which the engine reports as a good match rather than a problem. If you'd kept a 55-series tire on that 17" rim, it reports the speedometer error explicitly: at an indicated 100 km/h you'd actually be doing about 104.

**Rules can see sibling mods.** `evaluate(mod, car, allMods)` gets the whole build, which is how `lowered_plus_big_wheel` notices that your springs and your wheels are individually fine but jointly a clearance risk, and how a tire entry can borrow the rim diameter from a separate wheel entry to compute rolling diameter. The rolling-diameter rule reports only from the entry holding tire data, so the same warning doesn't appear twice on both rows.

Every message is phrased as something to verify, never as a fitment guarantee, and the editor says so under the warning list.

---

## Money model

Three statuses, two totals. The distinction matters and is applied consistently in Kotlin (`Mod.spent`, `Mod.projected`) and in SQL (`observeTotalSpent`, `observeTotalProjected`):

- **Spent** counts `BOUGHT` and `INSTALLED` only. Cash has left your account once the part is bought, even if it's still in a box.
- **Projected** counts every mod including `PLANNED` — what the build costs if you finish it.
- `estimatedPrice` is always required; `actualPrice` is optional. A bought or installed mod with no actual price recorded yet **falls back to its estimate** rather than counting as zero, which would understate spend. The editor states this explicitly when it applies.
- **Variance** (`actual − estimated`) shows on each mod row once an actual price exists — red over, green under.

The dashboard surfaces the gap between the two totals: if you're under cap now but the projection exceeds it, you get an explicit note naming the shortfall and the three ways out (trim scope, raise the cap, drop a low-priority item).

Amounts are stored as plain `Double` and rendered with thousands separators and **no currency symbol** — see [assumptions](#design-decisions-and-assumptions).

---

## Sample data

First launch seeds one worked build: a **2020 Honda City VX CVT**, budget cap 180,000, stock 185/55 R16.

Eleven mods spanning wheels, tires, suspension (×2), exhaust, engine, brakes, lighting, audio, exterior and interior. The figures are chosen to exercise every UI path:

- **118,500 spent** — 65.8% of cap, so the progress bar sits in its normal colour range.
- **203,500 projected** — 23,500 over cap, so the projected-overspend warning is visible immediately.
- A mix of all three statuses and all three priorities.
- Actual prices both under estimate (wheels −1,500) and over (tires +1,200), so variance renders in both colours.
- **17" wheels on a 16"-stock car**, which trips the headline diameter rule, paired with a correctly sized 205/45 R17 that the rolling-diameter rule confirms as a good match. Lowering springs at −35 mm additionally trip the combined lowered-plus-big-wheel clearance rule.

Seeding runs from Room's `onCreate` callback and is guarded by a row count, so it happens exactly once and never duplicates. Deleting the sample car does not bring it back.

---

## Tests

25 JVM tests, no device or emulator required:

```bash
./gradlew :app:testDebugUnitTest
```

**`CompatibilityRulesTest` (17 tests)** covers the rule engine and money semantics: that a bigger wheel warns about *both* profile and speedometer and suggests a replacement size; that a 2" jump escalates to critical; that a stock-size wheel stays silent; that a correctly sized upsize reports a good match while a tall-profile upsize reports real speedo error; that warnings sort most-severe-first; that missing specs and missing stock references both disable rules cleanly; and each of the four money-semantics rules.

**`DatabaseSeedTest` (8 tests, Robolectric)** exercises the **real Room schema and the actual aggregate SQL** rather than mocks: seed correctness and idempotency, both totals against hand-computed figures, SQL-vs-Kotlin agreement, category grouping and sort order, enum round-tripping through type converters, foreign-key cascade on car deletion, and that the seeded build really does surface the intended warnings.

All 25 pass. Confirming what was *not* verified: the app was built and its logic tested, but it was never launched on a device or emulator, so the UI is unexercised at runtime — see [limitations](#known-limitations).

---

## Design decisions and assumptions

Where the brief was open, these are the calls made and why.

**Currency is symbol-less.** Amounts render as `118,500`, not `₹118,500` or `$118,500`. The brief didn't name a market, and a Honda City build implies one where hardcoding `$` would be wrong. Formatting lives in `util/Money.kt`, so adding a symbol is a one-line change in one place.

**No decimals on money.** Mod pricing is realistically whole-unit. Values are stored as `Double` and rounded for display. For an expense tracker this is fine; if you needed audited accounting you'd store minor units as `Long` instead.

**Dark mode is the default, not "follow the system."** The brief said dark mode default, so `ModMoneyTheme(darkTheme = true)` ignores `isSystemInDarkTheme()`. The toggle in the garage top bar flips it for the session but does not persist across restarts — persisting it would need a preference store the brief didn't ask for. Dynamic colour (Material You) is intentionally off so the palette stays consistent.

**Categories are a fixed enum, not free text.** The per-category dashboard breakdown and several rules both depend on categories being a closed set. Free text would let "Wheel", "wheels" and "Rims" fragment the same bucket. Thirteen categories cover a street build; adding one is a single enum entry.

**Stock reference figures live on the car and are optional.** Rules need to know what the car left the factory with, and there's no offline database of every car's stock specs, so the user supplies them once per car. Leaving them blank disables the comparison rules rather than guessing.

**Structured specs are separate from the notes field.** The brief asked for "a compatibility notes field per mod with simple rule warnings." Free text alone can't drive arithmetic, so a mod has both: your prose notes, plus optional numeric specs that power the engine. Spec fields reveal themselves contextually — pick the Wheels category and the rim fields appear.

**Offset comparison assumes ET45 stock.** Factory offset isn't captured per car (that felt like one field too many), so the offset rule compares against a typical FWD-sedan ET45 and says so in the warning text.

**One car deletion cascades to its mods,** via a Room foreign key with `onDelete = CASCADE`, behind a confirmation dialog naming the consequence.

**Status cycles rather than being a dropdown** in the repository (`advanceStatus`: planned → bought → installed → planned), with explicit chips in the editor for direct selection.

**Release is minified but debug-signed.** R8 with `isMinifyEnabled` and resource shrinking is on for release, with ProGuard keep rules for Room's generated code and enum converters. The release APK is signed with the checked-in debug key so that `assembleRelease` produces something installable without shipping private credentials; swap in your own keystore before distributing.

**`minSdk 24`** covers effectively the entire active Android install base while letting the code target modern APIs without desugaring gymnastics.

---

## Known limitations

Being explicit about what isn't verified or isn't there:

- **Never run on a device.** The build environment had no KVM or hardware virtualisation, so no emulator could be started. The APK compiles, packages, signs and passes `aapt2 badging` and `apksigner verify`, and all business logic is covered by JVM tests — but no screen has been rendered at runtime. Compose layout bugs, if any, would surface on first launch. This is the main gap.
- **No Compose UI tests.** The `androidTest` dependencies are wired up but no instrumentation tests were written, since they can't be executed here.
- **Theme choice doesn't persist** across app restarts (defaults back to dark).
- **No multi-currency, no FX, no receipts or photo attachments.**
- **No CSV or JSON export.** Room's auto-backup rules include the database, so a device-transfer backup carries the data, but there's no in-app export.
- **No migrations yet.** Schema version is 1 with `fallbackToDestructiveMigration()`. That's appropriate pre-release, but before shipping an update that changes the schema you must write a real `Migration` or users lose their data.
- **Rules are heuristics, not engineering.** Thresholds are reasonable street-build defaults collected as constants at the top of `CompatibilityRules`. They are not a substitute for a fitment guide or an installer's judgement, and the UI says so.
- **Offset rule uses an assumed stock value** (ET45) rather than a per-car figure.

---

## Privacy

ModMoney declares **no permissions** in its manifest — not even `INTERNET`. There is no telemetry, no analytics SDK, no crash reporter, no advertising identifier, no third-party network call. Data lives in a Room database in the app's private storage and is included in Android's standard auto-backup rules, so transferring to a new device carries your build with it.

---

## License

MIT — see [LICENSE](LICENSE).

---

<div align="center">

**Built with Room, Jetpack Compose and a healthy paranoia about wheel offsets.**

[Report a bug](https://github.com/YapZhuSheng/ModMoney/issues) · [Request a feature](https://github.com/YapZhuSheng/ModMoney/issues) · [Browse the source](https://github.com/YapZhuSheng/ModMoney)

</div>
