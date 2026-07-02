package com.colossaldungeons.enhanced.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Custom dropdown/select widget with transparent style.
 * Shows the currently selected option; on click, expands a list of options below.
 * Closes on selection or click outside.
 */
@OnlyIn(Dist.CLIENT)
public class CDEDropdown extends AbstractWidget {

    private final List<String> options;
    private int selectedIndex;
    private boolean expanded;
    private int hoveredOptionIndex = -1;
    private final Consumer<Integer> onSelectionChanged;

    /**
     * Creates a new dropdown widget.
     *
     * @param x left position
     * @param y top position
     * @param width widget width
     * @param height widget height (collapsed)
     * @param options list of option strings
     * @param initialIndex initially selected option index
     * @param onSelectionChanged callback when selection changes
     */
    public CDEDropdown(int x, int y, int width, int height,
                       List<String> options, int initialIndex,
                       Consumer<Integer> onSelectionChanged) {
        super(x, y, width, height, Component.literal(
            options.isEmpty() ? "" : options.get(Math.max(0, Math.min(initialIndex, options.size() - 1)))
        ));
        this.options = new ArrayList<>(options);
        this.selectedIndex = Math.max(0, Math.min(initialIndex, options.size() - 1));
        this.expanded = false;
        this.onSelectionChanged = onSelectionChanged;
    }

    /**
     * Gets the currently selected option string.
     */
    public String getSelectedOption() {
        if (options.isEmpty()) return "";
        return options.get(selectedIndex);
    }

    /**
     * Gets the currently selected index.
     */
    public int getSelectedIndex() {
        return selectedIndex;
    }

    /**
     * Sets the selected index programmatically.
     */
    public void setSelectedIndex(int index) {
        if (index >= 0 && index < options.size()) {
            this.selectedIndex = index;
            setMessage(Component.literal(options.get(index)));
        }
    }

    /**
     * Whether the dropdown is currently expanded.
     */
    public boolean isExpanded() {
        return expanded;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Draw main button area
        int bgColor = isHoveredOrFocused() ? GuiConstants.BUTTON_HOVER : GuiConstants.BUTTON_BG;
        graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), bgColor);

        // Draw border
        int borderColor = expanded ? GuiConstants.ACCENT_COLOR : GuiConstants.BORDER_COLOR;
        drawBorder(graphics, getX(), getY(), getWidth(), getHeight(), borderColor);

        // Draw selected text
        String displayText = options.isEmpty() ? "None" : options.get(selectedIndex);
        graphics.drawString(Minecraft.getInstance().font, displayText,
            getX() + GuiConstants.PADDING, getY() + (getHeight() - 8) / 2,
            GuiConstants.TEXT_COLOR);

        // Draw arrow indicator
        String arrow = expanded ? "^" : "v";
        graphics.drawString(Minecraft.getInstance().font, arrow,
            getX() + getWidth() - 12, getY() + (getHeight() - 8) / 2,
            GuiConstants.TEXT_DIM);

        // Draw expanded options list
        if (expanded) {
            int optionY = getY() + getHeight();
            int dropdownHeight = options.size() * getHeight();

            // Dropdown background
            graphics.fill(getX(), optionY, getX() + getWidth(), optionY + dropdownHeight,
                GuiConstants.PANEL_DARK_COLOR);
            drawBorder(graphics, getX(), optionY, getWidth(), dropdownHeight, GuiConstants.BORDER_COLOR);

            // Track hovered option
            hoveredOptionIndex = -1;

            for (int i = 0; i < options.size(); i++) {
                int itemY = optionY + i * getHeight();
                boolean hovered = mouseX >= getX() && mouseX < getX() + getWidth()
                    && mouseY >= itemY && mouseY < itemY + getHeight();

                if (hovered) {
                    hoveredOptionIndex = i;
                    graphics.fill(getX() + 1, itemY, getX() + getWidth() - 1, itemY + getHeight(),
                        GuiConstants.BUTTON_HOVER);
                }

                int textColor = (i == selectedIndex) ? GuiConstants.ACCENT_COLOR : GuiConstants.TEXT_COLOR;
                graphics.drawString(Minecraft.getInstance().font, options.get(i),
                    getX() + GuiConstants.PADDING, itemY + (getHeight() - 8) / 2, textColor);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.active || !this.visible) return false;

        if (expanded) {
            // Check if clicked on an option
            int optionY = getY() + getHeight();
            for (int i = 0; i < options.size(); i++) {
                int itemY = optionY + i * getHeight();
                if (mouseX >= getX() && mouseX < getX() + getWidth()
                    && mouseY >= itemY && mouseY < itemY + getHeight()) {
                    selectedIndex = i;
                    setMessage(Component.literal(options.get(i)));
                    expanded = false;
                    if (onSelectionChanged != null) {
                        onSelectionChanged.accept(selectedIndex);
                    }
                    return true;
                }
            }
            // Clicked outside - close
            expanded = false;
            return true;
        } else {
            // Check if clicked on the main button
            if (mouseX >= getX() && mouseX < getX() + getWidth()
                && mouseY >= getY() && mouseY < getY() + getHeight()) {
                expanded = true;
                return true;
            }
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
        defaultButtonNarrationText(narration);
    }

    private void drawBorder(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    /**
     * Factory method for creating a dropdown.
     */
    public static CDEDropdown create(int x, int y, int width, List<String> options,
                                     int initialIndex, Consumer<Integer> callback) {
        return new CDEDropdown(x, y, width, GuiConstants.WIDGET_HEIGHT, options, initialIndex, callback);
    }
}
