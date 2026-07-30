package com.dmzdivine.mixin.client;

import com.dmzdivine.client.DivineAlignmentColors;
import com.dragonminez.client.render.layer.DMZHairLayer;
import com.dragonminez.common.stats.character.Character;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Recolors hair for the evil-alignment Rose palette (see DivineAlignmentColors). getRgbForForm only
 * has the form's group/name, not the player - DivineAlignmentColors.getCurrentAlignment() is kept
 * fresh by DMZPlayerRendererMixin at the top of each player's render pass, so no separate capture is
 * needed here. The array returned may be DragonMineZ's own cached FormData.rgbHairColor, so this
 * replaces the return value rather than mutating it in place.
 */
@Mixin(value = DMZHairLayer.class, remap = false)
public abstract class DMZHairLayerMixin {

    @Inject(method = "getRgbForForm", at = @At("RETURN"), cancellable = true)
    private void dmzdivine$overrideHairColor(Character character, String group, String formName,
            CallbackInfoReturnable<float[]> cir) {
        float[] override = DivineAlignmentColors.hairOverride(group, formName, DivineAlignmentColors.getCurrentAlignment());
        if (override != null) cir.setReturnValue(override);
    }
}
