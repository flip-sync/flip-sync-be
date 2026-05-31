# FlipSync Release Stabilization Handoff

Date: 2026-05-31

This handoff summarizes the current release-stabilization direction across `flip-sync-mobile` and `flip-sync-be`.

## Direction

The current priority is not broad feature expansion. The priority is to stabilize the internal-test release candidate:

- Lock down the current mobile and backend change scope.
- Verify the core P0 flows on a real installed app.
- Keep backend deployment, EAS Update, AAB build, and Google Play upload behind explicit user approval.
- Improve traceability for server errors, upload failures, and realtime state issues.

## Backend State

- Repository: `flip-sync-be`
- Branch: `main`
- Last known commit before this handoff: `e25cfa3 Force H2 for integration tests`

Backend release-candidate areas:

- Optional Redis realtime gateway with default `in-memory` mode.
- In-memory WebSocket stale-session cleanup and presence rebroadcast.
- `X-Request-Id` filter and log tracing.
- `/mob/invite/:id` preview/fallback improvements.
- Android App Links verification file at repository-root `assetlinks.json`, deployed to `/.well-known/assetlinks.json`.
- GitHub Actions uptime workflow for `/mob/ready` and `/mob/health`.

Backend checks before release:

```powershell
cd D:\codex\flip-sync\flip-sync-be
git diff --check
.\gradlew.bat :flip-sync-server:test
.\gradlew.bat :flip-sync-server:build --no-daemon
```

Backend smoke after deploy:

```powershell
curl.exe -i https://fliplyze.com/mob/ready
curl.exe -i https://fliplyze.com/mob/health
curl.exe -i https://fliplyze.com/.well-known/assetlinks.json
curl.exe -I https://fliplyze.com/mob/invite
curl.exe -I https://fliplyze.com/mob/invite/1
curl.exe -I https://fliplyze.com/mob/legal/privacy-policy
curl.exe -I https://fliplyze.com/mob/support
```

Android App Links note:

- The default `assetlinks.json` fingerprint is the Google Play App Signing certificate SHA-256 for `com.fliplyze.flipsync`.
- GitHub Actions copies repository-root `assetlinks.json` to the server, and `deploy.sh` installs it under the public `.well-known` path.
- If Google Play App Signing shows a different SHA-256 under Play Console > App integrity > App signing certificate, set `FLIPSYNC_APP_LINKS_ANDROID_SHA256_CERT_FINGERPRINTS` to that value before backend deployment.
- Multiple fingerprints can be provided as a comma-separated list.

## Mobile Companion State

- Repository: `flip-sync-mobile`
- Branch: `develop`
- Last known commit before this handoff: `237eebb feat: 1차 수정본 완료`
- Next orchestrator task: `RSA-A-002`, run `npx tsc --noEmit`

Mobile release-candidate areas:

- Auth/session refresh flow.
- Keyboard/SafeArea/input fixes.
- Room creation UX with 4-digit password policy and recent room settings.
- Score upload progress, score library filters, favorites, and recent scores.
- WebSocket token refresh and shared-view close handling.
- Invite, app-info, legal, and account-deletion screens.

## Do Not Proceed Without User Approval

- Backend production deployment.
- EAS Update.
- Android AAB build.
- Google Play internal-test upload.
- Enabling Redis realtime mode in production.
- Secret rotation or resource-yml repository changes.
- Reverting large dirty changes.

## Sensitive Files

- Do not print backend yml secrets in logs or docs.
- `flip-sync-server/src/main/resources` is connected to the yml repository and may contain sensitive DB/mail/JWT/AWS configuration.
- Do not commit generated build/cache/log files.
