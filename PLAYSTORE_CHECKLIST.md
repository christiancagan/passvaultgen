# Play Store Readiness Checklist — PassVaultGen

Status: code blockers fixed in-app. The items below are the remaining steps that
need **network access, a real device, and the Play Console**. Do them in order.

## A. Already done (this pass)

- [x] Brute-force protection: 5 free unlock attempts, then 30s → 1m → 2m → … → 15m
      cap escalating lockouts, persisted in DataStore (`AuthAttemptStore`, tested).
- [x] Delete-entry confirmation dialog (`EntryDetailsScreen`).
- [x] Clipboard label no longer reveals content (`"PassVaultGen"`, not `"password"`).
- [x] Clipboard timeout is now a real setting (−/+ stepper, 5–120s) and both copy
      paths honor it (previously hardcoded 30s with a display-only setting).
- [x] Change master password flow (DEK re-wrap, no re-encryption, biometric wrap
      preserved) + delete-vault flow with confirmation (`Settings`).
- [x] Backup exporter→importer round-trip tests (wrong-password and tamper cases
      included) via a `BackupBase64` abstraction — 35/35 unit tests pass.
- [x] Room annotation processing moved from kapt to KSP; monochrome launcher icon
      added (themed icons); lint is 0 errors.
- [x] Release signing wired to `keystore.properties` (see B below).
- [x] R8 release build verified; ProGuard keeps backup models (Gson field names).

## B. Upload key (done 2026-10-03 — KEEP SECRET FOREVER)

- [x] Generated `passvaultgen-upload.jks` (RSA-4096, SHA384withRSA, 25yr validity,
      DN `CN=PassVaultGen, OU=Personal, O=Personal, C=US`) at the project root.
- [x] `keystore.properties` created (gitignored via `.gitignore` + example file kept).
      Note: PKCS12 ignores a separate `-keypass`, so keyPassword == storePassword.
- [x] Release APK verified signed: cert SHA-256
      `a050732276595a8b7767f5278cd6d629fb7f1395f0d37335d3f03ef8e767400b`.
- [ ] **YOU MUST STILL:** back up the `.jks` + password in **two separate safe places**
      (password manager + encrypted USB). Losing them = losing the ability to update
      the app. The password is in `keystore.properties` — copy it to your password
      manager, then that file existing only here is fine.
- [ ] Enroll in **Play App Signing** (Google holds the app-signing key; upload key
      can then be reset if lost).

## C. SDK + dependencies (done 2026-10-03, needs one re-verify on your machine)

- [x] `compileSdk` / `targetSdk` **34 → 35** (`android-35` + build-tools 35 installed).
- [x] `androidx.biometric:biometric` **1.2.0-alpha04 → 1.1.0 stable** (latest line is
      still alpha; 1.1.0 has every API used here — no alpha in production paths).
- [x] `androidx.fragment:fragment` 1.5.1 → 1.5.6, `bcprov-jdk15on` 1.67 → 1.70.
- [x] Clean `:app:assembleDebug --rerun-tasks` + full unit suite green after bumps.
- [ ] AGP 8.2.0 / Kotlin 1.9.21 are stale but functional — upgrade as a batch later
      (needs Gradle wrapper bump; re-run the full suite after).

## D. Manual device test matrix (real phone, release build)

- [ ] Fresh install → create vault → add/edit/delete entry (confirm dialog appears).
- [ ] Wrong master password ×6 → 30s lockout message; correct password after wait works.
- [ ] Kill app during lockout → lockout persists.
- [ ] Enable biometrics → lock → biometric unlock works; disable works.
- [ ] Add/remove a system fingerprint → biometric unlock invalidates → re-enable works.
- [ ] Export backup → uninstall → reinstall → restore with old password → entries back.
- [ ] Restore with wrong password → generic failure, vault untouched.
- [ ] Change master password → unlock with new works, old fails, biometrics still work.
- [ ] Delete vault → Welcome screen, creating a new vault works.
- [ ] Copy password → system overlay shows app name, not "password" → auto-clears.
- [ ] Rotate screen on every screen; background/foreground → auto-lock fires.
- [ ] Enable autofill in system settings → open a test login form (e.g. a browser
      login page): matching entry is suggested and fills username + password.
- [ ] With vault locked: autofill shows the Unlock row; unlock, re-tap the field,
      suggestion fills.
- [ ] Confirm no suggestions appear inside PassVaultGen itself.
- [ ] Check logcat on release build: no secrets, no vault data in logs.

## E. Play Console (new personal accounts)

1. Pay the $25 developer registration; **new accounts must run a 14-day closed test
   with ≥ 12 testers before production access** — start this early.
2. Create app → `com.example.passwordvault` (or your final applicationId).
3. Upload the signed release AAB (`./gradlew :app:bundleRelease`).
4. **Data safety form:** no data collected, no data shared (no network permission
   to prove it). The `.pvault` export is user-initiated local file I/O, not collection.
5. **Privacy policy:** publish a short page (e.g. GitHub Pages) stating offline-only,
   no collection, and link it in the listing — required.
6. **Content rating questionnaire** (Everyone), **app access**: no login needed for
   review, but note the master password is set on first launch; provide a test
   password + backup file for reviewers if asked.
7. Listing assets: 512px icon (have), 1024×500 feature graphic, ≥ 2 phone screenshots.
8. Run the **pre-launch report**, fix anything, then staged rollout (e.g. 20% → 100%).
9. `versionCode` bumps (+1) for every uploaded artifact, forever.

- [x] Autofill service implemented (fill-only v1): `PassVaultAutofillService`,
      URL/package matcher (tested), auth-gated datasets when locked, Settings
      enable row with live status. Save-new-login flow is the remaining roadmap item.
- [x] Change master password + delete vault flows (done in-app).
- [x] Brute-force lockout, delete confirmations, clipboard fixes (done in-app).

## F. Conscious scope gaps (declare, don’t hide)

- Autofill is fill-only: saving *new* logins from other apps is not offered yet.
  State this in the listing or ship it in v1.1 (needs SaveInfo + save-consent UI).
- No crash reporting / analytics by design (offline-first). Field issues surface
  only via Play vitals (ANRs/crashes) and user email — put a support address in
  the listing.
