---
name: Reels Blocker
description: A calm control screen in Eido's cream-and-brown language; status and counter first, everything else set back.
colors:
  bg: "#FAF9F2"
  surface: "#F1F0E7"
  surface-high: "#EAE6DC"
  line: "#E1DDCF"
  ink: "#3B352B"
  ink-soft: "#70614B"
  ink-mute: "#8C795E"
  on-ink: "#FAF9F2"
  ok: "#2A7948"
  warn: "#B45A1A"
  warn-bg: "#FFF4EB"
  bg-night: "#121211"
  surface-night: "#1B1A17"
  surface-high-night: "#24231F"
  line-night: "#2D2B27"
  ink-night: "#E6E2DB"
  ink-soft-night: "#BDB6AD"
  ink-mute-night: "#928C84"
  on-ink-night: "#121211"
  ok-night: "#84D3A2"
  warn-night: "#FDAD74"
  warn-bg-night: "#2A2018"
typography:
  display:
    fontFamily: "Outfit"
    fontSize: "72sp"
    fontWeight: 600
    letterSpacing: "-0.03em"
    fontFeature: "tnum"
  headline:
    fontFamily: "Outfit"
    fontSize: "22sp"
    fontWeight: 500
  title:
    fontFamily: "Outfit"
    fontSize: "18sp"
    fontWeight: 500
  title-row:
    fontFamily: "Outfit"
    fontSize: "16sp"
    fontWeight: 500
  body-lead:
    fontFamily: "Outfit"
    fontSize: "16sp"
    fontWeight: 400
  body:
    fontFamily: "Outfit"
    fontSize: "14sp"
    fontWeight: 400
  label:
    fontFamily: "Outfit"
    fontSize: "14sp"
    fontWeight: 500
  caption:
    fontFamily: "Outfit"
    fontSize: "13sp"
    fontWeight: 400
  mono:
    fontFamily: "monospace"
    fontSize: "12sp"
    fontWeight: 400
rounded:
  inset: "16dp"
  pill: "20dp"
  card: "28dp"
  full: "9999dp"
spacing:
  xs: "4dp"
  sm: "8dp"
  md: "12dp"
  gutter: "16dp"
  card-x: "24dp"
  section: "32dp"
components:
  card:
    backgroundColor: "{colors.surface}"
    rounded: "{rounded.card}"
    padding: "20dp 24dp 24dp"
  card-setup:
    backgroundColor: "{colors.warn-bg}"
    rounded: "{rounded.card}"
    padding: "20dp 24dp 12dp"
  button-pill:
    backgroundColor: "{colors.ink}"
    textColor: "{colors.on-ink}"
    typography: "{typography.label}"
    rounded: "{rounded.pill}"
    padding: "0 18dp"
    height: "40dp"
  toggle-on:
    backgroundColor: "{colors.ink}"
    textColor: "{colors.bg}"
    rounded: "{rounded.full}"
    width: "52dp"
    height: "32dp"
  toggle-off:
    backgroundColor: "{colors.surface-high}"
    textColor: "{colors.ink-soft}"
    rounded: "{rounded.full}"
    width: "52dp"
    height: "32dp"
  setting-row:
    typography: "{typography.title-row}"
    padding: "12dp 20dp 12dp 24dp"
    height: "76dp"
  step-badge:
    backgroundColor: "{colors.ink}"
    textColor: "{colors.on-ink}"
    rounded: "{rounded.full}"
    size: "28dp"
  debug-inset:
    backgroundColor: "{colors.surface-high}"
    textColor: "{colors.ink-soft}"
    typography: "{typography.mono}"
    rounded: "{rounded.inset}"
    padding: "14dp 16dp"
---

# Design System: Reels Blocker

## Overview

**Creative North Star: "The Quiet Ledger"**

A single control screen that reads like a well-kept notebook page in Eido's language: warm cream paper, brown ink, one big number. The palette, the Outfit typeface and the 28dp card silhouette are inherited from the author's other app, Eido (palette source: `Eido/packages/ui-core/components/gluestack-ui-provider/config.ts`, light and dark modes); each Android color resource carries a comment naming the Eido scale step it came from (e.g. `bg` = background-900, `ink` = primary-0). New surfaces should keep that lineage rather than drift toward Material defaults.

