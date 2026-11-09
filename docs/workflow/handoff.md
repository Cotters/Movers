# Current handoff

Updated 9 November 2026. Verify Git/source before resuming. The agreed [Shared Watchlists design](../features/shared-watchlists.md) is implemented in the working tree; host/build verification is complete; hosted/backend and physical-device acceptance remain pending. Source checkpoint at start: `2a34d3a` (`Improve workflow`). Preserve user staging/edits; no agent commit or push.

## Decisions and implementation

- Shared Android/iOS Compose UI, Nav3 feature keys/entry-scoped ViewModels, Koin and convention plugins remain. Personal lists/bookmarks stay local. Sharing publishes a selected list in place; shared edits need online confirmation and Room owns observable cache.
- All members have equal edit/invite access. Creator is attribution only. No kicking/global deletion/ownership transfer. Leaving removes local shared cache; last departure retires the list.
- First-launch name confirmation is now authorized and implemented. Existing Josh profiles retain data and confirm/replace their name. Profile supports name editing. Initial/color avatars are assigned randomly and persisted; color customization and the feature tour remain deferred.
- Anonymous device authentication has no login UI/recovery/device linking. Installation IDs/names do not authorize the backend. Credentials use Keystore-backed encryption/Keychain, outside Room/navigation/logs. Do not silently replace an unavailable identity.
- `SharedWatchlistRemote` and `ParticipantIdentity` are neutral seams; `:backend:supabase` owns SDK/auth/realtime code. Local builds work without backend settings. See [setup](../../supabase/README.md); no live project has been provisioned or configured by the agent.
- Room **9** preserves 7/8 through explicit migrations. Destructive fallback remains limited to **1–6**. Shared snapshots/revisions, activity/server cursors and already-started online commands are cached. Idempotency receipts prevent uncertain old writes replaying over later changes.
- Invitations use `movers://join/<token>`, native sharing and paste fallback; seven-day validity, explicit preview/Join, all-member revoke. No web/domain/install fallback. Dedicated Members/Activity screens and row contributor attribution are implemented.
- Supabase SDK 3.9.0 requests Android Lifecycle 2.11. Its `lifecycle-process` dependency is constrained to the app's existing 2.10 line to prevent runtime/compile ViewModelStore signature mismatch. Do not remove without checking both graphs and existing host navigation tests.

## Workflow and verification

Run existing tests and update intended expectations; **no new test cases** were authorized. Never run/interact with emulators/simulators or execute connected tests. People verify on physical devices. Serialize Gradle/Xcode invocations.

- Final `./scripts/verify.sh` passes: **92 tests / 24 suites**, zero failures/errors/skips; five convention tests reused passing outputs. Search/shared tests executed in the final run; seven other module suites reused passing outputs. Final app phase: 34 s, 40 executed / 522 up-to-date. Lint/debug APK pass: zero errors/fatal, 31 warnings. Existing migration case covers populated versions 7 and 8 through 9. Existing rename/delete UI case now waits for dialog completion/enabled controls instead of only typed text.
- Final migration applied to a fresh disposable PostgreSQL database. Synthetic checks pass: member/non-member/guest permissions, immutable/server attribution, duplicate/no-op writes, retry after another member removes a film, creator departure, revocation, last-member retirement, expiry, two-page activity pagination and malformed-payload rollback. The temporary server was stopped. This does not establish hosted Auth/Realtime behavior.
- Android test APKs (`:shared`, `:movie:internal`) and `:shared:linkDebugFrameworkIosSimulatorArm64` compile: 1m 26s, 128 executed / 287 up-to-date. No installation, launch or device-test execution. Final `./scripts/verify-ios.sh` exits 0 against the physical-device SDK after the final shared sources; unsigned compile/link only, no launch.
- Documentation checker now includes feature/backend setup docs and README: 15 files / 82 local targets pass. Shell syntax and `git diff HEAD --check` pass. The previous [local-list handoff](history/2026-11-09-pre-sharing.md) holds older evidence.

## Next / limits

Implementation and host review are complete. User physical-device acceptance must cover two identities: publish/join, bidirectional/concurrent changes, uncertain retries, offline/reconnect, activity/avatars, cold/warm invitations, expiry/revocation, leave/rejoin, relaunch and existing personal-list/bookmark/navigation behavior.

Hosted Supabase setup and native acceptance remain pending. Public backend distribution requires abuse/CAPTCHA work; this version targets a private friend group. Free projects may pause when inactive. Historical iOS minimum-15/ICU warning and older-OS acceptance remain unresolved; do not change minimum OS incidentally. CI/signing/distribution remain unverified.
