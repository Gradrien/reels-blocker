---
name: Reels Blocker
description: Instagram's visual language (white/black, hairlines, compact rows, bold titles) with an identity of its own: an Ember-to-Raspberry gradient reserved for the brand and the counter.
colors:
  bg: "#FFFFFF"
  surface: "#F4F4F5"
  surface-high: "#EBEBED"
  line: "#E4E4E7"
  ink: "#0B0B0C"
  ink-soft: "#6B6B70"
  ink-mute: "#8E8E93"
  press: "#14000000"
  accent: "#2563EB"
  on-accent: "#FFFFFF"
  brand-start: "#F2552C"
  brand-end: "#D61F69"
  ok: "#16A34A"
  warn: "#EA580C"
  warn-bg: "#FFF4ED"
  switch-off: "#D4D4D8"
  switch-thumb: "#FFFFFF"
  bg-night: "#000000"
  surface-night: "#121212"
  surface-high-night: "#1F1F1F"
  line-night: "#262626"
  ink-night: "#F5F5F5"
  ink-soft-night: "#A3A3A8"
  ink-mute-night: "#6B6B70"
  press-night: "#1FFFFFFF"
  ok-night: "#4ADE80"
  warn-night: "#FB923C"
  warn-bg-night: "#1F140C"
  switch-off-night: "#3F3F46"
typography:
  display: { fontFamily: "sans-serif", fontSize: "72sp", fontWeight: 700, letterSpacing: "-0.03em", fontFeature: "tnum" }
  title-bar: { fontFamily: "sans-serif", fontSize: "22sp", fontWeight: 700 }
  card-title: { fontFamily: "sans-serif", fontSize: "17sp", fontWeight: 700 }
  section: { fontFamily: "sans-serif", fontSize: "16sp", fontWeight: 700 }
  row-title: { fontFamily: "sans-serif", fontSize: "15sp", fontWeight: 500 }
  stat-label: { fontFamily: "sans-serif", fontSize: "15sp", fontWeight: 500 }
  body: { fontFamily: "sans-serif", fontSize: "14sp", fontWeight: 400 }
  button: { fontFamily: "sans-serif", fontSize: "14sp", fontWeight: 700 }
  caption: { fontFamily: "sans-serif", fontSize: "13sp", fontWeight: 400 }
  footer: { fontFamily: "sans-serif", fontSize: "12sp", fontWeight: 400 }
  mono: { fontFamily: "monospace", fontSize: "12sp", fontWeight: 400 }
rounded:
  button: "8dp"
  inset: "12dp"
  card: "16dp"
  full: "9999dp"
spacing:
  gutter: "16dp"
  row-min: "64dp"
  section-top: "20dp"
---

# Design System: Reels Blocker

## Overview

**North star: "Feels like Instagram's settings, but it's ours."**

People open Reels Blocker straight from Instagram, so the app borrows Instagram's *language* to feel familiar: white (or true black) surfaces, full-bleed hairlines between sections, compact rows, bold section titles, a bright blue for actions, and black-and-white switches. What it does **not** borrow is Instagram's *identity*: no Instagram Sans, no logo or icons, no purple-pink-orange gradient. Our own signature is a two-stop **Ember → Raspberry** gradient, used in exactly two places: the brand tile and the avoided-Reels counter.

The screen is a single scrolling column:

1. Top bar: brand tile + "Reels Blocker".
2. Hero: status chip, the gradient counter, its label and the "+N since your last visit" line.
3. Setup card (only when the service is off).
4. Blocking settings, recent activity, advanced, footer; separated by full-bleed hairlines.

**Key characteristics**
- Neutral canvas; color carries meaning, never decoration.
- One gradient, two uses.
- Flat: no shadows, no elevation.
- System sans-serif: zero bytes in the APK, native on every phone.
- Full light and dark parity via `values/colors.xml` and `values-night/colors.xml`.

## Colors

