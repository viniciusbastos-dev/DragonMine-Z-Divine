package com.dmzdivine.client.gui;

import com.dmzdivine.DivineConfig;
import com.dmzdivine.network.DivineNetwork;
import com.dmzdivine.network.C2S.UltraInstinctTrialResultC2S;
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
 * Whis' Ultra Instinct trial: he strikes from one side at a time and the body has to move on its
 * own. A marker rushes in from one of four directions; the player must press that direction (WASD
 * or the arrow keys) inside the reaction window right before impact. Reacting too early counts as
 * reading the attack instead of feeling it, and misses just as hard as reacting too late.
 *
 * <p>Deliberately a different game from the god ritual's timing bar: that one is about channelling,
 * this one is about reflexes. Same trust model though - it runs on the client and the server
 * re-validates everything when the result packet arrives (see UltraInstinctTraining).
 */
public class UltraInstinctTrialScreen extends Screen {

    private static final int PANEL_WIDTH = 240;
    private static final int PANEL_HEIGHT = 168;
    private static final int FIELD_RADIUS = 56;

    private static final int COLOR_PANEL = 0xC0080D14;
    private static final int COLOR_PANEL_BORDER = 0xFF8FD8F5;
    private static final int COLOR_PLAYER = 0xFFE8F6FF;
    private static final int COLOR_ATTACK = 0xFFB03A3A;
    private static final int COLOR_ATTACK_WINDOW = 0xFFFFD24A;
    private static final int COLOR_PIP_ON = 0xFF8FD8F5;
    private static final int COLOR_PIP_OFF = 0xFF2A3440;
    private static final int COLOR_HIT = 0xFFFF6B6B;

    private static final int LEFT = 0;
    private static final int RIGHT = 1;
    private static final int UP = 2;
    private static final int DOWN = 3;

    /** Ticks between one attack resolving and the next winding up. */
    private static final int GAP_TICKS = 7;

    private final RandomSource random = RandomSource.create();

    private final int requiredDodges = DivineConfig.WHIS_TRIAL_DODGES.get();
    private final int allowedHits = DivineConfig.WHIS_TRIAL_ALLOWED_HITS.get();
    private final int reactWindow = DivineConfig.WHIS_TRIAL_REACT_WINDOW.get();
    private final double windupDecrease = DivineConfig.WHIS_TRIAL_WINDUP_DECREASE.get();
    private final int minWindup = DivineConfig.WHIS_TRIAL_MIN_WINDUP.get();

    private double windup = DivineConfig.WHIS_TRIAL_WINDUP_TICKS.get();

    private int direction = LEFT;
    private int progress;       // ticks into the current attack
    private int prevProgress;   // for render interpolation
    private int gap = GAP_TICKS;
    private boolean resolved;   // this attack already ended (dodged or landed)

    private int dodges;
    private int hits;
    private int flashTicks;

    private boolean finished;
    private boolean won;
    private boolean resultSent;
    private int closeTicks = -1;

    public UltraInstinctTrialScreen() {
        super(Component.translatable("gui.dmzdivine.uitrial.title"));
        nextAttack();
    }

    private void nextAttack() {
        direction = random.nextInt(4);
        progress = 0;
        prevProgress = 0;
        resolved = false;
    }

    private int windupTicks() {
        return Math.max(minWindup, (int) Math.round(windup));
    }

    @Override
    public void tick() {
        if (flashTicks > 0) flashTicks--;

        if (finished) {
            if (closeTicks > 0 && --closeTicks == 0) {
                Minecraft.getInstance().setScreen(null);
            }
            return;
        }

        if (resolved) {
            if (--gap <= 0) {
                gap = GAP_TICKS;
                nextAttack();
            }
            return;
        }

        prevProgress = progress;
        progress++;

        if (progress >= windupTicks()) {
            // The strike landed untouched.
            registerHit();
        }
    }

