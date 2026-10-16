package com.skrepy.overlayer.fabric.client.gui;

import com.skrepy.overlayer.fabric.Config;
import com.skrepy.overlayer.manager.OverlayerManager;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class ConfigScreen extends Screen {
    private static final Component[] LABELS = {
            Component.translatable("overlayer.configuration.titleScreenBtnXOffset"),
            Component.translatable("overlayer.configuration.titleScreenBtnYOffset"),
            Component.translatable("overlayer.configuration.optionsScreenBtnXOffset"),
            Component.translatable("overlayer.configuration.optionsScreenBtnYOffset")
    };

    private final EditBox[] inputFields = new EditBox[4];
    private Screen parentScreen;

    public ConfigScreen() {
        super(Component.translatable("overlayer.configuration"));
    }

    public ConfigScreen(Screen parent) {
        this();
        this.parentScreen = parent;
    }

    private static void applyIntegerFilter(EditBox box, String s) {
        if (s.matches("-?\\d*")) return;
        String filtered = s.replaceAll("[^0-9-]", "");
        box.setResponder(null);
        box.setValue(filtered);
        box.setResponder(v -> applyIntegerFilter(box, v));
    }

    private int getOptionsValueFromConfig(int index) {
        return switch (index) {
            case 0 -> Config.getTitleScreenBtnXOffset();
            case 1 -> Config.getTitleScreenBtnYOffset();
            case 2 -> Config.getOptionsScreenBtnXOffset();
            case 3 -> Config.getOptionsScreenBtnYOffset();
            default -> 0;
        };
    }

    @Override
    protected void init() {
        super.init();

        final int centerX = this.width / 2;
        final int lineHeight = this.font.lineHeight;

        // ---- 标题 ----
        StringWidget titleWidget = new StringWidget(this.title, this.font);
        titleWidget.setX(centerX - this.font.width(this.title) / 2);
        titleWidget.setY((int) (this.height * 0.10));
        addRenderableWidget(titleWidget);

        int titleBottom = titleWidget.getY() + lineHeight + 10;

        // ---- 相对尺寸计算 ----
        // 字段宽度：屏幕宽度的 18%，限制在 [80, 220] 之间
        int fieldWidth = Math.clamp((int) (this.width * 0.18), 80, 220);
        int fieldHeight = Math.clamp(lineHeight + 10, 16, 24);
        int spacing = Math.max(4, lineHeight / 2);
        int rowGap = fieldHeight + spacing + 6;

        // 标签与字段之间的间距，按屏幕比例
        int labelFieldGap = Math.clamp((int) (this.width * 0.04), 16, 64);

        // 最大标签宽度
        int maxLabelWidth = 0;
        for (Component labelText : LABELS) {
            int w = this.font.width(labelText);
            if (w > maxLabelWidth) maxLabelWidth = w;
        }

        int totalWidth = maxLabelWidth + labelFieldGap + fieldWidth;
        int startX = centerX - totalWidth / 2;

        // 起始 Y 位置：标题下方 20px 或屏幕高度的 15%，取较大值
        int startY = Math.max(titleBottom + 20, (int) (this.height * 0.15));

        // 检查所有行加上底部按钮是否会超出屏幕，如果超出则缩小间距
        int bottomButtonHeight = fieldHeight;
        int requiredHeight = 4 * rowGap + 40 + bottomButtonHeight;
        if (startY + requiredHeight > this.height - 10) {
            // 缩小间距
            int availableForRows = this.height - startY - 40 - bottomButtonHeight - 10;
            rowGap = Math.max(fieldHeight + 2, availableForRows / 4);
        }

        for (int i = 0; i < 4; i++) {
            int y = startY + i * rowGap;

            StringWidget label = new StringWidget(LABELS[i], this.font);
            label.setX(startX);
            label.setY(y + (fieldHeight - lineHeight) / 2);
            addRenderableWidget(label);

            EditBox textField = new EditBox(
                    this.font,
                    startX + maxLabelWidth + labelFieldGap,
                    y,
                    fieldWidth,
                    fieldHeight,
                    Component.literal("")
            );
            textField.setResponder(s -> applyIntegerFilter(textField, s));
            textField.setMaxLength(11);
            textField.setValue(Integer.toString(getOptionsValueFromConfig(i)));
            inputFields[i] = textField;
            addRenderableWidget(textField);
        }

        // ---- 底部按钮 ----
        // 按钮宽度：屏幕宽度的 35%，限制在 [180, 400] 之间
        int buttonWidth = Math.clamp((int) (this.width * 0.35), 180, 400);
        // 按钮位置：表单底部下方 20px，或屏幕底部往上 60px，取较小值
        int buttonY = Math.min(
                startY + 4 * rowGap + 20,
                this.height - 60
        );
        Button doneButton = Button.builder(
                CommonComponents.GUI_DONE,
                (btn) -> this.onClose()
        ).pos(centerX - buttonWidth / 2, buttonY).size(buttonWidth, fieldHeight).build();
        addRenderableWidget(doneButton);
    }

    @Override
    public void onClose() {
        Config.setTitleScreenBtnXOffset(getValue(0));
        Config.setTitleScreenBtnYOffset(getValue(1));
        Config.setOptionsScreenBtnXOffset(getValue(2));
        Config.setOptionsScreenBtnYOffset(getValue(3));
        OverlayerManager.getInstance().save();
        this.minecraft.gui.setScreen(parentScreen);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    public int getValue(int index) {
        if (index < 0 || index >= inputFields.length) return 0;
        try {
            return Integer.parseInt(inputFields[index].getValue());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public void setValue(int index, int value) {
        if (index >= 0 && index < inputFields.length) {
            inputFields[index].setValue(Integer.toString(value));
        }
    }
}