The screen is flat and tonal. Content sits on cream, grouped into soft-cornered surface cards; hierarchy comes from size and ink weight, not from color or depth. Color carries meaning only for state: green when protection runs, orange when it is cut, with the setup card tinting warm when action is needed. Everything else is a brown-on-cream ramp. The voice is French, sentence case, short.

Density is relaxed: generous 24dp card insets, 64-76dp row heights, 32dp breathing room above section labels. The one moment of motion that carries meaning is the counter climbing from the last-seen value.

**Key Characteristics:**
- Eido lineage: cream paper, brown ink, Outfit, 28dp cards.
- Flat tonal layering; no shadows anywhere.
- One display number per screen, tabular figures.
- Green/orange reserved for state.
- Full light and dark parity through `values` / `values-night` resources.

## Colors

A warm, low-chroma brown-on-cream ramp with two state hues borrowed from Eido.

### Primary
- **Walnut Ink** (`ink`): the primary text color, the filled pill button, the on-toggle track, the numbered step badge and the brand mark. In dark mode it inverts to **Bone** (`ink-night`), so filled controls become light-on-dark.

### Neutral
- **Eido Cream** (`bg`): window background, status-bar scrim, and the toggle thumb when on. Also the launcher icon background.
- **Linen** (`surface`): every grouping card.
- **Oat** (`surface-high`): the off-toggle track and the debug inset; one step above surface.
- **Seam** (`line`): 1px dividers inset 24dp, and the ripple color for rows.
- **Umber** (`ink-soft`): secondary text, section labels, timestamps, toggle outline and off-thumb, chevrons.
- **Faded Umber** (`ink-mute`): the paused-state status dot only.
- **On Ink** (`on-ink`): text on filled ink controls.

### Tertiary (state)
- **Eido Green** (`ok`): the status dot when protection is active. Nowhere else.
- **Ember** (`warn`): the status dot when protection is cut.
- **Peach Wash** (`warn-bg`): background of the setup card that leads the screen when the service is off.

### Named Rules
**The State-Only Color Rule.** Green and orange appear only as status signals (an 8dp dot, the setup card wash). Never as decoration, buttons, or links.

**The Eido Provenance Rule.** Every color is a named step of Eido's gluestack palette; add new colors by taking the next Eido step, with a comment naming it, never by inventing a hex.

## Typography

**Display Font:** Outfit SemiBold (bundled `res/font/outfit_semibold.ttf`)
**Body Font:** Outfit Regular and Medium (bundled)
**Label/Mono Font:** system monospace, debug dump only

**Character:** One geometric sans in three weights; hierarchy comes from size jumps and Medium versus Regular, never from color or case.

### Hierarchy
- **Display** (600, 72sp, -0.03 tracking, tabular figures): the blocked-Reels counter. One per screen.
- **Headline** (500, 22sp): the app bar title beside the mark.
- **Title** (500, 18sp): card titles that demand action ("Active la protection").
- **Row title** (500, 16sp): setting, step and link row titles; the "Avancé" header.
- **Body lead** (400, 16sp): the counter caption.
- **Body** (400, 14sp): row subtitles, log lines, setup explanation. Log timestamps use tabular figures in an aligned column.
- **Label** (500, 14sp, sentence case, `ink-soft`): section labels above cards, status text, the counter delta, pill button text.
- **Caption** (400, 13sp, centered): the footer privacy line with version.

### Named Rules
**The Sentence Case Rule.** No uppercase, no letterspaced labels. Section labels are plain sentence-case 14sp Medium in Umber.

**The Tabular Number Rule.** Any changing number (counter, timestamps) uses `tnum` so values do not jitter as they animate or align.

## Layout

