package com.dmzdivine.network.S2C;

import com.dmzdivine.client.ClientHooks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Opens Whis' dialogue. Carries the numbers the screen shows, all resolved server-side, so the client
 * never has to guess how much training it has banked or what the next trial costs.
 */
public class OpenWhisDialogueS2C {

    private final int trainedMinutes;
    private final int requiredMinutes;
    private final int ultraInstinctLevel;
    private final int tpCost;

    public OpenWhisDialogueS2C(int trainedMinutes, int requiredMinutes, int ultraInstinctLevel, int tpCost) {
        this.trainedMinutes = trainedMinutes;
        this.requiredMinutes = requiredMinutes;
        this.ultraInstinctLevel = ultraInstinctLevel;
        this.tpCost = tpCost;
    }

    public OpenWhisDialogueS2C(FriendlyByteBuf buf) {
        this.trainedMinutes = buf.readVarInt();
        this.requiredMinutes = buf.readVarInt();
        this.ultraInstinctLevel = buf.readVarInt();
        this.tpCost = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(trainedMinutes);
        buf.writeVarInt(requiredMinutes);
        buf.writeVarInt(ultraInstinctLevel);
        buf.writeVarInt(tpCost);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ClientHooks.openWhisDialogue(trainedMinutes, requiredMinutes, ultraInstinctLevel, tpCost));
        ctx.get().setPacketHandled(true);
    }
}
