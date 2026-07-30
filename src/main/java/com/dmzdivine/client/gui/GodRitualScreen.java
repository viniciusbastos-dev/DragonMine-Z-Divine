package com.dmzdivine.client.gui;

import com.dmzdivine.DivineConfig;
import com.dmzdivine.network.DivineNetwork;
import com.dmzdivine.network.C2S.GodRitualResultC2S;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.lwjgl.glfw.GLFW;

/**
 * The Super Saiyan God ritual minigame: five righteous Saiyans channel their ki
 * into the player. A cursor sweeps across a bar; the player must press SPACE
 * (or click) while it is inside the divine zone. Each hit lights one ki orb,
 * shrinks the zone and speeds up the cursor. Too many misses and the ritual fails.
 *
 * The minigame itself is client-side (same trust model as the base mod's
 * Ultimate ritual); the server re-validates prerequisites when the result packet
 * arrives, and only for players it marked as pending.
 */
public class GodRitualScreen extends Screen {

    private static final int BAR_WIDTH = 220;
    private static final int BAR_HEIGHT = 16;

    private static final int COLOR_PANEL = 0xC0100810;
    private static final int COLOR_PANEL_BORDER = 0xFFE9403D;
    private static final int COLOR_BAR_BG = 0xFF1D1D28;
    private static final int COLOR_BAR_BORDER = 0xFF6E6E85;
    private static final int COLOR_ZONE = 0xFFE9403D;
    private static final int COLOR_ZONE_CORE = 0xFFFFB74D;
    private static final int COLOR_CURSOR = 0xFFFFFFFF;
    private static final int COLOR_ORB_LIT = 0xFFFFB74D;
    private static final int COLOR_ORB_OFF = 0xFF3A3A4A;

    private final RandomSource random = RandomSource.create();

    private final int requiredSuccesses = DivineConfig.MINIGAME_REQUIRED_SUCCESSES.get();
    private final int allowedMisses = DivineConfig.MINIGAME_ALLOWED_MISSES.get();
    private final double speedIncrease = DivineConfig.MINIGAME_SPEED_INCREASE.get();
    private final double zoneShrink = DivineConfig.MINIGAME_ZONE_SHRINK.get();
    private final double minZoneWidth = DivineConfig.MINIGAME_MIN_ZONE_WIDTH.get();

    private double speed = DivineConfig.MINIGAME_CURSOR_SPEED.get();
    private double zoneWidth = Math.max(DivineConfig.MINIGAME_ZONE_WIDTH.get(), DivineConfig.MINIGAME_MIN_ZONE_WIDTH.get());
    private double zoneStart;

    private double cursor;      // 0..100, percent of the bar
    private double prevCursor;  // for render interpolation
    private int direction = 1;

    private int successes;
    private int misses;

    private boolean finished;
    private boolean won;
    private boolean resultSent;
    private int closeTicks = -1;

    public GodRitualScreen() {
        super(Component.translatable("gui.dmzdivine.ritual.title"));
        placeZone();
    }

    private void placeZone() {
        double max = 100.0 - zoneWidth - 4.0;
        zoneStart = 4.0 + random.nextDouble() * Math.max(1.0, max - 4.0);
    }

    @Override
    public void tick() {
        if (finished) {
            if (closeTicks > 0 && --closeTicks == 0) {
                Minecraft.getInstance().setScreen(null);
            }
            return;
        }
        prevCursor = cursor;
        cursor += speed * direction;
        if (cursor >= 100.0) {
            cursor = 100.0 - (cursor - 100.0);
            direction = -1;
        } else if (cursor <= 0.0) {
            cursor = -cursor;
            direction = 1;
        }
    }

