# Personal F-Droid Repository Setup Guide (Manual Setup)

This guide walks you through setting up a personal F-Droid repository to host your `invioce-app` APK **without** needing WSL or Docker locally.

## Prerequisites

- GitHub account (`sunuoy`)
- GitHub Personal Access Token (no scopes needed)
- The signed Release APK (`InvoiceGenerator_v1.8_release.apk`)

---

## Step 1: Fork the Template Repository

1. Go to [github.com/xarantolus/fdroid](https://github.com/xarantolus/fdroid)
2. Click **Fork** button (top right)
3. Name your repository: `fdroid` (or `fdroid-repo`)
4. Click **Create fork**

---

## Step 2: Clone Your Forked Repository

```bash
git clone https://github.com/sunuoy/fdroid.git
cd fdroid
```

---

## Step 3: Clean Up and Add Your App

1. **Delete everything in the `fdroid` directory** (this removes the original author's apps):
   ```bash
   # On Windows
   del /q fdroid\*
   rmdir /s /q fdroid
   
   # Or on Git Bash
   rm -rf fdroid/*
   ```

2. **Copy the `apps.yaml` file** from your `invioce-app` repository to the `fdroid` repository root:
   ```bash
   copy "D:\data\ai\invioce-app\fdroid\apps.yaml" .
   ```

3. **Copy the GitHub Actions workflow**:
   ```bash
   mkdir .github\workflows
   copy "D:\data\ai\invioce-app\.github\workflows\fdroid-publish.yml" .github\workflows\
   ```

---

## Step 4: Create GitHub Secrets

### 4.1 Generate GitHub Personal Access Token

1. Go to [github.com/settings/tokens/new](https://github.com/settings/tokens/new?description=f-droid%20repo)
2. Description: `f-droid repo`
3. Expiration: **No expiration** (or your preferred duration)
4. **Do NOT select any scopes** (leave all unchecked)
5. Click **Generate token**
6. **Copy the token** (you won't see it again!)

### 4.2 Add Secrets to GitHub

1. Go to your `fdroid` repository on GitHub
2. Navigate to **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret**:

| Secret Name | Value | Source |
|-------------|-------|--------|
| `GH_ACCESS_TOKEN` | Your GitHub token | From step 4.1 |

---

## Step 5: Enable GitHub Pages

1. Go to your `fdroid` repository **Settings** → **Pages**
2. Source: **Deploy from a branch**
3. Branch: `gh-pages` (will be created by the workflow)
4. Folder: `/ (root)`
5. Click **Save**

---

## Step 6: Create First Release on invioce-app

1. Go to your `invioce-app` repository
2. Navigate to **Releases** → **Create a new release**
3. Tag: `v1.8`
4. Title: `v1.8 Release`
5. Description:
   ```
   ## What's New in v1.8

   - Personal F-Droid repository support
   - Bug fixes and improvements
   ```
6. Upload: `releases\InvoiceGenerator_v1.8_release.apk`
7. Click **Publish release**

---

## Step 7: Trigger F-Droid Repository Update

After creating the release on `invioce-app`:

1. Go to your `fdroid` repository **Actions** tab
2. The workflow should trigger automatically
3. Wait for the workflow to complete
4. Check the Actions logs for the repository fingerprint

---

## Step 8: Add Repository to F-Droid App

### On your Android device:

1. Open **F-Droid** app
2. Go to **Settings** → **Repositories**
3. Tap **+** (Add Repository)
4. Enter:
   - **Name**: `Personal Repo` (or any name)
   - **URL**: 
     ```
     https://raw.githubusercontent.com/sunuoy/fdroid/gh-pages/fdroid/repo?fingerprint=YOUR_FINGERPRINT
     ```
     (Replace `YOUR_FINGERPRINT` with the SHA256 from Actions logs)
5. Tap **Add**
6. Go back and pull to refresh
7. Search for "Invoice Generator"
8. Install the app

---

## Updating Your App

When you release a new version:

1. Bump version in `app/build.gradle.kts`:
   ```kotlin
   versionCode = 10  // increment
   versionName = "1.9"  // increment
   ```

2. Build release APK:
   ```bash
   .\gradlew.bat assembleRelease
   ```

3. Copy APK to releases:
   ```powershell
   Copy-Item "app\build\outputs\apk\release\app-release.apk" "releases\InvoiceGenerator_v1.9_release.apk"
   ```

4. Create GitHub release with the new APK

5. The F-Droid repository will auto-update within minutes

---

## Verification

### Verify APK Signature

```bash
# Using apksigner (Android SDK build-tools)
apksigner verify --print-certs app\build\outputs\apk\release\app-release.apk
```

Expected output:
```
Signer #1 certificate DN: CN=Invoice App, OU=Development, O=MyApp, L=City, ST=State, C=IN
Signer #1 certificate SHA-256 digest: 3d6b5ff206f87c511171b08365fce19a28d6ebc263028a100051f548b7814a12
```

### Verify Repository Access

```bash
# Check if index files are accessible
curl https://raw.githubusercontent.com/sunuoy/fdroid/gh-pages/fdroid/repo/index-v1.jar
curl https://raw.githubusercontent.com/sunuoy/fdroid/gh-pages/fdroid/repo/index-v2.json
```

---

## Troubleshooting

### Issue: GitHub Actions workflow doesn't trigger
- Ensure `GH_ACCESS_TOKEN` secret is set correctly
- Check that the token has no scopes
- Verify the release has an `.apk` file attached

### Issue: F-Droid can't find the app
- Check that `apps.yaml` has the correct git URL
- Verify the repository fingerprint matches
- Ensure GitHub Pages is enabled

### Issue: APK signature verification fails
- Use `apksigner` instead of `keytool` for APK v2/v3 signatures
- Ensure you're using the release signing config, not debug

---

## Repository URL Template

Once set up, share this URL:

```
https://raw.githubusercontent.com/sunuoy/fdroid/gh-pages/fdroid/repo?fingerprint=YOUR_FINGERPRINT
```

Users can add this to F-Droid → Settings → Repositories → Add Repository.

---

## Files Created

| File | Location | Purpose |
|------|----------|---------|
| `FDROID_SETUP.md` | `invioce-app/` | This setup guide |
| `InvoiceGenerator_v1.8_release.apk` | `releases/` | Signed release APK |
| `apps.yaml` | `fdroid/` | App metadata (after fork) |
| `.github/workflows/fdroid-publish.yml` | `fdroid/` | GitHub Actions workflow |
