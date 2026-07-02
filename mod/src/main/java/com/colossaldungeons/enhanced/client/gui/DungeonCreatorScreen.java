package com.colossaldungeons.enhanced.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Main dungeon creator/configuration screen.
 * Full-screen semi-transparent dark overlay with tabbed navigation for configuring
 * dungeon settings, rooms, traps, puzzles, and mechanisms.
 * 
 * Design: Clean transparent style with gold accents.
 * Left panel: tab navigation buttons
 * Right panel: content area for the selected tab
 */
@OnlyIn(Dist.CLIENT)
public class DungeonCreatorScreen extends Screen {

    /** Currently active tab index */
    private int activeTab = 0;

    /** Tab names */
    private static final String[] TABS = {
        "Dungeon Settings",
        "Rooms",
        "Traps",
        "Puzzles",
        "Mechanisms"
    };

    // Layout dimensions (calculated in init)
    private int leftPanelWidth;
    private int contentX;
    private int contentWidth;
    private int panelY;
    private int panelHeight;

    // Dungeon settings widgets
    private CDETextField nameField;
    private CDESlider maxPlayersSlider;
    private CDESlider timeLimitSlider;
    private CDEButton respawnToggle;
    private boolean respawnEnabled = true;

    public DungeonCreatorScreen() {
        super(Component.translatable("gui.colossal_dungeons_enhanced.dungeon_creator"));
    }

    @Override
    protected void init() {
        super.init();

        // Calculate layout
        leftPanelWidth = (int) (this.width * GuiConstants.LEFT_PANEL_RATIO);
        contentX = leftPanelWidth + GuiConstants.MARGIN;
        contentWidth = this.width - leftPanelWidth - GuiConstants.MARGIN * 2;
        panelY = GuiConstants.HEADER_HEIGHT + GuiConstants.MARGIN;
        panelHeight = this.height - panelY - GuiConstants.MARGIN;

        // Create tab buttons
        for (int i = 0; i < TABS.length; i++) {
            final int tabIndex = i;
            CDEButton tabButton = CDEButton.create(
                GuiConstants.PADDING,
                panelY + i * (GuiConstants.TAB_HEIGHT + 4),
                leftPanelWidth - GuiConstants.PADDING * 2,
                GuiConstants.TAB_HEIGHT,
                Component.literal(TABS[i]),
                button -> switchTab(tabIndex)
            );
            addRenderableWidget(tabButton);
        }

        // Initialize content for active tab
        initTabContent();
    }

    /**
     * Initializes widgets for the currently active tab.
     */
    private void initTabContent() {
        // Clear previous tab content (non-tab buttons)
        // We re-init when switching tabs
        switch (activeTab) {
            case 0 -> initDungeonSettingsTab();
            case 1 -> initRoomsTab();
            case 2 -> initTrapsTab();
            case 3 -> initPuzzlesTab();
            case 4 -> initMechanismsTab();
        }
    }

    private void initDungeonSettingsTab() {
        int y = panelY + GuiConstants.MARGIN + 20;

        // Dungeon name field
        nameField = CDETextField.create(
            contentX + GuiConstants.PADDING, y,
            contentWidth - GuiConstants.PADDING * 2,
            "Dungeon Name", "Enter dungeon name..."
        );
        addRenderableWidget(nameField);
        y += 40;

        // Max players slider
        maxPlayersSlider = CDESlider.create(
            contentX + GuiConstants.PADDING, y + 12,
            contentWidth - GuiConstants.PADDING * 2,
            1, 8, 4, "%.0f Players",
            val -> { /* store value */ }
        );
        addRenderableWidget(maxPlayersSlider);
        y += 50;

        // Respawn toggle
        respawnToggle = CDEButton.create(
            contentX + GuiConstants.PADDING, y,
            contentWidth - GuiConstants.PADDING * 2,
            GuiConstants.WIDGET_HEIGHT,
            Component.literal("Respawn: " + (respawnEnabled ? "ON" : "OFF")),
            button -> {
                respawnEnabled = !respawnEnabled;
                button.setMessage(Component.literal("Respawn: " + (respawnEnabled ? "ON" : "OFF")));
            }
        );
        addRenderableWidget(respawnToggle);
        y += 30;

        // Time limit slider
        timeLimitSlider = CDESlider.create(
            contentX + GuiConstants.PADDING, y + 12,
            contentWidth - GuiConstants.PADDING * 2,
            0, 60, 30, "%.0f min",
            val -> { /* store value */ }
        );
        addRenderableWidget(timeLimitSlider);
    }

