package com.dmzdivine.network.C2S;

import com.dmzdivine.common.UltraInstinctTraining;
import com.dmzdivine.common.WhisServices;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * A button press in Whis' dialogue. Both actions re-validate on the server: the trial goes through
 * {@link UltraInstinctTraining#talk} (training time, TP, current level) and the weights through
 * {@link WhisServices#giveWeights} (Whis actually being there).
 */
public class WhisActionC2S {

    public static final int ACTION_TRIAL = 0;
    public static final int ACTION_WEIGHTS = 1;

    private final int action;
    private final int value;

    public WhisActionC2S(int action, int value) {
        this.action = action;
        this.value = value;
    }

    public WhisActionC2S(FriendlyByteBuf buf) {
        this.action = buf.readVarInt();
        this.value = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(action);
        buf.writeVarInt(value);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null) {
            switch (action) {
                case ACTION_TRIAL -> {
                    if (WhisServices.isWhisInRange(player)) UltraInstinctTraining.talk(player);
                }
                case ACTION_WEIGHTS -> WhisServices.giveWeights(player, value);
                default -> {
                }
            }
        }
        ctx.get().setPacketHandled(true);
    }
}
