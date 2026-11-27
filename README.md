# Twelve (Music)

Music player for Android fork on [Twelve](https://github.com/LineageOS/android_packages_apps_Twelve), the LineageOS music app. This repository is a fork set up to **build as a standalone APK and install on any ROM**, not just LineageOS, with automatic builds and releases through GitHub Actions.

- **Package:** `org.lineageos.twelve` (release) and `org.lineageos.twelve.dev` (debug)
- **Launcher name:** Music
- **Minimum Android:** 11 (API 30) · **compileSdk / targetSdk:** 36
- **License:** Apache-2.0

---

## Table of contents

- [Features](#features)
- [Compatibility with other ROMs](#compatibility-with-other-roms)
- [Installation](#installation)
- [Permissions](#permissions)
- [Building locally](#building-locally)
- [Continuous integration](#continuous-integration)
- [Setting up secrets and keystores](#setting-up-secrets-and-keystores)
- [Changes from upstream](#changes-from-upstream)
- [Project structure](#project-structure)
- [Troubleshooting](#troubleshooting)
- [License and credits](#license-and-credits)

---

## Features

**Music sources (providers)**

- Local music on the device, through `MediaStore`
- Subsonic / OpenSubsonic / Navidrome servers
- Jellyfin
- Ampache

**Library and playback**

- Browse by albums, artists, genres and playlists
- Your own playlists, with add and remove from any song
- Playback queue, shuffle and repeat (all / one / off)
- Synced lyrics, with follow-current-line
- Local playback statistics
- Streaming with HLS, RTSP, SmoothStreaming and MIDI support (Media3 / ExoPlayer)

**Interface**

- Material 3 Expressive and dynamic colors
- "Now playing" screen with several audio visualizer types
- Now playing widget
- Fullscreen mode
- Android Auto support

**Audio and playback settings**

- Offload output and float output
- Skip silence
- Stop playback when the app is removed from recents
- Rescan the media library

---

## Compatibility with other ROMs

The app only uses public Android SDK APIs. It doesn't depend on LineageOS code or services, doesn't use `sharedUserId`, and doesn't need platform signing. It therefore works on any ROM running Android 11 or later (AOSP, GrapheneOS, /e/OS, crDroid, etc.).

What to expect outside LineageOS:

| Aspect | Behavior |
| --- | --- |
| Installation | As a regular user app, with no special permissions |
| `MANAGE_MEDIA` | A protected permission that is declared but not used by the code. It isn't granted and doesn't affect functionality |
| Visualizer | Asks for `RECORD_AUDIO` at runtime. Some ROMs modify the audio effects and capture may not work well |
| Another music player | Twelve only competes for audio intents: the system lets the user choose which one to use |
| Updating the original LineageOS app | Not possible: the signature is different. Uninstall the original or change the `applicationId` |

> The `app/Android.bp` file and the `sysconfig` XML files are only for integrating the app into a ROM's source tree (Soong build). They aren't used for a standalone APK.

---

## Installation

1. Download the APK from the repository's [Releases](../../releases) section.
2. Verify its integrity with the `.sha256` file that accompanies each APK:

   ```bash
   sha256sum -c Twelve-<version>.apk.sha256
   ```

3. Install it on the device (enable "Install unknown apps" for your file manager or browser if needed).

**Debug** APKs (`org.lineageos.twelve.dev`) install side by side with **release** ones without conflict, but they may be signed with different keys, so they can't update each other.

---

## Permissions

| Permission | Purpose |
| --- | --- |
| `READ_MEDIA_AUDIO` (Android 13+) / `READ_EXTERNAL_STORAGE` (up to Android 12) | Read local music |
| `INTERNET` | Subsonic, Jellyfin and Ampache servers, and cover art |
| `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Background playback service |
| `WAKE_LOCK` | Keep playing with the screen off |
| `RECORD_AUDIO` | Only for the audio visualizer, requested when it is turned on |
| `MANAGE_MEDIA` | Declared but not used by the code (see the compatibility table) |

The app allows unencrypted HTTP traffic (`cleartextTrafficPermitted`) so it can connect to home servers without HTTPS. If your server has a certificate, always use `https://`.

---

## Building locally

### Requirements

- JDK 17 or later
- Android SDK with platform 36 and build-tools 36.0.0
- Internet access on the first build, to download Gradle and the dependencies

The project uses AGP 9.0.0, Kotlin 2.2.10 and Gradle 9.3.1. The wrapper downloads Gradle automatically, so you don't need to install it.

### Commands

```bash
git clone https://github.com/<user>/<repo>.git
cd <repo>

# Debug APK
./gradlew assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk

# Release APK (unsigned if you don't define the signing variables)
./gradlew assembleRelease

# Bundle (AAB)
./gradlew bundleRelease
```

If Gradle can't find the SDK, create `local.properties` in the root:

```properties
sdk.dir=/path/to/Android/Sdk
```

### Version and signing from the command line

```bash
export KEYSTORE_FILE=/path/to/release.jks
export KEYSTORE_PASSWORD='...'
export KEY_ALIAS='twelve'
export KEY_PASSWORD='...'          # if omitted or empty, KEYSTORE_PASSWORD is used

./gradlew assembleRelease -PversionName=0.1.5 -PversionCode=5
```

Without `KEYSTORE_FILE` the build behaves like upstream: debug is signed with the standard Android debug key and release is left unsigned.

---

## Continuous integration

There are two workflows in `.github/workflows/`:

### `debug.yaml` — debug build

- **When:** on every `push` to any branch other than `main` or `master` (documentation-only changes are ignored), and manually with `workflow_dispatch`
- **What it does:**
  1. Sets up JDK 17, the Android SDK and Gradle (the one from the wrapper)
  2. Decodes the debug keystore from the secrets
  3. Runs `./gradlew assembleDebug` with `versionCode` set to the run number
  4. Produces `Twelve-debug-<version>-<build>.apk` and its `.sha256`
  5. Writes a run summary and sends the APK to Telegram, if the secrets are configured
  6. Deletes the keystore from the runner

### `release.yaml` — signed release

- **When:** when a pull request from the `develop` branch is **merged** into `main` or `master`
- **What it does:**
  1. Computes the version `0.1.<run number>` and the tag `v<version>`
  2. Decodes the production keystore
  3. Runs `lintRelease` (non-blocking) and builds `assembleRelease` and `bundleRelease`
  4. Produces `Twelve-<version>.apk` and `Twelve-<version>.aab` with their `.sha256` files
  5. Builds the changelog from the pull request's commits and issues
  6. Publishes a GitHub Release with the files and sends the APK to Telegram
  7. Deletes the keystore from the runner

Recommended workflow:

```text
feature/* ──► develop ──(merged PR)──► main
   │             │                        │
 debug.yaml   debug.yaml             release.yaml
```

> The workflows inherited from upstream (`build.yml`, `gerrit.yml`, `reuse/`) remain in the repository, but `gerrit.yml` only serves LineageOS's review flow and can be removed in a fork.

---

## Setting up secrets and keystores

Secrets are defined in **Settings → Secrets and variables → Actions** of the repository. They are per repository: they are not shared with other projects unless you use organization secrets.

| Secret | Required | Description |
| --- | --- | --- |
| `ANDROID_KEYSTORE_DEBUG_BASE64` | Yes (or `KEYSTORE_BASE64`) | Debug keystore, Base64-encoded |
| `ANDROID_KEYSTORE_RELEASE_BASE64` | Yes (or `KEYSTORE_BASE64`) | Release keystore, Base64-encoded |
| `KEYSTORE_BASE64` | Alternative | Single keystore used as a fallback for both debug and release |
| `KEYSTORE_PASSWORD` | Yes | Keystore password |
| `KEY_ALIAS` | Recommended | Key alias. If missing or wrong, the workflow tries to detect it |
| `KEY_PASSWORD` | Recommended | Key password. If empty, `KEYSTORE_PASSWORD` is used |
| `TELEGRAM_BOT_TOKEN` | No | Telegram bot token |
| `TELEGRAM_CHAT_ID_DEBUG` | No | Chat the APKs are sent to. Without both Telegram secrets, the upload is skipped |

### Generating the keystores

```bash
read -rs -p "Password: " STOREPASS; echo
ALIAS="twelve"

# Release
keytool -genkeypair -v \
  -keystore release.jks -storetype PKCS12 \
  -alias "$ALIAS" -keyalg RSA -keysize 4096 -validity 10000 \
  -storepass "$STOREPASS" -keypass "$STOREPASS" \
  -dname "CN=Twelve, O=<organization>, C=ES"

# Debug
keytool -genkeypair -v \
  -keystore debug.jks -storetype PKCS12 \
  -alias "$ALIAS" -keyalg RSA -keysize 4096 -validity 10000 \
  -storepass "$STOREPASS" -keypass "$STOREPASS" \
  -dname "CN=Twelve Debug, O=<organization>, C=ES"
```

### Uploading them with the GitHub CLI

```bash
gh auth login
REPO="<user>/<repo>"

base64 -w0 release.jks | gh secret set ANDROID_KEYSTORE_RELEASE_BASE64 --repo "$REPO"
base64 -w0 debug.jks   | gh secret set ANDROID_KEYSTORE_DEBUG_BASE64   --repo "$REPO"

printf '%s' "$STOREPASS" | gh secret set KEYSTORE_PASSWORD --repo "$REPO"
printf '%s' "$STOREPASS" | gh secret set KEY_PASSWORD      --repo "$REPO"
printf '%s' "$ALIAS"     | gh secret set KEY_ALIAS         --repo "$REPO"

gh secret list --repo "$REPO"
```

> **Important:** keep `release.jks` and its password outside the repository, for example in a password manager. If you lose the release key, you won't be able to publish updates signed with it. Never commit a `.jks` file to the repository.

---

## Changes from upstream

This fork adds what is needed to build and publish outside the LineageOS tree:

- **`app/build.gradle.kts`**
  - `versionCode` and `versionName` can be overridden with `-PversionCode` and `-PversionName` (defaults `1` and `"1.0"`)
  - A new signing config called `ci`, which reads `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD` from the environment and is only enabled when `KEYSTORE_FILE` exists
- **`.github/workflows/debug.yaml` and `release.yaml`**: build, signing, publishing and notification
- **This `README.md`**

The application code is not modified, which makes it easier to merge upstream changes.

To pull in upstream updates:

```bash
git remote add upstream https://github.com/LineageOS/android_packages_apps_Twelve.git
git fetch upstream
git merge upstream/lineage-24.0
```

---

## Project structure

```text
.
├── .github/workflows/          # debug.yaml, release.yaml (+ inherited from upstream)
├── app/
│   ├── build.gradle.kts        # app module (signing and versioning)
│   ├── Android.bp              # only for building inside a ROM tree (Soong)
│   ├── libs/                   # local dependencies for Soong
│   ├── schemas/                # Room database schemas
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/org/lineageos/twelve/
│       │   ├── datasources/    # local, Subsonic, Jellyfin, Ampache
│       │   ├── fragments/      # UI screens
│       │   ├── services/       # playback service (Media3)
│       │   ├── viewmodels/
│       │   ├── models/
│       │   ├── database/       # Room
│       │   └── ui/             # views, widget and visualizer
│       └── res/                # resources and translations
├── gradle/                     # wrapper and version catalog
├── build.gradle.kts            # root file (only declares plugins)
├── settings.gradle.kts
└── LICENSES/
```

---

## Troubleshooting

| Symptom | Cause and fix |
| --- | --- |
| `…/src/main/AndroidManifest.xml which doesn't exist` | The **root** `build.gradle.kts` contains the module's content. The root file must only declare the plugins with `apply false`; the module's file goes in `app/` |
| `Script compilation error … Expecting '}'` | A closing brace is missing at the end of the root `build.gradle.kts` |
| `Failed to read key … Given final block not properly padded` | The key password is wrong. Check that `KEY_PASSWORD` and `KEYSTORE_PASSWORD` are uploaded and match the keystore |
| `Missing ANDROID_KEYSTORE_…_BASE64` | The keystore secret (or `KEYSTORE_BASE64`) is missing |
| `gh: To get started with GitHub CLI, please run: gh auth login` | `gh` isn't authenticated. Run `gh auth login` |
| `package="…" found in source AndroidManifest.xml` | Just a warning: AGP 9 ignores the attribute. You can remove it from the manifest |
| The app doesn't update over the LineageOS one | Different signature. Uninstall the original before installing |
| The visualizer shows nothing | Grant `RECORD_AUDIO` and check that the ROM doesn't disable audio effects |

---

## License and credits

- Code: [Apache License 2.0](LICENSES/Apache-2.0.txt), copyright The LineageOS Project
- Resources under [CC-BY-SA 4.0](LICENSES/CC-BY-SA-4.0.txt) according to `REUSE.toml` and the SPDX headers in each file
- Original project: [LineageOS/android_packages_apps_Twelve](https://github.com/LineageOS/android_packages_apps_Twelve)

Main libraries: AndroidX Media3 (ExoPlayer), Room, Coil, OkHttp, Material Components, Nier-Visualizer and kotlinx.serialization.
