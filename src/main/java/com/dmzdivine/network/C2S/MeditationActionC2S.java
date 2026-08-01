package com.dmzdivine.network.C2S;

import com.dmzdivine.server.MeditationTraining;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Opening or closing the meditation screen. The screen is the session: the client asks to start when
 * the player opens it and to stop when it closes, and {@link MeditationTraining} decides whether the
 * start is allowed - it is the server that pays the Training Points out.
 *
 * <p>Explicit start/stop rather than a toggle, because the server can end a session on its own (a
 * hit, leaving the arena, dropping the form) and a toggle would then mean the opposite of what the
 * client thought it meant.
 */
public class MeditationActionC2S {

    private final boolean start;

    public MeditationActionC2S(boolean start) {
        this.start = start;
    }

    public MeditationActionC2S(FriendlyByteBuf buf) {
        this.start = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(start);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null) {
            if (start) MeditationTraining.requestStart(player);
            else MeditationTraining.requestStop(player);
        }
        ctx.get().setPacketHandled(true);
    }
}
