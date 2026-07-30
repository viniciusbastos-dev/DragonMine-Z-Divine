# Divine form configs

**The addon installs these itself.** `DivineFormsInstaller` runs at common setup and, for each group
listed in `SHIPPED_GROUPS`:

1. copies `resources/data/dmzdivine/default_configs/races/<race>/forms/<group>.json` into
   `config/dragonminez/races/<race>/forms/` if that file does not exist yet;
2. adds a `godforms` entry to the race's `character.json` if the race defines no prices for it;
3. hands both paths to `ConfigManager.reloadSpecificConfig` so they apply on the same launch,
   instead of only after a restart.

Nothing already on disk is overwritten — edit the generated files freely, they are yours from then
on. Set `installDefaultForms = false` in `dmzdivine-common.toml` to stop the addon regenerating a
file you deleted on purpose.

The copy in this folder is a **reference snapshot for reading**, not the source of truth; the file
the addon actually ships is the one under `resources/data/dmzdivine/default_configs/`.

## Why files instead of injecting into ConfigManager

Everything downstream of the base mod reads config off disk: the server walks the config folder and
syncs every file to joining clients (`StatsCapability.onPlayerLogin`), `/dmzreload` re-reads them,
and the version upgrader can migrate them across base-mod releases. A group injected only into
`ConfigManager`'s in-memory maps would not exist for any connected client, because clients read
`SERVER_SYNCED_FORMS` while connected.

## What the base mod already provides

- `godforms` is a default form skill (`SkillsConfig.createDefaults`).
- Any `formType` containing `godform` maps to that skill
  (`TransformationsHelper.getSkillNameForType`), so the group name itself is free — ours is
  `divineforms`.
- `god_aura.png` / `god_cross.png` ship in the base jar, so `"auraType": "god"` renders with no
  asset from us.
- Divine forms are already hidden from ki sense, lock-on and instant transmission for players
  without the skill.

## The form group

`divineforms`, `formType` `godforms`, three forms: `supersaiyangod` (skill level 1),
`supersaiyanblue` (2), `supersaiyanblueevolved` (3).

SSJ God requires `supersaiyan.supersaiyan4` at 50% mastery (`formRequisite` + `unlockOnMastery`),
so the ritual unlock alone is not enough to transform.

`energyDrain` is `0.0` on SSJ God — divine ki costs nothing to hold. (`FormConfig.getEnergyDrain()`
clamps with `Math.max(0, …)`, so a negative value would mean the same thing rather than
regenerating ki.)

## The skill prices

```json
"godforms": {
  "buyFromMaster": true,
  "prices": [-1, 30000, 50000]
}
```

Three prices = three levels, matching the three forms: level 1 from the ritual, levels 2 and 3
bought with TP for Blue and Blue Evolved.

This entry is what makes the unlock storable at all. A form skill's max level *is* its number of
prices (`updateTransformationSkillLimits`), and `Skill.setLevel` clamps to that max — with the
base mod's generated empty list the max is 0 and the ritual could never grant level 1.
`GodRitual` refuses to start with `message.dmzdivine.ritual.misconfigured` if it ever finds that
state rather than running the minigame and granting nothing.

Why level 1 stays locked behind the ritual (verified against `UpdateSkillC2S`):

- `prices[0] = -1` blocks the `PURCHASE` path (it requires `effectiveCost >= 0`).
- `buyFromMaster: true` blocks the `UPGRADE` path from level 0 (`isMasterOnlyFormSkill`), which
  would otherwise clamp `-1` to a free upgrade.
- No master offers `godforms` in `skillOfferings`, so master screens cannot sell it.

`GodRitual.handleResult` registers the real max level before setting it, because `setSkillLevel`
alone would clamp to 0 — `Skills.calculateMaxLevel` consults skills.json's generic cost map, which
has no `godforms` entry (unlike the `ultimate` skill, which is why the base mod's Elder Kai ritual
can grant its skill directly).

## Translations

Form and group names come from `race.dragonminez.<race>.<group|form>.…` keys. Translation keys are
global, so the addon defines the `race.dragonminez.saiyan.*` entries for this group in its own lang
files. The `godforms` **skill** name and description are already translated by the base mod.
