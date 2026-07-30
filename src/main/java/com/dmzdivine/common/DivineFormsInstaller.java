package com.dmzdivine.common;

import com.dmzdivine.DMZDivine;
import com.dmzdivine.DivineConfig;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Ships the divine form groups with the addon instead of asking players to hand-write them.
 *
 * The forms are installed as ordinary DragonMineZ config files rather than injected into memory,
 * because everything downstream reads from disk: the server walks the config folder to sync each
 * file to joining clients ({@code StatsCapability.onPlayerLogin}), {@code /dmzreload} re-reads it,
 * and the base mod's version upgrader can migrate it later. An in-memory-only group would simply
 * not exist for clients, which read {@code SERVER_SYNCED_FORMS} while connected.
 *
 * Runs at common setup, after DragonMineZ has already loaded its configs during mod construction,
 * so each freshly written file is handed back to {@link ConfigManager#reloadSpecificConfig} to take
 * effect on this launch too.
 *
 * Player-owned files are never overwritten: a group is written only when its file is absent, and
 * skill prices only when the race defines none.
 */
public final class DivineFormsInstaller {

    private static final String RESOURCE_ROOT = "/data/dmzdivine/default_configs/";
    private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("dragonminez");

    /** Form groups shipped by this addon, as race -> config file name (without .json) -> prices. */
    private static final List<FormGroup> SHIPPED_GROUPS = List.of(
            // Level 1 is -1 on purpose: it makes both purchase paths in the base mod's
            // UpdateSkillC2S refuse the skill, leaving the ritual as the only way into God. Levels
            // 2 and 3 are ordinary TP upgrades that unlock Blue and Blue Evolved. Goku sells it since
            // no base-mod master offers "godforms" at all otherwise (see ensureGodformsOffering).
            new FormGroup("saiyan", "divineforms", List.of(-1, 200000, 300000), "goku"),
            // No ritual gates Demon God, so it's an ordinary TP purchase like any other form skill.
            // Babidi would fit the lore (he created Majin Buu) but can't sell anything: he's in the
            // base mod's TEXT_MASTERS set, which forces isSkillMaster=false and hides the Skills
            // button entirely (QuestNPCDialogueScreen.java:51/171) - only a "Services" dialogue
            // button shows for him, confirmed in-game. King Kai is a real skill master instead.
            new FormGroup("majin", "divineforms", List.of(500000, 500000), "kingkai")
    );

    private record FormGroup(String race, String group, List<Integer> godformsPrices, String master) {
    }

    private DivineFormsInstaller() {
    }

    public static void install() {
        if (!DivineConfig.INSTALL_DEFAULT_FORMS.get()) return;

        for (FormGroup shipped : SHIPPED_GROUPS) {
            if (!ConfigManager.isRaceLoaded(shipped.race())) {
                DMZDivine.LOGGER.warn("Race '{}' is not loaded, skipping the {} form group",
                        shipped.race(), shipped.group());
                continue;
            }
            installFormGroup(shipped.race(), shipped.group());
            ensureGodformsPrices(shipped.race(), shipped.godformsPrices());
            ensureGodformsOffering(shipped.master());
        }
    }

    private static void installFormGroup(String race, String group) {
        String relativePath = "races/" + race + "/forms/" + group;
        Path target = CONFIG_DIR.resolve(relativePath + ".json");
        if (Files.exists(target)) return;

        try (InputStream source = DivineFormsInstaller.class.getResourceAsStream(
                RESOURCE_ROOT + relativePath + ".json")) {
            if (source == null) {
                DMZDivine.LOGGER.error("Bundled config {} is missing from the jar", relativePath);
                return;
            }
            Files.createDirectories(target.getParent());
            Files.copy(source, target);
            ConfigManager.reloadSpecificConfig(relativePath);
            DMZDivine.LOGGER.info("Installed divine form group '{}' for race '{}'", group, race);
        } catch (Exception e) {
            DMZDivine.LOGGER.error("Could not install form group {}: {}", relativePath, e.toString());
        }
    }

    /**
     * A form skill's max level is its number of prices, and {@code Skill.setLevel} clamps to that
     * max - so with the base mod's generated empty price list the ritual could never store the
     * unlock. Fill it in when the race defines none.
     */
    private static void ensureGodformsPrices(String race, List<Integer> godformsPrices) {
        RaceCharacterConfig character = ConfigManager.getRaceCharacter(race);
        if (character == null) return;
        if (character.getFormSkillTpCosts(GodRitual.GODFORMS_SKILL).length > 0) return;

        String relativePath = "races/" + race + "/character";
        try {
            String json = ConfigManager.getSpecificConfigJson(relativePath);
            if (json == null) {
                DMZDivine.LOGGER.error("Could not read {} to add the godforms skill prices", relativePath);
                return;
            }

            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonObject costs = root.getAsJsonObject("formSkillsCosts");
            if (costs == null) {
                costs = new JsonObject();
                root.add("formSkillsCosts", costs);
            }

            JsonArray prices = new JsonArray();
            godformsPrices.forEach(prices::add);
            JsonObject entry = new JsonObject();
            entry.addProperty("buyFromMaster", true);
            entry.add("prices", prices);
            costs.add(GodRitual.GODFORMS_SKILL, entry);

            if (ConfigManager.saveRawConfig(relativePath, root.toString())) {
                ConfigManager.reloadSpecificConfig(relativePath);
                DMZDivine.LOGGER.info("Added godforms skill prices {} to race '{}'", godformsPrices, race);
            }
        } catch (Exception e) {
            DMZDivine.LOGGER.error("Could not add godforms prices to {}: {}", relativePath, e.toString());
        }
    }

    /**
     * A form skill only shows up in a master's Skills screen if that master's entry in
     * {@code skills.json}'s {@code skillOfferings} lists it (see MastersSkillsScreen#getMasterSkills)
     * - the price on the race's character.json alone doesn't make it purchasable anywhere. No
     * base-mod master offers "godforms" by default, so without this the skill has prices but no
     * seller.
     */
    private static void ensureGodformsOffering(String masterName) {
        try {
            String json = ConfigManager.getSpecificConfigJson("skills");
            if (json == null) {
                DMZDivine.LOGGER.error("Could not read skills.json to add {} as a godforms seller", masterName);
                return;
            }

            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonObject offerings = root.getAsJsonObject("skillOfferings");
            if (offerings == null) {
                offerings = new JsonObject();
                root.add("skillOfferings", offerings);
            }

            JsonArray offered = offerings.getAsJsonArray(masterName);
            if (offered == null) {
                offered = new JsonArray();
                offerings.add(masterName, offered);
            }

            boolean alreadyOffered = false;
            for (var element : offered) {
                if (element.isJsonPrimitive() && GodRitual.GODFORMS_SKILL.equalsIgnoreCase(element.getAsString())) {
                    alreadyOffered = true;
                    break;
                }
            }
            if (alreadyOffered) return;

            offered.add(GodRitual.GODFORMS_SKILL);

            if (ConfigManager.saveRawConfig("skills", root.toString())) {
                ConfigManager.reloadSpecificConfig("skills");
                DMZDivine.LOGGER.info("Added '{}' as a godforms seller", masterName);
            }
        } catch (Exception e) {
            DMZDivine.LOGGER.error("Could not add {} as a godforms seller: {}", masterName, e.toString());
        }
    }
}
