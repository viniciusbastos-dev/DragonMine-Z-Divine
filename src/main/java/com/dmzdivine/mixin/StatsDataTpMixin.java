package com.dmzdivine.mixin;

import com.dmzdivine.DivineConfig;
import com.dmzdivine.common.world.BeerusPlanet;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes Whis' arena behave exactly like the Hyperbolic Time Chamber for Training Points.
 *
 * <p>An earlier version multiplied {@code TPGainEvent} instead. That did award the right amount, but
 * it was invisible: the character screen builds its "TP multiplier" breakdown from the base mod's own
 * getters ({@code CharacterStatsScreen} reads {@code getTpHTCMultiplier} directly), and the base mod
 * only counts a boost source at all when it is listed in {@code gameplay.tpGainBoosts} - which the
 * event bypassed. Answering inside the getter puts the arena in both the math and the UI, and it
 * respects the per-source boost lists like every other bonus.
 */
@Mixin(value = StatsData.class, remap = false)
public abstract class StatsDataTpMixin {

    @Shadow
    private Player player;

    @Inject(method = "getTpHTCMultiplier", at = @At("RETURN"), cancellable = true)
    private void dmzdivine$arenaTpBonus(CallbackInfoReturnable<Double> cir) {
        if (!BeerusPlanet.isInTrainingArena(player)) return;

        double arena = DivineConfig.ARENA_TP_MULTIPLIER.get();
        Double current = cir.getReturnValue();
        if (current == null || arena > current) {
            cir.setReturnValue(arena);
        }
    }
}
