package com.dmzdivine.client.gui;

import com.dmzdivine.network.DivineNetwork;
import com.dmzdivine.network.C2S.WhisActionC2S;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Whis' dialogue: a progress line, the Ultra Instinct trial, and his weight bells.
 *
 * <p>Its own screen rather than the base mod's {@code MasterTextScreen}, which hardcodes which
 * services each master name offers and would need a mixin per button. Everything here is a request -
 * the server decides what actually happens (see {@link WhisActionC2S}).
 */
public class WhisDialogueScreen extends Screen {

    private static final int PANEL_WIDTH = 260;
    private static final int PANEL_HEIGHT = 150;

    private static final int COLOR_PANEL = 0xC0080D14;
    private static final int COLOR_PANEL_BORDER = 0xFF8FD8F5;

    private final int trainedMinutes;
    private final int requiredMinutes;
    private final int ultraInstinctLevel;
    private final int tpCost;

    private EditBox weightBox;
    private boolean weightsOpen;

    public WhisDialogueScreen(int trainedMinutes, int requiredMinutes, int ultraInstinctLevel, int tpCost) {
        super(Component.translatable("gui.dmzdivine.whis.title"));
        this.trainedMinutes = trainedMinutes;
        this.requiredMinutes = requiredMinutes;
        this.ultraInstinctLevel = ultraInstinctLevel;
        this.tpCost = tpCost;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int panelTop = this.height / 2 - PANEL_HEIGHT / 2;
        int buttonsY = panelTop + PANEL_HEIGHT - 58;

        if (!weightsOpen) {
            addRenderableWidget(Button.builder(Component.translatable("gui.dmzdivine.whis.button.trial"),
                            b -> {
                                DivineNetwork.sendToServer(new WhisActionC2S(WhisActionC2S.ACTION_TRIAL, 0));
                                onClose();
                            })
                    .bounds(centerX - 110, buttonsY, 105, 20)
                    .build());

            addRenderableWidget(Button.builder(Component.translatable("gui.dmzdivine.whis.button.weights"),
                            b -> {
                                weightsOpen = true;
                                rebuildWidgets();
                            })
                    .bounds(centerX + 5, buttonsY, 105, 20)
                    .build());
            return;
        }

        weightBox = new EditBox(this.font, centerX - 60, buttonsY - 24, 120, 18, Component.empty());
        weightBox.setMaxLength(6);
        weightBox.setFilter(value -> value.matches("\\d*"));
        addRenderableWidget(weightBox);
        setInitialFocus(weightBox);

        addRenderableWidget(Button.builder(Component.translatable("gui.dmzdivine.whis.button.weights_confirm"),
                        b -> {
                            int weight = parseWeight(weightBox.getValue());
                            if (weight > 0) {
                                DivineNetwork.sendToServer(new WhisActionC2S(WhisActionC2S.ACTION_WEIGHTS, weight));
                                onClose();
                            }
                        })
                .bounds(centerX - 110, buttonsY, 105, 20)
                .build());

        addRenderableWidget(Button.builder(Component.translatable("gui.dmzdivine.whis.button.back"),
                        b -> {
                            weightsOpen = false;
                            rebuildWidgets();
                        })
                .bounds(centerX + 5, buttonsY, 105, 20)
                .build());
    }

    private int parseWeight(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        int centerX = this.width / 2;
        int panelLeft = centerX - PANEL_WIDTH / 2;
        int panelTop = this.height / 2 - PANEL_HEIGHT / 2;

        graphics.fill(panelLeft - 1, panelTop - 1, panelLeft + PANEL_WIDTH + 1, panelTop + PANEL_HEIGHT + 1, COLOR_PANEL_BORDER);
        graphics.fill(panelLeft, panelTop, panelLeft + PANEL_WIDTH, panelTop + PANEL_HEIGHT, COLOR_PANEL);
        graphics.drawCenteredString(this.font, this.title, centerX, panelTop + 10, 0x8FD8F5);

        Component line = weightsOpen
                ? Component.translatable("gui.dmzdivine.whis.weights_prompt")
                : statusLine();
        graphics.drawCenteredString(this.font, line, centerX, panelTop + 34, 0xD8E8F0);

        if (!weightsOpen) {
            Component cost = Component.translatable("gui.dmzdivine.whis.trial_cost", tpCost);
            graphics.drawCenteredString(this.font, cost, centerX, panelTop + 50, 0x8F9FAF);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private Component statusLine() {
        if (requiredMinutes <= 0) {
            return Component.translatable("gui.dmzdivine.whis.status_mastered");
        }
        if (trainedMinutes >= requiredMinutes) {
            return Component.translatable("gui.dmzdivine.whis.status_ready", ultraInstinctLevel + 1);
        }
        return Component.translatable("gui.dmzdivine.whis.status_training", trainedMinutes, requiredMinutes);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
