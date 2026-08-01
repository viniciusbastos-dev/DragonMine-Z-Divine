package com.dmzdivine.common.entity;

import com.dmzdivine.common.GodRitual;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;

/**
 * Beerus, on his own planet: the second way into the Super Saiyan God ritual, for players who
 * reached the planet without ever meeting the Goku master NPC. The ritual itself is unchanged -
 * {@link GodRitual#start} re-checks every prerequisite and messages the player.
 */
public class BeerusMasterEntity extends DivineMasterEntity {

    public BeerusMasterEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level, "beerus");
    }

    @Override
    protected void onInteract(ServerPlayer player) {
        GodRitual.start(player);
    }
}
