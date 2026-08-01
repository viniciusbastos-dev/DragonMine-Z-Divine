package com.dmzdivine.common;

import com.dmzdivine.common.entity.WhisMasterEntity;
import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.init.item.WeightItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Whis' non-trial services. Right now that is the weight bells: the same
 * {@code workout_weights} King Kai hands out, because gravity training and weighted training are the
 * same lesson here and the arena is where you would want them.
 *
 * <p>Not routed through the base mod's {@code NPCActionC2S}: that packet resolves its service by NPC
 * name and validates that an NPC with that exact name stands within 8 blocks, so asking it for King
 * Kai's weights while standing next to Whis is rejected. The item and the weight tag are the base
 * mod's own API, so the result is identical to King Kai's.
 */
public final class WhisServices {

    /** Same ceiling the base mod puts on its own weight requests. */
    private static final int MAX_WEIGHT = 100000;
    private static final double INTERACTION_RANGE = 8.0;

    private WhisServices() {
    }

    public static void giveWeights(ServerPlayer player, int requested) {
        if (!isWhisInRange(player)) return;

        int weight = Math.max(1, Math.min(MAX_WEIGHT, requested));
        ItemStack stack = new ItemStack(MainItems.WORKOUT_WEIGHTS.get());
        WeightItem.setWeight(stack, weight);

        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
        player.sendSystemMessage(Component.translatable("message.dmzdivine.whis.weights_given", weight)
                .withStyle(ChatFormatting.AQUA));
    }

    /** Guards every Whis service: the screen is client-driven, so the server re-checks he is there. */
    public static boolean isWhisInRange(ServerPlayer player) {
        return !player.serverLevel().getEntitiesOfClass(WhisMasterEntity.class,
                player.getBoundingBox().inflate(INTERACTION_RANGE)).isEmpty();
    }
}