    private void initRoomsTab() {
        int y = panelY + GuiConstants.MARGIN + 20;
        addRenderableWidget(CDEButton.create(
            contentX + GuiConstants.PADDING, y,
            160, GuiConstants.WIDGET_HEIGHT,
            Component.literal("+ Add Room"),
            button -> openRoomEditor()
        ));
    }

    private void initTrapsTab() {
        int y = panelY + GuiConstants.MARGIN + 20;
        addRenderableWidget(CDEButton.create(
            contentX + GuiConstants.PADDING, y,
            160, GuiConstants.WIDGET_HEIGHT,
            Component.literal("Configure Traps"),
            button -> openTrapConfig()
        ));
    }

    private void initPuzzlesTab() {
        int y = panelY + GuiConstants.MARGIN + 20;
        addRenderableWidget(CDEButton.create(
            contentX + GuiConstants.PADDING, y,
            160, GuiConstants.WIDGET_HEIGHT,
            Component.literal("Configure Puzzles"),
            button -> openPuzzleConfig()
        ));
    }

    private void initMechanismsTab() {
        int y = panelY + GuiConstants.MARGIN + 20;
        addRenderableWidget(CDEButton.create(
            contentX + GuiConstants.PADDING, y,
            160, GuiConstants.WIDGET_HEIGHT,
            Component.literal("+ Add Mechanism"),
            button -> { /* open mechanism config */ }
        ));
    }

    /**
     * Switches to the given tab index and re-initializes content.
     */
    private void switchTab(int tabIndex) {
        if (tabIndex == activeTab) return;
        activeTab = tabIndex;
        clearWidgets();
        init();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Draw semi-transparent background overlay
        graphics.fill(0, 0, this.width, this.height, GuiConstants.BACKGROUND_COLOR);

        // Draw header
        graphics.fill(0, 0, this.width, GuiConstants.HEADER_HEIGHT, GuiConstants.PANEL_COLOR);
        graphics.drawCenteredString(this.font,
            Component.literal("Colossal Dungeons - Creator"),
            this.width / 2, (GuiConstants.HEADER_HEIGHT - 8) / 2,
            GuiConstants.ACCENT_COLOR);

        // Draw left panel background
        graphics.fill(0, panelY, leftPanelWidth, this.height,
            GuiConstants.PANEL_COLOR);

        // Draw active tab indicator
        int tabY = panelY + activeTab * (GuiConstants.TAB_HEIGHT + 4);
        graphics.fill(0, tabY, 3,
            tabY + GuiConstants.TAB_HEIGHT, GuiConstants.ACCENT_COLOR);

        // Draw content panel background
        graphics.fill(contentX - GuiConstants.PADDING, panelY,
            this.width - GuiConstants.PADDING, this.height - GuiConstants.PADDING,
            GuiConstants.PANEL_DARK_COLOR);

        // Draw content panel border
        drawBorder(graphics, contentX - GuiConstants.PADDING, panelY,
            this.width - GuiConstants.PADDING - contentX + GuiConstants.PADDING,
            this.height - GuiConstants.PADDING - panelY,
            GuiConstants.BORDER_COLOR);

        // Draw tab title in content area
        graphics.drawString(this.font, TABS[activeTab],
            contentX + GuiConstants.PADDING, panelY + GuiConstants.PADDING,
            GuiConstants.TEXT_COLOR);

        // Render widgets
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void openRoomEditor() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new RoomEditorScreen(this));
        }
    }

    private void openTrapConfig() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new TrapConfigScreen(this));
        }
    }

    private void openPuzzleConfig() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new PuzzleConfigScreen(this));
        }
    }

    private void drawBorder(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }
}
