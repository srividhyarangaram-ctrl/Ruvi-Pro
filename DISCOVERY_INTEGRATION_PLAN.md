# Secure automatic discovery engineering plan

## Gmail
Use Android AppAuth / Google Identity Services native OAuth with PKCE and minimal consented Gmail scopes. Verify Google API scope restrictions and verification requirements. Never embed client secrets in the APK or expose access tokens to WebView JavaScript. Parse relevant purchase/renewal receipts locally where feasible, with explicit opt-in, provenance, confidence, revocation, and deletion controls.

## Android notifications
Implement a separate native NotificationListenerService only after user-initiated system settings consent. Minimise collection, allow app allowlist, do not capture unrelated private notification text, and explain Android/Google Play policy implications. Notifications are incomplete and may omit payments.

## Google Play / GPay
Google Play Developer API supports subscriptions for the developer's own apps, not a universal list of a consumer's third-party subscriptions. No general public GPay API grants an unrelated app a full personal payment history. Use permitted receipts/notifications as partial evidence; never claim full coverage.

## Reconciliation
Deduplicate by merchant normalization, external transaction identifiers where available, amount, billing interval, date window and source fingerprint. Keep raw evidence separate from confirmed subscriptions. State model: candidate -> high-confidence/needs-review -> confirmed/rejected; cancellation only through provider. No 100% accuracy claim. Store minimal data securely with Android Keystore-backed encryption, consent revocation and retention limits.

## Release gates
OAuth integration tests; mocked receipt tests; notification opt-in/out tests; false-positive and duplicate tests; privacy/security audit; Play policy review; Android 16 test on OnePlus; signing and reproducible CI build. No production launch until passed.
