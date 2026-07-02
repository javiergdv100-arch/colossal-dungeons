package com.colossaldungeons.enhanced.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * Room editor screen with transparent overlay style.
 * Allows defining room bounds (min/max BlockPos), placing traps and puzzles,
 * and configuring room connections.
 */
@OnlyIn(Dist.CLIENT)
public class RoomEditorScreen extends Screen {

    private final Screen parent;

    // Room bounds inputs
    private CDETextField minXField;
    private CDETextField minYField;
    private CDETextField minZField;
    private CDETextField maxXField;
    private CDETextField maxYField;
    private CDETextField maxZField;
    private CDETextField roomNameField;

    // Trap placements
    private final List<String> placedTraps = new ArrayList<>();
    // Puzzle placements
    private final List<String> placedPuzzles = new ArrayList<>();
    // Connections
    private final List<String> connections = new ArrayList<>();

    public RoomEditorScreen(Screen parent) {
        super(Component.translatable("gui.colossal_dungeons_enhanced.room_editor"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        int panelWidth = (int) (this.width * 0.7f);
        int panelLeft = (this.width - panelWidth) / 2;
        int fieldWidth = (panelWidth - GuiConstants.PADDING * 6) / 3;
        int y = GuiConstants.HEADER_HEIGHT + GuiConstants.MARGIN * 2 + 20;

        // Room name
        roomNameField = CDETextField.create(
            panelLeft + GuiConstants.PADDING, y,
            panelWidth - GuiConstants.PADDING * 2,
            "Room Name", "e.g. Entrance Hall"
        );
        addRenderableWidget(roomNameField);
        y += 45;

        // Min bounds row
        minXField = CDETextField.create(
            panelLeft + GuiConstants.PADDING, y + 12,
            fieldWidth, "Min X", "0"
        );
        addRenderableWidget(minXField);

        minYField = CDETextField.create(
            panelLeft + GuiConstants.PADDING + fieldWidth + GuiConstants.PADDING, y + 12,
            fieldWidth, "Min Y", "0"
        );
        addRenderableWidget(minYField);

        minZField = CDETextField.create(
            panelLeft + GuiConstants.PADDING + (fieldWidth + GuiConstants.PADDING) * 2, y + 12,
            fieldWidth, "Min Z", "0"
        );
        addRenderableWidget(minZField);
        y += 50;

        // Max bounds row
        maxXField = CDETextField.create(
            panelLeft + GuiConstants.PADDING, y + 12,
            fieldWidth, "Max X", "16"
        );
        addRenderableWidget(maxXField);

        maxYField = CDETextField.create(
            panelLeft + GuiConstants.PADDING + fieldWidth + GuiConstants.PADDING, y + 12,
            fieldWidth, "Max Y", "8"
        );
        addRenderableWidget(maxYField);

        maxZField = CDETextField.create(
            panelLeft + GuiConstants.PADDING + (fieldWidth + GuiConstants.PADDING) * 2, y + 12,
            fieldWidth, "Max Z", "16"
        );
        addRenderableWidget(maxZField);
        y += 55;

        // Add trap button
        addRenderableWidget(CDEButton.create(
            panelLeft + GuiConstants.PADDING, y,
            150, GuiConstants.WIDGET_HEIGHT,
            Component.literal("+ Add Trap"),
            button -> addTrapPlacement()
        ));

        // Add puzzle button
        addRenderableWidget(CDEButton.create(
            panelLeft + GuiConstants.PADDING + 160, y,
            150, GuiConstants.WIDGET_HEIGHT,
            Component.literal("+ Add Puzzle"),
            button -> addPuzzlePlacement()
        ));
        y += 30;

        // Add connection button
        addRenderableWidget(CDEButton.create(
            panelLeft + GuiConstants.PADDING, y,
            150, GuiConstants.WIDGET_HEIGHT,
            Component.literal("+ Add Connection"),
            button -> addConnection()
        ));
        y += 50;

        // Save / Cancel
        int buttonWidth = 120;
        addRenderableWidget(CDEButton.create(
            panelLeft + GuiConstants.PADDING, y,
            buttonWidth, GuiConstants.WIDGET_HEIGHT,
            Component.literal("Save Room"),
            button -> save()
        ));
        addRenderableWidget(CDEButton.create(
            panelLeft + GuiConstants.PADDING + buttonWidth + GuiConstants.MARGIN, y,
            buttonWidth, GuiConstants.WIDGET_HEIGHT,
            Component.literal("Cancel"),
            button -> cancel()
        ));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Full-screen overlay
        graphics.fill(0, 0, this.width, this.height, GuiConstants.BACKGROUND_COLOR);

        // Header
        graphics.fill(0, 0, this.width, GuiConstants.HEADER_HEIGHT, GuiConstants.PANEL_COLOR);
        graphics.drawCenteredString(this.font,
            Component.literal("Room Editor"),
            this.width / 2, (GuiConstants.HEADER_HEIGHT - 8) / 2,
            GuiConstants.ACCENT_COLOR);

        // Main panel
        int panelWidth = (int) (this.width * 0.7f);
        int panelLeft = (this.width - panelWidth) / 2;
        int panelTop = GuiConstants.HEADER_HEIGHT + GuiConstants.MARGIN;
        int panelBottom = this.height - GuiConstants.MARGIN;

        graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelBottom,
            GuiConstants.PANEL_DARK_COLOR);
        drawBorder(graphics, panelLeft, panelTop, panelWidth, panelBottom - panelTop,
            GuiConstants.BORDER_COLOR);

        // Section title
        graphics.drawString(this.font, "Room Bounds",
            panelLeft + GuiConstants.PADDING,
            panelTop + GuiConstants.PADDING,
            GuiConstants.TEXT_COLOR);

        // Placed traps list
        int listY = GuiConstants.HEADER_HEIGHT + GuiConstants.MARGIN * 2 + 230;
        graphics.drawString(this.font, "Traps: " + placedTraps.size(),
            panelLeft + GuiConstants.PADDING, listY, GuiConstants.TEXT_DIM);
        graphics.drawString(this.font, "Puzzles: " + placedPuzzles.size(),
            panelLeft + GuiConstants.PADDING + 100, listY, GuiConstants.TEXT_DIM);
        graphics.drawString(this.font, "Connections: " + connections.size(),
            panelLeft + GuiConstants.PADDING + 200, listY, GuiConstants.TEXT_DIM);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void addTrapPlacement() {
        placedTraps.add("trap_" + placedTraps.size());
    }

    private void addPuzzlePlacement() {
        placedPuzzles.add("puzzle_" + placedPuzzles.size());
    }

    private void addConnection() {
        connections.add("connection_" + connections.size());
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
