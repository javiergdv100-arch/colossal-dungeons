package com.colossaldungeons.enhanced.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

/**
 * Trap configuration screen with transparent overlay style.
 * Left panel: scrollable list of trap types.
 * Right panel: configuration for the selected trap type including:
 * - Type dropdown
 * - Activator type dropdown
 * - Range slider
 * - Damage slider
 * - Timing sliders (warning/active/retract)
 * - Vanilla interactions checkboxes
 * - Repeatable toggle
 * - Cooldown slider
 */
@OnlyIn(Dist.CLIENT)
public class TrapConfigScreen extends Screen {

    private final Screen parent;

    // Selected trap type
    private int selectedTrapIndex = 0;

    // Configuration widgets
    private CDEDropdown typeDropdown;
    private CDEDropdown activatorDropdown;
    private CDESlider rangeSlider;
    private CDESlider damageSlider;
    private CDESlider warningSlider;
    private CDESlider activeSlider;
    private CDESlider retractSlider;
    private CDESlider cooldownSlider;
    private CDEButton repeatableToggle;
    private boolean repeatable = true;

    // Trap types available
    private static final List<String> TRAP_TYPES = List.of(
        "Spike Plate", "Flame Jet", "Poison Dart", "Falling Block",
        "Pressure Plate", "Trip Wire", "Arrow Trap", "Lava Pool"
    );

    private static final List<String> ACTIVATOR_TYPES = List.of(
        "Proximity", "Pressure", "Timed", "Triggered", "Manual"
    );

    // Layout
    private int listWidth;
    private int configX;
    private int configWidth;

