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

    /** Form groups shipped by this addon, as race -> config file name (without .json). */
    private static final List<FormGroup> SHIPPED_GROUPS = List.of(
            new FormGroup("saiyan", "divineforms")
    );

    /**
     * TP prices for the godforms skill ladder. Level 1 is {@code -1} on purpose: it makes both
     * purchase paths in the base mod's UpdateSkillC2S refuse the skill, leaving the ritual as the
     * only way in. Levels 2 and 3 are ordinary TP upgrades that unlock Blue and Blue Evolved.
     */
    private static final List<Integer> GODFORMS_PRICES = List.of(-1, 30000, 50000);

    private record FormGroup(String race, String group) {
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
            ensureGodformsPrices(shipped.race());
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
    private static void ensureGodformsPrices(String race) {
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
            GODFORMS_PRICES.forEach(prices::add);
            JsonObject entry = new JsonObject();
            entry.addProperty("buyFromMaster", true);
            entry.add("prices", prices);
            costs.add(GodRitual.GODFORMS_SKILL, entry);

            if (ConfigManager.saveRawConfig(relativePath, root.toString())) {
                ConfigManager.reloadSpecificConfig(relativePath);
                DMZDivine.LOGGER.info("Added godforms skill prices {} to race '{}'", GODFORMS_PRICES, race);
            }
        } catch (Exception e) {
            DMZDivine.LOGGER.error("Could not add godforms prices to {}: {}", relativePath, e.toString());
        }
    }
}
