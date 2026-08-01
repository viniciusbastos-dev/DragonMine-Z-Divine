package com.dmzdivine.client.gui;

import com.dmzdivine.network.DivineNetwork;
import com.dmzdivine.network.C2S.WhisActionC2S;
import com.dmzdivine.server.MeditationTraining;
import com.dragonminez.client.util.TextUtil;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Whis' dialogue: his line, the Ultra Instinct trial, the meditation lesson and his weight bells.
 *
 * <p>Built to read exactly like one of the base mod's own master dialogues - same parchment panel
 * ({@code textmenu.png}), same bottom row of {@code TexturedTextButton}s, same {@code dragonminez:smooth}
 * font, same "name in bold, then the line" layout as {@code MasterTextScreen}. It is still our own
 * screen rather than that one: {@code MasterTextScreen} dispatches its buttons from a switch over
 * hardcoded master names and routes everything through the base mod's {@code NPCActionC2S}, which
 * validates services against an NPC list Whis is not in. Borrowing the look costs nothing; borrowing
 * the screen would cost a mixin per button.
 *
 * <p>Everything here is a request - the server decides what actually happens (see {@link WhisActionC2S}).
 */
public class WhisDialogueScreen extends Screen {

    private static final ResourceLocation MENU_TEXT =
            new ResourceLocation("dragonminez", "textures/gui/menu/textmenu.png");
    private static final ResourceLocation BUTTONS_TEXTURE =
            new ResourceLocation("dragonminez", "textures/gui/buttons/characterbuttons.png");
    private static final ResourceLocation DMZ_FONT = new ResourceLocation("dragonminez", "smooth");

    /** Button geometry copied from MasterTextScreen so the row lines up with every other master's. */
    private static final int BUTTON_WIDTH = 74;
    private static final int BUTTON_HEIGHT = 20;
    private static final int TEXT_WIDTH = 230;

    private final int trainedMinutes;
    private final int requiredMinutes;
    private final int ultraInstinctLevel;
    private final int tpCost;

    private EditBox weightBox;
    private boolean weightsOpen;

    public WhisDialogueScreen(int trainedMinutes, int requiredMinutes, int ultraInstinctLevel, int tpCost) {
        super(Component.translatable("gui.dmzdivine.whis.title").withStyle(Style.EMPTY.withFont(DMZ_FONT)));
        this.trainedMinutes = trainedMinutes;
        this.requiredMinutes = requiredMinutes;
        this.ultraInstinctLevel = ultraInstinctLevel;
        this.tpCost = tpCost;
    }

    @Override
    protected void init() {
        super.init();
        int buttonY = this.height - 23;
        List<Entry> row = new ArrayList<>();

        if (weightsOpen) {
            weightBox = new EditBox(this.font, this.width / 2 - 60, buttonY - 28, 120, 16, Component.empty());
            weightBox.setMaxLength(6);
            weightBox.setFilter(value -> value.matches("\\d*"));
            addRenderableWidget(weightBox);
            setInitialFocus(weightBox);

            row.add(new Entry("gui.dmzdivine.whis.button.weights_confirm", b -> {
                int weight = parseWeight(weightBox.getValue());
                if (weight > 0) {
                    DivineNetwork.sendToServer(new WhisActionC2S(WhisActionC2S.ACTION_WEIGHTS, weight));
                    onClose();
                }
            }));
            row.add(new Entry("gui.dmzdivine.whis.button.back", b -> {
                weightsOpen = false;
                refreshButtons();
            }));
            layout(row, buttonY);
            return;
        }

        row.add(new Entry("gui.dmzdivine.whis.button.trial", b -> {
            DivineNetwork.sendToServer(new WhisActionC2S(WhisActionC2S.ACTION_TRIAL, 0));
            onClose();
        }));
        // A lesson already learned is not on offer any more - the training lives in the minigame list
        // from then on.
        if (!knowsMeditation()) {
            row.add(new Entry("gui.dmzdivine.whis.button.meditation", b -> {
                DivineNetwork.sendToServer(new WhisActionC2S(WhisActionC2S.ACTION_MEDITATION, 0));
                onClose();
            }));
        }
        row.add(new Entry("gui.dmzdivine.whis.button.weights", b -> {
            weightsOpen = true;
            refreshButtons();
        }));
        layout(row, buttonY);
    }

    private record Entry(String key, Button.OnPress onPress) {
    }

    /**
     * Spreads the row across the panel's 180px of travel, which lands two buttons at the base mod's
     * own x and x+180 and three at x, x+90, x+180 - the same places every other master's dialogue
     * puts them, whatever this one happens to be offering right now.
     */
    private void layout(List<Entry> row, int buttonY) {
        int left = this.width / 2 - 120;
        int span = 180;
        for (int i = 0; i < row.size(); i++) {
            int x = row.size() == 1 ? left + span / 2 : left + (span * i) / (row.size() - 1);
            Entry entry = row.get(i);
            addRenderableWidget(new FittedTextButton(x, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT,
                    BUTTONS_TEXTURE, 0, 28, 0, 48, tr(entry.key()), entry.onPress()));
        }
    }

    /** Read straight off the synced character data - the same flag the minigame list reads. */
    private static boolean knowsMeditation() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return false;
        return StatsProvider.get(StatsCapability.INSTANCE, player)
                .map(data -> data.getCharacter().isMinigameKnown(MeditationTraining.MINIGAME_ID))
                .orElse(false);
    }

    private void refreshButtons() {
        clearWidgets();
        init();
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
        int centerX = this.width / 2;
        int bottom = this.height;

        // The base mod draws this panel as a raw quad rather than a blit, anchored to the bottom of
        // the screen - matched here so the two dialogues sit in exactly the same place.
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, MENU_TEXT);

        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.vertex(centerX - 140, bottom + 250, 0.0D).uv(0.0F, 1.0F).endVertex();
        buffer.vertex(centerX + 140, bottom + 250, 0.0D).uv(1.0F, 1.0F).endVertex();
        buffer.vertex(centerX + 140, bottom - 90, 0.0D).uv(1.0F, 0.0F).endVertex();
        buffer.vertex(centerX - 140, bottom - 90, 0.0D).uv(0.0F, 0.0F).endVertex();
        Tesselator.getInstance().end();

        RenderSystem.disableBlend();

        TextUtil.drawStringWithBorder(graphics, this.font,
                this.title.copy().withStyle(ChatFormatting.BOLD), centerX - 120, bottom - 87, 0xFFFFFF);

        int textY = bottom - 74;
        for (var line : this.font.split(dialogue(), TEXT_WIDTH)) {
            TextUtil.drawStringWithBorder(graphics, this.font, line, centerX - 120, textY, 0xFFFFFF);
            textY += this.font.lineHeight + 2;
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /** What Whis is saying right now: the weights prompt, or where the player stands on the trial. */
    private Component dialogue() {
        if (weightsOpen) return tr("gui.dmzdivine.whis.weights_prompt");
        if (requiredMinutes <= 0) return tr("gui.dmzdivine.whis.status_mastered");
        if (trainedMinutes >= requiredMinutes) {
            return tr("gui.dmzdivine.whis.status_ready", ultraInstinctLevel + 1)
                    .append(" ")
                    .append(tr("gui.dmzdivine.whis.trial_cost", tpCost));
        }
        return tr("gui.dmzdivine.whis.status_training", trainedMinutes, requiredMinutes)
                .append(" ")
                .append(tr("gui.dmzdivine.whis.trial_cost", tpCost));
    }

    private MutableComponent tr(String key, Object... args) {
        return Component.translatable(key, args).withStyle(Style.EMPTY.withFont(DMZ_FONT));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
