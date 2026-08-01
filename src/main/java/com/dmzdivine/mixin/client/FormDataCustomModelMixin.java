package com.dmzdivine.mixin.client;

import com.dmzdivine.client.DivineRaceModels;
import com.dragonminez.common.config.FormConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gives Ultra Instinct Mastered a body per race instead of the one its shared JSON can name.
 *
 * <p>{@code customModel} is read from three places on the render path - the model itself
 * ({@code DMZPlayerModel#getModelResource}), the skin layers ({@code SkinGathererProvider}) and the
 * texture index ({@code TextureCounter}) - and all three go through this one getter, so overriding it
 * keeps model and textures agreeing with each other. {@link DivineRaceModels} decides, and answers
 * null for every form but this one.
 *
 * <p>Note this only fires while the JSON value is non-empty: both callers check
 * {@code hasCustomModel()} first, and that reads the field directly. Emptying customModel in
 * forms/ultrainstinct.json therefore turns the per-race override off as well.
 */
@Mixin(value = FormConfig.FormData.class, remap = false)
public abstract class FormDataCustomModelMixin {

    @Shadow
    public abstract String getName();

    @Inject(method = "getCustomModel", at = @At("RETURN"), cancellable = true)
    private void dmzdivine$raceBody(CallbackInfoReturnable<String> cir) {
        String override = DivineRaceModels.masteredBodyOverride(getName());
        if (override != null) cir.setReturnValue(override);
    }
}
