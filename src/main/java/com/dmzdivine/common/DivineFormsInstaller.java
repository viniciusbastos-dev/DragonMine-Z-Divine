package com.dmzdivine.common;

import com.dmzdivine.DMZDivine;
import com.dmzdivine.DivineConfig;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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
 * so each freshly written/merged file is handed back to {@link ConfigManager#reloadSpecificConfig}
 * to take effect on this launch too.
 *
 * Existing content is never overwritten: groups are installed only when their file is absent, and
 * price lists/skill entries are only appended/added when missing. Players still own whatever they
 * have edited.
 */
public final class DivineFormsInstaller {

    private static final String RESOURCE_ROOT = "/data/dmzdivine/default_configs/";
    private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("dragonminez");

    /** Majin's Demon God still uses its own skill - it has no "superforms"-equivalent ladder to fold into. */
    private static final String MAJIN_GODFORMS_SKILL = "godforms";

    /** Levels appended to Saiyan's existing superforms ladder (which ends at supersaiyan4, level 8):
     * 9 = God (-1 blocks purchase, ritual-only, same trick as the base mod's "ultimate" skill),
     * 10 = Blue, 11 = Blue Evolved. See races/saiyan/forms/divineforms.json and GodRitual. */
    private static final List<Integer> SAIYAN_GOD_PRICE_ADDITIONS = List.of(-1, 200000, 300000);
    private static final int SAIYAN_SUPERFORMS_BASELINE = 8;

    /** Level 1 unlocks "sign", level 2 unlocks "mastered" - see forms/ultrainstinct.json. */
    private static final List<Integer> ULTRAINSTINCT_TP_COSTS = List.of(500000, 1000000);

    /** Form groups installed as their own standalone file: race -> config file name (without .json) -> prices. */
    private static final List<FormGroup> SHIPPED_GROUPS = List.of(
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
            ensureMajinGodformsPrices(shipped.race(), shipped.godformsPrices());
            ensureMajinGodformsOffering(shipped.master());
        }

        installSaiyanGodforms();
        installUltraInstinct();
    }

    // --- Saiyan God/Blue/Blue Evolved: its own group sharing the "superforms" skill/ladder instead
    // --- of a separate "godforms" skill, so they show up in the normal Super Saiyan menu as their
    // --- own branch (see GodRitual).

    private static void installSaiyanGodforms() {
        if (!ConfigManager.isRaceLoaded("saiyan")) {
            DMZDivine.LOGGER.warn("Race 'saiyan' is not loaded, skipping Super Saiyan God/Blue/Blue Evolved");
            return;
        }
        // Its own group/file (formType "superforms"), same as ssgrades.json sits beside supersaiyan.json
        // in the base mod - two groups can share a formType/skill ladder while still rendering as
        // separate sibling branches, so God shows up next to Mastered instead of buried under it.
        installFormGroup("saiyan", "divineforms");
        extendSuperformsPrices();
    }

    /** Appends the 3 God/Blue/Blue Evolved price levels to superforms, never touching prices already there. */
    private static void extendSuperformsPrices() {
        RaceCharacterConfig character = ConfigManager.getRaceCharacter("saiyan");
        if (character == null) return;

        int currentLevels = character.getFormSkillTpCosts("superforms").length;
        if (currentLevels >= SAIYAN_SUPERFORMS_BASELINE + SAIYAN_GOD_PRICE_ADDITIONS.size()) return;

        String relativePath = "races/saiyan/character";
        try {
            String json = ConfigManager.getSpecificConfigJson(relativePath);
            if (json == null) {
                DMZDivine.LOGGER.error("Could not read {} to extend the superforms prices", relativePath);
                return;
            }

            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonObject costs = root.getAsJsonObject("formSkillsCosts");
            if (costs == null) {
                costs = new JsonObject();
                root.add("formSkillsCosts", costs);
            }
            JsonObject superforms = costs.getAsJsonObject("superforms");
            if (superforms == null) {
                DMZDivine.LOGGER.error("{} has no formSkillsCosts.superforms - cannot extend it safely", relativePath);
                return;
            }
            JsonArray prices = superforms.getAsJsonArray("prices");
            if (prices == null) {
                prices = new JsonArray();
                superforms.add("prices", prices);
            }

            int missingFromIndex = Math.max(0, prices.size() - SAIYAN_SUPERFORMS_BASELINE);
            for (int i = missingFromIndex; i < SAIYAN_GOD_PRICE_ADDITIONS.size(); i++) {
                prices.add(SAIYAN_GOD_PRICE_ADDITIONS.get(i));
            }

            if (ConfigManager.saveRawConfig(relativePath, root.toString())) {
                ConfigManager.reloadSpecificConfig(relativePath);
                DMZDivine.LOGGER.info("Extended superforms prices for race 'saiyan' with God/Blue/Blue Evolved costs");
            }
        } catch (Exception e) {
            DMZDivine.LOGGER.error("Could not extend superforms prices: {}", e.toString());
        }
    }

    // --- Ultra Instinct: root-level stack form, next to kaioken/ultimate ---

    private static void installUltraInstinct() {
        String relativePath = "forms/ultrainstinct";
        Path target = CONFIG_DIR.resolve(relativePath + ".json");
        if (!Files.exists(target)) {
            try (InputStream source = DivineFormsInstaller.class.getResourceAsStream(RESOURCE_ROOT + relativePath + ".json")) {
                if (source == null) {
                    DMZDivine.LOGGER.error("Bundled config {} is missing from the jar", relativePath);
                    return;
                }
                Files.createDirectories(target.getParent());
                Files.copy(source, target);
                ConfigManager.reloadSpecificConfig(relativePath);
                DMZDivine.LOGGER.info("Installed the Ultra Instinct stack form group");
            } catch (Exception e) {
                DMZDivine.LOGGER.error("Could not install {}: {}", relativePath, e.toString());
                return;
            }
        }

        wireUltraInstinctSkill();
    }

    /** Marks "ultrainstinct" as a stack skill (layers on the active form instead of replacing it,
     * same as kaioken/ultimate), gives it a TP cost open to every race, and lists King Kai as a
     * seller - all in the global skills.json, never overwriting anything already there. */
    private static void wireUltraInstinctSkill() {
        try {
            String json = ConfigManager.getSpecificConfigJson("skills");
            if (json == null) {
                DMZDivine.LOGGER.error("Could not read skills.json to wire up Ultra Instinct");
                return;
            }

            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            boolean changed = false;

            JsonArray stackSkills = root.getAsJsonArray("stackSkills");
            if (stackSkills == null) {
                stackSkills = new JsonArray();
                root.add("stackSkills", stackSkills);
            }
            if (!containsIgnoreCase(stackSkills, "ultrainstinct")) {
                stackSkills.add("ultrainstinct");
                changed = true;
            }

            JsonObject skills = root.getAsJsonObject("skills");
            if (skills == null) {
                skills = new JsonObject();
                root.add("skills", skills);
            }
            if (!skills.has("ultrainstinct")) {
                JsonArray costs = new JsonArray();
                ULTRAINSTINCT_TP_COSTS.forEach(costs::add);
                JsonObject entry = new JsonObject();
                entry.add("costs", costs);
                entry.add("allowedRaces", new JsonArray()); // empty = every race, same as kaioken/ultimate
                skills.add("ultrainstinct", entry);
                changed = true;
            }

            JsonObject offerings = root.getAsJsonObject("skillOfferings");
            if (offerings == null) {
                offerings = new JsonObject();
                root.add("skillOfferings", offerings);
            }
            JsonArray kingkai = offerings.getAsJsonArray("kingkai");
            if (kingkai == null) {
                kingkai = new JsonArray();
                offerings.add("kingkai", kingkai);
            }
            if (!containsIgnoreCase(kingkai, "ultrainstinct")) {
                kingkai.add("ultrainstinct");
                changed = true;
            }

            if (changed && ConfigManager.saveRawConfig("skills", root.toString())) {
                ConfigManager.reloadSpecificConfig("skills");
                DMZDivine.LOGGER.info("Wired Ultra Instinct into skills.json (stackSkills/costs/offerings)");
            }
        } catch (Exception e) {
            DMZDivine.LOGGER.error("Could not wire Ultra Instinct into skills.json: {}", e.toString());
        }
    }

    private static boolean containsIgnoreCase(JsonArray array, String value) {
        for (JsonElement element : array) {
            if (element.isJsonPrimitive() && value.equalsIgnoreCase(element.getAsString())) return true;
        }
        return false;
    }

    // --- Majin Demon God: standalone group + its own "godforms" skill, unchanged from before ---

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
    private static void ensureMajinGodformsPrices(String race, List<Integer> godformsPrices) {
        RaceCharacterConfig character = ConfigManager.getRaceCharacter(race);
        if (character == null) return;
        if (character.getFormSkillTpCosts(MAJIN_GODFORMS_SKILL).length > 0) return;

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
            costs.add(MAJIN_GODFORMS_SKILL, entry);

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
    private static void ensureMajinGodformsOffering(String masterName) {
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

            if (containsIgnoreCase(offered, MAJIN_GODFORMS_SKILL)) return;

            offered.add(MAJIN_GODFORMS_SKILL);

            if (ConfigManager.saveRawConfig("skills", root.toString())) {
                ConfigManager.reloadSpecificConfig("skills");
                DMZDivine.LOGGER.info("Added '{}' as a godforms seller", masterName);
            }
        } catch (Exception e) {
            DMZDivine.LOGGER.error("Could not add {} as a godforms seller: {}", masterName, e.toString());
        }
    }
}
