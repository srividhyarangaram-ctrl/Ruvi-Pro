# Ruvi 5.2 — PRIVATE ANDROID TEST BUILD (NOT PRODUCTION APPROVED)

This is an Android Studio/Gradle source project for a private test APK. It has NOT been compiled, signed for release, tested on a OnePlus, or reviewed by Google Play. The GitHub Actions workflow produces a **debug-signed APK**, not a production-signed release or Play Store AAB. Do not distribute it publicly.

## Phone-only build using GitHub
1. On your phone create a **private** GitHub repository; enable two-factor authentication.
2. Upload the extracted project files preserving folders, including `.github/workflows/android.yml` (mobile GitHub UI can make this difficult; a browser in desktop mode or GitHub's web editor may help).
3. Open Actions > Private Ruvi Android test APK > Run workflow (or push to main).
4. Wait for a successful build, download `Ruvi-private-debug-APK` artifact and extract its APK on your phone.
5. Install only your own generated APK. Android may request permission to install from the browser/file manager. Revoke that permission afterward.
6. Test with **fictional data only** until security work is complete. Verify dashboard, manual entries, duplicate alerts, refund handling, backup/restore and cancellation-pending behavior.

## Deliberate restrictions
- Gmail scanning is **disabled** in this APK because a local WebView cannot safely reuse the web OAuth flow as-is. Production requires a compliant Android OAuth integration, consent, scope minimisation and Google verification as applicable.
- Direct bank, UPI, ecommerce account sync and automatic background transaction refresh are **not implemented**.
- Local data is in WebView browser storage and is **not encrypted at the application layer**. Do not put real sensitive banking data in this test build.
- The app cannot cancel provider billing; it can only track cancellation intent.
- No external INTERNET permission is requested; this test build is offline and uses only bundled assets.
- Review security, privacy policy, account/data deletion, Play Data Safety, financial features declaration, Gmail restricted scope rules, Android target SDK, accessibility and closed testing before any Play Store submission.

## Known limitations
- CSV upload and backup file download may require Android-native file chooser/download handlers; these are not implemented in this minimal WebView wrapper.
- Build workflow and dependency compatibility are unverified here because Android SDK/Gradle are unavailable in this environment.
- No production signing key is generated or stored. Never commit signing keys or credentials to GitHub.
