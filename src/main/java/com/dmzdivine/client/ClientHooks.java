package com.dmzdivine.client;

import com.dmzdivine.client.gui.GodRitualScreen;
import com.dmzdivine.client.gui.UltraInstinctTrialScreen;
import com.dmzdivine.client.gui.WhisDialogueScreen;
import net.minecraft.client.Minecraft;

/** Client-only entry points, kept out of common classes for dedicated-server safety. */
public final class ClientHooks {

    private ClientHooks() {
    }

    public static void openGodRitualScreen() {
        Minecraft.getInstance().setScreen(new GodRitualScreen());
    }

    public static void openUltraInstinctTrialScreen() {
        Minecraft.getInstance().setScreen(new UltraInstinctTrialScreen());
    }

    public static void openWhisDialogue(int trainedMinutes, int requiredMinutes, int ultraInstinctLevel, int tpCost) {
        Minecraft.getInstance().setScreen(
                new WhisDialogueScreen(trainedMinutes, requiredMinutes, ultraInstinctLevel, tpCost));
    }
}
