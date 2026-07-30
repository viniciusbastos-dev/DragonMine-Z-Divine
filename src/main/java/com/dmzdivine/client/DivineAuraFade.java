package com.dmzdivine.client;

import com.dmzdivine.DMZDivine;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Character;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects the moment a player's regular form crosses in or out of a god-type group, independent of
 * whether the aura mesh happens to be rendering right then. DMZAuraLayer only draws the aura while
 * isAuraActive/isPermanentAura is true (sprint/fly/combat bursts, or the kicontrol toggle) - a
 * player standing still right after transforming may not render an aura frame at all for a while.
 * A Mixin living purely inside AuraRenderer#getAuraLayers would miss those transitions entirely, so
 * detection runs here on every client tick instead, and AuraRendererMixin just consumes whatever is
 * pending via {@link #getActiveTransition} whenever the aura next actually draws.
 */
@Mod.EventBusSubscriber(modid = DMZDivine.MODID, value = Dist.CLIENT)
public final class DivineAuraFade {

    /** startTick is set lazily on first render so the fade always plays its full length once seen. */
    public static final class Transition {
        public final float[] fromColor;
        public long startTick = -1L;

        private Transition(float[] fromColor) {
            this.fromColor = fromColor;
        }
    }

    private static final Map<UUID, String> lastSignature = new ConcurrentHashMap<>();
    private static final Map<UUID, Transition> active = new ConcurrentHashMap<>();

    private DivineAuraFade() {
    }

    public static Transition getActiveTransition(UUID playerId) {
        return active.get(playerId);
    }

    public static void clearTransition(UUID playerId) {
        active.remove(playerId);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        for (Player player : mc.level.players()) {
            StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
            if (data == null) continue;

            Character character = data.getCharacter();
            String race = character.getRaceName();
            String group = character.getActiveFormGroup();
            String form = character.getActiveForm();
            String signature = group + ":" + form;

            UUID id = player.getUUID();
            String previous = lastSignature.put(id, signature);
            if (previous == null || previous.equals(signature)) continue;

            int split = previous.indexOf(':');
            String prevGroup = split >= 0 ? previous.substring(0, split) : null;
            String prevForm = split >= 0 ? previous.substring(split + 1) : null;

            if (isGodGroup(race, group) || isGodGroup(race, prevGroup)) {
                int alignment = data.getResources().getAlignment();
                active.put(id, new Transition(resolveColor(character, race, prevGroup, prevForm, alignment)));
            }
        }
    }

    private static float[] resolveColor(Character character, String race, String group, String form, int alignment) {
        float[] roseOverride = DivineAlignmentColors.auraInnerOverride(group, form, alignment);
        if (roseOverride != null) return roseOverride;

        if (group != null && !group.isEmpty() && !"null".equals(group) && form != null && !form.isEmpty() && !"null".equals(form)) {
            FormConfig config = ConfigManager.getFormGroup(race, group);
            FormConfig.FormData formData = config != null ? config.getForm(form) : null;
            if (formData != null && formData.getAuraColor() != null && !formData.getAuraColor().isEmpty()) {
                return formData.getRgbAuraColor();
            }
        }
        return character.getRgbAuraColor();
    }

    /** Matches on formType rather than the group name, so renaming the group in JSON keeps working. */
    private static boolean isGodGroup(String race, String group) {
        if (group == null || group.isEmpty() || "null".equals(group)) return false;
        FormConfig config = ConfigManager.getFormGroup(race, group);
        return config != null && config.getFormType() != null
                && config.getFormType().toLowerCase(Locale.ROOT).contains("god");
    }
}
