package com.dmzdivine.mixin.client;

import com.dmzdivine.client.DivineAlignmentColors;
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
 * Refreshes DivineAlignmentColors' "player currently being rendered" alignment once per player per
 * frame, at the single choke point every GeoRenderLayer (hair, race parts/tail, skin) funnels
 * through. Those layers, and FormConfig.FormData's own color getters, have no player context of
 * their own to check alignment with otherwise.
 */
@Mixin(value = DMZPlayerRenderer.class, remap = false)
public abstract class DMZPlayerRendererMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void dmzdivine$captureAlignment(AbstractClientPlayer entity, float entityYaw, float partialTick,
            PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, CallbackInfo ci) {
        var stats = StatsProvider.get(StatsCapability.INSTANCE, entity).orElse(null);
        DivineAlignmentColors.setCurrentAlignment(stats != null ? stats.getResources().getAlignment() : 100);
    }
}
