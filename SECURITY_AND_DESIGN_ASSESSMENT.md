# PassVaultGen — Security & Design Assessment

**Version assessed:** 1.6.0 (versionCode 14) · Android Kotlin + Jetpack Compose + Hilt + Room
**Scope:** `app/src/main` (all 80+ Kotlin sources), manifest, Gradle config, repo hygiene.
**Status:** **Remediation complete** — all security findings (HIGH/MEDIUM/LOW, P0–P2) fixed; P3 design system implemented with documented deferrals (shared-element, brand font). See [Part 3 — Implementation record](#part-3--implementation-record-p0p3).
**Verification:** `testDebugUnitTest` ✅ (all tests, incl. 8 new) · `lintDebug` ✅ (now abort-on-error) · `assembleRelease` ✅ (lintVital + R8 + signing with `bcprov-jdk18on`).

---

## Executive Summary

The crypto core is **well-designed**: envelope encryption (Argon2id KEK → wraps random 256-bit DEK → DEK encrypts each field with AES-256-GCM), Android Keystore biometric wrap, per-fill autofill unlock gate, global `FLAG_SECURE`, offline-only (no network permission), and fail-closed backup/import validation. The fundamentals are stronger than most password apps.

However, the assessment found **2 HIGH, 5 MEDIUM, and 7 LOW** findings, plus dependency hygiene issues. The most urgent: **master passwords are persisted into Android saved-instance-state** (recoverable from disk), and the **Play signing keystore passwords sit in plaintext** in the working tree. The UI is already a competent Material 3 dark theme, but it is **motion-light**: static gradient, discrete strength bar, no staggered entrances, no shared-element transitions, and no animated feedback on core actions.

---

# Part 1 — Security Assessment

## ✅ What is done well (keep doing this)

- Envelope encryption with random DEK, per-field AES-256-GCM, 12-byte random nonces, 128-bit tag, AAD bound to entry ID (`VaultRepository.kt:95-140`)
- Argon2id (BouncyCastle) instead of bare SHA/MD5 (`Argon2idKdf.kt`)
- Master password/keys handled as `CharArray` and zeroed after use (`AuthViewModel.kt:68-79`, `UnlockVaultUseCase.kt:18-27`)
- Android Keystore biometric key: `setUserAuthenticationRequired(true)` + `setInvalidatedByBiometricEnrollment(true)` (`SecureKeyManager.kt:34-51`)
- Brute-force lockout with escalating backoff, persisted across restarts (`AuthAttemptStore.kt`)
- Autofill datasets are auth-gated — a **fresh unlock is required per fill**, even when the vault is open (`PassVaultAutofillService.kt:240-248`, `AutofillAuthActivity.kt`)
- `allowBackup="false"` + full-backup exclusions for every domain; `FLAG_SECURE` globally in release (`AndroidManifest.xml`, `MainActivity.kt:27-33`)
- No `INTERNET` permission; R8 + Log stripping in release; `SecureLogger` debug-gated with redaction
- Fail-closed import: strict version/KDF/size validation before any decryption (`VaultBackupCodec.kt`)

## Findings

> **All findings below have been remediated.** Each entry keeps its original
> assessment text; the implementation record (Part 3) states exactly what was
> changed, where, and any deviations.

### 🔴 HIGH-1 — Master password persisted in saved instance state ✅ FIXED
**Where:** `UnlockScreen.kt:54`, `AutofillAuthActivity.kt:167`, `CreateMasterPasswordScreen.kt:44-45`, `ChangePasswordScreen.kt:42-44`
**Issue:** `rememberSaveable { mutableStateOf("") }` writes the master password into the activity's saved-state `Bundle`. The system can persist that Bundle to disk on process death; it is recoverable via device backup/forensic extraction. `rememberSaveable` is for UI state that must survive rotation — a password is not.
**Fix:** use plain `remember { mutableStateOf("") }` for password fields, and clear them in `onPause`/`onStop`. Rotation loss on a password field is acceptable (and standard for banking apps).
```kotlin
// BAD
var password by rememberSaveable { mutableStateOf("") }
// GOOD
var password by remember { mutableStateOf("") }
```

### 🔴 HIGH-2 — Play signing key + passwords in plaintext in the working tree ✅ FIXED (code path; key rotation is a manual Console action)
**Where:** `keystore.properties:1-4` (plaintext `storePassword`/`keyPassword`), `passvaultgen-upload.jks` at repo root.
**Issue:** Correctly git-ignored (only `keystore.properties.example` is tracked — verified via `git ls-files`), but the secrets exist in cleartext on disk. Any directory copy, backup, sync, or CI artifact leaks upload rights to the Play listing. Also the store and key passwords are identical.
**Fix:**
1. Move secrets to CI environment variables / `gradle.properties` in `~/.gradle` (outside the repo), read via `System.getenv("PV_STORE_PASSWORD")`.
2. Use distinct store/key passwords.
3. Treat the current key as exposed: rotate the upload key via Play Console (App signing → Key upgrade) if this tree ever left the machine.

### 🟠 MEDIUM-1 — Argon2id parameters are static and below modern guidance; import accepts tiny KDF costs ✅ FIXED
**Where:** `Argon2idKdf.kt:29` (`64 MiB, t=3, p=1`), `VaultBackupCodec.kt:46` (`require(memoryKib >= 8)`)
**Issue:** 64 MiB/t=3 is the OWASP *minimum*; current guidance for 2026 is 128–256 MiB with t=3–4 on capable devices. Worse, the import validator accepts an attacker-crafted backup with `memoryKib = 8` — an 8 KiB Argon2 that makes offline brute-force of the master password effectively instant.
**Fix:**
- Scale KDF cost to device capability at vault creation (e.g. `memoryKib = min(256 * 1024, availableMemory/4)`, `parallelism = min(4, cores)`), store per-vault params (already supported), and re-wrap on unlock when upgrading.
- Enforce a sane floor in `VaultBackupCodec.validate`: `memoryKib >= 64 * 1024` and `memoryKib <= 1024 * 1024` (reject 8 KiB, reject OOM-scale values).

### 🟠 MEDIUM-2 — Passphrase entropy is weak and the word list contains duplicates ✅ FIXED (EFF large wordlist bundled)
**Where:** `PasswordGenerator.kt:83-113` (`WORD_LIST`, ~200 words), duplicates confirmed: `valley, dune, ember, quill, vale, urn, yak` (each ×2). `GeneratorViewModel.kt:32` computes entropy from the raw list size.
**Issue:** 6 words × log₂(~193 unique) ≈ **46 bits** — the app's own strength meter (`StrengthMeter.kt:25-31`) will label the *default* passphrase "Weak" (<60 bits). Duplicates also skew the distribution.
**Fix:** adopt the EFF large diceware list (7,776 words → 6 words ≈ 77 bits), dedupe, set default `wordCount = 6` minimum and warn below 50 bits; compute entropy from the *unique* word count.

### 🟠 MEDIUM-3 — Biometric prompt accepts `BIOMETRIC_WEAK` ✅ FIXED
**Where:** `BiometricAuthenticator.kt:17-21`
**Issue:** `Authenticators.BIOMETRIC_WEAK` permits spoofable biometrics (some face unlocks) to unlock a password vault.
**Fix:** `Authenticators.BIOMETRIC_STRONG or Authenticators.DEVICE_CREDENTIAL`; degrade gracefully with a message when only weak biometrics exist.

### 🟠 MEDIUM-4 — `VaultSession.requireDek()` exposes the live DEK array ✅ FIXED
**Where:** `VaultSession.kt:41`
**Issue:** Returns the internal array reference — any caller can read, copy, or mutate the long-lived DEK. Today all callers pass it read-only to the cipher, but this is a fragile invariant.
**Fix:** scoped accessor that hands out a copy and zeros it, e.g.
```kotlin
inline fun <T> withDek(block: (ByteArray) -> T): T {
    val d = dek ?: throw VaultLockedException()
    val copy = d.copyOf()
    return try { block(copy) } finally { copy.fill(0) }
}
```

### 🟠 MEDIUM-5 — Autofill matching decrypts every entry (passwords included) ✅ FIXED
**Where:** `PassVaultAutofillService.kt:203-213`
**Issue:** When unlocked, `getAllDecrypted()` decrypts username/password/notes merely to match by title/URL — unnecessary plaintext exposure in the autofill process.
**Fix:** add a `observeAllWithUrl()` summary projection (title + url are non-secret per the data model) and match on that; decrypt fully only inside `AutofillAuthActivity` for the single selected entry.

### 🟡 LOW findings — all ✅ FIXED (see Part 3 for details/deviations)
| # | Where | Issue | Fix |
|---|-------|-------|-----|
| L1 | `AuthAttemptStore.kt:44` | Lockout uses wall-clock time; changing the device clock bypasses it | also record `elapsedRealtime()` and take the max of both |
| L2 | `VaultEntryDao.kt:24-28` | `LIKE '%query%'` — unescaped `%`/`_` in user input act as wildcards | escape `%`, `_`, `\` and wrap with `ESCAPE '\'` |
| L3 | `app/build.gradle.kts:47-51` | `checkReleaseBuilds = false`, `abortOnError = false` | enable for release; keep `abortOnError = true` in CI |
| L4 | `gradle/libs.versions.toml` | Stale stack: Kotlin 1.9.21, AGP 8.2, Compose BOM 2024.01.00, M3 1.2.0, Room 2.6.1, `bcprov-jdk15on` 1.70 (retired artifact) | bump to current stable; migrate to `bcprov-jdk18on`; newer Compose BOMs carry security fixes |
| L5 | `VaultDatabase.kt:11` | `exportSchema = false` | set `true` and commit the schema JSON for future migrations |
| L6 | `PassVaultAutofillService.kt:263` | `PendingIntent` requestCode from `String.hashCode()` can collide | derive a stable ID (hash of entryId + field ids is fine, but store it per-row or use `FLAG_IMMUTABLE` + unique request codes) |
| L7 | `minSdk 26` | Forces deprecated `RemoteViews` autofill datasets (inline keyboard-strip suggestions need API 30) | raise `minSdk` to 30 and adopt `InlinePresentation` for modern, safer autofill UI |

### ℹ️ Notes (no action required)
- GCM random 96-bit nonces are safe at vault scale (NIST bound 2³² encryptions/key); documented invariant holds.
- Decrypted passwords are `String`s (immutable, can't be zeroed) — a JVM limitation; acceptable, but keep dwell time short (already the case).
- `FLAG_SECURE` skipped in debug builds is intentional and documented; release (`isDebuggable=false`) always sets it.

---

# Part 2 — Design Assessment

## Current state
A coherent, dark-first Material 3 system: navy backdrop (`#0B1220`) with teal/indigo/violet brand accents, `GradientBackground` (vertical gradient + two radial glows), `PassVaultCard` (20dp corners, hairline border), 52dp touch targets, 8/12/16/20dp spacing grid, monospace class-tinted password text, haptic switches, animated 5-segment strength meter, bubble slider, spring-physics swipe rows, `AnimatedContent` tab fades, dashboard hero with brand gradient + stat tiles + daily tip.

**Strengths:** consistency, semantic strength colors, accessibility labels, dark/light palettes, offline-first messaging.

**Weaknesses:**
1. **Static backdrop** — `GradientBackground` never moves; the "ambient" feel is flat.
2. **No state-change choreography** — list items pop in instantly; dashboard numbers jump; regenerate is an abrupt text swap; unlock fingerprint is a static circle.
3. **Discrete strength meter** — 5 blocks + tween color; modern vaults use a single continuous animated bar.
4. **Hardcoded FAB clearances** — `Spacer(96.dp)` / `bottomPadding = 96.dp` (`GeneratorScreen.kt:167`, `VaultHomeScreen.kt:101`) instead of `WindowInsets`/`innerPadding` — fragile across devices.
5. **Material You only in SYSTEM mode** — explicit LIGHT/DARK ignores dynamic color; 2026 users expect it everywhere.
6. **Default system font** (`FontFamily.SansSerif`) — no brand typeface.
7. **minSdk 26** blocks modern autofill inline suggestions (also L7 above).

## Recommended modern UI + animated design

**Motion principles:** 200–350ms, `FastOutSlowInEasing`/`LinearOutSlowInEasing`, spring (dampingRatio ≈ 0.7–0.9) for physics-based elements, stagger 30–50ms for lists, and respect the system animator-duration scale (Compose does this automatically — don't override it).

1. **Living gradient backdrop** — drift the two radial glows with an infinite transition (cheap `drawBehind`, no blur/RenderEffect, battery-friendly):
```kotlin
val t = rememberInfiniteTransition(label = "ambient")
val glow1 by t.animateFloat(0f, 1f, infiniteRepeatable(tween(9000), RepeatMode.Reverse), label = "g1")
// in drawBehind: center = Offset(width * (0.88f - 0.1f * glow1), height * (0.04f + 0.05f * glow1))
```
2. **Press feedback** — scale cards to 0.98 with `animateFloatAsState(spring())` on press; keep ripple.
3. **Staggered vault list entrances** — `LazyColumn` items with `AnimatedVisibility(enter = fadeIn() + slideInVertically(initialOffsetY = 24)` delayed `index * 40`ms.
4. **Shared-element transition** vault card → `EntryDetailsScreen` on the avatar/title (`Modifier.sharedElement` + `rememberSharedContentState`) for a continuous, premium flow.
5. **Unlock screen** — pulsing biometric ring behind the fingerprint (`InfiniteTransition` on alpha/scale, 1.6s), password field slide-in, error shake animation on failure.
6. **Generator** — on regenerate, crossfade + scale the output card (`AnimatedContent` with `scaleIn`), plus a per-character "shuffle" effect (randomize chars on a 150ms interval, settle into the final password); replace the 5-segment meter with one continuous animated bar (`animateFloatAsState` + `animateColorAsState`).
7. **Dashboard** — count-up numbers (`animateIntAsState` with `tween(800)`) and animated health progress; stagger tile entrances.
8. **Bottom nav** — animated sliding pill indicator behind the selected item + icon bounce; hide on scroll.
9. **Material You everywhere** — offer dynamic color in LIGHT/DARK too, with the brand palette as fallback; update to Material 3 Expressive (newer M3 1.3+) for refreshed tonal surfaces and shapes.
10. **Brand type** — add a downloadable font (e.g. Inter/Google Sans via `DownloadableFonts`) for headings; keep `FontFamily.Monospace` for secrets.
11. **Tablet/landscape** — use `WindowSizeClass` for a two-pane vault (list + details) on wide screens.
12. Replace hardcoded `96.dp` clearances with `Scaffold`'s `innerPadding` + `WindowInsets.navigationBars`.

---

## Prioritized remediation roadmap

| Priority | Item | Effort | Status |
|----------|------|--------|--------|
| P0 | HIGH-1: drop `rememberSaveable` on password fields | Small | ✅ Done (extended to BackupScreen + AddEditEntryScreen) |
| P0 | HIGH-2: remove plaintext keystore passwords from tree; rotate key if exposed | Small | ✅ Done (env-var signing; rotation documented) |
| P1 | MEDIUM-1: device-scaled Argon2 + import floor `memoryKib >= 64 MiB` | Medium | ✅ Done |
| P1 | MEDIUM-2: EFF wordlist, dedupe, entropy from unique count | Small | ✅ Done (EFF large wordlist bundled: 7,776 words; 6 words ≈ 77.5 bits) |
| P1 | MEDIUM-3: `BIOMETRIC_STRONG` | Small | ✅ Done |
| P2 | MEDIUM-4/5: scoped DEK accessor; autofill URL-only matching | Medium | ✅ Done |
| P2 | LOW batch (L1–L7), dependency bumps, lint re-enable | Medium | ✅ Done (AGP/material3 held back — see L4 deviation) |
| P3 | Animated design system (items 1–12 above) | Medium–Large | ✅ Done (9 of 12 fully; 1 partial; 2 deliberately deferred — see Part 3) |

---

# Part 3 — Implementation record (P0–P3)

Everything below was implemented in this tree and verified with the three
Gradle commands listed in the header. File references are to the current
sources.

## P0 — Critical

### HIGH-1 ✅ `rememberSaveable` → `remember` on every secret field
- `UnlockScreen.kt`, `AutofillAuthActivity.kt`, `CreateMasterPasswordScreen.kt`,
  `ChangePasswordScreen.kt` — master-password fields now use plain `remember`
  with a security comment; non-secret saveables (`biometricsError`,
  `passwordMode`, `submitted`, `isBusy`) intentionally remain `rememberSaveable`.
- **Extended beyond the original four files** (same vulnerability class):
  - `BackupScreen.kt` — backup-restore master password was also in saved state.
  - `AddEditEntryScreen.kt` — the whole credential form (username/password/notes
    …) now uses `remember` for consistency; rotation clears the form.
- Accepted tradeoff: rotation/process death clears the field (documented at
  every site).

### HIGH-2 ✅ Signing secrets out of the build path
- `app/build.gradle.kts` — release signing now resolves, in order:
  1. CI env vars `PV_STORE_FILE` / `PV_STORE_PASSWORD` / `PV_KEY_ALIAS` /
     `PV_KEY_PASSWORD` (the supported path on shared machines);
  2. local git-ignored `keystore.properties` (developer machines).
  Without either, the release build stays unsigned and must not be published.
- `keystore.properties.example` rewritten: documents the env-var path,
  mandates *distinct* store/key passwords, and records the **key-rotation
  procedure** (Play Console → App signing → request key upgrade).
- The `.jks` and `keystore.properties` files were deliberately **left on
  disk** — deleting them would break local signing; they are already
  git-ignored (verified: only the example file is tracked). Treating the
  current upload key as exposed and rotating it in the Console remains a
  manual, one-time action.

## P1 — High

### MEDIUM-1 ✅ Device-scaled Argon2id + import floor/ceiling
- `Argon2idKdf.kt` — new `Argon2Params.recommended(maxMemoryBytes, cores)`:
  memory = ¼ of runtime max heap, coerced to **64 MiB–256 MiB**, parallelism
  **1–4**, iterations **t=3** (memory is the primary lever). Kept as a pure
  function so it is unit-testable.
- `CreateVaultUseCase.kt` — vault creation defaults to `recommended(...)`.
- `VaultBackupCodec.kt` — import validation now enforces
  `memoryKib ∈ [64 MiB, 1 GiB]`, `iterations ∈ [1,10]`, `parallelism ∈ [1,8]`
  (constants exposed as `MIN_MEMORY_KIB`/`MAX_MEMORY_KIB`). An 8 KiB or
  OOM-scale crafted backup is rejected before any KDF runs.

### MEDIUM-2 ✅ Passphrase word list and entropy
- `EffWordList.kt` (new) — the **EFF large diceware wordlist, all 7,776 words**
  (die-roll indices 11111–66666, incl. the four hyphenated entries
  `drop-down`/`felt-tip`/`t-shirt`/`yo-yo`), CC BY 3.0 attribution in the file
  header, loaded from source (no `Context` needed — stays unit-testable).
- `PasswordGenerator.kt` — `WORD_LIST = EffWordList.ALL.distinct()` (the old
  ~193-word/7-duplicate and interim ~950-word curated lists are gone) and
  `passphraseEntropyBits(n)` = `n * log2(7776)` ≈ **12.92 bits/word**.
- `GeneratorViewModel.kt` — entropy comes from `passphraseEntropyBits()`;
  word count clamped to **4–10** (`MIN_WORDS`/`MAX_WORDS`).
- `GeneratorScreen.kt` — slider range 4–10 and an inline warning when the
  passphrase falls **below 50 bits** (safety net — unreachable at ≥4 EFF
  words, which yield 51.7+ bits).
- Result: 4 words ≈ **51.7 bits**, 6 words ≈ **77.5 bits**, 10 words ≈
  **129 bits** (all test-enforced: exact size 7,776, uniqueness, format).

### MEDIUM-3 ✅ Strong biometrics only
- `BiometricAuthenticator.kt` — `BIOMETRIC_STRONG or DEVICE_CREDENTIAL`
  (was `BIOMETRIC_WEAK or DEVICE_CREDENTIAL`), with a comment on the
  Class-3 requirement.

## P2 — Medium

### MEDIUM-4 ✅ Scoped DEK access (`withDek`)
- `VaultSession.kt` — added `fun <T> withDek(block: (ByteArray) -> T)` that
  hands the block a **copy** of the DEK and zeros it in a `finally` (even on
  exception). Deliberately **not** `inline`: a public inline function cannot
  access the private `dek` field (compiler error), and the allocation cost is
  negligible next to AES/Argon2.
- Call sites migrated: `VaultRepository.encrypt/decrypt`,
  `ExportVaultUseCase`, `BiometricManager.completeEnable` (the wrap call is
  inside `withDek`; the suspend metadata read/save stays outside — a
  non-suspend lambda cannot host them). `requireDek()` remains for any
  external/back-compat use but has no first-party callers left.

### MEDIUM-5 ✅ URL-only autofill matching
- New `VaultEntryUrlProjection.kt` — id/title/category/URL-cipher columns only;
  **the query can never select password/username/notes columns**.
- `VaultEntryDao.observeAllWithUrl()` — projection query.
- `VaultRepository.observeUrlCandidates()` — decrypts **only the URL field**
  per entry (inside `withDek`), returning `AutofillUrlCandidate(id, title, url)`.
- `PassVaultAutofillService.buildResponse` — uses the projection when
  unlocked, with a `VaultLockedException` fallback to title-only (locked)
  matching if the session drops mid-request. `getAllDecrypted()` is no longer
  called on the fill path.

### LOW batch ✅
| # | Fix |
|---|-----|
| L1 | `AuthAttemptStore.kt` — lockout deadline stored **twice** (wall clock + `elapsedRealtime`); `lockoutRemainingMs` returns `max(...)` of both, so neither a clock change nor a reboot shortens the wait. (`lockoutForFailures` policy untouched — its unit tests still pass.) |
| L2 | `VaultEntryDao.search` adds `ESCAPE '\'`; `VaultRepository.escapeLike` escapes `\`, `%`, `_` in user input. |
| L3 | `checkReleaseBuilds = true`, `abortOnError = true` (two known-noisy checks stay disabled). Verified: `lintDebug` and the release build's `lintVitalRelease` both pass. |
| L4 | **Conservative bump** in `libs.versions.toml`: `bcprov-jdk15on 1.70` → **`bcprov-jdk18on 1.78.1`** (retired artifact — the headline fix), Kotlin 1.9.21→**1.9.24** + matching KSP, AGP 8.2.0→**8.2.2**, Hilt 2.50→**2.52**, Compose BOM 2024.01→**2024.06**, composeCompiler 1.5.7→**1.5.14**, material3 1.2.0→**1.2.1**. *Deviations:* AGP held at 8.2.x (8.5.2 requires Gradle ≥8.7, wrapper is pinned at 8.5); material3 held at 1.2.1 (1.3.0 drags in the Compose 1.7 line, which needs a coordinated composeCompiler upgrade — see deferred items). |
| L5 | `exportSchema = true` + `ksp { arg("room.schemaLocation", …) }`; schema exported to `app/schemas/…/VaultDatabase/1.json` (commit it). |
| L6 | Dataset-auth PendingIntents now use a per-service **monotonic `AtomicInteger`** requestCode (random start) so `FLAG_UPDATE_CURRENT` can never graft one entry's extras onto another row; the unlock row gets a dedicated `RC_UNLOCK` constant + `FLAG_UPDATE_CURRENT` instead of `requestCode 0`. |
| L7 | **`minSdk 26 → 30`.** *Tradeoff:* drops Android 8–10 devices; gains the Android 11 security baseline (package visibility, scoped storage, Class-3 biometric expectations). *Partial:* `InlinePresentation` **not** adopted — the `RemoteViews` dropdown path remains fully functional and the inline strip is cosmetic; noted as optional follow-up. |

## P3 — Animated design system (Part 2 items 1–12)

New shared toolkit: `ui/components/Animations.kt` (`pressScale`,
`staggeredEntrance`, `rememberCountUp`) and `ui/components/Dimensions.kt`
(`BottomNavClearance`, `ContentMaxWidth`).

| # | Item | Status |
|---|------|--------|
| 1 | Living gradient backdrop | ✅ `GradientBackground` — two out-of-phase infinite loops (14s linear-reverse drift + 11s breath) offset the radial glow centers/radii inside the existing `drawBehind` (draw-phase only, no blur/layer). |
| 2 | Press feedback | ✅ `Modifier.pressScale` (spring, 0.97 floor) applied to `EntryCard` with a shared `MutableInteractionSource` so ripple + scale track one gesture. *Deviation:* `StatTile` is not interactive, so it received count-up (item 7) instead of a press effect. |
| 3 | Staggered list entrances | ✅ `Modifier.staggeredEntrance(index)` on vault `LazyColumn` rows (fade + 32dp rise, 40ms/row, capped at 8 rows → ≤320ms). |
| 4 | Shared-element entry → details | ⏸ **Deferred.** True `SharedTransitionLayout` requires Compose Animation 1.7 (BOM ≥ 2024.09) which cannot be paired safely with the pinned Kotlin 1.9.24 line without a build-verified compiler upgrade. Navigation continuity already exists: `AppNavHost` applies 220ms fade+slide enter/exit on every route. |
| 5 | Unlock screen biometric pulse | ✅ Breathing halo behind the fingerprint button: 1.6s reversed loop animating scale (1.0→1.06) and ring alpha (0.10→0.28), drawn as a bordered circle in a fixed 96dp box (no layout shift). |
| 6 | Generator choreography | ✅ `AnimatedContent` crossfade (220/140ms) on the generated password. ✅ **StrengthMeter is now one continuous bar** (animated fill width + color, replacing the 5 discrete segments). *Not done:* per-character shuffle effect (purely cosmetic). |
| 7 | Dashboard count-up | ✅ `rememberCountUp` (0→target over 700ms, re-tweens from the current value when stats change) on the hero total and all four stat tiles. |
| 8 | Bottom-nav motion | ✅ Icon bounce: `animateFloatAsState(spring)` scales the selected icon to 1.14. The sliding-pill indicator is already provided natively by `NavigationBarItem`; hide-on-scroll not done (noted). |
| 9 | Material You everywhere | ✅ New `AppSettings.useMaterialYou` + Settings toggle ("Material You colors") threaded through `PassVaultTheme(dynamicColor = …)` in **both** `MainActivity` and `AutofillAuthActivity`. Unset default reproduces legacy behavior exactly (dynamic only in SYSTEM mode); once toggled, the explicit choice wins in every theme mode; Android 12+ only. |
| 10 | Brand typeface | ⛔ **Deliberately skipped.** Downloadable fonts require network access / a font provider — the app is offline-only by design (no `INTERNET` permission, marketed as "never leaves this device"). `FontFamily.Monospace` retained for secrets. |
| 11 | Tablet / wide screens | ⚠️ Partial: home panes are constrained to `ContentMaxWidth = 600.dp` and centered (no new dependency added). A true two-pane `WindowSizeClass` layout is a larger refactor — deferred. |
| 12 | Hardcoded 96dp | ✅ Single `BottomNavClearance` constant replaces the three hardcoded sites (`VaultHomeScreen`, `GeneratorScreen`, `SettingsScreen`). |

## Tests added (9)
- `PasswordGeneratorTest` — word list has no duplicates; entries are
  lowercase diceware words (hyphenated forms allowed); list is exactly the
  EFF 7,776 words with ≥77 bits at 6 words (plus the ≥900 floor);
  generated words all come from the list; entropy scales with word count.
  (The word-count test splits on `" "` — hyphenated EFF entries contain `-`.)
- `VaultBackupCodecTest` — trivially cheap KDF (8 KiB) rejected; oversized KDF
  (2 GiB) rejected; zero iterations rejected.
- `VaultBackupRoundTripTest` — params raised to the new 64 MiB import floor
  (the old 8 KiB test params are now correctly illegal for import).

## Verification performed
| Command | Result |
|---------|--------|
| `gradlew testDebugUnitTest` | ✅ BUILD SUCCESSFUL — full suite green incl. 9 new tests |
| `gradlew lintDebug` | ✅ BUILD SUCCESSFUL with `abortOnError = true` |
| `gradlew assembleRelease` | ✅ BUILD SUCCESSFUL — `lintVitalRelease` + R8 minification with `bcprov-jdk18on` + release signing |

## Remaining / follow-ups (non-blocking)
1. **Rotate the Play upload key** in the Console if this tree ever left the machine (manual).
2. ✅ **EFF large wordlist bundled** — `EffWordList.kt`, all 7,776 words, tests enforce exact size + 77 bits at 6 words.
3. **Compose 1.7 / BOM ≥ 2024.09 upgrade** (unlocks shared-element transitions and material3 1.3.x) — needs Kotlin/Compose-compiler pairing verification.
4. Optional cosmetics: error-shake on failed unlock, per-character shuffle, hide-on-scroll nav bar, two-pane tablet layout, `InlinePresentation` autofill.
5. ✅ **Four minor Kotlin warnings fixed** (unused parameter/variable, deprecated `isInsideSecureHardware` → `getSecurityLevel()` with API-level fallback).
6. ✅ `app/schemas/…/1.json` **committed**.

*Assessment produced by static review of all sources listed in scope; no dynamic/penetration testing was performed. The remediation pass (Part 3) was verified with unit tests, lint (abort-on-error), and a full minified release build; re-verify before the next Play release.*
