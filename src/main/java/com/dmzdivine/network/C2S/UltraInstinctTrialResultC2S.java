package com.dmzdivine.network.C2S;

import com.dmzdivine.common.UltraInstinctTraining;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent by the client when Whis' trial ends (passed, failed or the screen being closed).
 * Honored only for players the server marked as pending, and every prerequisite is
 * re-validated before anything is granted.
 */
public class UltraInstinctTrialResultC2S {

    private final boolean success;

    public UltraInstinctTrialResultC2S(boolean success) {
        this.success = success;
    }

    public UltraInstinctTrialResultC2S(FriendlyByteBuf buf) {
        this.success = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(this.success);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        UltraInstinctTraining.handleResult(ctx.get().getSender(), this.success);
        ctx.get().setPacketHandled(true);
    }
}
