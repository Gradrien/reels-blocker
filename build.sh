#!/usr/bin/env bash
# Builds the release APK. With --install, installs it on the phone connected over USB (USB debugging on).
#
# Needs JDK 17 and the Android SDK. JAVA_HOME and ANDROID_HOME are read from the environment;
# on macOS with Homebrew (openjdk@17, android-commandlinetools) they are found automatically.
set -euo pipefail
cd "$(dirname "$0")"

if [[ -z "${JAVA_HOME:-}" && -d /opt/homebrew/opt/openjdk@17 ]]; then
  export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
fi
if [[ -z "${ANDROID_HOME:-}" && -d /opt/homebrew/share/android-commandlinetools ]]; then
  export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
fi

./gradlew --no-daemon -q assembleRelease
APK=app/build/outputs/apk/release/app-release.apk
echo "APK: $PWD/$APK"
if [[ "${1:-}" == "--install" ]]; then adb install -r "$APK"; fi
