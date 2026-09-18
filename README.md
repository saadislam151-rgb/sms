# MFS Payment Logger

An Android app that watches SMS from selected senders (e.g. bKash, Nagad,
Rocket), extracts the payer's number, transaction ID, and amount from
"payment received" messages, and logs each one as a row in a Google Sheet.

**Scope note:** this only reads incoming SMS your own phone already
received and copies the numbers out of it. It never sends money,
enters a PIN, or talks to the MFS provider — it's a read-only logger.

---

## Part 1 — Set up the Google Sheet + Apps Script (do this first)

1. Create a new Google Sheet (or open an existing one).
2. In the Sheet, go to **Extensions > Apps Script**.
3. Delete the placeholder code and paste in the contents of
   `google-apps-script/Code.gs` from this project.
4. Pick a secret password (any string). In the Apps Script editor:
   **Project Settings (gear icon, left sidebar) > Script Properties >
   Add script property** → name it `SHARED_SECRET`, value = your chosen
   password. Save.
5. Click **Deploy > New deployment**.
   - Type: **Web app**
   - Description: anything
   - Execute as: **Me**
   - Who has access: **Anyone** (this doesn't mean anyone can read your
     sheet — it just means anyone who knows the URL *and* your secret
     can POST to it; without the correct secret, requests are rejected)
6. Click **Deploy**, authorize the permissions it asks for, then copy
   the **Web app URL** it gives you (ends in `/exec`). You'll paste
   this into the Android app.
7. Optional sanity check: in the Apps Script editor, select the
   `testAppendRow` function from the dropdown and click **Run**. A
   test row should appear in a new "Payments" tab in your sheet.

## Part 2 — Build the Android app

1. Install [Android Studio](https://developer.android.com/studio) if
   you don't have it.
2. Open this folder (`MfsPaymentLogger/`) in Android Studio as an
   existing project — it will download the Gradle wrapper and
   dependencies automatically the first time.
3. Connect your Android phone via USB with USB debugging enabled
   (Settings > About phone > tap "Build number" 7 times to unlock
   Developer Options > enable USB debugging), or use an emulator
   (note: emulators can't receive real SMS, so use a real phone for
   actual testing).
4. Click **Run** in Android Studio to install it on your phone.

## Part 3 — Configure the app

1. Open the app, tap **Grant SMS permission**, and allow it.
2. Tap **Manage senders** and add the sender ID(s) you want to watch —
   this is whatever shows as the "From" name/number for that
   provider's SMS on your phone, e.g. `bKash`, `Nagad`, `Rocket`, or a
   short code number. Add as many as you like.
3. Back on the main screen, paste the **Apps Script Web App URL** from
   Part 1 into "Apps Script Web App URL", and enter the same secret
   you set as `SHARED_SECRET`.
4. Tap **Save**, then **Send test row** to confirm it reaches your
   sheet. Check the "Payments" tab.
5. Done — real payment SMS from the senders you added will now be
   logged automatically, even while the app is in the background.

## How message parsing works

`SmsParser.kt` looks for:
- An amount after "Tk" or "BDT"
- A phone number after "from"
- A transaction ID after a label like "TrxID", "TxnID", "Transaction ID", or "Ref"
- The word "received"/"credited" (so outgoing payments/cash-outs from
  your own account aren't logged as incoming)

This covers the common bKash/Nagad/Rocket wording. If a provider's
message format doesn't match, the safest fix is to open
`app/src/main/java/com/mfslogger/app/parser/SmsParser.kt` and share the
exact (redacted) message text so the regex can be adjusted — real MFS
SMS wording does vary and may need small tweaks.

## Building via GitHub Actions (optional)

`.github/workflows/android-build.yml` builds a debug APK automatically
whenever you push to `main` (or trigger it manually from the Actions
tab). Push this project to a GitHub repo and check the **Actions**
tab — the built `app-debug.apk` is attached as a downloadable
artifact on each run, so you don't need Android Studio installed on
every machine you use.

## Notes / limitations

- **Android only.** iOS does not allow any app to read SMS content, so
  there's no equivalent iPhone build.
- **Battery optimization:** some phone brands (Xiaomi, Oppo, Vivo,
  Huawei, some Samsung models) aggressively kill background apps.
  If messages stop being logged after the phone sits idle for a
  while, check your phone's battery-optimization / auto-start
  settings and exclude this app.
- **Duplicate protection:** the Apps Script checks the transaction ID
  column and skips rows that already exist, so re-sending the same
  message twice won't create duplicate rows.
- **Security:** anyone with your Web App URL and secret could submit
  fake rows. Keep the secret private, and periodically glance at the
  sheet for anything that looks off.
