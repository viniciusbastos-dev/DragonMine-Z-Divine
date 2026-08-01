package com.dmzdivine.common.entity;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;

/**
 * Goku. He used to start the Super Saiyan God ritual; now that Beerus stands on his own planet, the
 * ritual is Beerus' alone and Goku only points the way - one door, on the planet you had to reach.
 * The entity is kept (worlds already have him placed, and he is the natural person to hear the
 * rumour from) rather than removed.
 *
 * Reuses MastersEntity's shape through {@link DivineMasterEntity} (non-collidable, look-at-player,
 * GeckoLib idle anim) rather than writing a new one, and registering it under the entity path
 * "master_goku" makes it render with the base mod's real Goku model/texture/animation for free:
 * MasterGlobalModel resolves those purely from the entity's registry path under the dragonminez:
 * namespace, regardless of which mod actually registered the entity type (see DivineEntities).
 */
public class GodMasterEntity extends DivineMasterEntity {

    public GodMasterEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level, "godmaster");
    }

    @Override
    protected void onInteract(ServerPlayer player) {
        player.sendSystemMessage(Component.translatable("message.dmzdivine.goku.seek_beerus")
                .withStyle(ChatFormatting.GOLD));
    }
}
