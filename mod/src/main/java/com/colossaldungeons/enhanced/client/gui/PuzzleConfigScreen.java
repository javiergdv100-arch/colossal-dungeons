package com.colossaldungeons.enhanced.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

/**
 * Puzzle configuration screen with transparent overlay style.
 * Allows configuring puzzle type, solution definition, max attempts,
 * time limit, rewards, and hints.
 */
@OnlyIn(Dist.CLIENT)
public class PuzzleConfigScreen extends Screen {

    private final Screen parent;

    private static final List<String> PUZZLE_TYPES = List.of(
        "Sound Sequence", "Light Redirect", "Weight Balance",
        "Pattern Match", "Lever Sequence", "Symbol Rotation"
    );

    // Widgets
    private CDEDropdown typeDropdown;
    private CDESlider maxAttemptsSlider;
    private CDESlider timeLimitSlider;
    private CDETextField hintField1;
    private CDETextField hintField2;
    private CDETextField solutionField;

    public PuzzleConfigScreen(Screen parent) {
        super(Component.translatable("gui.colossal_dungeons_enhanced.puzzle_config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int panelWidth = (int) (this.width * 0.6f);
        int panelLeft = centerX - panelWidth / 2;
        int widgetWidth = panelWidth - GuiConstants.PADDING * 4;
        int y = GuiConstants.HEADER_HEIGHT + GuiConstants.MARGIN * 2;

        // Puzzle type dropdown
        typeDropdown = CDEDropdown.create(
            panelLeft + GuiConstants.PADDING * 2, y,
            widgetWidth, PUZZLE_TYPES, 0, idx -> { /* store */ }
        );
        addRenderableWidget(typeDropdown);
        y += 35;

        // Solution definition field
        solutionField = CDETextField.create(
            panelLeft + GuiConstants.PADDING * 2, y + 12,
            widgetWidth, "Solution Definition", "e.g. A,B,C,D or 1,3,2,4"
        );
        addRenderableWidget(solutionField);
        y += 50;

        // Max attempts slider
        maxAttemptsSlider = CDESlider.create(
            panelLeft + GuiConstants.PADDING * 2, y + 12,
            widgetWidth, 1, 10, 3, "Max Attempts: %.0f",
            val -> { /* store */ }
        );
        addRenderableWidget(maxAttemptsSlider);
        y += 45;

        // Time limit slider (in minutes)
        timeLimitSlider = CDESlider.create(
            panelLeft + GuiConstants.PADDING * 2, y + 12,
            widgetWidth, 0, 30, 5, "Time Limit: %.0f min",
            val -> { /* store */ }
        );
        addRenderableWidget(timeLimitSlider);
        y += 45;

        // Hint fields
        hintField1 = CDETextField.create(
            panelLeft + GuiConstants.PADDING * 2, y + 12,
            widgetWidth, "Hint 1", "Enter first hint..."
        );
        addRenderableWidget(hintField1);
        y += 40;

        hintField2 = CDETextField.create(
            panelLeft + GuiConstants.PADDING * 2, y + 12,
            widgetWidth, "Hint 2", "Enter second hint..."
        );
        addRenderableWidget(hintField2);
        y += 50;

        // Save/Cancel buttons
        int buttonWidth = (widgetWidth - GuiConstants.MARGIN) / 2;
        addRenderableWidget(CDEButton.create(
            panelLeft + GuiConstants.PADDING * 2, y,
            buttonWidth, GuiConstants.WIDGET_HEIGHT,
            Component.literal("Save"),
            button -> save()
        ));
        addRenderableWidget(CDEButton.create(
            panelLeft + GuiConstants.PADDING * 2 + buttonWidth + GuiConstants.MARGIN, y,
            buttonWidth, GuiConstants.WIDGET_HEIGHT,
            Component.literal("Cancel"),
            button -> cancel()
        ));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Full-screen semi-transparent overlay
        graphics.fill(0, 0, this.width, this.height, GuiConstants.BACKGROUND_COLOR);

        // Header
        graphics.fill(0, 0, this.width, GuiConstants.HEADER_HEIGHT, GuiConstants.PANEL_COLOR);
        graphics.drawCenteredString(this.font,
            Component.literal("Puzzle Configuration"),
            this.width / 2, (GuiConstants.HEADER_HEIGHT - 8) / 2,
            GuiConstants.ACCENT_COLOR);

        // Central panel
        int panelWidth = (int) (this.width * 0.6f);
        int panelLeft = this.width / 2 - panelWidth / 2;
        int panelTop = GuiConstants.HEADER_HEIGHT + GuiConstants.MARGIN;
        int panelBottom = this.height - GuiConstants.MARGIN;

        graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelBottom,
            GuiConstants.PANEL_DARK_COLOR);
        drawBorder(graphics, panelLeft, panelTop, panelWidth, panelBottom - panelTop,
            GuiConstants.BORDER_COLOR);

        // Section title
        graphics.drawString(this.font, "Puzzle Type",
            panelLeft + GuiConstants.PADDING * 2,
            panelTop + GuiConstants.PADDING,
            GuiConstants.TEXT_COLOR);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void save() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }

    private void cancel() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }

    private void drawBorder(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }
}
