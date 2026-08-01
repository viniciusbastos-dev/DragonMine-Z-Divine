package com.dmzdivine.mixin;

import com.dmzdivine.server.MeditationTraining;
import com.dragonminez.common.config.TrainingConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Teaches the base mod's training config about the meditation entry.
 *
 * <p>{@code TrainingConfig} is a fixed set of Java fields - rhythm, control, memory, precision,
 * gravity - with no map an addon could add a key to, and {@code getSettings} answers anything it does
 * not recognise with the rhythm config. Left alone, meditation would inherit Popo's settings, and the
 * minigame screen would tell players to go learn it from Popo.
 *
 * <p>Only the two fields that are actually read for our entry matter: {@code masterName}, for the
 * "learn this from X" hint, and {@code unlockedByDefault}, which has to stay false so the entry is
 * greyed out until Whis teaches it. The reward fields are never consulted - meditation pays per
 * second from {@link MeditationTraining}, not per cleared level.
 */
@Mixin(value = TrainingConfig.class, remap = false)
public abstract class TrainingConfigMixin {

    /** Its own subclass, because MinigameSettings' fields are protected with no setters. */
    private static final class MeditationSettings extends TrainingConfig.MinigameSettings {
        {
            masterName = "whis";
            unlockedByDefault = false;
        }
    }

    private static final MeditationSettings DMZDIVINE_MEDITATION = new MeditationSettings();

    @Inject(method = "getSettings", at = @At("HEAD"), cancellable = true)
    private void dmzdivine$meditationSettings(String minigameId, CallbackInfoReturnable<TrainingConfig.MinigameSettings> cir) {
        if (MeditationTraining.MINIGAME_ID.equalsIgnoreCase(minigameId)) {
            cir.setReturnValue(DMZDIVINE_MEDITATION);
        }
    }
}