### Neutrals
- **bg**: window background. White in light, true black in dark (OLED-friendly, like Instagram's dark mode).
- **surface**: the debug inset. **surface-high**: secondary button fill.
- **line**: hairlines and the status chip outline.
- **ink**: primary text, the on switch track, step number outline.
- **ink-soft**: secondary text (subtitles, timestamps, footer). 5.3:1 on white.
- **ink-mute**: paused status dot only.
- **press**: ripple color (8% black / 12% white).

### Action
- **accent** (Signal Blue): filled primary buttons only ("Open"). White label at 5.2:1. Never used for text links or decoration.

### Brand
- **brand-start → brand-end** (Ember `#F2552C` → Raspberry `#D61F69`): the brand tile, the launcher icon and the counter. Both ends pass 3:1 for large text on white and on black.

### State
- **ok** green: status dot when protection runs. **warn** orange: status dot when the service is off. **warn-bg**: the setup card.

### Rules
- **The Two-Places Gradient Rule.** The gradient appears on the brand tile/icon and the counter, nowhere else. No gradient buttons, borders, or backgrounds.
- **The State-Only Color Rule.** Green and orange appear only as status signals.
- **The Blue-Means-Do Rule.** Accent blue is reserved for the primary action on screen.

## Typography

System `sans-serif` (Roboto on most devices) in three weights: 400, 500, 700. Hierarchy comes from weight and size, never from color or case.

| Role | Size / weight | Used for |
|---|---|---|
| Display | 72sp / 700, -0.03em, tabular | The counter (gradient fill) |
| Title bar | 22sp / 700 | App name |
| Card title | 17sp / 700 | "Turn on protection" |
| Section | 16sp / 700 | "Blocking", "Recent activity", "Advanced" |
| Row title / stat label | 15sp / 500 | Setting and link titles, "Reels avoided" |
| Body | 14sp / 400 | Log lines, setup explanation |
| Button | 14sp / 700 | Button labels |
| Caption | 13sp / 400 | Row subtitles, status chip, counter delta |
| Footer | 12sp / 400 | Privacy line and version |

- **Sentence case everywhere.** No all-caps labels.
- **Tabular figures** on anything that changes (counter, timestamps).

## Layout

- Full-bleed column; every row carries its own 16dp horizontal gutter so ripples and hairlines reach the screen edges.
- Rows: 64dp minimum (setting, link, step), 56dp (Advanced header).
- Section title: 20dp above, 8dp below.
- Sections are separated by a 1px full-bleed `line`.
- Edge-to-edge under transparent system bars; insets are added to the column, a `bg` scrim covers the status bar.
- Touch targets are at least 48dp (switch and buttons are wrapped in padding).
- While the service is off, settings and log fade to 45% and are disabled; the setup card leads.

## Components

### Brand tile
The launcher icon in miniature: a rounded square (radius 26% of the side) filled with the brand gradient, carrying a white "play" triangle struck through by a round-capped bar. The gap around the bar is drawn with the same gradient so it reads as a cut-out. 30dp in the top bar.

### Status chip
Pill, 32dp tall, 1dp `line` outline, 8dp state dot + 13sp/500 ink text ("Protection on", "Paused", "Protection off").

### Counter (signature)
72sp bold tabular number filled with the brand gradient, measured on the glyphs so even "3" shows both ends. Counts up from the last-seen value over 700ms (decelerate 2). Below: "Reels avoided" (15sp/500 ink) and, when positive, "+N since your last visit" (13sp ink-soft).

### Setup card
`warn-bg`, 16dp corners, 16dp padding. Bold title, subtitle, then numbered steps: 26dp outlined number circle, title + subtitle, primary "Open" button.

### Buttons
8dp corners, 36dp visual height inside a 48dp frame, 16dp horizontal padding, 14sp bold.
- **Primary:** `accent` fill, `on-accent` label.
- **Secondary:** `surface-high` fill, `ink` label (used for "Copy report").

### Switch
44×26dp track, 22dp thumb, 160ms decelerate. Off: `switch-off` track, white thumb. On: `ink` track, `bg` thumb (black on white in light, white on black in dark). The whole row toggles it; TalkBack announces a standard Switch.

### Rows
- **Setting row:** title + subtitle, switch on the right.
- **Link row:** title + subtitle, chevron rotated -90°.
- **Log row:** time column (ink-soft, tabular, width of the widest stamp + 16dp) then the message in ink.

### Hold button (pause)
Secondary-styled (surface-high, 8dp corners, 40dp tall). Pressing starts a fill in 15% ink that sweeps left to right over 1.5s; releasing early eases it back out; completing gives a haptic tick and pauses. TalkBack and switch access activate it with a normal click. When paused, it is replaced by a primary "Resume now" button, and the status chip reads "Paused until 14:37".

### Onboarding
One full-screen page per step: step counter and "Skip" on top, 26sp bold title, 16sp body, an optional `surface` note for edge cases (restricted settings), and full-width 52dp buttons pinned to the bottom (primary accent, secondary text). A step that Android reports as done shows a green "✓ Done" and its primary button becomes "Next".

### Advanced section
56dp header with a chevron that rotates 180° over 200ms; the body animates in with `LayoutTransition`.

## Do's and Don'ts

**Do**
- Add new colors as tokens in both `values/colors.xml` and `values-night/colors.xml`, and document them here.
- Keep one display number per screen.
- Keep touch targets ≥ 48dp and check light, dark and 130% font size.

**Don't**
- Use Instagram's logo, glyphs, Instagram Sans, or its multi-stop gradient. The app must never look like it is made by Meta.
- Use the brand gradient anywhere but the brand tile/icon and the counter.
- Add shadows, elevation, or a second typeface.
- Use blue for anything but the primary action.
