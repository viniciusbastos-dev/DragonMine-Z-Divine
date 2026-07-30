package com.dmzdivine.mixin.client;

import com.dmzdivine.DivineConfig;
import com.dmzdivine.client.DivineAlignmentColors;
import com.dmzdivine.client.DivineAuraFade;
import com.dragonminez.client.render.effects.AuraRenderer;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.UUID;

/**
 * DragonMineZ's AuraRenderer snaps a form's aura straight to its configured color the instant
 * activeForm changes - there's no charge-up involved for instant transforms, and no event fires
 * around the swap either way. Divine transformations should read as the aura's hue shifting as the
 * form lands, so this eases every returned layer's color in from the departing form's color instead
 * of letting it jump. DivineAuraFade owns detecting the transition (it has to run on every client
 * tick, not just when this renders - see its javadoc); this just applies whatever is pending.
 *
 * Also applies the evil-alignment Rose recolor (see DivineAlignmentColors) before the fade blend
 * runs, so a transition into/out of an evil-aligned Blue or Blue Evolved fades toward/from the Rose
 * colors rather than the plain form JSON ones.
 */
@Mixin(value = AuraRenderer.class, remap = false)
public abstract class AuraRendererMixin {

    @Inject(method = "getAuraLayers", at = @At("RETURN"))
    private static void dmzdivine$recolorAndFadeAura(Player player, StatsData stats, float partialTick,
            CallbackInfoReturnable<List<AuraRenderer.AuraLayer>> cir) {
        List<AuraRenderer.AuraLayer> layers = cir.getReturnValue();
        if (layers == null || layers.isEmpty()) return;

        var character = stats.getCharacter();
        String group = character.getActiveFormGroup();
        String form = character.getActiveForm();
        int alignment = stats.getResources().getAlignment();

        float[] innerOverride = DivineAlignmentColors.auraInnerOverride(group, form, alignment);
        float[] outerOverride = DivineAlignmentColors.auraOuterOverride(group, form, alignment);
        if (innerOverride != null || outerOverride != null) {
            for (AuraRenderer.AuraLayer layer : layers) {
                if (layer.layerId == 0 && innerOverride != null) layer.color = innerOverride;
                else if (layer.layerId == 2 && outerOverride != null) layer.color = outerOverride;
            }
        }

        if (!DivineConfig.AURA_COLOR_TRANSITION.get()) return;

        UUID id = player.getUUID();
        DivineAuraFade.Transition transition = DivineAuraFade.getActiveTransition(id);
        if (transition == null) return;

        if (transition.startTick < 0) transition.startTick = player.tickCount;

        int durationTicks = DivineConfig.AURA_COLOR_TRANSITION_TICKS.get();
        float elapsed = (player.tickCount - transition.startTick) + partialTick;
        float progress = Mth.clamp(elapsed / durationTicks, 0.0f, 1.0f);

        if (progress >= 1.0f) {
            DivineAuraFade.clearTransition(id);
            return;
        }

        float[] from = transition.fromColor;
        for (AuraRenderer.AuraLayer layer : layers) {
            if (layer.color == null || layer.color.length < 3) continue;
            layer.color = new float[]{
                    Mth.lerp(progress, from[0], layer.color[0]),
                    Mth.lerp(progress, from[1], layer.color[1]),
                    Mth.lerp(progress, from[2], layer.color[2])
            };
        }
    }
}
