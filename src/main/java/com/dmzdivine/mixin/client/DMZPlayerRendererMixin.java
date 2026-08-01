package com.dmzdivine.mixin.client;

import com.dmzdivine.client.DivineAlignmentColors;
import com.dmzdivine.client.DivineRaceModels;
import com.dragonminez.client.render.DMZPlayerRenderer;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Refreshes the "player currently being rendered" facts once per player per frame, at the single
 * choke point every GeoRenderLayer (hair, race parts/tail, skin) funnels through: their alignment,
 * for DivineAlignmentColors, and their race and active stack form, for DivineRaceModels. Those
 * layers, and FormConfig.FormData's own getters, have no player context of their own otherwise.
 */
@Mixin(value = DMZPlayerRenderer.class, remap = false)
public abstract class DMZPlayerRendererMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void dmzdivine$captureRenderContext(AbstractClientPlayer entity, float entityYaw, float partialTick,
            PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, CallbackInfo ci) {
        var stats = StatsProvider.get(StatsCapability.INSTANCE, entity).orElse(null);
        DivineAlignmentColors.setCurrentAlignment(stats != null ? stats.getResources().getAlignment() : 100);

        if (stats == null) {
            DivineRaceModels.setCurrent(null, null);
            return;
        }
        var character = stats.getCharacter();
        DivineRaceModels.setCurrent(character.getRaceName(),
                character.hasActiveStackForm() ? character.getActiveStackFormGroup() : null);
    }
}
