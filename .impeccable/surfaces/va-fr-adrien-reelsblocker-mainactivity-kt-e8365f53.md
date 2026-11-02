---
version: 1
slug: "va-fr-adrien-reelsblocker-mainactivity-kt-e8365f53"
primary_target: "app/src/main/java/fr/adrien/reelsblocker/MainActivity.kt"
related_targets: []
---

# Surface: écran principal (MainActivity)

Scope: unique écran de l'app Android, mode Operate. Tâche : vérifier d'un coup d'œil que la protection tourne et combien de Reels ont été évités ; activer le service la première fois ; ajuster deux réglages ; diagnostiquer si Instagram change.

## Direction contract
THESIS: un écran de contrôle calme qui parle la langue d'Eido (même auteur) : l'état et le compteur d'abord, le reste en retrait. Refuse le cliché « bien-être numérique » violet à anneau de progression.
OWN-WORLD: palette Eido épinglée par l'utilisateur : crème #FAF9F2, surfaces #F1F0E7, encre brun #3B352B, brun doux #70614B/#8C795E, sombre #121211/#1B1A17/#E6E2DB ; Outfit (Regular/Medium/SemiBold) ; cartes rayon 28dp, boutons pilule encre, interrupteurs encre façon M3 ; vert/orange Eido seulement pour l'état.
STORY: on ouvre, on voit « Protection active » + le grand compteur ; si coupé, la carte d'activation prend le devant et le reste s'efface ; journal en phrases courtes ; Avancé replié.
FIRST VIEWPORT: barre « Reels Blocker » avec la marque (play barré) ; carte héros : pastille d'état, compteur 72sp SemiBold tabulaire, « Reels évités », delta vert « +N depuis ta dernière visite » ; dessous section Blocage (2 interrupteurs) ; Activité récente commence au pli.
FORM: direction épinglée par l'utilisateur (« la même DA que Eido ») ; seed: 791736bf21a24368 (tirages présentés puis abandonnés au profit de la direction épinglée). Signature : le compteur monte depuis la valeur vue à la dernière visite (animation 700 ms, respecte l'échelle d'animation système).
FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance
