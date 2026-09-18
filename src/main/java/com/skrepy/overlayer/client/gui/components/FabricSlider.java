package com.skrepy.overlayer.client.gui.components;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Fabric 环境下的 ExtendedSlider 替代实现。
 * 父类 AbstractSliderButton 存储的是 0~1 的进度值，
 * 本类通过 minValue/maxValue 换算为实际数值。
 */
public abstract class FabricSlider extends AbstractSliderButton {
    private final Component prefix;
    private final Component suffix;
    private final double minValue;
    private final double maxValue;
    private final double stepSize;
    private final int precision;
    private final boolean drawString;
    private final java.text.DecimalFormat df;

    public FabricSlider(int x, int y, int width, int height, Component prefix, Component suffix, double minValue, double maxValue, double currentValue, double stepSize, int precision, boolean drawString) {
        super(x, y, width, height, Component.empty(), toProgress(currentValue, minValue, maxValue));
        this.prefix = prefix;
        this.suffix = suffix;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.stepSize = stepSize;
        this.precision = precision;
        this.drawString = drawString;
        this.df = new java.text.DecimalFormat("0." + "0".repeat(Math.max(0, precision)));
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
     * 当前实际数值
     */
    public double getValue() {
        return fromProgress(this.value, minValue, maxValue);
    }

    /**
     * 设置实际数值。
     * 注意：不命名为 setValue，避免重写父类的 protected setValue(double)。
     */
    public void setActualValue(double actualValue) {
        // 调用父类实现：内部会 clamp 到 [0,1]，变化时触发 applyValue() + updateMessage()
        super.setValue(toProgress(actualValue, minValue, maxValue));
    }

    @Override
    protected void updateMessage() {
        if (!drawString) {
            setMessage(Component.empty());
            return;
        }
        double v = getValue();
        String num = precision > 0 ? df.format(v) : String.valueOf((int) v);
        setMessage(Component.empty().append(prefix).append(num).append(suffix));
    }

    @Override
    protected void applyValue() {
        // 步长对齐：直接修改受保护字段，避免递归触发 applyValue
        if (stepSize > 0) {
            double v = getValue();
            double snapped = Math.round(v / stepSize) * stepSize;
            if (Math.abs(v - snapped) > 1e-9) {
                double progress = toProgress(snapped, minValue, maxValue);
                this.value = Mth.clamp(progress, 0.0, 1.0);
            }
        }
        onValueChanged(getValue());
    }

    /**
     * 子类重写此方法处理值变化
     */
    protected abstract void onValueChanged(double value);
}
