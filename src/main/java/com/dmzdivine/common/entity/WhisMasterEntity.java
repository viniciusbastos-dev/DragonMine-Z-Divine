package com.dmzdivine.common.entity;

import com.dmzdivine.common.UltraInstinctTraining;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;

/**
 * Whis, standing at the mouth of his training arena. Right-clicking him opens his dialogue: the
 * Ultra Instinct trial (the only way to that skill now - see {@link UltraInstinctTraining}, which
 * checks how long the player actually trained inside the arena) and his weight bells.
 */
public class WhisMasterEntity extends DivineMasterEntity {

    public WhisMasterEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level, "whis");
    }

    @Override
    protected void onInteract(ServerPlayer player) {
        UltraInstinctTraining.openDialogue(player);
    }
}
