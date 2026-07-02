package com.colossaldungeons.enhanced.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Custom button widget with transparent background and gold accent on hover.
 * Uses the CDE clean transparent GUI style with smooth visual feedback.
 */
@OnlyIn(Dist.CLIENT)
public class CDEButton extends Button {

    private float hoverAnimation = 0.0f;

    public CDEButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, Button.DEFAULT_NARRATION);
    }

    public CDEButton(int x, int y, int width, int height, Component message, OnPress onPress, CreateNarration narration) {
        super(x, y, width, height, message, onPress, narration);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Update hover animation
        if (isHoveredOrFocused()) {
            hoverAnimation = Math.min(1.0f, hoverAnimation + partialTick * 0.15f);
        } else {
            hoverAnimation = Math.max(0.0f, hoverAnimation - partialTick * 0.15f);
        }

        // Determine background color based on state
        int bgColor;
        if (!this.active) {
            bgColor = GuiConstants.BUTTON_DISABLED;
        } else if (hoverAnimation > 0.5f) {
            bgColor = GuiConstants.BUTTON_HOVER;
        } else {
            bgColor = GuiConstants.BUTTON_BG;
        }

        // Draw background
        graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), bgColor);

        // Draw border
        int borderColor = isHoveredOrFocused() ? GuiConstants.ACCENT_COLOR : GuiConstants.BORDER_COLOR;
        drawBorder(graphics, getX(), getY(), getWidth(), getHeight(), borderColor);

        // Draw text centered
        int textColor = this.active ? GuiConstants.TEXT_COLOR : GuiConstants.TEXT_DIM;
        graphics.drawCenteredString(
            net.minecraft.client.Minecraft.getInstance().font,
            getMessage(),
            getX() + getWidth() / 2,
            getY() + (getHeight() - 8) / 2,
            textColor
        );
    }

    /**
     * Draws a 1-pixel border around the given rectangle.
     */
    private void drawBorder(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        // Top
        graphics.fill(x, y, x + width, y + 1, color);
        // Bottom
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        // Left
        graphics.fill(x, y, x + 1, y + height, color);
        // Right
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    /**
     * Builder for creating CDEButton instances with fluent API.
     */
    public static CDEButton create(int x, int y, int width, int height, Component message, OnPress onPress) {
        return new CDEButton(x, y, width, height, message, onPress);
    }
}
