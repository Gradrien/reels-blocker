# Security Policy

Reels Blocker runs as an Android accessibility service, which is a powerful permission. Security reports are taken seriously.

## Supported versions

Only the latest release receives fixes.

## Reporting a vulnerability

Please **do not open a public issue**. Use GitHub's private reporting instead: **Security → Report a vulnerability** on this repository.

Include what an attacker could do, the steps to reproduce, and the app and Android versions. You should get a first answer within a week.

## Scope

In scope: anything that lets the app read more than Instagram's view ids, leak data off the device, act on apps other than Instagram, or be driven by another app.

Out of scope: Instagram changing its UI so that detection stops working. That is a regular bug; use the “Detection broke” issue template.
