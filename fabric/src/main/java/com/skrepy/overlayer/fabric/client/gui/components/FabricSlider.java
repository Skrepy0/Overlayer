package com.skrepy.overlayer.fabric.client.gui.components;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

import java.text.DecimalFormat;

/**
 * Fabric 环境下的 ExtendedSlider 替代实现。
 * 父类 AbstractSliderButton 存储的是 0~1 的进度值，
 * 本类通过 minValue/maxValue 换算为实际数值。
 * 拖动逻辑说明：
 * - this.value (父类) 保存 0~1 的连续进度，由鼠标拖动更新；
 * - getValue() 返回换算后的连续实际值；
 * - getSnappedValue() 返回按 stepSize 对齐后的值，仅用于显示和回调；
 * - 绝不在 applyValue() 里修改 this.value，否则会破坏拖动连续性。
 */
public abstract class FabricSlider extends AbstractSliderButton {
    private final Component prefix;
    private final Component suffix;
    private final double minValue;
    private final double maxValue;
    private final double stepSize;
    private final int precision;
    private final boolean drawString;
    private final DecimalFormat df;

    public FabricSlider(int x, int y, int width, int height,
                        Component prefix, Component suffix,
                        double minValue, double maxValue, double currentValue,
                        double stepSize, int precision, boolean drawString) {
        super(x, y, width, height, Component.empty(), toProgress(currentValue, minValue, maxValue));
        this.prefix = prefix;
        this.suffix = suffix;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.stepSize = stepSize;
        this.precision = precision;
        this.drawString = drawString;
        this.df = new DecimalFormat("0." + "0".repeat(Math.max(0, precision)));
        updateMessage();
    }

    private static double toProgress(double value, double min, double max) {
        if (max == min) return 0.0;
        return (value - min) / (max - min);
    }

    private static double fromProgress(double progress, double min, double max) {
        return min + progress * (max - min);
    }

    /**
     * 当前实际数值（连续，随拖动变化）
     */
    public double getValue() {
        return fromProgress(this.value, minValue, maxValue);
    }

    /**
     * 按步长对齐后的值，仅用于显示和回调，不影响拖动连续性
     */
    public double getSnappedValue() {
        double v = getValue();
        if (stepSize > 0) {
            return Math.round(v / stepSize) * stepSize;
        }
        return v;
    }

    /**
     * 设置实际数值。
     * 注意：不命名为 setValue，避免重写父类的 protected setValue(double)。
     */
    public void setActualValue(double actualValue) {
        super.setValue(toProgress(actualValue, minValue, maxValue));
        // 显式刷新一次显示，防止值未变化时父类跳过 updateMessage()
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        if (!drawString) {
            setMessage(Component.empty());
            return;
        }
        double v = getSnappedValue();
        String num = precision > 0 ? df.format(v) : String.valueOf((int) v);
        setMessage(Component.empty().append(prefix).append(num).append(suffix));
    }

    @Override
    protected void applyValue() {
        // ✅ 只把对齐后的值传给回调，不修改 this.value，保证拖动连续
        onValueChanged(getSnappedValue());
    }

    /**
     * 子类重写此方法处理值变化
     */
    protected abstract void onValueChanged(double value);
}