package com.skrepy.overlayer.client.gui;

import com.skrepy.overlayer.Config;
import com.skrepy.overlayer.manager.OverlayerManager;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class ConfigScreen extends Screen {
    private static final Component[] LABELS = {Component.translatable("overlayer.configuration.titleScreenBtnXOffset"), Component.translatable("overlayer.configuration.titleScreenBtnYOffset"), Component.translatable("overlayer.configuration.optionsScreenBtnXOffset"), Component.translatable("overlayer.configuration.optionsScreenBtnYOffset")
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

    /**
     * 允许的字符：数字和可选的一个前导负号。
     * 因 EditBox.setValue 会触发 responder，所以设置前先断开，避免递归。
     */
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

        int centerX = this.width / 2;
        final int fontHeight = 9;  // 26.2 中 Font.lineHeight 固定为 9

        // ---- 标题 ----
        StringWidget titleWidget = new StringWidget(this.title, this.font);
        titleWidget.setX(centerX - this.font.width(this.title) / 2);
        titleWidget.setY(20);
        addRenderableWidget(titleWidget);

        int titleBottom = titleWidget.getY() + fontHeight + 20;

        int startY = titleBottom + 10;
        int fieldWidth = 120;
        int fieldHeight = 20;
        int spacing = 10;
        int labelFieldGap = 64;

        int maxLabelWidth = 0;
        for (Component labelText : LABELS) {
            int w = this.font.width(labelText);
            if (w > maxLabelWidth) maxLabelWidth = w;
        }

        int totalWidth = maxLabelWidth + labelFieldGap + fieldWidth;
        int startX = centerX - totalWidth / 2;

        for (int i = 0; i < 4; i++) {
            int y = startY + i * (fieldHeight + spacing + 12);

            StringWidget label = new StringWidget(LABELS[i], this.font);
            label.setX(startX);
            label.setY(y + (fieldHeight - fontHeight) / 2);
            addRenderableWidget(label);

            EditBox textField = new EditBox(
                    this.font, startX + maxLabelWidth + labelFieldGap, y, fieldWidth, fieldHeight, Component.literal("")
            );
            // EditBox 没有 setTextPredicate，用 setResponder 过滤
            textField.setResponder(s -> applyIntegerFilter(textField, s));
            textField.setMaxLength(11);
            textField.setValue(Integer.toString(getOptionsValueFromConfig(i)));
            inputFields[i] = textField;
            addRenderableWidget(textField);
        }

        int buttonY = startY + 5 * (fieldHeight + spacing + 12) + 20;
        Button doneButton = Button.builder(
                CommonComponents.GUI_DONE, (btn) -> this.onClose()
        ).pos(centerX - 130, buttonY).size(260, 20).build();
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
