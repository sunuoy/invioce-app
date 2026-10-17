# App Version Change & GitHub APK Upload Rule

Whenever the application version (`versionCode` or `versionName` in `app/build.gradle.kts`) changes:
- **ALWAYS ask the user for confirmation** every time before building, tagging, and uploading the APK file to GitHub Releases.
- Do not automatically publish releases or push `v*` tags to GitHub without the user's explicit consent.
