package com.dmzdivine.client.gui;

import com.dmzdivine.network.C2S.MeditationActionC2S;
import com.dmzdivine.network.DivineNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The meditation session, as a screen the player sits in - the same shape as the base mod's training
 * minigames, except that the game here is doing nothing at all.
 *
 * <p>Being a screen is the mechanic, not decoration: an open screen swallows movement input, so the
 * body stays still on its own, and the player can genuinely walk away from the keyboard. It reports
 * nothing and decides nothing - the server runs the session and pushes every number through
 * {@link com.dmzdivine.network.S2C.MeditationStateS2C}. Closing it is the one thing the client does
 * decide, and that only ever ends a session.
 *
 * <p>{@link #isPauseScreen()} must stay false: a pause screen would stop the singleplayer world, and
 * with it the very ticks the session is counting.
 */
public class MeditationScreen extends Screen {

    private static final int PANEL_WIDTH = 220;
    private static final int PANEL_HEIGHT = 132;

    private static final int COLOR_PANEL = 0xC0080D14;
    private static final int COLOR_PANEL_BORDER = 0xFF8FD8F5;
    private static final int COLOR_TITLE = 0x8FD8F5;
    private static final int COLOR_RATE = 0x9BE8B0;
    private static final int COLOR_TEXT = 0xD8E8F0;
    private static final int COLOR_HINT = 0x8F9FAF;

    /** True once the server has told us the session ended, so onClose does not report it back. */
    private boolean serverClosed;

    private int tpPerSecond;
    private int sessionTp;
    private int seconds;

    public MeditationScreen(int tpPerSecond, int sessionTp, int seconds) {
        super(Component.translatable("gui.dmzdivine.meditation.title"));
        update(tpPerSecond, sessionTp, seconds);
    }

    public void update(int tpPerSecond, int sessionTp, int seconds) {
        this.tpPerSecond = tpPerSecond;
        this.sessionTp = sessionTp;
        this.seconds = seconds;
    }

    /** The server ended the session: close without sending a stop it already knows about. */
    public void closeFromServer() {
        this.serverClosed = true;
        onClose();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int panelTop = this.height / 2 - PANEL_HEIGHT / 2;

        addRenderableWidget(Button.builder(Component.translatable("gui.dmzdivine.meditation.stop"), b -> onClose())
                .bounds(centerX - 55, panelTop + PANEL_HEIGHT - 30, 110, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        int centerX = this.width / 2;
        int panelLeft = centerX - PANEL_WIDTH / 2;
        int panelTop = this.height / 2 - PANEL_HEIGHT / 2;

        graphics.fill(panelLeft - 1, panelTop - 1, panelLeft + PANEL_WIDTH + 1, panelTop + PANEL_HEIGHT + 1, COLOR_PANEL_BORDER);
        graphics.fill(panelLeft, panelTop, panelLeft + PANEL_WIDTH, panelTop + PANEL_HEIGHT, COLOR_PANEL);

        graphics.drawCenteredString(this.font, this.title, centerX, panelTop + 12, COLOR_TITLE);
        graphics.drawCenteredString(this.font,
                Component.translatable("gui.dmzdivine.meditation.rate", tpPerSecond),
                centerX, panelTop + 36, COLOR_RATE);
        graphics.drawCenteredString(this.font,
                Component.translatable("gui.dmzdivine.meditation.session", sessionTp, formatTime(seconds)),
                centerX, panelTop + 54, COLOR_TEXT);
        graphics.drawCenteredString(this.font,
                Component.translatable("gui.dmzdivine.meditation.hint"),
                centerX, panelTop + 78, COLOR_HINT);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private static String formatTime(int totalSeconds) {
        return String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60);
    }

    @Override
    public void onClose() {
        if (!serverClosed) {
            DivineNetwork.sendToServer(new MeditationActionC2S(false));
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
