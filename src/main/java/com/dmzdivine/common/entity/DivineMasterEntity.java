package com.dmzdivine.common.entity;

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
 * Shared shape for the addon's teaching NPCs: reuses {@link MastersEntity} (non-collidable,
 * look-at-player, GeckoLib idle animation) and turns a right-click into a single server-side
 * callback, after the same "have you made a character yet" check the base mod does.
 *
 * <p>Registering each subclass under an entity path the base mod's {@code MasterGlobalModel}
 * recognises is what gives them a model for free: it resolves geo/texture/animation purely from
 * the entity's registry path under the {@code dragonminez:} namespace, regardless of which mod
 * registered the type (see {@link com.dmzdivine.common.init.DivineEntities}).
 */
public abstract class DivineMasterEntity extends MastersEntity {

    protected DivineMasterEntity(EntityType<? extends PathfinderMob> type, Level level, String masterName) {
        super(type, level);
        this.masterName = masterName;
        this.setPersistenceRequired();
    }

    /** Called on the server when a player right-clicks this master with a valid character. */
    protected abstract void onInteract(ServerPlayer player);

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
            onInteract(serverPlayer);
        });
        return InteractionResult.SUCCESS;
    }
}