A single scrolling column with 16dp side gutters, edge-to-edge under transparent system bars; system insets are added to the column padding and a `bg`-colored scrim covers the status bar. Cards span the column. Inside cards, content is inset 24dp horizontally (20dp on the trailing side of rows that end in a control). Rhythm: 8dp between stacked cards at the top, 32dp above a section label and 10dp below it, 32dp before the Advanced card, 24dp before the footer. Rows hold a minimum of 64dp (header, collapsible), 72dp (step, link) or 76dp (setting with toggle). Every touch target is at least 48dp, wrapping smaller visual controls (40dp pills, 32dp toggle track) in padding. Text scales with system font size; the layout reflows rather than clipping (verified at 130%).

Priority is expressed by order and dimming: when the service is off, the setup card moves to the top and the settings and log blocks drop to 45% opacity and become disabled.

## Elevation & Depth

Flat. No shadows or elevation on any element. Depth is tonal only: cream background, Linen cards, Oat insets, each one step apart. Press feedback is a ripple in `line` on rows and a translucent white ripple on ink pills; theme `colorControlHighlight` is 8% black (light) or 12% white (dark).

### Named Rules
**The Paper-Flat Rule.** Surfaces separate by tone, never by shadow. If a new layer is needed, take the next Eido step.

## Shapes

Soft, generous corners. Cards round at 28dp; pills round at 20dp (fully round at 40dp height); insets at 16dp; dots, badges and toggle tracks are fully round. The brand mark is a rounded-join play triangle struck through by a round-capped bar, with a background-colored gap around the bar; round caps echo the blades of Eido's pinwheel. Dividers are 1px hairlines inset to the card's text edge. Icons are stroked vectors with round caps and joins (2px chevron).

## Components

### Buttons
- **Shape:** pill (20dp radius), 40dp visual height, 88dp minimum width, inside a 48dp touch frame.
- **Primary:** Walnut Ink fill, On Ink 14sp Medium label, 18dp horizontal padding.
- **Press:** ripple over the fill. There is no secondary button style; rows themselves are the secondary action.

### Cards / Containers
- **Corner Style:** 28dp.
- **Background:** Linen; Peach Wash for the setup card only.
- **Shadow Strategy:** none (see Elevation).
- **Border:** none; internal 1px Seam dividers between rows.
- **Internal Padding:** 24dp horizontal; rows carry their own vertical padding.

### Toggle (signature)
Hand-drawn Material 3 style switch. Track 52 by 32dp, fully round. Off: Oat track, 2dp Umber outline, 16dp Umber thumb. On: Walnut Ink track, 24dp Cream thumb. Thumb grows and slides over 160ms decelerate. Announced to TalkBack as a standard Switch. The whole row toggles it.

### List rows
- **Setting row:** title + subtitle left, toggle right, 76dp min.
- **Link row:** title + subtitle left, chevron rotated -90 right, 72dp min.
- **Step row:** 28dp ink circle with SemiBold number, title + subtitle, pill button.
- **Log row:** timestamp column (Umber, tabular, width of the widest stamp + 16dp) then Walnut sentence; 10dp vertical padding.

### Status line
8dp round dot in the state color followed by 14sp Medium Umber text ("Protection active", "En pause", "Protection coupée").

### Collapsible section
"Avancé" header row, 64dp, chevron rotates 180 degrees over 200ms; body animates in with LayoutTransition.

### Counter (signature)
Display-sized number that counts up from the last-seen value over 700ms, decelerate(2), honoring the system animator scale; a Label-styled "+N depuis ta dernière visite" appears beneath it only when N is positive.

## Do's and Don'ts

### Do:
- **Do** take every new color from Eido's palette and note the step it maps to, in both `values` and `values-night`.
- **Do** group content in Linen cards with 28dp corners and 24dp insets.
- **Do** keep one 72sp tabular number as the screen's focal point.
- **Do** keep touch targets at 48dp minimum even when the visual control is smaller.
- **Do** dim and disable (45%) content that cannot work yet, and lead with the card that fixes it.

### Don't:
- **Don't** add shadows or elevation; separate layers by tone.
- **Don't** use green or orange outside status signals.
- **Don't** use uppercase or tracked-out labels.
- **Don't** reach for the purple wellbeing look or progress rings; this is a calm control screen, not a wellness tracker.
- **Don't** introduce a second typeface for display; Outfit carries all roles.
