# PassVaultGen — User Manual

PassVaultGen is an **offline-first password vault and password generator** for Android.
Everything is stored **only on your device** — there is no account, no cloud sync,
and the app never connects to the internet.

---

## 1. Requirements

- Android 8.0 (API 26) or newer.
- About 50 MB of free storage.
- Optional: a fingerprint / face unlock enrolled on the device, if you want
  biometric unlock (see §5).

## 2. First launch — creating your vault

1. Open PassVaultGen. You will see the Welcome screen.
2. Tap **Create new vault**.
3. Choose a **master password** and confirm it.
   - Minimum 8 characters. Longer is much better — a memorable phrase of
     5+ random words is ideal (you can generate one in §6).
   - **There is no "forgot password" option.** If you lose the master password,
     your vault cannot be recovered — except from an encrypted backup (§9)
     whose password you still remember.
4. Tap **Create**. Your vault is created and unlocked.

How it works (plain language): your master password derives a key that unwraps
the vault's encryption key. The master password itself is never stored anywhere.

## 3. Unlocking and locking

- **Unlock:** enter your master password on the Unlock screen and tap **Unlock**,
  or tap **Unlock with biometrics** if you enabled it (§5).
- **Lock:** tap the **lock icon** in the top bar of the Home screen at any time.
- **Auto-lock:** if enabled (Settings, on by default), the vault locks automatically
  whenever the app goes to the background. Get into the habit of locking before
  handing your phone to someone.

If unlocking fails, double-check caps lock / keyboard language. After 5 wrong
attempts the app imposes a short waiting period that grows with each further
failure (30s, 1min, 2min, … up to 15min) — this is brute-force protection and
survives app restarts. A successful unlock resets the counter.

## 4. Managing credentials

### Adding an entry

1. On the Home screen tap **+**.
2. Fill in the fields:
   - **Title** (e.g. "Work email") — shown in the list, not encrypted, so keep it discreet.
   - **Category** (e.g. "Email", "Banking") — used for grouping.
   - **Account name, Username, Password, URL, Notes**.
   - **Favorite** — star it for the Favorites view.
3. Need a password? Tap the **dice icon** to insert a generated one (§6).
4. Tap **Save**.

### Viewing, copying, editing, deleting

- Tap an entry in the list to open its **details**. The password stays masked
  (`••••`) until you tap the **eye icon**.
- Tap the **copy icon** next to the password to copy it. It is cleared from the
  clipboard automatically after ~30 seconds (configurable in Settings).
- **Paste quickly** into the target app, then return — do not leave passwords
  sitting in the clipboard.
- Tap the **pencil icon** to edit, the **trash icon** to delete.
  A confirmation dialog appears — deletion is permanent.

### Finding entries

- Use the **Search** bar at the top of the Home screen (searches titles and categories).
- Browse by **category** or switch to **Favorites** from the Home screen.

## 5. Biometric unlock (optional)

Biometrics are a convenience shortcut — your master password always keeps working.

1. Unlock the vault with your master password.
2. Go to **Settings** (gear icon) and turn on **Biometric unlock**.
3. Confirm with your fingerprint / face when prompted.

Notes:

- If you add or remove a fingerprint/face in the Android system settings, the
  biometric key is invalidated for your protection — simply re-enable biometric
  unlock in PassVaultGen with your master password.
- To turn it off, switch the same toggle off. This also deletes the device key.

## 6. Password generator

Open the generator via the **dice/shuffle icon** in the top bar.

- **Random passwords:** choose the length (12–64) and which character groups to
  include (lowercase, uppercase, digits, symbols). Longer + all groups = stronger.
- **Passphrases:** choose a word count (4–8). Easier to type and remember —
  great for master passwords and Wi-Fi keys.
- The **strength meter** rates the result and warns about common, sequential,
  or personal-data-based passwords.
- Tap **Copy** to copy the result (auto-cleared from the clipboard), or
  **Regenerate** for a new one.

Tip: a 6-word passphrase is both strong and memorable — ideal for the vault's
own master password.

## 7. Autofill in other apps

PassVaultGen can fill usernames and passwords directly into login forms in other
apps and browsers — no copy/paste needed.

### Enabling it (once)

1. In PassVaultGen, go to **Settings** and look at the **Autofill service** row.
2. Tap **Enable**. Android opens the system autofill settings.
3. Select **PassVaultGen** and confirm. The row in Settings now shows "on".

You can switch back at any time in Android's Settings → System → Languages &
input → Autofill service.

### Using it

1. Tap a username/password field in any app (or in Chrome). If the vault is
   unlocked, matching logins appear as suggestions above the keyboard (or in a
   dropdown) — entries whose saved website matches the app are offered.
