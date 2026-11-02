# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users
- Primary: the author, on a Xiaomi phone (HyperOS), who wants to stop doomscrolling Instagram Reels without giving up Instagram itself.
- Secondary: people who find the public GitHub repo and sideload the APK. They meet the app cold, so the screen has to walk them through activation (Accessibility service, Xiaomi autostart/battery).

## Product Purpose
Blocks Instagram Reels at the point of entry: the Reels tab redirects to Home, a fullscreen Reel opened from the feed/Explore is closed. Reels a friend sent in DMs stay watchable; swiping on to further Reels from there is blocked (toggle). Success = the user keeps using Instagram for friends and DMs while the infinite Reels feed stops being reachable.

## Positioning
A tiny, open-source, no-network alternative to ad-supported blockers like NoScroll: zero dependencies, ~50 KB APK, no tracking, and it deliberately keeps Reels shared by friends.

## Operating Context
- Opened rarely: first-run setup, then occasional check that protection is running and how many Reels were avoided.
- Sideloaded APK; Play Protect warnings and Android "restricted settings" are part of install.
- Xiaomi/HyperOS kills background services unless autostart + "no battery restriction" are set; Accessibility can silently turn off after updates.
- Instagram view ids change with updates; an in-app debug dump is the repair tool.

## Capabilities and Constraints
- Kotlin, framework Views only, no AndroidX/Compose, no dependencies, no INTERNET permission. minSdk 28, targetSdk 35.
- Screen priorities (confirmed): 1) protection status + "Reels évités" counter, 2) setup steps when not active, 3) settings (blocking on/off, block swipe from DM), 4) recent activity log, 5) debug tucked into an "Avancé" section.
- UI language: French.

## Brand Commitments
- Name: "Reels Blocker" (confirmed by user).
- Current launcher icon is a placeholder system drawable; user explicitly rejected it — needs a real icon.

## Evidence on Hand
- Blocked-count and decision log are real, local-only data. No users, testimonials, or stats to cite beyond that.

## Product Principles
1. Never trap the user: no navigation that closes Instagram or loops; fail safe (do nothing) when unsure.
2. Friends' Reels are not the enemy; the infinite feed is.
3. Nothing leaves the phone.
4. Setup must be self-explanatory for someone installing from GitHub.
