package com.dmzdivine.network.S2C;

import com.dmzdivine.client.ClientHooks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent by the server after it validated the god ritual prerequisites;
 * tells the client to open the ritual minigame screen.
 */
public class OpenGodRitualS2C {

    public OpenGodRitualS2C() {
    }

    public OpenGodRitualS2C(FriendlyByteBuf buf) {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientHooks::openGodRitualScreen);
        ctx.get().setPacketHandled(true);
    }
}
