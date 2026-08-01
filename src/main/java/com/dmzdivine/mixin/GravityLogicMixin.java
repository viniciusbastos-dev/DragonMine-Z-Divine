package com.dmzdivine.mixin;

import com.dmzdivine.DivineConfig;
import com.dmzdivine.common.world.BeerusPlanet;
import com.dragonminez.server.util.GravityLogic;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Raises gravity inside Whis' arena on Beerus' planet.
 *
 * <p>The base mod's gravity is per dimension ({@code gravityPerWorld} in general-server.json), plus
 * gravity devices, plus WorldGuard regions - there is no hook for "this area of this world", and
 * the planet's hub has to stay walkable for the NPCs to be reachable. So the addon answers inside
 * {@code computeGravity} itself, which is the single point every consumer goes through: penalties,
 * the training TP bonus, flight, stat reduction and the HUD all read it.
 *
 * <p>Deliberately a floor and not an addition: whatever the base mod already computed (planet
 * ambient gravity, a gravity device someone hauled in, a Kaio nearby) wins if it is higher.
 */
@Mixin(value = GravityLogic.class, remap = false)
public abstract class GravityLogicMixin {

    @Inject(method = "computeGravity", at = @At("RETURN"), cancellable = true)
    private static void dmzdivine$trainingArenaGravity(Player player,
                                                       boolean includeDimension,
                                                       CallbackInfoReturnable<Double> cir) {
        if (!BeerusPlanet.isInTrainingArena(player)) return;

        double arena = DivineConfig.ARENA_GRAVITY.get();
        Double current = cir.getReturnValue();
        if (current == null || arena > current) {
            cir.setReturnValue(arena);
        }
    }
}
