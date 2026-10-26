# Project Rules & Guidelines

## 1. Automatic Version Increment Rules (SemVer)
Every time changes are made to the app code, automatically bump the version in `app/build.gradle.kts` according to the change scope:
- **Small Changes (`+0.0.1`)**: Bug fixes, minor UI adjustments, startup optimizations, small refactorings. Increment the PATCH version by 1 (e.g., `1.22.13` -> `1.22.14`).
- **Medium Changes (`+0.1.0`)**: New features, new screens, UI enhancements, API/service integrations. Increment the MINOR version by 1 and reset patch to 0 (e.g., `1.22.13` -> `1.23.0`).
- **Major Changes (`+1.0.0`)**: Architectural rewrites, breaking database/data migrations, fundamental core redesigns. Increment the MAJOR version by 1 and reset minor and patch to 0 (e.g., `1.22.13` -> `2.0.0`).
- **VersionCode Rule**: Whenever `versionName` changes, always increment `versionCode` by +1 (e.g., `233` -> `234`).

## 2. GitHub APK Release Confirmation Rule
- **MANDATORY**: Whenever the app version (`versionCode` or `versionName` in `app/build.gradle.kts`) is changed:
  - **ALWAYS ask the user for confirmation** before building, tagging, and uploading/publishing the APK file to GitHub Releases.
  - Never automatically create/push release tags (e.g. `v*`) or trigger the GitHub Release workflow without asking the user first.
  - Provide clear options (e.g., using `ask_question`) so the user can choose whether to upload the new APK to GitHub or keep it local.
