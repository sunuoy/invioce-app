# App Version Change & GitHub APK Release Rules

## 1. Automatic Version Increment Rules (SemVer)
Every time changes are made to the app code, automatically bump the version in `app/build.gradle.kts`:
- **Small Changes (`+0.0.1`)**: Bug fixes, minor UI tweaks, startup/performance optimizations. Increment PATCH by 1 (e.g., `1.22.13` -> `1.22.14`).
- **Medium Changes (`+0.1.0`)**: New features, new screens, significant enhancements. Increment MINOR by 1 and reset patch to 0 (e.g., `1.22.13` -> `1.23.0`).
- **Major Changes (`+1.0.0`)**: Architectural changes, breaking updates, major redesigns. Increment MAJOR by 1 and reset minor/patch to 0 (e.g., `1.22.13` -> `2.0.0`).
- **versionCode**: Always increment `versionCode` by +1 whenever `versionName` changes.

## 2. GitHub Release Confirmation
- Whenever the application version (`versionCode` or `versionName` in `app/build.gradle.kts`) changes:
  - **ALWAYS ask the user for confirmation** before building, tagging, and uploading the APK file to GitHub Releases.
  - Do not automatically publish releases or push `v*` tags to GitHub without the user's explicit consent.
