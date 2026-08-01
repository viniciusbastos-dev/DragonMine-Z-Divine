package com.dmzdivine.client;

import com.dmzdivine.client.gui.GodRitualScreen;
import com.dmzdivine.client.gui.MeditationScreen;
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

    /**
     * One entry point for the whole meditation session: opens the screen on the first update, keeps
     * refreshing the same instance afterwards, and closes it when the server says the session ended.
     * Replacing the screen on every tick would drop the player's cursor and rebuild the widgets once
     * a second, so an existing screen is updated in place.
     */
    public static void meditationState(boolean active, int tpPerSecond, int sessionTp, int seconds) {
        Minecraft mc = Minecraft.getInstance();
        MeditationScreen open = mc.screen instanceof MeditationScreen screen ? screen : null;

        if (!active) {
            if (open != null) open.closeFromServer();
            return;
        }
        if (open != null) {
            open.update(tpPerSecond, sessionTp, seconds);
            return;
        }
        mc.setScreen(new MeditationScreen(tpPerSecond, sessionTp, seconds));
    }
}
