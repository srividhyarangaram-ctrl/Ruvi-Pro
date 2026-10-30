# Ruvi 5.4 Premium — Full Android Source

## Features
- Premium dark Android WebView dashboard and existing local subscription/purchase management.
- **Sync Now** requests user confirmation, asks Google Play services for Gmail read-only OAuth authorization, and scans up to 100 recent matching receipt metadata items.
- Android notification listener captures possible payment notifications *after* notification-access permission is enabled.
- Candidate extraction and best-effort deduplication by reference or merchant/amount/day; all candidates require review.
- Android/Google Play services remember account authorization where supported; no Gmail password, UPI PIN, or refresh token is stored by Ruvi. Reauthorization may be required.

## Required configuration before Gmail can work
1. Create a Google Cloud project and enable **Gmail API**.
2. Configure OAuth consent screen, add test users if in testing, and request `gmail.readonly` scope (restricted scope; public production use may require Google verification and a security assessment).
3. Register an **Android OAuth client** for package `com.ruvi.pro` with the signing certificate SHA-1 of the APK. Debug and release signing certificates differ.
4. Install on an Android phone with Google Play services. Tap Sync Now and grant consent.
5. On Android, grant Ruvi Notification access in system Settings. The listener only receives notifications after access is enabled and may miss alerts or receive incomplete messages.

## Safety / limitations
- This is source code, **not a verified APK**; compile and test via GitHub Actions and on a device.
- UPI/bank accounts are **not directly accessed**; notification-derived payment candidates are not verified transactions.
- Gmail receipt scanning is a heuristic, not guaranteed discovery of all active subscriptions.
- Candidate previews and notification content are stored locally in app storage and are not encrypted; do not use this prototype with sensitive accounts until encrypted storage and retention controls are implemented.
- Deduplication is best-effort and can have false positives; do not treat totals as reconciled financial statements.
- The UI does not yet automatically commit candidates to financial records: they are reviewable signals.
- Google authorization is delegated to Google Play services; Ruvi does not persist Google credentials.
