# Bluebook Companion — Android (Kotlin + Jetpack Compose)

Frontend built from the hi-fi screens, backed by Supabase (Auth + Postgres + Edge Functions) in
`bluebook-companion-be`'s `supabase/` project — see that repo's `docs/SUPABASE_LAYOUT.md` for the
schema, RLS, and deployment details.

## Open it
1. Android Studio (Koala or newer), JDK 17.
2. Copy `local.properties.sample` to `local.properties` and fill in `SUPABASE_URL`/`SUPABASE_ANON_KEY`
   from the target Supabase project (local dev stack or the deployed remote project).
3. File > Open > select this folder. Let Gradle sync (it downloads Gradle 8.7 from the wrapper properties).
4. Run on an emulator or device, Android 8.0 (API 26) or higher.

## The core flow
Splash > accept privacy notice (also triggers anonymous sign-in, FR04) > Answer Sheet tab > pick a
published exam set > Answer, flag, Save and Exit > Home shows "Continue?" > Submit (scored
server-side via `submit_attempt()`, SEC-08/09/11) > Results > Analysis / Answer Key > Review Plan
(FR21, via the `plan-gateway` Edge Function).
Unpublished sets show "No published answer key yet" on purpose (FR05).

## Where things are
- `data/remote/CatalogRepository.kt`  exam set catalogue + item tags, read from `v_sheet_catalog`/`v_sheet_items` (Postgrest). Replaces the old hardcoded sample data.
- `data/remote/AttemptRepository.kt`  submits an attempt via the `submit_attempt()` RPC — the only place the answer key is read; it never reaches this client.
- `data/remote/SupabaseErrors.kt`     turns a `RestException` into a safe user-facing message (never surfaces `.message`, which embeds the request's raw headers/bearer token).
- `data/SupabaseClient.kt`            the shared `SupabaseClient` (Auth, Postgrest, Functions).
- `data/AuthRepository.kt`            anonymous sign-in, gated on the privacy notice (FR04).
- `data/AppConfig.kt`                 client-pending thresholds (CC03 reference time, CC04 minimum items, FR21 budget defaults). Never hardcode these in screens.
- `data/Scoring.kt`                   scoring, difficulty, topic, branch mastery, and speed split — all read from `Attempt.outcomes`/`correctLetters`, not a client-held key. Pure functions: easy to test against a manually scored sheet.
- `data/LocalStore.kt`                on-device save/resume, attempt history, profile, a cache of the Supabase-issued user id (never the source of truth).
- `data/ReviewPlanApi.kt`             FR21's one Edge Function call (`plan-gateway`, via `functions-kt`). The request carries only the budget/strong-topic tuning — the server builds the topic stats itself from the caller's own history.
- `state/AppViewModel.kt`             single source of UI state: auth phase, catalogue, submit, and review-plan loading/error states.
- `ui/theme/`                         colors and theme. Colors are estimated from the screens: copy exact values from Figma.
- `ui/components/`                    reusable pieces (answer rows, cards, bars, dialogs).
- `ui/screens/`                       one file per screen group; `ReviewPlanScreen.kt` is FR21's screen.

## Offline scope
Browsing the catalogue and taking an answer sheet (including the in-progress draft, which stays
local-only and is never synced) work fully offline; viewing already-scored past results stays
offline too (outcomes are cached locally at submit time). Submitting an attempt and Review Plan
both require network — a failed submit keeps the draft intact (never discards answers) and surfaces
a Retry dialog.

## Known gaps
- Logo/branding images (placeholders in text), Inter font, exact hex values.
- Not built, not designed: Cognitive Skill analysis (FR15), Progress Over Time (FR19), Estimated Score (FR20),
  Cohort Comparison (FR22), jump-to-item (FR06).
- Exam Date card exists in the design but not in any requirement or user story.
- Terms and Conditions text is a placeholder.
