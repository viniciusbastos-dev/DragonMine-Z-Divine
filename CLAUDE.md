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

- **`reference/dragonmine-z-super-addon`** — despite the folder name, this is **not a standalone addon**:
  it's a **fork of the base mod** ("DragonMineZ: Super Expansion" 2.0-beta, modId `dragonminez`, by facub8),
  built on an older base API (e.g. `setKaiokenStackable` instead of 2.1.3's `formStackable`). Its only
  mixins target vanilla classes, same as the base mod. Useful as a **design reference** for divine content —
  translate its ideas into external-addon mechanisms (events/packets against the real 2.1.3 jar), never copy
  its base-class edits:
  - `common/config/DefaultFormsFactory.createSaiyanForms` — its God group: SSG (`unlockOnSkillLevel` 1,
    ×3.5 str/skp/pwr, ×2.5 def, 0.20 ki drain) and SSB (level 3, ×5.0, 0.40 drain), `formType "god"`.
  - `common/config/SkillsConfig` — its `godform` skill ladder: level 1 cost `-1` ("desbloqueo por comandos
    o por NPC"), levels 2-5 = 20k-80k TP (drain reductions / SSB unlock).
  - Note the base mod **2.1.3 already supports god forms natively** (see `reference/dragonminez`):
    `godforms` is a default form skill in `SkillsConfig`, `TransformationsHelper.getSkillNameForType` maps
    `formType` containing `godform` → skill `godforms`, and ki sense / lock-on / instant transmission are
    already gated for divine forms. The base Ultimate ritual (`UltimateChallenge` +
    `NPCActionC2S("oldkai", 1)` → server validates + `setSkillLevel("ultimate", 1)`) is the canonical
    minigame-unlock flow our rituals mirror.
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
- **The addon ships its own form JSON.** `DivineFormsInstaller` (common setup) writes the bundled groups from
  `resources/data/dmzdivine/default_configs/` into `config/dragonminez/races/<race>/forms/`, adds the
  `godforms` skill prices to the race's `character.json`, and hands each file to
  `ConfigManager.reloadSpecificConfig` so it applies on the same launch. Add new divine groups by dropping a
  JSON under `default_configs/` and listing it in `SHIPPED_GROUPS` — never by mutating `ConfigManager`'s maps
  directly, since clients read `SERVER_SYNCED_FORMS` (files the server walks off disk) while connected.
  Existing files and existing prices are never overwritten; players still own whatever they have edited.
