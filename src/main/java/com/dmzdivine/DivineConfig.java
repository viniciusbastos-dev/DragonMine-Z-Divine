package com.dmzdivine;

import java.util.List;
import net.minecraftforge.common.ForgeConfigSpec;

public class DivineConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // --- Content installation ---
    public static final ForgeConfigSpec.BooleanValue INSTALL_DEFAULT_FORMS;

    // --- God ritual prerequisites (checked server-side) ---
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> RITUAL_RACES;
    public static final ForgeConfigSpec.ConfigValue<String> RITUAL_REQUIRED_SKILL;
    public static final ForgeConfigSpec.IntValue RITUAL_REQUIRED_SKILL_LEVEL;
    public static final ForgeConfigSpec.IntValue RITUAL_MIN_ALIGNMENT;
    public static final ForgeConfigSpec.IntValue RITUAL_REQUIRED_TP;

    // --- God ritual minigame tuning (read client-side) ---
    public static final ForgeConfigSpec.DoubleValue MINIGAME_CURSOR_SPEED;
    public static final ForgeConfigSpec.DoubleValue MINIGAME_SPEED_INCREASE;
    public static final ForgeConfigSpec.DoubleValue MINIGAME_ZONE_WIDTH;
    public static final ForgeConfigSpec.DoubleValue MINIGAME_ZONE_SHRINK;
    public static final ForgeConfigSpec.DoubleValue MINIGAME_MIN_ZONE_WIDTH;
    public static final ForgeConfigSpec.IntValue MINIGAME_REQUIRED_SUCCESSES;
    public static final ForgeConfigSpec.IntValue MINIGAME_ALLOWED_MISSES;

    // --- Ultra Instinct auto-dodge tuning (server-side) ---
    public static final ForgeConfigSpec.DoubleValue UI_MASTERY_WEIGHT;
    public static final ForgeConfigSpec.DoubleValue UI_VIT_WEIGHT;
    public static final ForgeConfigSpec.IntValue UI_VIT_REFERENCE;
    public static final ForgeConfigSpec.ConfigValue<String> UI_DODGE_RESOURCE;
    public static final ForgeConfigSpec.IntValue UI_DODGE_COST;

    // --- Beerus' planet: travel, gravity and Whis' arena ---
    public static final ForgeConfigSpec.IntValue BEERUS_UNLOCK_LEVEL;
    public static final ForgeConfigSpec.DoubleValue PLANET_GRAVITY;
    public static final ForgeConfigSpec.DoubleValue ARENA_GRAVITY;
    public static final ForgeConfigSpec.DoubleValue ARENA_TP_MULTIPLIER;

    // --- Whis' Ultra Instinct trial ---
    public static final ForgeConfigSpec.IntValue WHIS_TRAINING_MINUTES;
    public static final ForgeConfigSpec.IntValue WHIS_TRIAL_TP;
    public static final ForgeConfigSpec.IntValue WHIS_TRIAL_DODGES;
    public static final ForgeConfigSpec.IntValue WHIS_TRIAL_ALLOWED_HITS;
    public static final ForgeConfigSpec.IntValue WHIS_TRIAL_WINDUP_TICKS;
    public static final ForgeConfigSpec.IntValue WHIS_TRIAL_MIN_WINDUP;
    public static final ForgeConfigSpec.DoubleValue WHIS_TRIAL_WINDUP_DECREASE;
    public static final ForgeConfigSpec.IntValue WHIS_TRIAL_REACT_WINDOW;

    // --- Divine transformation visuals (client-side) ---
    public static final ForgeConfigSpec.BooleanValue TRANSFORM_PARTICLES;
    public static final ForgeConfigSpec.ConfigValue<String> TRANSFORM_PARTICLE_COLOR;
    public static final ForgeConfigSpec.IntValue TRANSFORM_BURST_PARTICLES;
    public static final ForgeConfigSpec.BooleanValue AURA_COLOR_TRANSITION;
    public static final ForgeConfigSpec.IntValue AURA_COLOR_TRANSITION_TICKS;
    public static final ForgeConfigSpec.BooleanValue ALIGNMENT_RECOLOR;
    public static final ForgeConfigSpec.IntValue ALIGNMENT_EVIL_THRESHOLD;

    static {
        BUILDER.comment("Divine content shipped by this addon").push("content");

        INSTALL_DEFAULT_FORMS = BUILDER
                .comment("Write the addon's divine form groups into config/dragonminez on startup,",
                        "along with the godforms skill prices the ritual needs.",
                        "Existing files and existing prices are never overwritten - turn this off only if",
                        "you want to delete a generated file without it coming back.")
                .define("installDefaultForms", true);

        BUILDER.pop();
        BUILDER.comment("Super Saiyan God ritual - unlock requirements (validated on the server)").push("god_ritual");

        RITUAL_RACES = BUILDER
                .comment("Races allowed to attempt the ritual (DragonMineZ race names).")
                .defineListAllowEmpty("allowedRaces", List.of("saiyan"), o -> o instanceof String);
        RITUAL_REQUIRED_SKILL = BUILDER
                .comment("DragonMineZ skill required before attempting the ritual (empty = no requirement).",
                        "Default 'superforms' is the Saiyan transformation skill.")
                .define("requiredSkill", "superforms");
        RITUAL_REQUIRED_SKILL_LEVEL = BUILDER
                .comment("Minimum level of requiredSkill. With the default Saiyan configs level 8 = Super Saiyan 4,",
                        "matching the God form's own formRequisite (100% SSJ4 mastery).")
                .defineInRange("requiredSkillLevel", 8, 0, 50);
        RITUAL_MIN_ALIGNMENT = BUILDER
                .comment("Minimum alignment (0-100). The god ritual demands a righteous heart, like the base mod's Ultimate ritual (>61).")
                .defineInRange("minAlignment", 62, 0, 100);
        RITUAL_REQUIRED_TP = BUILDER
                .comment("Training Points the player must have to attempt the ritual. Spent on success, kept on failure.")
                .defineInRange("requiredTp", 500000, 0, Integer.MAX_VALUE);

        BUILDER.pop();
        BUILDER.comment("Super Saiyan God ritual - minigame tuning").push("god_minigame");

        MINIGAME_CURSOR_SPEED = BUILDER
                .comment("Cursor speed, in percent of the bar per tick (20 ticks = 1 second).")
                .defineInRange("cursorSpeed", 1.6, 0.1, 25.0);
        MINIGAME_SPEED_INCREASE = BUILDER
                .comment("Cursor speed added after each successful hit.")
                .defineInRange("speedIncreasePerSuccess", 0.25, 0.0, 10.0);
        MINIGAME_ZONE_WIDTH = BUILDER
                .comment("Initial width of the divine zone, in percent of the bar.")
                .defineInRange("zoneWidthPercent", 14.0, 1.0, 100.0);
        MINIGAME_ZONE_SHRINK = BUILDER
                .comment("Divine zone width removed after each successful hit.")
                .defineInRange("zoneShrinkPerSuccess", 1.5, 0.0, 50.0);
        MINIGAME_MIN_ZONE_WIDTH = BUILDER
                .comment("Divine zone never shrinks below this width (percent).")
                .defineInRange("minZoneWidthPercent", 6.0, 1.0, 100.0);
        MINIGAME_REQUIRED_SUCCESSES = BUILDER
                .comment("Hits needed to complete the ritual. Lore: five righteous Saiyans lend their ki.")
                .defineInRange("requiredSuccesses", 5, 1, 50);
        MINIGAME_ALLOWED_MISSES = BUILDER
                .comment("Misses tolerated before the ritual fails.")
                .defineInRange("allowedMisses", 2, 0, 50);

        BUILDER.pop();
        BUILDER.comment("Ultra Instinct - auto-dodge tuning (server-side)").push("ultra_instinct");

        UI_MASTERY_WEIGHT = BUILDER
                .comment("Share of the dodge chance driven by mastery of the active ultrainstinct form",
                        "(0.0-1.0). Mastery and VIT weights are meant to add up to 1.0: mastery alone caps",
                        "here, it never reaches 100% by itself - VIT below fills the rest.")
                .defineInRange("masteryWeight", 0.40, 0.0, 1.0);
        UI_VIT_WEIGHT = BUILDER
                .comment("Share of the dodge chance driven by the VIT stat (0.0-1.0). See masteryWeight.")
                .defineInRange("vitWeight", 0.60, 0.0, 1.0);
        UI_VIT_REFERENCE = BUILDER
                .comment("VIT value that counts as a full investment for vitWeight's bonus (VIT beyond this",
                        "doesn't add more chance).")
                .defineInRange("vitReference", 2500, 1, Integer.MAX_VALUE);
        UI_DODGE_RESOURCE = BUILDER
                .comment("Resource spent per successful auto-dodge: \"ki\" or \"stamina\".")
                .define("dodgeResource", "ki");
        UI_DODGE_COST = BUILDER
                .comment("Flat cost per successful dodge - intentionally NOT reduced by mastery or anything",
                        "else. This is the actual balance lever: reaching a high dodge chance only helps if",
                        "the player has also built enough of the chosen resource pool to sustain it.")
                .defineInRange("dodgeCost", 30, 0, Integer.MAX_VALUE);

        BUILDER.pop();
        BUILDER.comment("Beerus' planet - space pod destination, gravity and Whis' training arena").push("beerus_planet");

        BEERUS_UNLOCK_LEVEL = BUILDER
                .comment("Character level needed before the planet shows up as a reachable space pod destination.",
                        "The base mod ships the destination permanently locked (it was reserved for another addon);",
                        "this addon rewrites it, so this value is the only gate.",
                        "Calibrated against the base mod's own late game: the Buu saga asks for level ~2060 to",
                        "reach the Sacred World, so the divine planet sits just past the end of that saga.")
                .defineInRange("unlockLevel", 2500, 1, Integer.MAX_VALUE);
        PLANET_GRAVITY = BUILDER
                .comment("Ambient gravity of the whole planet, written into DragonMineZ's",
                        "general-server.json gravityPerWorld map on first launch (never overwritten afterwards -",
                        "edit it there if you want to change it on an existing world).")
                .defineInRange("planetGravity", 10.0, 0.0, 1000.0);
        ARENA_GRAVITY = BUILDER
                .comment("Gravity inside Whis' arena, the ring of obsidian pillars east of the landing pad.",
                        "Applied as a floor on top of the base mod's own gravity math, so a gravity device or a",
                        "Kaio standing nearby still wins if they are heavier.")
                .defineInRange("arenaGravity", 30.0, 0.0, 1000.0);
        ARENA_TP_MULTIPLIER = BUILDER
                .comment("Training Points earned inside the arena are multiplied by this - the same idea as the",
                        "base mod's Hyperbolic Time Chamber bonus. 1.0 disables it.")
                .defineInRange("arenaTpMultiplier", 3.0, 1.0, 100.0);

        BUILDER.pop();
        BUILDER.comment("Whis' Ultra Instinct trial - how much training he demands, and the trial itself").push("whis_trial");

        WHIS_TRAINING_MINUTES = BUILDER
                .comment("Minutes of training inside the arena before Whis agrees to test you for Ultra Instinct",
                        "Sign. Mastered asks for twice this in total. Time is banked per player, per world.",
                        "This is the whole gate: Ultra Instinct cannot be bought from any master anymore.")
                .defineInRange("trainingMinutes", 60, 0, 10000);
        WHIS_TRIAL_TP = BUILDER
                .comment("Training Points spent when a trial is passed (doubled for the Mastered trial).",
                        "Kept on failure.")
                .defineInRange("trialTpCost", 250000, 0, Integer.MAX_VALUE);
        WHIS_TRIAL_DODGES = BUILDER
                .comment("Successful dodges needed to pass a trial.")
                .defineInRange("trialDodges", 8, 1, 50);
        WHIS_TRIAL_ALLOWED_HITS = BUILDER
                .comment("Strikes you may take before the trial fails.")
                .defineInRange("trialAllowedHits", 2, 0, 50);
        WHIS_TRIAL_WINDUP_TICKS = BUILDER
                .comment("Ticks a strike takes to reach you at the start of the trial (20 ticks = 1 second).")
                .defineInRange("trialWindupTicks", 26, 4, 200);
        WHIS_TRIAL_MIN_WINDUP = BUILDER
                .comment("Strikes never wind up faster than this, no matter how many you dodged.")
                .defineInRange("trialMinWindupTicks", 12, 3, 200);
        WHIS_TRIAL_WINDUP_DECREASE = BUILDER
                .comment("Ticks shaved off the wind-up after each successful dodge.")
                .defineInRange("trialWindupDecrease", 1.5, 0.0, 20.0);
        WHIS_TRIAL_REACT_WINDOW = BUILDER
                .comment("Ticks before impact during which a dodge counts. Reacting earlier than this is reading",
                        "the attack instead of feeling it, and misses.")
                .defineInRange("trialReactWindow", 10, 1, 100);

        BUILDER.pop();
        BUILDER.comment("Divine transformation visuals (read on the client)").push("divine_visuals");

        TRANSFORM_PARTICLES = BUILDER
                .comment("Spawn gathering ki particles while a player charges into a god-type form.")
                .define("transformParticles", true);
        TRANSFORM_PARTICLE_COLOR = BUILDER
                .comment("Hex color of the ki gathered during the transformation.",
                        "Once the form lands, its own aura takes over - that comes from auraType/auraColor",
                        "in the DragonMineZ form JSON, not from here.")
                .define("transformParticleColor", "#3FA9FF");
        TRANSFORM_BURST_PARTICLES = BUILDER
                .comment("Particles released in the burst when the god form lands. 0 disables the burst.")
                .defineInRange("transformBurstParticles", 45, 0, 300);
        AURA_COLOR_TRANSITION = BUILDER
                .comment("Ease the aura's own color across a divine transformation (base<->god<->blue<->evolved)",
                        "instead of it snapping straight to the landed form's color. Covers instant transforms too,",
                        "not just held charge-ups.")
                .define("auraColorTransition", true);
        AURA_COLOR_TRANSITION_TICKS = BUILDER
                .comment("How long the aura color fade takes, in ticks (20 = 1 second).")
                .defineInRange("auraColorTransitionTicks", 25, 1, 200);
        ALIGNMENT_RECOLOR = BUILDER
                .comment("Recolor Super Saiyan Blue / Blue Evolved into a Super Saiyan Rose - style palette",
                        "while the player's alignment is evil, reverting to the normal blue once it isn't.")
                .define("alignmentRecolor", true);
        ALIGNMENT_EVIL_THRESHOLD = BUILDER
                .comment("Alignment (0-100) at or below which the Rose recolor applies.",
                        "Matches the base mod's own good/neutral/evil banding (AlignmentBand: evil <= 40).")
                .defineInRange("alignmentEvilThreshold", 40, 0, 100);

        BUILDER.pop();
    }

    public static final ForgeConfigSpec SPEC = BUILDER.build();
}
