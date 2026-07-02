package com.colossaldungeons.enhanced.client.gui;

/**
 * Color scheme and styling constants for the CDE GUI system.
 * All screens use a clean, transparent, modern design language with gold accents.
 */
public final class GuiConstants {

    private GuiConstants() {
        // Constants only
    }

    // ========== Background Colors ==========
    /** Semi-transparent black for full-screen overlays */
    public static final int BACKGROUND_COLOR = 0x80000000;
    /** Slightly more opaque dark for panels */
    public static final int PANEL_COLOR = 0xB0111111;
    /** Darker panel variant for nested containers */
    public static final int PANEL_DARK_COLOR = 0xC00A0A0A;

    // ========== Accent Colors ==========
    /** Gold accent matching the dungeon theme */
    public static final int ACCENT_COLOR = 0xFFD4AF37;
    /** Lighter gold for hover states */
    public static final int ACCENT_HOVER = 0xFFE8C860;
    /** Dimmed gold for inactive accents */
    public static final int ACCENT_DIM = 0xFFA08020;

    // ========== Text Colors ==========
    /** Primary light text */
    public static final int TEXT_COLOR = 0xFFE8EAF0;
    /** Dimmed text for labels and descriptions */
    public static final int TEXT_DIM = 0xFFA2A9B8;
    /** Very dim text for placeholders */
    public static final int TEXT_PLACEHOLDER = 0xFF606878;

    // ========== UI Element Colors ==========
    /** Subtle border for panels and widgets */
    public static final int BORDER_COLOR = 0xFF2A3040;
    /** Active/focused border */
    public static final int BORDER_ACTIVE = 0xFFD4AF37;
    /** Success indicator */
    public static final int SUCCESS_COLOR = 0xFF7EC89F;
    /** Error indicator */
    public static final int ERROR_COLOR = 0xFFE87070;
    /** Warning indicator */
    public static final int WARNING_COLOR = 0xFFE8C860;

    // ========== Slider Colors ==========
    /** Slider track background */
    public static final int SLIDER_BG = 0xFF1A1E28;
    /** Slider fill (gold) */
    public static final int SLIDER_FILL = 0xFFD4AF37;
    /** Slider handle */
    public static final int SLIDER_HANDLE = 0xFFE8EAF0;

    // ========== Button Colors ==========
    /** Button background (semi-transparent) */
    public static final int BUTTON_BG = 0xA0181C25;
    /** Button hover state */
    public static final int BUTTON_HOVER = 0xC0242A36;
    /** Button active/pressed state */
    public static final int BUTTON_ACTIVE = 0xC0303845;
    /** Disabled button */
    public static final int BUTTON_DISABLED = 0x60101418;

    // ========== Tab Colors ==========
    /** Active tab background */
    public static final int TAB_ACTIVE = 0xC0242A36;
    /** Inactive tab background */
    public static final int TAB_INACTIVE = 0x80181C25;

    // ========== Layout Constants ==========
    /** Standard padding inside panels */
    public static final int PADDING = 8;
    /** Standard margin between elements */
    public static final int MARGIN = 12;
    /** Conceptual border radius (used for rendering calculations) */
    public static final int BORDER_RADIUS = 6;
    /** Standard widget height */
    public static final int WIDGET_HEIGHT = 20;
    /** Tab button height */
    public static final int TAB_HEIGHT = 24;
    /** Header height */
    public static final int HEADER_HEIGHT = 30;
    /** Left panel width ratio (0.0 to 1.0) */
    public static final float LEFT_PANEL_RATIO = 0.25f;
}