    public TrapConfigScreen(Screen parent) {
        super(Component.translatable("gui.colossal_dungeons_enhanced.trap_config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        listWidth = (int) (this.width * 0.3f);
        configX = listWidth + GuiConstants.MARGIN;
        configWidth = this.width - listWidth - GuiConstants.MARGIN * 2;

        int headerOffset = GuiConstants.HEADER_HEIGHT + GuiConstants.MARGIN;

        // Trap type list buttons (left panel)
        for (int i = 0; i < TRAP_TYPES.size(); i++) {
            final int index = i;
            CDEButton trapButton = CDEButton.create(
                GuiConstants.PADDING,
                headerOffset + i * (GuiConstants.WIDGET_HEIGHT + 4),
                listWidth - GuiConstants.PADDING * 2,
                GuiConstants.WIDGET_HEIGHT,
                Component.literal(TRAP_TYPES.get(i)),
                button -> selectTrap(index)
            );
            addRenderableWidget(trapButton);
        }

        // Configuration panel widgets (right side)
        int y = headerOffset + 20;
        int widgetWidth = configWidth - GuiConstants.PADDING * 4;

        // Type dropdown
        typeDropdown = CDEDropdown.create(configX + GuiConstants.PADDING, y,
            widgetWidth, TRAP_TYPES, selectedTrapIndex, this::onTypeChanged);
        addRenderableWidget(typeDropdown);
        y += 30;

        // Activator type dropdown
        activatorDropdown = CDEDropdown.create(configX + GuiConstants.PADDING, y,
            widgetWidth, ACTIVATOR_TYPES, 0, idx -> { /* store */ });
        addRenderableWidget(activatorDropdown);
        y += 30;

        // Range slider
        rangeSlider = CDESlider.create(configX + GuiConstants.PADDING, y + 12,
            widgetWidth, 1, 16, 5, "Range: %.1f", val -> { /* store */ });
        addRenderableWidget(rangeSlider);
        y += 40;

        // Damage slider
        damageSlider = CDESlider.create(configX + GuiConstants.PADDING, y + 12,
            widgetWidth, 0, 20, 4, "Damage: %.1f", val -> { /* store */ });
        addRenderableWidget(damageSlider);
        y += 40;

        // Warning ticks slider
        warningSlider = CDESlider.create(configX + GuiConstants.PADDING, y + 12,
            widgetWidth, 0, 100, 20, "Warning: %.0f ticks", val -> { /* store */ });
        addRenderableWidget(warningSlider);
        y += 40;

        // Active ticks slider
        activeSlider = CDESlider.create(configX + GuiConstants.PADDING, y + 12,
            widgetWidth, 1, 100, 30, "Active: %.0f ticks", val -> { /* store */ });
        addRenderableWidget(activeSlider);
        y += 40;

        // Retract ticks slider
        retractSlider = CDESlider.create(configX + GuiConstants.PADDING, y + 12,
            widgetWidth, 0, 100, 20, "Retract: %.0f ticks", val -> { /* store */ });
        addRenderableWidget(retractSlider);
        y += 40;

        // Repeatable toggle
        repeatableToggle = CDEButton.create(
            configX + GuiConstants.PADDING, y,
            widgetWidth, GuiConstants.WIDGET_HEIGHT,
            Component.literal("Repeatable: " + (repeatable ? "ON" : "OFF")),
            button -> {
                repeatable = !repeatable;
                button.setMessage(Component.literal("Repeatable: " + (repeatable ? "ON" : "OFF")));
            }
        );
        addRenderableWidget(repeatableToggle);
        y += 30;

        // Cooldown slider
        cooldownSlider = CDESlider.create(configX + GuiConstants.PADDING, y + 12,
            widgetWidth, 0, 200, 60, "Cooldown: %.0f ticks", val -> { /* store */ });
        addRenderableWidget(cooldownSlider);
        y += 50;

        // Save and Cancel buttons
        int buttonWidth = (widgetWidth - GuiConstants.MARGIN) / 2;
        addRenderableWidget(CDEButton.create(
            configX + GuiConstants.PADDING, y,
            buttonWidth, GuiConstants.WIDGET_HEIGHT,
            Component.literal("Save"),
            button -> save()
        ));
        addRenderableWidget(CDEButton.create(
            configX + GuiConstants.PADDING + buttonWidth + GuiConstants.MARGIN, y,
            buttonWidth, GuiConstants.WIDGET_HEIGHT,
            Component.literal("Cancel"),
            button -> cancel()
        ));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Semi-transparent overlay
        graphics.fill(0, 0, this.width, this.height, GuiConstants.BACKGROUND_COLOR);

        // Header
        graphics.fill(0, 0, this.width, GuiConstants.HEADER_HEIGHT, GuiConstants.PANEL_COLOR);
        graphics.drawCenteredString(this.font,
            Component.literal("Trap Configuration"),
            this.width / 2, (GuiConstants.HEADER_HEIGHT - 8) / 2,
            GuiConstants.ACCENT_COLOR);

        // Left panel (trap list)
        int headerOffset = GuiConstants.HEADER_HEIGHT;
        graphics.fill(0, headerOffset, listWidth, this.height, GuiConstants.PANEL_COLOR);

        // Selected indicator
        int selectedY = headerOffset + GuiConstants.MARGIN + selectedTrapIndex * (GuiConstants.WIDGET_HEIGHT + 4);
        graphics.fill(0, selectedY, 3, selectedY + GuiConstants.WIDGET_HEIGHT, GuiConstants.ACCENT_COLOR);

        // Right panel (configuration)
        graphics.fill(configX - GuiConstants.PADDING, headerOffset,
            this.width - GuiConstants.PADDING, this.height - GuiConstants.PADDING,
            GuiConstants.PANEL_DARK_COLOR);

        // Section labels
        int y = headerOffset + GuiConstants.MARGIN;
        graphics.drawString(this.font, "Configuration", configX + GuiConstants.PADDING, y + 4, GuiConstants.TEXT_COLOR);

        // State machine preview (text-based)
        int previewY = this.height - 60;
        graphics.drawString(this.font, "State: INACTIVE > PREPARED > ARMED > TRIGGERED > COOLDOWN",
            configX + GuiConstants.PADDING, previewY, GuiConstants.TEXT_DIM);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void selectTrap(int index) {
        selectedTrapIndex = index;
        if (typeDropdown != null) {
            typeDropdown.setSelectedIndex(index);
        }
    }

    private void onTypeChanged(int index) {
        selectedTrapIndex = index;
    }

    private void save() {
        // Save configuration and return to parent
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }

    private void cancel() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }
}
