package com.dmzdivine.network.S2C;

import com.dmzdivine.client.ClientHooks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * The whole life of a meditation session, in one packet: it opens the screen, refreshes its numbers
 * once a second, and closes it when the server ends the session for any reason (a hit, leaving the
 * arena, the form dropping). One packet instead of three because the client must never be able to
 * disagree with the server about whether it is meditating.
 *
 * <p>{@code tpPerSecond} is the amount actually credited - the base mod's multipliers, the arena's
 * included, are resolved server-side before it is sent.
 */
public class MeditationStateS2C {

    private final boolean active;
    private final int tpPerSecond;
    private final int sessionTp;
    private final int seconds;

    public MeditationStateS2C(boolean active, int tpPerSecond, int sessionTp, int seconds) {
        this.active = active;
        this.tpPerSecond = tpPerSecond;
        this.sessionTp = sessionTp;
        this.seconds = seconds;
    }

    public MeditationStateS2C(FriendlyByteBuf buf) {
        this.active = buf.readBoolean();
        this.tpPerSecond = buf.readVarInt();
        this.sessionTp = buf.readVarInt();
        this.seconds = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(active);
        buf.writeVarInt(tpPerSecond);
        buf.writeVarInt(sessionTp);
        buf.writeVarInt(seconds);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ClientHooks.meditationState(active, tpPerSecond, sessionTp, seconds));
        ctx.get().setPacketHandled(true);
    }
}
