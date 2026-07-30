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

    // --- God ritual minigame tuning (read client-side) ---
    public static final ForgeConfigSpec.DoubleValue MINIGAME_CURSOR_SPEED;
    public static final ForgeConfigSpec.DoubleValue MINIGAME_SPEED_INCREASE;
    public static final ForgeConfigSpec.DoubleValue MINIGAME_ZONE_WIDTH;
    public static final ForgeConfigSpec.DoubleValue MINIGAME_ZONE_SHRINK;
    public static final ForgeConfigSpec.DoubleValue MINIGAME_MIN_ZONE_WIDTH;
    public static final ForgeConfigSpec.IntValue MINIGAME_REQUIRED_SUCCESSES;
    public static final ForgeConfigSpec.IntValue MINIGAME_ALLOWED_MISSES;

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
                .comment("Minimum level of requiredSkill. With the default Saiyan configs level 6 = Super Saiyan 3.")
                .defineInRange("requiredSkillLevel", 6, 0, 50);
        RITUAL_MIN_ALIGNMENT = BUILDER
                .comment("Minimum alignment (0-100). The god ritual demands a righteous heart, like the base mod's Ultimate ritual (>61).")
                .defineInRange("minAlignment", 62, 0, 100);

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
