# Contributing to Reels Blocker

Thanks for helping. The project is small on purpose, so a few ground rules keep it that way.

## Ground rules

- **No new permissions.** In particular, never `INTERNET`. If a change seems to need one, open an issue first.
- **No dependencies.** The app uses the Android framework only (no AndroidX, no Compose, no libraries). This keeps the APK tiny and the code easy to audit.
- **Fail safe.** When the service is unsure what is on screen, it must do nothing. Never add an action that could close Instagram or loop.
- **Friends' Reels stay watchable.** Reels opened from a DM are allowed by design.

## Ways to help

### Fix detection after an Instagram update

This is the most common and most useful contribution.

1. Install the app, turn on **Advanced → Diagnostic mode**.
2. Reproduce the case in Instagram (open the Reels tab, a Reel from the feed, a Reel from a DM…).
3. Come back to Reels Blocker and read the report: each screen type (`REELS_TAB`, `REELS_VIEWER`, `DM`, `MAIN`, `OTHER`, `NO_ID`) shows the view ids that were on top and visible.
4. Update the id lists at the bottom of [`ReelsBlockerService.kt`](app/src/main/java/fr/adrien/reelsblocker/ReelsBlockerService.kt) (`REELS_VIEWER_IDS`, `DM_IDS`, `TAB_BAR_IDS`, `HOME_TAB_ID`).
5. In the PR, mention the Instagram version you tested with.

### Translate the app

All user-facing text lives in `app/src/main/res/values/strings.xml` (English, the default). To add a language, copy it to `values-<code>/strings.xml` (for example `values-es`), translate every string except the ones marked `translatable="false"`, and keep the `%1$s` / `%1$d` placeholders. See `values-fr/strings.xml` for an example.

### Report a bug or suggest a feature

Use the [issue templates](https://github.com/Gradrien/reels-blocker/issues/new/choose).

## Development setup

You need JDK 17 and the Android SDK (platform 35). Android Studio works but is not required.

```sh
./gradlew assembleDebug     # debug APK
./build.sh --install        # release APK, installed on a USB-connected phone
adb logcat -s ReelsBlocker  # what the service decides, live
```

### Project layout

| File | Role |
|---|---|
| `ReelsBlockerService.kt` | The accessibility service: classifies Instagram's screen and acts |
| `MainActivity.kt` | The single app screen, built in code |
| `ToggleView.kt` | Custom switch component |
| `Prefs.kt` | Settings, counter, log and diagnostic storage (SharedPreferences) |
| `res/values*/` | Colors, theme and strings (light, dark, translations) |
| `DESIGN.md` / `PRODUCT.md` | Design system and product intent; read these before changing the UI |

### Testing a change

There are no automated UI tests yet, since detection depends on the live Instagram app. Before opening a PR, check on a real phone:

- Reels tab → you land on Home.
- A Reel from the feed or Explore → it closes.
- A Reel from a DM → it stays open; swiping to the next one closes it (if the setting is on).
- Light and dark themes, and system font size at 130%, if you touched the UI.

## Pull requests

- Keep each PR focused on one thing.
- Write commit messages and code comments in English.
- CI builds every PR; it must be green.
- Fill in the PR template, especially how you tested.

## Release signing

Releases are built by GitHub Actions when a `v*` tag is pushed (see `.github/workflows/release.yml`) and signed with the key stored in the repository secrets `RB_KEYSTORE_BASE64`, `RB_KEYSTORE_PASSWORD`, `RB_KEY_ALIAS` and `RB_KEY_PASSWORD`.

For a signed build on your machine, create an untracked `keystore.properties` at the repository root:

```properties
storeFile=/absolute/path/to/release.jks
storePassword=…
keyAlias=…
keyPassword=…
```

Without it, release builds are signed with the debug key.

## Code of conduct

This project follows the [Code of Conduct](CODE_OF_CONDUCT.md). By participating you agree to uphold it.
