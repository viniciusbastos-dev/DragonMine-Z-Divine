package com.dmzdivine.client.gui;

import com.dragonminez.client.gui.buttons.TexturedTextButton;
import com.dragonminez.common.init.MainSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * A {@link TexturedTextButton} whose label shrinks to fit instead of running off the sides.
 *
 * <p>The base mod's button draws its background with {@code blit(texture, x, y, u, v, textureWidth,
 * textureHeight)} - at the texture's size, not the widget's - so a button cannot simply be made wider
 * to hold a longer word: the frame would stay 74px while the click area grew. The label is what has
 * to give, which matters more here than in the base mod because the DMZ font is wide and our labels
 * are whole phrases in two languages.
 *
 * <p>Done by lending the parent an empty message for the frame and drawing the real one scaled on
 * top, rather than reimplementing the blit - the texture coordinates and hover states stay the base
 * mod's business.
 */
public class FittedTextButton extends TexturedTextButton {

    /** Pixels kept clear on each side, so the text never touches the frame. */
    private static final int PADDING = 5;
    /** Below this the text stops being readable; better to clip than to render a smudge. */
    private static final float MIN_SCALE = 0.5f;

    private static final int COLOR_NORMAL = 0xFFFFFF;
    private static final int COLOR_HOVER = 0x7CFDD6;
    private static final int COLOR_DISABLED = 0xA0A0A0;

    private final Component label;

    public FittedTextButton(int x, int y, int width, int height, ResourceLocation texture,
                            int normalU, int normalV, int hoverU, int hoverV,
                            Component message, OnPress onPress) {
        super(x, y, width, height, texture, normalU, normalV, hoverU, hoverV,
                width, height, COLOR_NORMAL, COLOR_HOVER, 0, false, message, onPress,
                MainSounds.PIP_MENU.get());
        this.label = message;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        setMessage(Component.empty());
        super.renderWidget(graphics, mouseX, mouseY, partialTick);
        setMessage(label);

        Font font = Minecraft.getInstance().font;
        int available = this.width - PADDING * 2;
        int textWidth = font.width(label);
        float scale = textWidth <= available ? 1.0f : Math.max(MIN_SCALE, (float) available / textWidth);

        int color = !this.active ? COLOR_DISABLED : (isHoveredOrFocused() ? COLOR_HOVER : COLOR_NORMAL);

        graphics.pose().pushPose();
        graphics.pose().translate(this.getX() + this.width / 2.0, this.getY() + this.height / 2.0, 0.0);
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.drawCenteredString(font, label, 0, -font.lineHeight / 2, color);
        graphics.pose().popPose();
    }
}