2. Tap a suggestion to fill both fields.
3. If the vault is locked, tap the **Unlock PassVaultGen** suggestion, unlock,
   then tap the field again.

Notes:

- Matching works best when your entries have the **website URL** filled in
  (e.g. `mybank.com`) — the app compares it against the requesting app.
- If nothing is suggested, no entry matched: open the vault and check the URL,
  or copy/paste manually (§4).
- Saving *new* logins from other apps into the vault is not supported yet —
  create them in PassVaultGen directly.
- Autofill never suggests anything inside PassVaultGen itself.

## 8. Security dashboard
Open it via the **dashboard icon** in the top bar. It analyzes your vault and flags:

- **Weak passwords** (short or low-strength),
- **Reused passwords** (the same password on multiple entries — change these first),
- **Old entries** not updated in a long time.

Work through the list top-down: unique, generated passwords everywhere is the goal.
The analysis runs only on your device and only while the vault is unlocked.

## 9. Backup & restore (important)

Your vault lives only on this phone. If the phone is lost, broken, or the app is
uninstalled, **everything is gone** — unless you made a backup.

### Exporting a backup

1. Unlock the vault → **Settings** → **Backup & restore** → **Export encrypted backup**.
2. Choose where to save the `.pvault` file (e.g. a USB stick, an encrypted drive,
   or a private cloud folder — the file itself is encrypted).
3. Keep the file somewhere safe and **separate from your phone**.

The backup is encrypted with your **current master password**. Anyone holding the
file still needs that password to read it — but store it privately anyway.

### Restoring a backup

1. Install PassVaultGen on the (new) device.
2. Go to **Settings → Backup & restore → Choose backup file to restore** and pick
   the `.pvault` file.
3. Enter the **master password that was active when the backup was made**.
4. Tap **Restore**.

⚠️ Warnings:

- Restore **replaces all current entries** on the device. Export first if unsure.
- After a restore, **biometric unlock is turned off** — re-enable it (§5) if wanted.
- If you changed your master password after exporting, use the **old** password
  to restore that backup.

## 10. Settings reference

| Setting | What it does |
|---|---|
| Auto-lock on background | Locks the vault when you leave the app (recommended: on). |
| Biometric unlock | Enables fingerprint/face unlock (§5). |
| Clipboard auto-clear | Seconds until a copied password is wiped (adjust with −/+ in 5s steps, 5–120s). |
| Backup & restore | Opens the export/import screen (§9). |
| Autofill service | Shows whether system autofill is on; **Enable** opens Android settings (§7). |
| Change master password | Re-wraps the vault key under a new password; entries are untouched and biometric unlock keeps working. |
| Delete vault & all entries | Permanently erases everything after a confirmation dialog. Use only with a backup in hand. |

## 11. Privacy & security facts

- **No internet permission.** The app physically cannot send data anywhere.
- **No account, no analytics, no ads, no tracking.**
- **Screenshots and screen recordings are blocked** app-wide.
- **No cloud / OS backup of vault data** — excluded from Android Auto Backup.
- Vault contents are encrypted with AES-256-GCM; the master password is stretched
  with Argon2id. Wrong passwords and tampered files fail closed with a generic error.
- Logs contain no secrets; logging is disabled in release builds.

## 12. Troubleshooting

| Problem | What to do |
|---|---|
| "Unable to unlock" | Check keyboard language/caps; try again slowly. If truly forgotten, restore from a backup (§9). |
| Biometric unlock stopped working | You likely changed system biometrics — re-enable it in Settings with your master password. |
| "Restore failed" | Wrong password for *that* backup file, or a corrupted file. Try the password as it was at export time; try a different backup copy. |
| Copy/paste shows a system notice | Normal on Android 13+: the OS announces clipboard access. The password is still auto-cleared. |
| App was uninstalled | Reinstall and **restore from backup** (§9). Without a backup, data is unrecoverable. |
| Too many failed attempts | Wait out the timer shown on screen — it resets after a successful unlock. |
| No autofill suggestions appear | Check Settings shows autofill "on"; make sure the entry has the website URL saved; if the vault is locked, tap the Unlock suggestion first. Some apps label their fields unusually and are not detected — copy/paste instead. |
| Forgot master password, no backup | The vault cannot be recovered. Your only option is to uninstall/reinstall and start a new vault. |

## 13. Good habits checklist

- [ ] Master password is long, unique, and memorized (or stored somewhere safe offline).
- [ ] At least one encrypted backup exists **off the phone**, tested by restoring once.
- [ ] Auto-lock is on; you lock manually before lending the phone.
- [ ] Every important account has a unique, generated password (dashboard is clean).
- [ ] Biometric unlock re-enabled after any device restore or biometric change.
