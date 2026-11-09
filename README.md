# Bluebook Companion — Android (Kotlin + Jetpack Compose)

Frontend starter built from the hi-fi screens. It runs entirely on sample data and on-device storage,
so nothing is blocked on the backend.

## Open it
1. Android Studio (Koala or newer), JDK 17.
2. File > Open > select this folder. Let Gradle sync (it downloads Gradle 8.7 from the wrapper properties).
3. Run on an emulator or device, Android 8.0 (API 26) or higher.

## The core flow
Splash > accept privacy notice > Answer Sheet tab > Set A > Mathematics > Geometry (the only published set)
> answer, flag, Save and Exit > Home shows "Continue?" > Submit > Results > Analysis / Answer Key.
Other sets show "No published answer key yet" on purpose (FR05).

## Where things are
- `data/SampleData.kt`   fake catalogue + the 10-item Geometry key and tags. Replace with Supabase later.
- `data/AppConfig.kt`    client-pending thresholds (CC03 reference time, CC04 minimum items). Never hardcode these in screens.
- `data/Scoring.kt`      scoring, difficulty, topic, branch mastery, speed split. Pure functions: easy to test against a manually scored sheet.
- `data/LocalStore.kt`   on-device save/resume, attempt history, profile.
- `state/AppViewModel.kt` single source of UI state.
- `ui/theme/`            colors and theme. Colors are estimated from the screens: copy exact values from Figma.
- `ui/components/`       reusable pieces (answer rows, cards, bars, dialogs).
- `ui/screens/`          one file per screen group.

## Known gaps 
- Logo/branding images (placeholders in text), Inter font, exact hex values.
- Not built, not designed: Cognitive Skill analysis (FR15), Progress Over Time (FR19), Estimated Score (FR20),
  Review Plan (FR21), Cohort Comparison (FR22), jump-to-item (FR06).
- Exam Date card exists in the design but not in any requirement or user story.
- Terms and Conditions text is a placeholder.
- Answer key versioning (BR06) and the real backend are not wired.
