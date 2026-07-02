package com.colossaldungeons.enhanced.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Custom text input field with transparent background, gold border when focused,
 * placeholder text support, and an optional label above the field.
 */
@OnlyIn(Dist.CLIENT)
public class CDETextField extends EditBox {

    private final String label;
    private final String placeholder;

    /**
     * Creates a new styled text field.
     *
     * @param font the font renderer
     * @param x left position
     * @param y top position
     * @param width widget width
     * @param height widget height
     * @param label label text displayed above the field (empty for no label)
     * @param placeholder placeholder text when empty
     */
    public CDETextField(Font font, int x, int y, int width, int height,
                        String label, String placeholder) {
        super(font, x, y, width, height, Component.literal(label));
        this.label = label;
        this.placeholder = placeholder;
        this.setTextColor(GuiConstants.TEXT_COLOR);
        this.setTextColorUneditable(GuiConstants.TEXT_DIM);
        this.setBordered(false);
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!this.isVisible()) return;

        Font font = Minecraft.getInstance().font;

        // Draw label above
        if (label != null && !label.isEmpty()) {
            graphics.drawString(font, label, getX(), getY() - 12, GuiConstants.TEXT_DIM);
        }

        // Draw background
        graphics.fill(getX() - 2, getY() - 2,
            getX() + getWidth() + 2, getY() + getHeight() + 2,
            GuiConstants.BUTTON_BG);

        // Draw border (gold when focused, subtle otherwise)
        int borderColor = isFocused() ? GuiConstants.BORDER_ACTIVE : GuiConstants.BORDER_COLOR;
        drawBorder(graphics, getX() - 2, getY() - 2, getWidth() + 4, getHeight() + 4, borderColor);

        // Draw placeholder if empty and not focused
        if (getValue().isEmpty() && !isFocused() && !placeholder.isEmpty()) {
            graphics.drawString(font, placeholder,
                getX() + 2, getY() + (getHeight() - 8) / 2,
                GuiConstants.TEXT_PLACEHOLDER);
        }

        // Render the actual text input
        super.renderWidget(graphics, mouseX, mouseY, partialTick);
    }

    private void drawBorder(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    /**
     * Factory method for creating a text field with label and placeholder.
     */
    public static CDETextField create(int x, int y, int width, String label, String placeholder) {
        Font font = Minecraft.getInstance().font;
        return new CDETextField(font, x, y, width, GuiConstants.WIDGET_HEIGHT, label, placeholder);
    }
}
