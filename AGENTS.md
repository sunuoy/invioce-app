# Project Rules & Guidelines

## App Version Changes & GitHub APK Release
- **MANDATORY**: Whenever the app version (`versionCode` or `versionName` in `app/build.gradle.kts`) is changed:
  - **ALWAYS ask the user for confirmation** before building and uploading/publishing the APK file to GitHub Releases.
  - Never automatically create/push release tags (e.g. `v*`) or trigger the GitHub Release workflow without asking the user first.
  - Provide clear options (e.g., using `ask_question`) so the user can choose whether to upload the new APK to GitHub or keep it local.
