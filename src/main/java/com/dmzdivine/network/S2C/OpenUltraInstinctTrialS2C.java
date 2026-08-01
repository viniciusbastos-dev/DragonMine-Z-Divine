package com.dmzdivine.network.S2C;

import com.dmzdivine.client.ClientHooks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent by the server once Whis accepted to test the player; opens the Ultra Instinct
 * dodging trial on the client.
 */
public class OpenUltraInstinctTrialS2C {

    public OpenUltraInstinctTrialS2C() {
    }

    public OpenUltraInstinctTrialS2C(FriendlyByteBuf buf) {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientHooks::openUltraInstinctTrialScreen);
        ctx.get().setPacketHandled(true);
    }
}
