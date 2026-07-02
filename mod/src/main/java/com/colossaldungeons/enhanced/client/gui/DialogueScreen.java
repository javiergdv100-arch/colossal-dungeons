package com.colossaldungeons.enhanced.client.gui;

import com.colossaldungeons.enhanced.network.CDENetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Client-side dialogue screen for NPC conversations.
 * Displays NPC name, dialogue text, and player choices in a clean, minimalist,
 * semi-transparent overlay that matches the CDE visual style.
 *
 * Layout:
 * - Bottom-aligned dialogue panel (roughly bottom third of screen)
 * - NPC name rendered in gold above the dialogue text
 * - Dialogue text in white below the name
 * - Choice buttons listed vertically below the text
 */
@OnlyIn(Dist.CLIENT)
public class DialogueScreen extends Screen {

    /** The entity ID of the NPC being talked to */
    private final int npcEntityId;

    /** The current dialogue node ID */
    private final String dialogueNodeId;

    /** NPC speaker name */
    private final String speakerName;

    /** Dialogue text body */
    private final String dialogueText;

    /** Available player choices (text only - server validates conditions) */
    private final List<String> choices;

    // Layout dimensions (computed in init)
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;

    /** Spacing between choice buttons */
    private static final int CHOICE_SPACING = 4;

    /** Horizontal padding inside the dialogue panel */
    private static final int INNER_PADDING = 16;

    /** Vertical padding inside the dialogue panel */
    private static final int INNER_PADDING_V = 12;

    /** Height of each choice button */
    private static final int CHOICE_HEIGHT = 20;

    /** Panel background color - very dark, semi-transparent */
    private static final int PANEL_BG = 0xD90A0B0E;

    /** Separator line color */
    private static final int SEPARATOR_COLOR = 0xFF2A2E38;

    public DialogueScreen(int npcEntityId, String dialogueNodeId, String speakerName,
                          String dialogueText, List<String> choices) {
        super(Component.literal(speakerName));
        this.npcEntityId = npcEntityId;
        this.dialogueNodeId = dialogueNodeId;
        this.speakerName = speakerName;
        this.dialogueText = dialogueText;
        this.choices = choices;
    }

    @Override
    protected void init() {
        super.init();

        // Panel occupies the bottom portion of the screen, centered horizontally
        panelWidth = Math.min(this.width - 40, 420);
        panelX = (this.width - panelWidth) / 2;

        // Calculate panel height based on content
        int textLines = this.font.split(Component.literal(dialogueText), panelWidth - INNER_PADDING * 2).size();
        int textHeight = textLines * (this.font.lineHeight + 1);
        int choicesHeight = choices.size() * (CHOICE_HEIGHT + CHOICE_SPACING);

        // Name (12) + spacing (6) + text + spacing (10) + separator (6) + choices + padding
        panelHeight = INNER_PADDING_V + 12 + 6 + textHeight + 10 + 1 + 6 + choicesHeight + INNER_PADDING_V;

        // Position panel near the bottom with some margin
        panelY = this.height - panelHeight - 30;

        // Create choice buttons
        int choiceY = panelY + panelHeight - INNER_PADDING_V - choicesHeight;
        for (int i = 0; i < choices.size(); i++) {
            final int choiceIndex = i;
            String choiceText = choices.get(i);

            CDEButton choiceButton = CDEButton.create(
                panelX + INNER_PADDING,
                choiceY,
                panelWidth - INNER_PADDING * 2,
                CHOICE_HEIGHT,
                Component.literal(choiceText),
                button -> onChoiceSelected(choiceIndex)
            );
            addRenderableWidget(choiceButton);
            choiceY += CHOICE_HEIGHT + CHOICE_SPACING;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Dim background slightly (less than full overlay to keep game visible)
        graphics.fill(0, 0, this.width, this.height, 0x40000000);

        // Draw main dialogue panel background
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, PANEL_BG);

        // Draw panel border (subtle gold)
        drawPanelBorder(graphics, panelX, panelY, panelWidth, panelHeight, GuiConstants.BORDER_COLOR);

        // Draw gold accent line at top of panel
        graphics.fill(panelX + 1, panelY, panelX + panelWidth - 1, panelY + 2, GuiConstants.ACCENT_COLOR);

        // Draw NPC name in gold
        int textX = panelX + INNER_PADDING;
        int textY = panelY + INNER_PADDING_V;
        graphics.drawString(this.font, Component.literal(speakerName), textX, textY, GuiConstants.ACCENT_COLOR);
        textY += 12 + 6;

        // Draw dialogue text (word-wrapped)
        var lines = this.font.split(Component.literal(dialogueText), panelWidth - INNER_PADDING * 2);
        for (var line : lines) {
            graphics.drawString(this.font, line, textX, textY, GuiConstants.TEXT_COLOR);
            textY += this.font.lineHeight + 1;
        }

        // Draw separator line before choices
        textY += 10;
        graphics.fill(panelX + INNER_PADDING, textY, panelX + panelWidth - INNER_PADDING, textY + 1, SEPARATOR_COLOR);

        // Render widgets (choice buttons)
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /**
     * Handles when the player selects a dialogue choice.
     * Sends a C2S packet to the server and closes the screen.
     */
    private void onChoiceSelected(int choiceIndex) {
        // Send choice to server
        PacketDistributor.sendToServer(
            new CDENetworking.DialogueChoicePayload(npcEntityId, choiceIndex, dialogueNodeId)
        );

        // Close the screen - the server will send the next node if conversation continues
        onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Allow escape to close
        if (keyCode == 256) { // GLFW_KEY_ESCAPE
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * Draws a 1-pixel border around the panel.
     */
    private void drawPanelBorder(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }
}
