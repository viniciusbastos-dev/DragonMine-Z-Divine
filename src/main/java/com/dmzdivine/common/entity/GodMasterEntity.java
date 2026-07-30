package com.dmzdivine.common.entity;

import com.dmzdivine.common.GodRitual;
import com.dragonminez.common.init.entities.MastersEntity;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Stands in for the master who starts the Super Saiyan God ritual (GodRitual.start already does
 * every prerequisite check and messages the player itself). Right-click opens the ritual directly
 * instead of the base mod's quest dialogue, so it doesn't need any quest/dialogue data.
 *
 * Reuses MastersEntity's shape (non-collidable, look-at-player, GeckoLib idle anim) rather than
 * writing a new one, and registering it under the entity path "master_goku" makes it render with
 * the base mod's real Goku model/texture/animation for free: MasterGlobalModel resolves those purely
 * from the entity's registry path under the dragonminez: namespace, regardless of which mod actually
 * registered the entity type (see DivineEntities).
 */
public class GodMasterEntity extends MastersEntity {

    public GodMasterEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.masterName = "godmaster";
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (this.level().isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }

        StatsProvider.get(StatsCapability.INSTANCE, serverPlayer).ifPresent(data -> {
            if (!data.getStatus().isHasCreatedCharacter()) {
                serverPlayer.displayClientMessage(
                        Component.translatable("gui.dragonminez.lines.generic.createcharacter"), true);
                return;
            }
            GodRitual.start(serverPlayer);
        });
        return InteractionResult.SUCCESS;
    }
}
