# Ruvi 5.3 — safe onboarding patch

## Status
This is a working onboarding/tooltips patch, NOT a working Gmail, Google Play or GPay integration. It does not silently access messages, transactions or notifications. Do not advertise full automatic discovery until separately implemented, consented, tested and approved.

## Target repository and branch
Repository: `srividhyarangaram-ctrl/Ruvi-Pro`
Base branch: `Main` (capital M)
Create branch: `feature/ruvi-5.3-discovery`

## Apply on GitHub using your phone
1. Open the repository in Chrome; choose `Main` in the branch selector, then create `feature/ruvi-5.3-discovery` from `Main`.
2. Upload `app/src/main/assets/onboarding53.js` from this ZIP to the same repository path on the new branch. GitHub's web editor may require desktop-site mode.
3. Edit `app/src/main/assets/index.html` on the new branch. Immediately before `</body>`, add exactly:
   `<script src="onboarding53.js"></script>`
4. Edit `app/build.gradle`: change `versionCode 52` to `versionCode 53`, and `versionName '5.2'` to `versionName '5.3'`.
5. Commit the changes to `feature/ruvi-5.3-discovery` (not Main). Open a pull request to Main after checking the Actions build and testing the UI on your phone.
6. Optional: upload `docs/DISCOVERY_INTEGRATION_PLAN.md` for future engineering reference.

## What works
First-launch guided walkthrough, skip/reopen option, contextual help for spend, duplicates and shopping, explicit unavailable-integration warning. Does not alter saved subscriptions.

## Not included
Gmail OAuth, Gmail receipt parsing, native notification listener, Google Play account subscription API, Google Pay transaction API, background sync, production encryption, Play Store compliance verification. These require additional code, permissions, security review, and live-device testing.
