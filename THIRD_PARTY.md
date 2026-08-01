# Third-party content shipped by DMZ Divine

## Beerus and Whis models/textures — GPL-3.0

These four files are taken verbatim from **DragonMine Z: Super (`dmzsuper`) 1.8.2 by facub8**, which is
licensed **GPL-3.0**:

| File in this repo | Origin in `dmzsuper-1.8.2.jar` |
| --- | --- |
| `assets/dragonminez/geo/entity/master/master_beerus.geo.json` | `assets/dmzsuper/geo/entity/masters/beerus.geo.json` |
| `assets/dragonminez/geo/entity/master/master_whis.geo.json` | `assets/dmzsuper/geo/entity/masters/whis.geo.json` |
| `assets/dragonminez/textures/entity/master/master_beerus.png` | `assets/dmzsuper/textures/entity/masters/beerus.png` |
| `assets/dragonminez/textures/entity/master/master_whis.png` | `assets/dmzsuper/textures/entity/masters/whis.png` |

They are renamed (not edited) so DragonMineZ's `MasterGlobalModel` resolves them from the entity registry
path of `dmzdivine:master_beerus` / `dmzdivine:master_whis`. DragonMineZ 2.1.3 registers entity types under
those names but ships no assets for them, so without these the NPCs render as the robot fallback.

**What this means:** GPL-3.0 is copyleft. Distributing a jar that contains these files makes the
distributed work a derivative of `dmzsuper`, so any public release of DMZ Divine that bundles them has to
be released under GPL-3.0 as well, with source available and credit to facub8. That is fine for a private
server; it is a decision to make consciously before publishing.

**If you would rather not inherit GPL-3.0:** delete the four files. Nothing breaks - Beerus and Whis
fall back to DragonMineZ's robot model, and the planet, gravity, arena and trials all still work. Drop
in your own art at the same paths whenever you have it.
