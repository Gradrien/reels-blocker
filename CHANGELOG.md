# Changelog

All notable changes to this project are documented here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versions follow [Semantic Versioning](https://semver.org/).

## [Unreleased]

## [1.0.0] - 2026-11-04

First public release.

### Features
- Blocks the Reels tab (back to Home) and fullscreen Reels opened from the feed, Explore or a profile.
- Keeps Reels sent in DMs watchable; optionally blocks swiping on from them.
- First-run guided setup that checks each step (accessibility, battery, notifications).
- Hold-to-pause for 5 minutes, with one-tap resume.
- Quick Settings tile showing protection status.
- Watchdog: a periodic check that notifies once if Android stopped the service.
- Avoided-Reels counter, recent activity log, diagnostic mode.
- Instagram-like design with its own Ember-to-Raspberry gradient, light and true-black dark themes. See DESIGN.md.
- English UI by default; French follows the phone's language.

### Project
- English documentation: README, contributing guide, code of conduct, security policy.
- Issue and pull request templates, including a “Detection broke” template built around the diagnostic report.
- GitHub Actions: build on every pull request, signed APK and checksum on `v*` tags.
- Release signing from environment variables or an untracked `keystore.properties`.

### Note
- If you sideloaded an earlier build (signed with a debug key), uninstall it once before installing this release.

[Unreleased]: https://github.com/Gradrien/reels-blocker/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/Gradrien/reels-blocker/releases/tag/v1.0.0
