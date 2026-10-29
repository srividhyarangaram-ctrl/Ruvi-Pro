# Ruvi 5.1 integration and security notes

## What actually works
- Manual transaction entry and CSV import; immediate local dashboard refresh after additions, imports and removals.
- User-initiated Gmail API receipt search using the existing Google OAuth Gmail readonly scope, with metadata and snippets. Each candidate requires manual verification and approval. Gmail access token is kept in memory and can be revoked from the UI.
- Previously approved Gmail message IDs are remembered locally to avoid importing the same receipt again. Matching date, normalized merchant, amount and type is an additional heuristic duplicate guard; legitimate identical purchases can be rejected, so review is required.
- Works as a PWA on HTTPS after you deploy and configure your own OAuth web client ID.

## Not implemented / not promised
- Direct access to Amazon, Flipkart, Myntra, Ajio, BigBasket, Blinkit or Zepto account orders: no general consumer OAuth transaction API is configured.
- Direct UPI/bank account reads, transaction webhooks or bank push alerts: require authorised bank/aggregator integrations and applicable consents. Do not collect bank passwords or UPI PINs.
- Background Gmail scanning or refresh after every real-world payment: the Gmail scan is manually initiated while the app is open. Email may arrive late or not at all.
- Gmail metadata can represent an order, a refund, a payment alert or spam; it is not verified settlement data. Amount extraction currently supports INR patterns only. Review each item before approval.

## Production hardening
1. Host under HTTPS on a domain you control. Configure Google Cloud Gmail API, OAuth consent screen, authorized JavaScript origin, test users and verification as required. Never embed OAuth client secrets in this PWA.
2. Avoid storing raw email contents, access tokens, bank credentials or full card numbers in localStorage. The app currently uses localStorage for subscription/transaction records, which are not encrypted and may be exposed on shared or compromised devices.
3. For automatic real-time refresh, build a backend with verified payment-provider/bank webhook signatures, encrypted storage, consent logs, idempotent event IDs and server-to-device updates (WebSocket/SSE/push). A webhook cannot be implemented securely as a static PWA alone.
4. If the app is offered to other users, obtain privacy/legal review, implement account deletion and consent revocation, and comply with Google restricted-scope verification where applicable.
