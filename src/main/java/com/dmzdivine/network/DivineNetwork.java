package com.dmzdivine.network;

import com.dmzdivine.DMZDivine;
import com.dmzdivine.network.C2S.GodRitualResultC2S;
import com.dmzdivine.network.C2S.UltraInstinctTrialResultC2S;
import com.dmzdivine.network.C2S.WhisActionC2S;
import com.dmzdivine.network.S2C.OpenGodRitualS2C;
import com.dmzdivine.network.S2C.OpenUltraInstinctTrialS2C;
import com.dmzdivine.network.S2C.OpenWhisDialogueS2C;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class DivineNetwork {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(DMZDivine.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    // Packet ids are assigned by registration order - append new packets at the end.
    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(OpenGodRitualS2C.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenGodRitualS2C::encode)
                .decoder(OpenGodRitualS2C::new)
                .consumerMainThread(OpenGodRitualS2C::handle)
                .add();
        CHANNEL.messageBuilder(GodRitualResultC2S.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(GodRitualResultC2S::encode)
                .decoder(GodRitualResultC2S::new)
                .consumerMainThread(GodRitualResultC2S::handle)
                .add();
        CHANNEL.messageBuilder(OpenUltraInstinctTrialS2C.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenUltraInstinctTrialS2C::encode)
                .decoder(OpenUltraInstinctTrialS2C::new)
                .consumerMainThread(OpenUltraInstinctTrialS2C::handle)
                .add();
        CHANNEL.messageBuilder(UltraInstinctTrialResultC2S.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(UltraInstinctTrialResultC2S::encode)
                .decoder(UltraInstinctTrialResultC2S::new)
                .consumerMainThread(UltraInstinctTrialResultC2S::handle)
                .add();
        CHANNEL.messageBuilder(OpenWhisDialogueS2C.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenWhisDialogueS2C::encode)
                .decoder(OpenWhisDialogueS2C::new)
                .consumerMainThread(OpenWhisDialogueS2C::handle)
                .add();
        CHANNEL.messageBuilder(WhisActionC2S.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(WhisActionC2S::encode)
                .decoder(WhisActionC2S::new)
                .consumerMainThread(WhisActionC2S::handle)
                .add();
    }

    public static void sendTo(ServerPlayer player, Object message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static void sendToServer(Object message) {
        CHANNEL.sendToServer(message);
    }
}