    private void attempt() {
        if (finished) return;

        if (cursor >= zoneStart && cursor <= zoneStart + zoneWidth) {
            successes++;
            playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.9F + successes * 0.08F);
            if (successes >= requiredSuccesses) {
                finish(true);
            } else {
                speed += speedIncrease;
                zoneWidth = Math.max(minZoneWidth, zoneWidth - zoneShrink);
                placeZone();
            }
        } else {
            misses++;
            playSound(SoundEvents.NOTE_BLOCK_BASS.value(), 0.6F);
            if (misses > allowedMisses) {
                finish(false);
            }
        }
    }

    private void finish(boolean success) {
        finished = true;
        won = success;
        closeTicks = success ? 50 : 40;
        sendResult(success);
        playSound(success ? SoundEvents.TOTEM_USE : SoundEvents.FIRE_EXTINGUISH, 1.0F);
    }

    private void sendResult(boolean success) {
        if (resultSent) return;
        resultSent = true;
        DivineNetwork.sendToServer(new GodRitualResultC2S(success));
    }

    private void playSound(net.minecraft.sounds.SoundEvent sound, float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_SPACE || keyCode == GLFW.GLFW_KEY_ENTER) {
            attempt();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            attempt();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        int centerX = this.width / 2;
        int panelWidth = BAR_WIDTH + 40;
        int panelHeight = 128;
        int panelLeft = centerX - panelWidth / 2;
        int panelTop = this.height / 2 - panelHeight / 2;

        graphics.fill(panelLeft - 1, panelTop - 1, panelLeft + panelWidth + 1, panelTop + panelHeight + 1, COLOR_PANEL_BORDER);
        graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, COLOR_PANEL);

        graphics.drawCenteredString(this.font, this.title, centerX, panelTop + 10, 0xFFD75C);

        // Ki orbs - one per required success, lore of the five righteous Saiyans
        int orbSize = 8;
        int orbSpacing = 14;
        int orbsLeft = centerX - (requiredSuccesses * orbSpacing - (orbSpacing - orbSize)) / 2;
        int orbY = panelTop + 28;
        for (int i = 0; i < requiredSuccesses; i++) {
            int x = orbsLeft + i * orbSpacing;
            int color = i < successes ? COLOR_ORB_LIT : COLOR_ORB_OFF;
            graphics.fill(x + 2, orbY, x + orbSize - 2, orbY + orbSize, color);
            graphics.fill(x, orbY + 2, x + orbSize, orbY + orbSize - 2, color);
        }

        // Bar
        int barLeft = centerX - BAR_WIDTH / 2;
        int barTop = panelTop + 52;
        graphics.fill(barLeft - 2, barTop - 2, barLeft + BAR_WIDTH + 2, barTop + BAR_HEIGHT + 2, COLOR_BAR_BORDER);
        graphics.fill(barLeft, barTop, barLeft + BAR_WIDTH, barTop + BAR_HEIGHT, COLOR_BAR_BG);

        // Divine zone (with a brighter core at its center third)
        int zoneLeft = barLeft + (int) Math.round(zoneStart / 100.0 * BAR_WIDTH);
        int zoneRight = barLeft + (int) Math.round((zoneStart + zoneWidth) / 100.0 * BAR_WIDTH);
        graphics.fill(zoneLeft, barTop, zoneRight, barTop + BAR_HEIGHT, COLOR_ZONE);
        int core = Math.max(1, (zoneRight - zoneLeft) / 3);
        int coreLeft = (zoneLeft + zoneRight) / 2 - core / 2;
        graphics.fill(coreLeft, barTop, coreLeft + core, barTop + BAR_HEIGHT, COLOR_ZONE_CORE);

        // Cursor, interpolated between ticks for smooth motion
        double renderCursor = finished ? cursor : Mth.lerp(partialTick, prevCursor, cursor);
        int cursorX = barLeft + (int) Math.round(renderCursor / 100.0 * BAR_WIDTH);
        graphics.fill(cursorX - 1, barTop - 4, cursorX + 1, barTop + BAR_HEIGHT + 4, COLOR_CURSOR);

        // Status line
        Component missLine = Component.translatable("gui.dmzdivine.ritual.misses", misses, allowedMisses);
        graphics.drawCenteredString(this.font, missLine, centerX, barTop + BAR_HEIGHT + 12, 0xB0B0C0);

        if (finished) {
            Component result = won
                    ? Component.translatable("gui.dmzdivine.ritual.win")
                    : Component.translatable("gui.dmzdivine.ritual.lose");
            graphics.drawCenteredString(this.font, result, centerX, panelTop + panelHeight - 20, won ? 0xFFD75C : 0xFF5C5C);
        } else {
            Component hint = Component.translatable("gui.dmzdivine.ritual.instructions");
            graphics.drawCenteredString(this.font, hint, centerX, panelTop + panelHeight - 20, 0x8F8F9F);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        // Covers ESC, disconnects and any other path that closes the screen early:
        // the server must always get a result so the pending state is cleared.
        sendResult(won);
        super.removed();
    }
}
