package com.dmzdivine.mixin.client;

import com.dragonminez.client.events.FlySkillEvent;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * FlySkillEvent.handleHovering sinks an idle hovering player at a fixed -0.02 blocks/tick
 * regardless of fly skill level - the constant isn't scaled by level anywhere, and there's no
 * DMZEvent around flight movement to hook instead. Divine players who maxed the fly skill (level
 * 10) should be able to hold a perfectly still hover, so this zeroes the descent once maxed and
 * leaves every other level's sink rate untouched.
 *
 * yMovement is declared double, so javac widens the SLOW_DESCENT_RATE float constant to double
 * at compile time and bakes it straight into the bytecode as `ldc2_w -0.019999999552965164d` -
 * the imprecise double value you get from widening the float -0.02F, not a clean -0.02D. The
 * modifier has to match that exact widened literal or the injection silently finds 0 targets.
 */
@Mixin(value = FlySkillEvent.class, remap = false)
public abstract class FlyHoverDecayMixin {

    @ModifyConstant(method = "handleHovering", constant = @Constant(doubleValue = -0.019999999552965164D))
    private static double dmzdivine$zeroHoverDecayAtMaxFly(double original) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return original;

        int flyLevel = StatsProvider.get(StatsCapability.INSTANCE, player)
                .map(data -> data.getSkills().getSkillLevel("fly"))
                .orElse(0);

        return flyLevel >= 10 ? 0.0D : original;
    }
}