    private void attempt(int pressed) {
        if (finished || resolved) return;

        boolean inWindow = progress >= windupTicks() - reactWindow;
        if (pressed == direction && inWindow) {
            dodges++;
            resolved = true;
            windup = Math.max(minWindup, windup - windupDecrease);
            playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.6F);
            if (dodges >= requiredDodges) finish(true);
        } else {
            registerHit();
        }
    }

    private void registerHit() {
        hits++;
        resolved = true;
        flashTicks = 4;
        playSound(SoundEvents.PLAYER_ATTACK_CRIT, 0.7F);
        if (hits > allowedHits) finish(false);
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
        DivineNetwork.sendToServer(new UltraInstinctTrialResultC2S(success));
    }

    private void playSound(net.minecraft.sounds.SoundEvent sound, float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        int pressed = switch (keyCode) {
            case GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_LEFT -> LEFT;
            case GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_RIGHT -> RIGHT;
            case GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_UP -> UP;
            case GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_DOWN -> DOWN;
            default -> -1;
        };
        if (pressed >= 0) {
            attempt(pressed);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        int centerX = this.width / 2;
        int panelLeft = centerX - PANEL_WIDTH / 2;
        int panelTop = this.height / 2 - PANEL_HEIGHT / 2;
        int fieldCenterY = panelTop + 96;

        graphics.fill(panelLeft - 1, panelTop - 1, panelLeft + PANEL_WIDTH + 1, panelTop + PANEL_HEIGHT + 1, COLOR_PANEL_BORDER);
        graphics.fill(panelLeft, panelTop, panelLeft + PANEL_WIDTH, panelTop + PANEL_HEIGHT, COLOR_PANEL);
        graphics.drawCenteredString(this.font, this.title, centerX, panelTop + 8, 0x8FD8F5);

        // Dodge pips
        int pipSize = 6;
        int pipSpacing = 10;
        int pipsLeft = centerX - (requiredDodges * pipSpacing - (pipSpacing - pipSize)) / 2;
        for (int i = 0; i < requiredDodges; i++) {
            int x = pipsLeft + i * pipSpacing;
            graphics.fill(x, panelTop + 24, x + pipSize, panelTop + 24 + pipSize, i < dodges ? COLOR_PIP_ON : COLOR_PIP_OFF);
        }

        // The player, flashing red when a strike lands
        int bodyColor = flashTicks > 0 ? COLOR_HIT : COLOR_PLAYER;
        graphics.fill(centerX - 4, fieldCenterY - 10, centerX + 4, fieldCenterY + 10, bodyColor);
        graphics.fill(centerX - 3, fieldCenterY - 15, centerX + 3, fieldCenterY - 10, bodyColor);

        if (!finished && !resolved) {
            int windupTotal = windupTicks();
            double travelled = Mth.lerp(partialTick, prevProgress, progress) / (double) windupTotal;
            int distance = (int) Math.round(FIELD_RADIUS * (1.0 - Mth.clamp(travelled, 0.0, 1.0)));
            boolean inWindow = progress >= windupTotal - reactWindow;
            int color = inWindow ? COLOR_ATTACK_WINDOW : COLOR_ATTACK;

            int markerX = centerX;
            int markerY = fieldCenterY;
            switch (direction) {
                case LEFT -> markerX = centerX - 12 - distance;
                case RIGHT -> markerX = centerX + 12 + distance;
                case UP -> markerY = fieldCenterY - 12 - distance;
                case DOWN -> markerY = fieldCenterY + 12 + distance;
                default -> {
                }
            }
            graphics.fill(markerX - 5, markerY - 5, markerX + 5, markerY + 5, color);

            Component key = Component.translatable("gui.dmzdivine.uitrial.direction." + directionKey());
            graphics.drawCenteredString(this.font, key, centerX, panelTop + 38, inWindow ? COLOR_ATTACK_WINDOW : 0x60707F);
        }

        Component status = Component.translatable("gui.dmzdivine.uitrial.hits", hits, allowedHits);
        graphics.drawCenteredString(this.font, status, centerX, panelTop + PANEL_HEIGHT - 34, 0xB0B0C0);

        Component footer;
        int footerColor;
        if (finished) {
            footer = Component.translatable(won ? "gui.dmzdivine.uitrial.win" : "gui.dmzdivine.uitrial.lose");
            footerColor = won ? 0x8FD8F5 : 0xFF5C5C;
        } else {
            footer = Component.translatable("gui.dmzdivine.uitrial.instructions");
            footerColor = 0x8F8F9F;
        }
        graphics.drawCenteredString(this.font, footer, centerX, panelTop + PANEL_HEIGHT - 20, footerColor);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private String directionKey() {
        return switch (direction) {
            case RIGHT -> "right";
            case UP -> "up";
            case DOWN -> "down";
            default -> "left";
        };
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        // ESC, disconnects, anything: the server must get a result so the pending flag clears.
        sendResult(won);
        super.removed();
    }
}
