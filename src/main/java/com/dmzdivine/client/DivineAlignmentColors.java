package com.dmzdivine.client;

import com.dmzdivine.DivineConfig;
import com.dragonminez.client.util.ColorUtils;

/**
 * Super Saiyan Blue and Blue Evolved recolor into a "corrupted" palette while the player's
 * alignment is evil - canonically Super Saiyan Rosé for Blue (Goku Black). Blue Evolved has no
 * anime equivalent, so it gets a darker, more saturated take on the same family rather than purple
 * (that's Vegeta's Ultra Ego).
 *
 * Colors are read fresh every render call (see AuraRendererMixin / DMZHairLayerMixin), so this is
 * purely a live "what should render right now" lookup - no state, no transition tracking. Reverting
 * to neutral/good alignment snaps back to the form's normal JSON colors on the very next frame.
 */
public final class DivineAlignmentColors {

    private static final String DIVINE_GROUP = "divineforms";
    private static final String BLUE_FORM = "supersaiyanblue";
    private static final String BLUE_EVOLVED_FORM = "supersaiyanblueevolved";

    /** Canon Super Saiyan Rosé: pink hair, warm pink-red aura. */
    private static final float[] ROSE_HAIR = ColorUtils.hexToRgb("#FF99CC");
    private static final float[] ROSE_AURA_INNER = ColorUtils.hexToRgb("#400053");
    private static final float[] ROSE_AURA_OUTER = ColorUtils.hexToRgb("#DE1C52");

    /** No canon Blue Evolved equivalent: darker/more intense rose, deliberately nowhere near purple. */
    private static final float[] ROSE_EVOLVED_HAIR = ColorUtils.hexToRgb("#D82C75");
    private static final float[] ROSE_EVOLVED_AURA_INNER = ColorUtils.hexToRgb("#1E0036");
    private static final float[] ROSE_EVOLVED_AURA_OUTER = ColorUtils.hexToRgb("#FF007F");
    private static final float[] ROSE_EVOLVED_EYE = ColorUtils.hexToRgb("#FF66B2");

    /**
     * The player currently being rendered, refreshed at the head of every DMZPlayerRenderer#render
     * call (see DMZPlayerRendererMixin). Rendering is single-threaded and one player's full render
     * pass (all GeoRenderLayers) completes before the next starts, so a single field is enough -
     * FormConfig.FormData getters have no player context of their own to key a map by anyway.
     */
    private static int currentAlignment = 100;

    private DivineAlignmentColors() {
    }

    public static void setCurrentAlignment(int alignment) {
        currentAlignment = alignment;
    }

    public static int getCurrentAlignment() {
        return currentAlignment;
    }

    public static boolean isEvil(int alignment) {
        return DivineConfig.ALIGNMENT_RECOLOR.get() && alignment <= DivineConfig.ALIGNMENT_EVIL_THRESHOLD.get();
    }

    /** group is optional (some callers only have the form name available) - null skips that check. */
    private static boolean isRoseEligible(String group, String form) {
        if (group != null && !DIVINE_GROUP.equalsIgnoreCase(group)) return false;
        return BLUE_FORM.equalsIgnoreCase(form) || BLUE_EVOLVED_FORM.equalsIgnoreCase(form);
    }

    public static float[] hairOverride(String group, String form, int alignment) {
        if (!isEvil(alignment) || !isRoseEligible(group, form)) return null;
        return (BLUE_EVOLVED_FORM.equalsIgnoreCase(form) ? ROSE_EVOLVED_HAIR : ROSE_HAIR).clone();
    }

    /** Tail color and the face layer's secondary tint both read bodyColor2 - mirror the hair color. */
    public static float[] bodyColor2Override(String form, int alignment) {
        return hairOverride(null, form, alignment);
    }

    public static float[] eyeOverride(String form, int alignment) {
        if (!isEvil(alignment) || !isRoseEligible(null, form)) return null;
        return (BLUE_EVOLVED_FORM.equalsIgnoreCase(form) ? ROSE_EVOLVED_EYE : ROSE_HAIR).clone();
    }

    public static float[] auraInnerOverride(String group, String form, int alignment) {
        if (!isEvil(alignment) || !isRoseEligible(group, form)) return null;
        return (BLUE_EVOLVED_FORM.equalsIgnoreCase(form) ? ROSE_EVOLVED_AURA_INNER : ROSE_AURA_INNER).clone();
    }

    public static float[] auraOuterOverride(String group, String form, int alignment) {
        if (!isEvil(alignment) || !isRoseEligible(group, form)) return null;
        return (BLUE_EVOLVED_FORM.equalsIgnoreCase(form) ? ROSE_EVOLVED_AURA_OUTER : ROSE_AURA_OUTER).clone();
    }
}
