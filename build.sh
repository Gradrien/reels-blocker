#!/bin/zsh
# Compile l'APK ; avec --install, l'installe sur le téléphone branché en USB (débogage USB activé).
set -e
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
cd "$(dirname "$0")"
./gradlew --no-daemon -q assembleRelease
APK=app/build/outputs/apk/release/app-release.apk
echo "APK : $PWD/$APK"
[[ "$1" == "--install" ]] && adb install -r "$APK"
