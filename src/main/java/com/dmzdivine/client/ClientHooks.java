package com.dmzdivine.client;

import com.dmzdivine.client.gui.GodRitualScreen;
import net.minecraft.client.Minecraft;

/** Client-only entry points, kept out of common classes for dedicated-server safety. */
public final class ClientHooks {

    private ClientHooks() {
    }

    public static void openGodRitualScreen() {
        Minecraft.getInstance().setScreen(new GodRitualScreen());
    }
}
