# Ruvi 2.0 — Subscription Manager + Gmail receipt discovery

## What works
- Add/edit/delete subscriptions, recurring-cost totals, JSON backup and import.
- Optional Gmail receipt discovery using Google Identity Services OAuth 2.0 and Gmail API read-only scope.
- Scans up to 60 possible receipt messages from the past two years. Shows subject, sender, snippet and estimated payment details. You must review and explicitly save each candidate.
- Gmail access token stays in memory, not localStorage; no Ruvi backend receives Gmail content.

## Google Cloud setup (required for Gmail scan)
1. In Google Cloud Console, create a project you control and enable **Gmail API**.
2. Configure Google Auth Platform / OAuth consent screen; use the appropriate External audience and add your Gmail address as a test user while in testing mode.
3. Create **OAuth Client ID → Web application**. Add the **exact HTTPS origin** where you deploy Ruvi to **Authorized JavaScript origins** (for example `https://YOURNAME.github.io`; the origin does not include `/ruvi/`).
4. Copy the **client ID**, NOT a client secret, into Ruvi's Gmail section. Google may require app verification and a privacy policy for public use of Gmail read-only scope. In testing mode, only allowed test users can authorise; Google may impose token expiry and test-user limits.
5. Open Ruvi at the registered origin, click Connect Gmail & scan, and consent. Google may warn about an unverified app if you have not completed verification; do not bypass warnings for projects you don't control.

## Secure Android install
1. Publish the `ruvi` directory using an HTTPS static host (GitHub Pages, Cloudflare Pages, Netlify, etc.). Use an account and project you control. Do not upload secrets, credentials or downloaded email content.
2. Verify the site URL, HTTPS padlock and that it is your own deployment. Open it in Chrome on Android.
3. Use Chrome menu → Add to Home screen / Install app (label varies). Ruvi opens as a standalone web app. No APK sideloading, unknown-sources permission, or banking permissions are required.
4. Connect Gmail from the installed app. If OAuth fails in standalone mode, open the same HTTPS URL in Chrome and connect there; tokens are deliberately not persisted across app sessions.
5. Use Disconnect to revoke the current token. Also revoke Ruvi from Google Account → Security → Third-party connections if desired.

## Security and limitations
- This is a **PWA**, not a signed Play Store APK. No production deployment or OAuth verification has been done on your behalf.
- Gmail access is **read-only**; no emails are modified or deleted. It uses a broad Gmail scope, so connect only to a deployment you trust. Google shows the actual permissions during consent.
- Search is heuristic: may miss subscriptions, include one-off receipts, or misread amounts/currency/cadence. It cannot reliably prove active status, usage, or renewal date. Always verify before saving. Non-INR values are intentionally not prefilled as INR.
- Receipt metadata includes snippets, which can contain personal information. They are displayed on the device but not saved to Ruvi's subscription database. Browser localStorage holds manually saved subscription data **unencrypted**; avoid shared devices and export backups carefully.
- Data does not automatically sync across devices. Google access tokens are kept in memory and disappear on reload. No server-side scheduled scanning or push alerts are implemented.
- For broader deployment: add an explicit privacy policy, verified OAuth app, stronger encrypted storage and security review.

## Version 2.1 duplicate detection
Ruvi flags identical or similar subscription service names, displays possible duplicate pairs, warns before adding a matching subscription, and flags Gmail receipt suggestions already in the dashboard. This is heuristic matching, not proof of duplicate charges. It never automatically deletes, cancels, or combines entries. Review payment accounts and billing periods before taking action.
