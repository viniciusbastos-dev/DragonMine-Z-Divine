# CLAUDE.md

Guidance for Claude Code when working in this repository.

## What this is

**DMZ Divine** is a Forge 1.20.1 addon for **DragonMineZ** (mod id `dragonminez`). It adds Saiyan/Majin
divine-tier transformations (Super Saiyan God, Super Saiyan Blue, Super Saiyan Blue Evolved, Majin Divine,
Majin Purification) plus, eventually, a custom unlock ritual for the God form (minigame-gated, similar in
spirit to the base mod's Ultimate ritual with the Elder Kai).

This is **not a fork**. DragonMineZ stays an external dependency (`libs/dragonminez-2.1.3.jar`, deobfuscated
via `fg.deobf`). We only add what a plain JSON addon config cannot express: per-form particle effects,
alignment-linked visuals, custom master NPCs/rituals, custom minigames, and anything requiring a Mixin or a
`DMZEvent` listener.

## Reference repositories (read, do not build)

Two repos are checked out under `reference/` purely for study. Neither is part of this project's build —
don't add them as Gradle subprojects, don't copy files wholesale.

- **`reference/dragonminez`** — the DragonMineZ mod source. This is the ground truth for:
  - `common/config/FormConfig.java` — every field a form JSON can set (colors, aura layers, multipliers,
    mastery curve, mob effects). If a field isn't here, it doesn't exist for JSON configs — that's exactly
    why an addon is needed for anything beyond it (particles, dynamic colors, etc.).
  - `common/events/DMZEvent.java` — the full Forge event bus surface addons can hook: `FormChangeEvent`,
    `StackFormChangeEvent`, `FusionEvent`, `StatChangeEvent`, `TPGainEvent`, `DamageModifyEvent`,
    `CritChanceEvent`, resource regen events, quest lifecycle events, `PlayerDataSave/LoadEvent`.
  - `AI/Context.md` — detailed architecture notes (config versioning, quest/wish loading, storage
    backends). Read before non-trivial work in the base mod's domain.
  - `common/quest/QuestAvailabilityChecker.java` / `common/quest/rewards/TransformationReward.java` — how
    quest-gated form unlocks work (alignment/skill/race/class conditions, mastery-chain backfill on reward).
  - License: **GPL-3.0**. Treat any code copied verbatim from here as copyleft-encumbered.

- **`reference/dragonmine-z-super-addon`** — a real, shipped DMZ addon (adds Ultra Instinct / Ultra Ego).
  This is the pattern reference for **how to actually build a DMZ addon**:
  - Uses ~30 Mixins (not just events) to extend base-mod classes: `FormDataMixin` (new fields on
    `FormData`), `ConfigManagerMixin` / `DefaultFormsFactoryMixin` (registering new config), `MastersSkillsScreenMixin` (custom master offerings), `DMZHairLayerMixin` / `DMZSkinLayerMixin` (render hooks),
    `TransformationsHelperMixin`, `StackFormModeHandlerMixin`, `UpdateSkillC2SMixin`.
  - Extends `StatsData` capability data without touching the base mod, via `IStatsDataExtras` /
    `IResourcesExtras` / `IStatusExtras` interfaces + matching Mixins (duck-typing pattern for adding a new
    resource, e.g. their "Rage" for Ultra Ego).
  - Ships a **configurable minigame** (`dmzsuper-minigame.toml`: cursor speed, green-zone width, reaction
    windows, required successes, allowed failures) gating a skill at level 0 → level 1 for 0 TP. This is the
    direct precedent for our God-form ritual.
  - Adds custom master NPCs (`MasterBeerusEntity`, `MasterWhisEntity`) with GeckoLib models and a custom
    dimension (`data/dmzsuper/dimension/beerus_planet.json`) that the base mod's space pod destinations
    already reference (`dmzsuper:beerus_planet`) — proof addons can register destinations the base mod
    recognizes.
  - License: **GPL-3.0**. Same copyleft caveat as above — study the pattern, don't paste the code, unless
    this project is itself released under GPL-3.0.

## Build

- Minecraft 1.20.1, Forge **47.4.10**, Java **17**, official mappings.
- Required at dev/runtime (mirrors DragonMineZ's own mandatory deps): GeckoLib 4.8.3, TerraBlender
  3.0.1.10, Curios 5.14.1+1.20.1.
- DragonMineZ jar lives in `libs/dragonminez-2.1.3.jar`, referenced as
  `implementation fg.deobf(files("libs/dragonminez-2.1.3.jar"))`. Bump this path/version if the base mod
  updates.
- `mods.toml` declares `[[dependencies.dmzdivine]] modId="dragonminez" ordering="AFTER"` — keep `AFTER` if
  we ever read `ConfigManager` at common setup, so the base mod's registries are populated first.
- The base mod throws at startup if Legendary Tooltips, Epic Fight, or Better Combat are installed — don't
  add these to the dev run's mod list.

## Working agreements

- Don't touch `reference/*` — treat as read-only source material.
- Prefer a Forge event listener (`DMZEvent`) over a Mixin when either would work; only reach for a Mixin
  when the base mod doesn't fire an event for the hook point needed (that's most of what the reference
  addon uses them for: new fields on existing classes, new capability data, GUI screen edits).
- The actual in-game JSON configs (races/forms/skills) for the divine transformations already exist and are
  maintained separately in the player's `config/dragonminez` folder — this addon project is for the code-only
  pieces JSON can't express, not a place to duplicate that JSON.
