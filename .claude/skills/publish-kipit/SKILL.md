---
name: publish-kipit
description: Use when publishing a new version or release of the Kipit Android application to GitHub Releases
---

# Publishing Kipit Releases

## Overview

Automates the complete release lifecycle for the Kipit Android application—including version bumping, pre-release compilation and test verification, git commit and tag orchestration, GitHub Actions CI release triggering, and post-release verification for the in-app update checker.

## When to Use

- Use when preparing and deploying a new release of Kipit
- Use when publishing a new version tag (`v*.*.*`) to GitHub Releases
- Use when cutting a patch, minor, or major version update that users will receive via in-app update or direct APK download
- Do NOT use for regular local development commits or feature branches without intent to release
- Do NOT use if the git working tree has uncommitted unrelated changes or broken unit tests

## Quick Reference

| Action | Target / Command | Purpose |
|---|---|---|
| **Version Config** | `app/build.gradle.kts` | Update `versionCode` (int) & `versionName` (semver string) |
| **Pre-Flight Test** | `./gradlew testDebugUnitTest` | Verify all unit and integration tests pass |
| **Pre-Flight Build** | `./gradlew assembleRelease` | Verify release APK compiles cleanly |
| **Commit Version** | `git commit -m "chore(release): bump version to vX.Y.Z"` | Commit version increment |
| **Tag Release** | `git tag -a vX.Y.Z -m "Release vX.Y.Z"` | Create annotated git tag |
| **Trigger CI** | `git push origin main && git push origin vX.Y.Z` | Trigger GitHub Actions `.github/workflows/release.yml` |
| **Verify Release** | `https://api.github.com/repos/Patkik/Kipit/releases/latest` | Confirm GitHub Release and APK asset are published |

## Release Protocol

Follow this exact step-by-step sequence when publishing a release:

### 1. Pre-Flight Cleanliness & Branch Check
Ensure you are on the `main` branch with no unstaged or dirty changes:
```bash
git status --porcelain
git branch --show-current
```
If there are uncommitted changes, commit or stash them first. Never trigger a release with unexpected working directory changes.

### 2. Version Increment Strategy
Open [app/build.gradle.kts](file:///app/build.gradle.kts) and inspect the current `versionCode` and `versionName`:
- **`versionCode` (Integer):** ALWAYS increment by +1 (e.g. `1` -> `2`). Android prevents installing APK updates with lower or identical versionCode.
- **`versionName` (String):** Increment according to Semantic Versioning:
  - **Patch (`0.1.0` -> `0.1.1`):** Bug fixes, UI polishes, small refinements.
  - **Minor (`0.1.0` -> `0.2.0`):** New features, new screens, backward-compatible enhancements.
  - **Major (`0.1.0` -> `1.0.0`):** Breaking changes or major architectural milestones.

Example modification in `app/build.gradle.kts`:
```kotlin
defaultConfig {
    applicationId = "com.kipit.app"
    minSdk = 26
    targetSdk = 36
    versionCode = 2
    versionName = "0.1.1"
    buildConfigField("boolean", "DEMO_DATA", "false")
}
```

### 3. Local Verification Gate (MANDATORY)
Before touching git tags, verify that the project builds and all unit tests pass:
```bash
# On Windows pwsh
.\gradlew.bat testDebugUnitTest assembleRelease
# On Linux / macOS
./gradlew testDebugUnitTest assembleRelease
```
> [!IMPORTANT]
> If any test fails or compilation errors occur, **STOP IMMEDIATELY**. Do not proceed to tagging until all tests pass with code 0.

### 4. Generate Changelog Summary
Review commits since the previous release tag:
```bash
PREV_TAG=$(git describe --tags --abbrev=0 2>/dev/null || echo "")
if [ -n "$PREV_TAG" ]; then
    git log "$PREV_TAG..HEAD" --oneline --no-merges
else
    git log --oneline -n 10 --no-merges
fi
```
Compile a brief bulleted list of changes for the release notes (Features, Fixes, Improvements).

### 5. Git Commit & Annotated Tag
Commit the version bump and create an annotated tag with the `v` prefix matching `versionName`:
```bash
NEW_VERSION="0.1.1"
git add app/build.gradle.kts
git commit -m "chore(release): bump version to v${NEW_VERSION}"
git tag -a "v${NEW_VERSION}" -m "Release v${NEW_VERSION} - [Short summary of changes]"
```

### 6. Push to GitHub & Trigger CI Release
Push both the commit and the new tag to GitHub:
```bash
git push origin main
git push origin "v${NEW_VERSION}"
```
This triggers the `.github/workflows/release.yml` GitHub Actions workflow, which will:
1. Checkout the tagged code.
2. Setup Java 21 and Android Gradle.
3. Decode the release signing keystore (or use debug fallback).
4. Run unit tests and build the release APK `kipit-v${NEW_VERSION}.apk`.
5. Create a GitHub Release with auto-generated release notes and attach the APK.

### 7. Post-Release Verification
Poll the public GitHub Releases API until the new release is live:
```bash
curl -s https://api.github.com/repos/Patkik/Kipit/releases/latest
```
Verify that:
1. `"tag_name"` equals `"v${NEW_VERSION}"`.
2. `"assets"` array contains `kipit-v${NEW_VERSION}.apk` with a non-zero `"size"`.
3. In Kipit Settings -> "Check for Updates", the app reports the new version and downloads the APK.

## Keystore Setup (One-Time)
For cryptographically signed releases in production:
1. Run `.\scripts\generate-keystore.ps1` to create `release.jks` and get the Base64 key.
2. In GitHub repository `Settings -> Secrets and variables -> Actions`, add:
   - `RELEASE_KEYSTORE_BASE64`
   - `KEYSTORE_PASSWORD`
   - `KEY_ALIAS`
   - `KEY_PASSWORD`
*(If secrets are not added, the workflow gracefully falls back to debug signing so the release APK build still succeeds).*

## Common Mistakes & Troubleshooting

| Mistake | Consequence | Prevention / Fix |
|---|---|---|
| **Skipping local build gate** | CI fails on GitHub Actions, leaving an orphan release tag | Always execute `./gradlew testDebugUnitTest assembleRelease` before `git tag` |
| **Forgetting to bump `versionCode`** | Android refuses to update the installed app ("App not installed") | Always increment `versionCode` integer by at least 1 for every release |
| **Using lightweight tag (`git tag v1.0.0`)** | Missing tagger metadata and annotations in GitHub Releases | Always use annotated tags with `-a` and `-m` |
| **Omitting the `v` prefix (`1.0.0` vs `v1.0.0`)** | In-app updater regex and CI filter match `v*` | Always format tags as `vX.Y.Z` |
| **Committing `.jks` file to git** | Security vulnerability & potential credential leak | `release.jks` is ignored in `.gitignore`. Never use `git add -f` on keys |
