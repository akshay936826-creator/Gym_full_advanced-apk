# GYM HISAB

A native Android app (Kotlin + Jetpack Compose) for Indian gym owners: members, plans, fees (full / partial / refunds), receipts, expenses, attendance, reminders, staff logins, reports, optional Google Sheets copy and an optional AI assistant. Works offline and syncs through Supabase.

English first, Hindi included (`values-hi`). Switch in **More → Language**.

> **Status:** the code, database, tests and CI are written, but this project was authored in an environment with no Android SDK, so it has **not been compiled or run yet**. The first CI run is the real build test. If it fails, send the error log and it gets fixed (see "If the build fails").

## What you need
* A free [Supabase](https://supabase.com) account
* A GitHub account
* (Optional) Razorpay account for subscriptions, Anthropic API key for AI, a Google Cloud service account for Sheets

## 1. Create the database (10 minutes)
1. Supabase → **New project** (region: Mumbai). Save the database password somewhere safe.
2. **Authentication → Providers → Email**: keep enabled. For the simplest start, turn **off** "Confirm email".
3. Open **SQL Editor** and run these four files from `supabase/migrations/` **in order** (paste each, press Run):
   1. `20260101000001_core_schema.sql`
   2. `20260101000002_security_rls.sql`
   3. `20260101000003_reports.sql`
   4. `20260101000004_bootstrap.sql`

   (Or with the CLI: `supabase link --project-ref YOUR_REF && supabase db push`.)
4. **Project Settings → API**: copy the **Project URL** and the **anon public** key. Never copy the `service_role` key into the app.

## 2. Deploy the server functions (only the ones you need)
Install the [Supabase CLI](https://supabase.com/docs/guides/cli), then:
```bash
supabase login
supabase link --project-ref YOUR_REF
supabase functions deploy invite-staff            # staff logins (needed for Team)
supabase functions deploy ai-assistant            # optional
supabase functions deploy sheets-sync             # optional
supabase functions deploy razorpay-create-link    # optional (subscriptions)
supabase functions deploy razorpay-webhook --no-verify-jwt
```
Secrets (never committed) — copy `.env.example` to `.env`, fill only what you use, then:
```bash
supabase secrets set --env-file .env
```
`SUPABASE_URL`, `SUPABASE_ANON_KEY` and `SUPABASE_SERVICE_ROLE_KEY` are provided to functions automatically.

## 3. Build the APK on GitHub (no Android Studio needed)
1. Create a GitHub repo and upload this folder (`git init && git add . && git commit -m "GYM HISAB" && git push`). The `.gitignore` keeps secrets out.
2. Repo → **Settings → Secrets and variables → Actions → New repository secret**:
   `SUPABASE_URL` and `SUPABASE_ANON_KEY` (the public values from step 1.4).
3. Repo → **Actions → Android build → Run workflow**. When it is green, open the run and download **gym-hisab-debug-apk**. Unzip, copy the `.apk` to your phone, install (allow "unknown sources").

Without the two secrets the app still installs and shows **Demo mode** with clearly labelled sample data stored only on the phone.

### Or build locally
Install Android Studio (JDK 17), open this folder, let it sync (it creates the Gradle wrapper), then:
```bash
echo "SUPABASE_URL=https://YOUR_REF.supabase.co" >> local.properties
echo "SUPABASE_ANON_KEY=YOUR_ANON_KEY" >> local.properties
./gradlew testDebugUnitTest assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

## 4. First run
Create account → name your gym → you get a **14-day free trial** and four default plans (Monthly ₹1,000 · Quarterly ₹2,700 · Half-yearly ₹5,000 · Annual ₹9,000). Edit prices in **More → Plans**.
Daily flow: **Add member** (name + plan + amount paid → receipt) · **Collect fee** (pick member → amount → receipt) · **Check-in**.

## Google Sheets (optional)
1. Create a Google Cloud service account, enable the **Google Sheets API**, download its JSON key.
2. Put the whole JSON on one line in `.env` as `GOOGLE_SERVICE_ACCOUNT_JSON`, run `supabase secrets set --env-file .env`, deploy `sheets-sync`.
3. In the app: **More → Google Sheets** shows the service-account email. Share your sheet with it as *Editor*, paste the sheet link, tap **Connect & sync**.

Supabase stays the master copy; the sheet is overwritten with Members, Payments and Expenses tabs each sync.

## Subscriptions
Set the Razorpay secrets, deploy both Razorpay functions, and in Razorpay Dashboard add a webhook to
`https://YOUR_REF.supabase.co/functions/v1/razorpay-webhook` for the **payment_link.paid** event using the same secret as `RAZORPAY_WEBHOOK_SECRET`. Prices default to ₹499/month and ₹4,999/year (`PRICE_MONTHLY_PAISE`, `PRICE_YEARLY_PAISE`). The app never decides who has paid: only the verified webhook can activate a gym, and the database refuses writes from expired gyms.

## Tests
```bash
./gradlew testDebugUnitTest     # money, ledger, dashboard, reports, CSV (JVM, no phone needed)
supabase test db                # Row Level Security + ledger rules (needs Docker + Supabase CLI)
```
CI runs both automatically (`.github/workflows`).

## Release APK (Play Store)
Create a keystore **outside the repo**, then set `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` as environment variables and run `./gradlew bundleRelease`. Never commit keystores.

## If the build fails
Open the failed GitHub Actions run, copy the red error lines and send them over. Typical first-build issues are a Gradle/AGP version mismatch or a small Compose API difference; both are one-line fixes.

## Project layout
```
app/                      Android app (com.gym.hisab)
  core/                   Money, dates, ledger, receipts, CSV  (pure, unit-tested)
  data/local/             Room entities + DAO
  data/remote/            Supabase REST/Auth client
  data/                   Repository (offline-first + sync), prefs, secure session
  work/                   WorkManager sync + daily reminders
  ui/                     Compose theme, components, screens
supabase/migrations/      Schema, RLS, reports, bootstrap
supabase/functions/       Edge functions
supabase/tests/           pgTAP security tests
docs/                     ARCHITECTURE.md, SECURITY.md
```

## Next steps (not built yet)
* Phone-number OTP login (needs an SMS provider such as MSG91 / Twilio).
* Switching between several gyms in one login (data model already supports it).
* PDF receipts with logo; member self-service QR check-in.
* Hindi translations of server error messages.
