package com.dmzdivine.mixin.client;

import com.dmzdivine.network.C2S.MeditationActionC2S;
import com.dmzdivine.network.DivineNetwork;
import com.dmzdivine.server.MeditationTraining;
import com.dragonminez.client.gui.character.MinigamesScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;

/**
 * Puts meditation in the base mod's training list instead of hiding it behind a keybind only.
 *
 * <p>The screen keeps its minigames in a {@code private static final String[]} and dispatches Play
 * through a switch over the same ids, so there is no registry to add to - the list is extended in
 * {@code <clinit>} and the Play button is intercepted for our id. Everything else the screen does for
 * the entry already works without touching it: the name and description come from lang keys built
 * from the id, {@code hasAccess} reads {@code Character.isMinigameKnown} (which is what Whis writes
 * when he teaches it) plus {@code unlockedByDefault}, and the "learn from" hint reads the master name
 * - both answered for our id by {@link com.dmzdivine.mixin.TrainingConfigMixin}.
 *
 * <p>Play only asks: the session lives on the server, which re-checks the arena and the form before
 * opening anything (see {@link MeditationTraining}).
 */
@Mixin(value = MinigamesScreen.class, remap = false)
public abstract class MinigamesScreenMixin {

    @Shadow
    @Final
    @Mutable
    private static String[] MINIGAMES;

    @Shadow
    private int selectedIndex;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void dmzdivine$addMeditation(CallbackInfo ci) {
        for (String id : MINIGAMES) {
            if (MeditationTraining.MINIGAME_ID.equals(id)) return;
        }
        String[] extended = Arrays.copyOf(MINIGAMES, MINIGAMES.length + 1);
        extended[extended.length - 1] = MeditationTraining.MINIGAME_ID;
        MINIGAMES = extended;
    }

    @Inject(method = "playSelected", at = @At("HEAD"), cancellable = true)
    private void dmzdivine$playMeditation(CallbackInfo ci) {
        if (selectedIndex < 0 || selectedIndex >= MINIGAMES.length) return;
        if (!MeditationTraining.MINIGAME_ID.equals(MINIGAMES[selectedIndex])) return;

        DivineNetwork.sendToServer(new MeditationActionC2S(true));
        ci.cancel();
    }
}
