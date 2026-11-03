# Changelog

All notable changes to this project are documented here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versions follow [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added
- English UI, now the default; French translation follows the phone's language.
- English documentation: README, contributing guide, code of conduct, security policy.
- Issue and pull request templates, including a “Detection broke” template built around the diagnostic report.
- GitHub Actions: build on every pull request, signed release on `v*` tags.
- Release signing from environment variables or an untracked `keystore.properties`.

### Changed
- Code comments and the diagnostic report are in English.
- `build.sh` runs with bash and reads `JAVA_HOME` / `ANDROID_HOME` from the environment.

### Note
- The first release signed with the new release key cannot update an install signed with the debug key: uninstall the old app once, then install the new one.

## [1.5] - 2026-11-02

First public version.

- Blocks the Reels tab (back to Home) and fullscreen Reels opened from the feed, Explore or a profile.
- Keeps Reels sent in DMs watchable; optionally blocks swiping on from them.
- Avoided-Reels counter, recent activity log, diagnostic mode.
- Light and dark themes.
