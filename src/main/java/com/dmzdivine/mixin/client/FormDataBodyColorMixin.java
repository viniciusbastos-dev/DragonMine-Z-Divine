package com.dmzdivine.mixin.client;

import com.dmzdivine.client.DivineAlignmentColors;
import com.dragonminez.common.config.FormConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Recolors everything the face/body render layers read straight off FormData for the evil-alignment
 * Rose palette: the tail and face layer's secondary tint (bodyColor2, mirrors hair), the eyebrow
 * texture (DMZSkinLayer#renderHumanFace tints it with FormData.getRgbHairColor() directly, bypassing
 * DMZHairLayer's getRgbForForm entirely - that one's covered by DMZHairLayerMixin, this one isn't),
 * and both eye layers. FormData is a single shared instance per form (not per-player), so none of
 * these can mutate the cached array in place; each replaces the return value instead, gated by
 * DivineAlignmentColors.getCurrentAlignment() which DMZPlayerRendererMixin keeps fresh per rendered
 * player.
 */
@Mixin(value = FormConfig.FormData.class, remap = false)
public abstract class FormDataBodyColorMixin {

    @Shadow
    public abstract String getName();

    @Inject(method = "getRgbBodyColor2", at = @At("RETURN"), cancellable = true)
    private void dmzdivine$overrideBodyColor2(CallbackInfoReturnable<float[]> cir) {
        float[] override = DivineAlignmentColors.bodyColor2Override(getName(), DivineAlignmentColors.getCurrentAlignment());
        if (override != null) cir.setReturnValue(override);
    }

    @Inject(method = "getRgbHairColor", at = @At("RETURN"), cancellable = true)
    private void dmzdivine$overrideHairColor(CallbackInfoReturnable<float[]> cir) {
        float[] override = DivineAlignmentColors.hairOverride(null, getName(), DivineAlignmentColors.getCurrentAlignment());
        if (override != null) cir.setReturnValue(override);
    }

    @Inject(method = "getRgbEye1Color", at = @At("RETURN"), cancellable = true)
    private void dmzdivine$overrideEye1Color(CallbackInfoReturnable<float[]> cir) {
        float[] override = DivineAlignmentColors.eyeOverride(getName(), DivineAlignmentColors.getCurrentAlignment());
        if (override != null) cir.setReturnValue(override);
    }

    @Inject(method = "getRgbEye2Color", at = @At("RETURN"), cancellable = true)
    private void dmzdivine$overrideEye2Color(CallbackInfoReturnable<float[]> cir) {
        float[] override = DivineAlignmentColors.eyeOverride(getName(), DivineAlignmentColors.getCurrentAlignment());
        if (override != null) cir.setReturnValue(override);
    }
}
