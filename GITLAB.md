# GitLab Integration and Package Registry Guide

This guide explains how to sync this project with **gitlab.com** and publish the built APK to the **GitLab Package Registry (Packages section)**.

---

## 1. Syncing with GitLab

If you haven't configured a GitLab remote yet, run the following commands to add the remote and push the code:

### Add GitLab Remote
```bash
# Add the GitLab remote URL (replace username and repo-name with your GitLab details)
git remote add gitlab https://gitlab.com/sunuoy/invioce-app.git
```

### Push Code to GitLab
```bash
# Push the main branch to GitLab
git push -u gitlab main
```

*Note: If you have a Personal Access Token (PAT) or use SSH, make sure your credentials are configured in Git or use the SSH URL format (`git@gitlab.com:sunuoy/invioce-app.git`).*

---

## 2. Uploading APK to GitLab Package Registry

GitLab offers a **Generic Package Registry** where you can host raw files (like Android APKs). You can upload the APK either manually or via CI/CD.

### A. Manual Upload (via Curl or PowerShell)

You can upload the APK from your local machine using the GitLab API with a **Personal Access Token (PAT)**.

#### Using Curl:
```bash
curl --header "PRIVATE-TOKEN: YOUR_PERSONAL_ACCESS_TOKEN" \
     --upload-file app/build/outputs/apk/debug/app-debug.apk \
     "https://gitlab.com/api/v4/projects/YOUR_PROJECT_ID/packages/generic/invoice-generator/1.1/InvoiceGenerator_v1.1_debug.apk"
```

#### Using PowerShell:
```powershell
$token = "YOUR_PERSONAL_ACCESS_TOKEN"
$projectId = "YOUR_PROJECT_ID"
$filePath = "app/build/outputs/apk/debug/app-debug.apk"
$uri = "https://gitlab.com/api/v4/projects/$projectId/packages/generic/invoice-generator/1.1/InvoiceGenerator_v1.1_debug.apk"

$headers = @{
    "PRIVATE-TOKEN" = $token
}

$fileBytes = [System.IO.File]::ReadAllBytes($filePath)
Invoke-RestMethod -Uri $uri -Headers $headers -Method Put -Body $fileBytes
```

*To find your **Project ID**, go to your GitLab project's home page; it is displayed right below the project name.*

---

## 3. Automated Upload via GitLab CI/CD

To make the build and upload completely automated, we have configured a `.gitlab-ci.yml` pipeline. When you push to GitLab:
1. GitLab CI will spin up an Android environment.
2. It will build the debug APK.
3. It will automatically upload the built APK directly to **Packages & Registries > Package Registry** on GitLab.

This requires no manual tokens or setup.
