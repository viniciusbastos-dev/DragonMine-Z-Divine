package com.dmzdivine.network.C2S;

import com.dmzdivine.common.GodRitual;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent by the client when the ritual minigame ends (success, failure or the
 * screen being closed). The server only honors it for players it previously
 * marked as pending, and re-validates every prerequisite before granting.
 */
public class GodRitualResultC2S {

    private final boolean success;

    public GodRitualResultC2S(boolean success) {
        this.success = success;
    }

    public GodRitualResultC2S(FriendlyByteBuf buf) {
        this.success = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(this.success);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        GodRitual.handleResult(ctx.get().getSender(), this.success);
        ctx.get().setPacketHandled(true);
    }
}
