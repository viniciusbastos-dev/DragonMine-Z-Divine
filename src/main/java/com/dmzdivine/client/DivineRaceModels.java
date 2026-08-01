package com.dmzdivine.client;

import com.dmzdivine.DivineConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Which body Ultra Instinct Mastered puts on, per race.
 *
 * <p>Ultra Instinct is a single stack form group shared by every race - one file in
 * {@code config/dragonminez/forms/}, not one per race - so its JSON has exactly one {@code customModel}
 * to give. It says {@code "buffed"}, which is the human/saiyan build: a Namekian or a Frost Demon
 * reaching Mastered currently swaps into a human body. There is no per-race field to set instead, so
 * the value is overridden at read time from the config map here.
 *
 * <p>Same shape as {@link DivineAlignmentColors}: {@code FormConfig.FormData} is one shared instance
 * per form with no player context, so the race of the player currently being rendered is captured at
 * the head of every {@code DMZPlayerRenderer#render} (see
 * {@link com.dmzdivine.mixin.client.DMZPlayerRendererMixin}) and read back by
 * {@link com.dmzdivine.mixin.client.FormDataCustomModelMixin}. Rendering is single-threaded and one
 * player's whole pass finishes before the next starts, so one field each is enough.
 */
public final class DivineRaceModels {

    private static final String UI_GROUP = "ultrainstinct";
    private static final String MASTERED_FORM = "mastered";

    private static String currentRace = "";
    private static String currentStackGroup = "";

    /** Parsed form of the config list, rebuilt when the list instance changes (a /reload or edit). */
    private static List<? extends String> parsedFrom;
    private static Map<String, String> models = Map.of();

    private DivineRaceModels() {
    }

    public static void setCurrent(String race, String stackGroup) {
        currentRace = race == null ? "" : race.toLowerCase(Locale.ROOT);
        currentStackGroup = stackGroup == null ? "" : stackGroup;
    }

    /**
     * The model key this form should use for the player being rendered, or null to leave the JSON
     * value alone. Both the group and the form name are checked: "mastered" is a plausible form name
     * in somebody else's group, and only Ultra Instinct's should be redirected.
     */
    public static String masteredBodyOverride(String formName) {
        if (!MASTERED_FORM.equalsIgnoreCase(formName)) return null;
        if (!UI_GROUP.equalsIgnoreCase(currentStackGroup)) return null;
        if (currentRace.isEmpty()) return null;

        String model = models().get(currentRace);
        return model == null || model.isEmpty() ? null : model;
    }

    private static Map<String, String> models() {
        List<? extends String> configured = DivineConfig.UI_MASTERED_BODY_PER_RACE.get();
        if (configured == parsedFrom) return models;

        Map<String, String> parsed = new HashMap<>();
        for (String entry : configured) {
            if (entry == null) continue;
            int split = entry.indexOf('=');
            if (split <= 0 || split == entry.length() - 1) continue;
            parsed.put(entry.substring(0, split).trim().toLowerCase(Locale.ROOT),
                    entry.substring(split + 1).trim().toLowerCase(Locale.ROOT));
        }

        parsedFrom = configured;
        models = parsed;
        return models;
    }
}
