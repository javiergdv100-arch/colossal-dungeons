package com.colossaldungeons.enhanced.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.function.Consumer;

/**
 * Custom slider widget with transparent track, gold fill, and value label.
 * Supports min/max/step configuration with a callback on value change.
 */
@OnlyIn(Dist.CLIENT)
public class CDESlider extends AbstractSliderButton {

    private final double minValue;
    private final double maxValue;
    private final double step;
    private final String labelFormat;
    private final Consumer<Double> onValueChanged;

    /**
     * Creates a new CDE slider.
     *
     * @param x left position
     * @param y top position
     * @param width widget width
     * @param height widget height
     * @param label the label component
     * @param initialValue normalized value (0.0 to 1.0)
     * @param minValue the minimum real value
     * @param maxValue the maximum real value
     * @param step the step size (0 for continuous)
     * @param labelFormat printf format for the value label (e.g. "%.0f")
     * @param onValueChanged callback when value changes
     */
    public CDESlider(int x, int y, int width, int height, Component label,
                     double initialValue, double minValue, double maxValue,
                     double step, String labelFormat, Consumer<Double> onValueChanged) {
        super(x, y, width, height, label, normalizeValue(initialValue, minValue, maxValue));
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.step = step;
        this.labelFormat = labelFormat;
        this.onValueChanged = onValueChanged;
        updateMessage();
    }

    private static double normalizeValue(double value, double min, double max) {
        if (max <= min) return 0.0;
        return (value - min) / (max - min);
    }

    /**
     * Gets the actual (denormalized) value of the slider.
     */
    public double getRealValue() {
        double realValue = minValue + (value * (maxValue - minValue));
        if (step > 0) {
            realValue = Math.round(realValue / step) * step;
        }
        return realValue;
    }

    @Override
    protected void updateMessage() {
        double realValue = getRealValue();
        String formatted = String.format(labelFormat, realValue);
        setMessage(Component.literal(formatted));
    }

    @Override
    protected void applyValue() {
        if (step > 0) {
            // Snap to step
            double realValue = minValue + (value * (maxValue - minValue));
            realValue = Math.round(realValue / step) * step;
            value = normalizeValue(realValue, minValue, maxValue);
        }
        if (onValueChanged != null) {
            onValueChanged.accept(getRealValue());
        }
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Draw track background
        graphics.fill(getX(), getY() + getHeight() / 2 - 2,
            getX() + getWidth(), getY() + getHeight() / 2 + 2,
            GuiConstants.SLIDER_BG);

        // Draw filled portion
        int fillWidth = (int) (getWidth() * value);
        graphics.fill(getX(), getY() + getHeight() / 2 - 2,
            getX() + fillWidth, getY() + getHeight() / 2 + 2,
            GuiConstants.SLIDER_FILL);

        // Draw handle
        int handleX = getX() + fillWidth - 3;
        int handleY = getY() + getHeight() / 2 - 5;
        graphics.fill(handleX, handleY, handleX + 6, handleY + 10,
            isHoveredOrFocused() ? GuiConstants.ACCENT_HOVER : GuiConstants.SLIDER_HANDLE);

        // Draw border around track
        graphics.fill(getX(), getY() + getHeight() / 2 - 3,
            getX() + getWidth(), getY() + getHeight() / 2 - 2,
            GuiConstants.BORDER_COLOR);
        graphics.fill(getX(), getY() + getHeight() / 2 + 2,
            getX() + getWidth(), getY() + getHeight() / 2 + 3,
            GuiConstants.BORDER_COLOR);

        // Draw value label above
        graphics.drawCenteredString(
            net.minecraft.client.Minecraft.getInstance().font,
            getMessage(),
            getX() + getWidth() / 2,
            getY() - 2,
            GuiConstants.TEXT_COLOR
        );
    }

    /**
     * Factory method for creating a slider with common defaults.
     */
    public static CDESlider create(int x, int y, int width, double min, double max,
                                   double initial, String format, Consumer<Double> callback) {
        return new CDESlider(x, y, width, GuiConstants.WIDGET_HEIGHT,
            Component.empty(), initial, min, max, 0, format, callback);
    }
}
